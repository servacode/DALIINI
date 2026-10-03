"""«هذه منشأتي»: finding a facility nobody owns and asking to own it (`claims`)."""

from __future__ import annotations

from typing import Any
from uuid import UUID

from django.core.exceptions import ValidationError as DjangoValidationError
from drf_spectacular.utils import OpenApiParameter, extend_schema
from rest_framework import serializers
from rest_framework.exceptions import NotFound, ValidationError
from rest_framework.parsers import FormParser, MultiPartParser
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from accounts.authentication import AuthenticatedRequest
from core.openapi import CONFLICT_409, DOMAIN_400, NOT_FOUND_404, VALIDATION_400, protected
from core.throttles import EvidenceUploadThrottle, SearchThrottle, UserDefaultThrottle

from . import claims
from .models import Facility, FacilityApplication, VerificationEvidence
from .serializers import EvidenceUploadSerializer
from .views import _request_id, _validation_error

RESULTS = 20


class ClaimableFacilitySerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    nameAr = serializers.CharField()
    categoryNameAr = serializers.CharField()
    provinceNameAr = serializers.CharField()
    cityNameAr = serializers.CharField(allow_null=True)
    addressAr = serializers.CharField(allow_null=True)


class ClaimableFacilityListSerializer(serializers.Serializer[Any]):
    items = ClaimableFacilitySerializer(many=True)


class ClaimFacilitySerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    nameAr = serializers.CharField()
    categoryNameAr = serializers.CharField()
    provinceNameAr = serializers.CharField()
    addressAr = serializers.CharField(allow_null=True)


class ClaimRequirementSerializer(serializers.Serializer[Any]):
    id = serializers.IntegerField()
    labelAr = serializers.CharField()
    required = serializers.BooleanField()  # type: ignore[assignment]
    minFiles = serializers.IntegerField()
    maxFiles = serializers.IntegerField()


class ClaimEvidenceSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    requirementId = serializers.IntegerField()
    createdAt = serializers.DateTimeField()


class ClaimSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    status = serializers.ChoiceField(choices=FacilityApplication.Status.choices)
    rejectionReason = serializers.CharField(allow_null=True)
    submittedAt = serializers.DateTimeField(allow_null=True)
    reviewedAt = serializers.DateTimeField(allow_null=True)
    facility = ClaimFacilitySerializer()
    requirements = ClaimRequirementSerializer(many=True)
    evidence = ClaimEvidenceSerializer(many=True)


class ClaimListSerializer(serializers.Serializer[Any]):
    items = ClaimSerializer(many=True)


class ClaimStartSerializer(serializers.Serializer[Any]):
    facilityId = serializers.UUIDField()


def _q(name: str, description: str) -> OpenApiParameter:
    return OpenApiParameter(
        name, str, OpenApiParameter.QUERY, required=False, description=description
    )


