from django.shortcuts import get_object_or_404
from drf_spectacular.utils import OpenApiParameter, extend_schema
from rest_framework.response import Response
from rest_framework.views import APIView

from core.openapi import NOT_FOUND_404

from .legal_schemas import (
    LegalDocumentListSerializer,
    LegalDocumentSerializer,
)
from .models import LegalDocument
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
        published = LegalDocument.objects.filter(active=True).order_by("key")
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
        document = get_object_or_404(LegalDocument, key=key.upper(), active=True)
        return Response(legal_document(document))
