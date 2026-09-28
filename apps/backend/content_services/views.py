import uuid

from django.db.models import Q
from django.shortcuts import get_object_or_404
from drf_spectacular.utils import OpenApiParameter, extend_schema
from rest_framework.exceptions import NotFound, ValidationError
from rest_framework.permissions import AllowAny
from rest_framework.request import Request
from rest_framework.response import Response
from rest_framework.views import APIView

from accounts.phone import normalize_syrian_phone
from core.openapi import NOT_FOUND_404, THROTTLED_429, VALIDATION_400
from core.throttles import ContactThrottle

from .content_schemas import (
    ContactMessageCreatedSerializer,
    ContactMessageRequestSerializer,
    ContentPageSerializer,
    EmergencyNumberListSerializer,
    FaqListSerializer,
)
from .legal_schemas import (
    LegalDocumentListSerializer,
    LegalDocumentSerializer,
)
from .models import ContactMessage, EmergencyNumber, FaqEntry, LegalDocument
from .pages import BUILT_IN_KEYS, key_for, public_page_payload
from .schemas import PublicAdvertisementListSerializer
from .selectors import active_ads
from .serializers import legal_document, legal_summary, public_ad


class PublicAdsView(APIView):
    authentication_classes = []
    permission_classes = []

    @extend_schema(
        operation_id="publicAdsList",
        tags=["Ads"],
        summary="List advertisements currently in flight",
        description=(
            "First-party advertisements only, filtered server-side by schedule, enabled "
            "state and targeting scope. Capped at 50 items."
        ),
        parameters=[
            OpenApiParameter(
                "provinceId",
                str,
                OpenApiParameter.QUERY,
                required=False,
                description="Restrict to advertisements targeting this province.",
            ),
            OpenApiParameter(
                "categoryId",
                str,
                OpenApiParameter.QUERY,
                required=False,
                description="Restrict to advertisements targeting this category.",
            ),
        ],
        responses={200: PublicAdvertisementListSerializer},
    )
    def get(self, request):
        rows = active_ads(
            province_id=request.query_params.get("provinceId"),
            category_id=request.query_params.get("categoryId"),
        )[:50]
        return Response({"items": [public_ad(row) for row in rows]})


class PublicLegalDocumentsView(APIView):
    """What the platform has published: its pages, their versions, and when each was published."""

    authentication_classes = []
    permission_classes = []

    @extend_schema(
        operation_id="publicLegalDocumentsList",
        tags=["Content"],
        summary="List the published pages",
        description=(
            "Titles and versions only. A client compares the version it cached with the one "
            "here and fetches a page's words only when they have changed."
        ),
        responses={200: LegalDocumentListSerializer},
    )
    def get(self, request):
        # Only the built-in pages: their keys are the enum this contract declares. Pages
        # operators add later are served by `/content/pages/<slug>/`.
        published = LegalDocument.objects.filter(active=True, key__in=BUILT_IN_KEYS).order_by(
            "key"
        )
        return Response({"items": [legal_summary(row) for row in published]})


class PublicLegalDocumentView(APIView):
    """One published page, in full."""

    authentication_classes = []
    permission_classes = []

    @extend_schema(
        operation_id="publicLegalDocumentRetrieve",
        tags=["Content"],
        summary="Retrieve one published page",
        responses={200: LegalDocumentSerializer, 404: NOT_FOUND_404},
    )
    def get(self, request, key):
        if key.upper() not in BUILT_IN_KEYS:
            raise NotFound()
        document = get_object_or_404(LegalDocument, key=key.upper(), active=True)
        return Response(legal_document(document))


PUBLIC_CACHE = "public, max-age=300"


class PublicContentPageView(APIView):
    """One published content page by slug: legal, FAQ-style or a plain page."""

    authentication_classes = []
    permission_classes = []

    @extend_schema(
        operation_id="publicContentPageRetrieve",
        tags=["Content"],
        summary="Retrieve one published content page",
        description=(
            "Only the published version is served; an unpublished or unknown slug is 404. "
            "Cacheable for five minutes (`Cache-Control: public, max-age=300`)."
        ),
        responses={200: ContentPageSerializer, 404: NOT_FOUND_404},
    )
    def get(self, request: Request, slug: str) -> Response:
        document = get_object_or_404(LegalDocument, key=key_for(slug), active=True)
        response = Response(public_page_payload(document))
        response["Cache-Control"] = PUBLIC_CACHE
        return response