class ClaimableFacilitiesView(APIView):
    permission_classes = [IsAuthenticated]
    throttle_classes = [SearchThrottle]

    @extend_schema(
        operation_id="ownerClaimableFacilitiesList",
        tags=["Owner"],
        summary="Find a published facility nobody owns yet",
        description=(
            "For «هذه منشأتي». Matches the Arabic or English name, Arabic spelling folded "
            f"as in search. At most {RESULTS} results; `q` needs two characters."
        ),
        parameters=[
            OpenApiParameter("q", str, OpenApiParameter.QUERY, required=True),
            _q("provinceId", "Keep facilities in this province."),
            _q("categoryId", "Keep facilities of this category."),
        ],
        responses={200: ClaimableFacilityListSerializer, 400: VALIDATION_400, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        from django.db.models import Q

        text = (request.query_params.get("q") or "").strip()
        if len(text) < 2:
            raise ValidationError({"q": ["Type at least two characters of the name."]})
        rows = claims.claimable().filter(
            Q(name_ar__ar_contains=text) | Q(name_en__ar_contains=text)
        )
        for param, field in (("provinceId", "province_id"), ("categoryId", "category_id")):
            if value := request.query_params.get(param):
                rows = rows.filter(**{field: value})
        return Response(
            {
                "items": [
                    {
                        "id": str(row.pk),
                        "nameAr": row.name_ar,
                        "categoryNameAr": row.category.name_ar,
                        "provinceNameAr": row.province.name_ar,
                        "cityNameAr": row.city.name_ar if row.city is not None else None,
                        "addressAr": row.address_ar or None,
                    }
                    for row in rows.order_by("name_ar", "id")[:RESULTS]
                ]
            }
        )


class ClaimListCreateView(APIView):
    permission_classes = [IsAuthenticated]
    throttle_classes = [UserDefaultThrottle]

    @extend_schema(
        operation_id="ownerClaimsList",
        tags=["Owner"],
        summary="This account's claims, newest first",
        responses={200: ClaimListSerializer, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        return Response(
            {"items": [claims.claim_payload(row) for row in claims.my_claims(request.user)]}
        )

    @extend_schema(
        operation_id="ownerClaimStart",
        tags=["Owner"],
        summary="Start claiming a facility",
        description=(
            "Returns the open claim this account already has for the facility, if any. "
            "404 when the facility is not claimable; 409 FACILITY_ALREADY_OWNED when it has an "
            "owner, TOO_MANY_CLAIMS past five open claims."
        ),
        request=ClaimStartSerializer,
        responses={
            201: ClaimSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
            409: CONFLICT_409,
        },
    )
    def post(self, request: AuthenticatedRequest) -> Response:
        payload = ClaimStartSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        try:
            application = claims.start(
                user=request.user,
                facility_id=payload.validated_data["facilityId"],
                request_id=_request_id(request),
            )
        except Facility.DoesNotExist as exc:
            raise NotFound() from exc
        return Response(claims.claim_payload(application), status=201)


class ClaimDetailView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="ownerClaimRetrieve",
        tags=["Owner"],
        summary="One of this account's claims",
        responses={200: ClaimSerializer, **protected(), 404: NOT_FOUND_404},
    )
    def get(self, request: AuthenticatedRequest, claim_id: UUID) -> Response:
        application = claims.my_claims(request.user).filter(pk=claim_id).first()
        if application is None:
            raise NotFound()
        return Response(claims.claim_payload(application))

    @extend_schema(
        operation_id="ownerClaimWithdraw",
        tags=["Owner"],
        summary="Withdraw a claim and delete its documents",
        responses={204: None, **protected(), 404: NOT_FOUND_404, 409: CONFLICT_409},
    )
    def delete(self, request: AuthenticatedRequest, claim_id: UUID) -> Response:
        try:
            claims.withdraw(
                user=request.user, application_id=claim_id, request_id=_request_id(request)
            )
        except FacilityApplication.DoesNotExist as exc:
            raise NotFound() from exc
        return Response(status=204)


class ClaimEvidenceView(APIView):
    permission_classes = [IsAuthenticated]
    parser_classes = [MultiPartParser, FormParser]
    throttle_classes = [EvidenceUploadThrottle]

    @extend_schema(
        operation_id="ownerClaimEvidenceCreate",
        tags=["Media"],
        summary="Upload a verification document for a claim",
        description=(
            "Private, like a facility's own documents. It belongs to the claim until the claim "
            "is approved, and is deleted if the claim is withdrawn or rejected."
        ),
        request={"multipart/form-data": EvidenceUploadSerializer},
        responses={
            201: ClaimEvidenceSerializer,
            400: DOMAIN_400,
            **protected(),
            404: NOT_FOUND_404,
            409: CONFLICT_409,
        },
    )
    def post(self, request: AuthenticatedRequest, claim_id: UUID) -> Response:
        payload = EvidenceUploadSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        try:
            evidence = claims.add_evidence(
                user=request.user,
                application_id=claim_id,
                requirement_id=payload.validated_data["requirementId"],
                upload=payload.validated_data["file"],
                request_id=_request_id(request),
            )
        except FacilityApplication.DoesNotExist as exc:
            raise NotFound() from exc
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        return Response(
            {
                "id": str(evidence.pk),
                "requirementId": evidence.requirement_id,
                "createdAt": evidence.created_at.isoformat(),
            },
            status=201,
        )


class ClaimEvidenceDeleteView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="ownerClaimEvidenceDelete",
        tags=["Media"],
        summary="Remove a document from a claim not yet sent",
        responses={204: None, **protected(), 404: NOT_FOUND_404, 409: CONFLICT_409},
    )
    def delete(self, request: AuthenticatedRequest, claim_id: UUID, evidence_id: UUID) -> Response:
        try:
            claims.remove_evidence(
                user=request.user,
                application_id=claim_id,
                evidence_id=evidence_id,
                request_id=_request_id(request),
            )
        except (FacilityApplication.DoesNotExist, VerificationEvidence.DoesNotExist) as exc:
            raise NotFound() from exc
        return Response(status=204)


class ClaimSubmitView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="ownerClaimSubmit",
        tags=["Owner"],
        summary="Send a claim for review",
        description=(
            "Every required document must be uploaded to the claim. 409 CLAIM_PENDING while "
            "another claim on the same facility is being reviewed; FACILITY_ALREADY_OWNED if it "
            "gained an owner meanwhile."
        ),
        request=None,
        responses={
            200: ClaimSerializer,
            400: DOMAIN_400,
            **protected(),
            404: NOT_FOUND_404,
            409: CONFLICT_409,
        },
    )
    def post(self, request: AuthenticatedRequest, claim_id: UUID) -> Response:
        try:
            application = claims.submit(
                user=request.user, application_id=claim_id, request_id=_request_id(request)
            )
        except FacilityApplication.DoesNotExist as exc:
            raise NotFound() from exc
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        return Response(claims.claim_payload(application))