class PublicFaqView(APIView):
    authentication_classes = []
    permission_classes = []

    @extend_schema(
        operation_id="publicFaqList",
        tags=["Content"],
        summary="List the published questions and answers, in order",
        description="Cacheable for five minutes (`Cache-Control: public, max-age=300`).",
        responses={200: FaqListSerializer},
    )
    def get(self, request: Request) -> Response:
        entries = FaqEntry.objects.filter(published=True).order_by("sort_order", "created_at")
        response = Response(
            {
                "items": [
                    {
                        "id": str(entry.pk),
                        "questionAr": entry.question_ar,
                        "answerAr": entry.answer_ar,
                        "sortOrder": entry.sort_order,
                    }
                    for entry in entries
                ]
            }
        )
        response["Cache-Control"] = PUBLIC_CACHE
        return response


def emergency_number_payload(number: EmergencyNumber) -> dict[str, object]:
    return {
        "id": str(number.pk),
        "scope": "PROVINCE" if number.province_id else "NATIONAL",
        "provinceId": str(number.province_id) if number.province_id else None,
        "labelAr": number.label_ar,
        "phone": number.phone,
        "kind": number.kind,
        "sortOrder": number.sort_order,
    }


class PublicEmergencyNumbersView(APIView):
    authentication_classes = []
    permission_classes = []

    @extend_schema(
        operation_id="publicEmergencyNumbersList",
        tags=["Content"],
        summary="Emergency numbers: national, plus the province's own",
        description=(
            "National numbers come first, then those of `provinceId` when one is given. "
            "Cacheable for five minutes."
        ),
        parameters=[
            OpenApiParameter(
                "provinceId",
                str,
                OpenApiParameter.QUERY,
                required=False,
                description="Also include this province's numbers.",
            )
        ],
        responses={200: EmergencyNumberListSerializer, 400: VALIDATION_400},
    )
    def get(self, request: Request) -> Response:
        scope = Q(province__isnull=True)
        if value := request.query_params.get("provinceId"):
            try:
                scope |= Q(province_id=uuid.UUID(value))
            except ValueError as exc:
                raise ValidationError({"provinceId": ["Must be a valid UUID."]}) from exc
        numbers = EmergencyNumber.objects.filter(scope, active=True).order_by(
            "sort_order", "label_ar"
        )
        ordered = sorted(numbers, key=lambda row: row.province_id is not None)
        response = Response({"items": [emergency_number_payload(row) for row in ordered]})
        response["Cache-Control"] = PUBLIC_CACHE
        return response


class PublicContactView(APIView):
    """The public contact form. No IP address, device or location is stored with a message."""

    permission_classes = [AllowAny]
    throttle_classes = [ContactThrottle]

    @extend_schema(
        operation_id="publicContactCreate",
        tags=["Content"],
        summary="Send a message to the platform team",
        description=(
            "Anonymous or signed in; a signed-in sender is linked to their account. Strictly "
            "throttled per account or per client address (3/hour by default). The client "
            "address is taken from X-Forwarded-For only when the server is configured with "
            "the number of trusted proxies."
        ),
        request=ContactMessageRequestSerializer,
        responses={
            201: ContactMessageCreatedSerializer,
            400: VALIDATION_400,
            429: THROTTLED_429,
        },
    )
    def post(self, request: Request) -> Response:
        payload = ContactMessageRequestSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        data = payload.validated_data
        name = data["name"].strip()
        message_text = data["message"].strip()
        errors = {}
        if not name:
            errors["name"] = ["Required."]
        if not message_text:
            errors["message"] = ["Required."]
        phone = ""
        if data.get("phone", "").strip():
            try:
                phone = normalize_syrian_phone(data["phone"])
            except ValueError:
                errors["phone"] = ["Enter a valid Syrian mobile number."]
        if errors:
            raise ValidationError(errors)
        message = ContactMessage.objects.create(
            user_id=request.user.pk if request.user.is_authenticated else None,
            name=name,
            phone=phone,
            message=message_text,
            kind=data["kind"],
        )
        return Response(
            {"id": str(message.pk), "createdAt": message.created_at.isoformat()}, status=201
        )
