from datetime import datetime
from typing import Any, cast
from uuid import UUID

from django.core.exceptions import ValidationError as DjangoValidationError
from django.db import transaction
from django.db.models import QuerySet
from django.shortcuts import get_object_or_404
from drf_spectacular.utils import OpenApiParameter, extend_schema
from rest_framework.parsers import FormParser, MultiPartParser
from rest_framework.permissions import IsAuthenticated
from rest_framework.request import Request
from rest_framework.response import Response
from rest_framework.views import APIView

from accounts.authentication import AuthenticatedRequest
from accounts.models import User
from audit.services import record_audit
from core.exceptions import ConflictError, DomainError
from core.openapi import (
    CONFLICT_409,
    DOMAIN_400,
    NOT_FOUND_404,
    VALIDATION_400,
    protected,
)
from core.throttles import EvidenceUploadThrottle, OwnerSubmitThrottle
from directory.models import CategoryProvince, VerificationRequirement
from directory.presenters import category_capabilities
from directory.tags import TagChoices, tag_choices
from locations.models import Province
from locations.presenters import map_center
from storage.backends import PrivateS3Storage, PublicS3Storage

from .media import save_private_evidence, save_public_image
from .models import (
    Facility,
    FacilityApplication,
    FacilityImage,
    FacilityMembership,
    VerificationEvidence,
)
from .permissions import require_facility_member, require_facility_owner
from .presenters import facility_detail, facility_summary
from .schemas import (
    OwnerConfigSerializer,
    OwnerEvidenceCreatedSerializer,
    OwnerFacilityDetailSerializer,
    OwnerFacilityImageListSerializer,
    OwnerFacilityImageSerializer,
    OwnerFacilitySummaryListSerializer,
    OwnerMemberListSerializer,
    OwnerMemberUpsertedSerializer,
    OwnerSubmitResultSerializer,
)
from .serializers import (
    EvidenceUploadSerializer,
    FacilityCreateSerializer,
    FacilityLocationSerializer,
    FacilityMemberSerializer,
    FacilityPatchSerializer,
    PublicImageUploadSerializer,
)
from .services import (
    create_facility_draft,
    ensure_editable_by_owner,
    submit_facility,
    update_facility_core,
    update_facility_location,
)


def _request_id(request: Request) -> str:
    return getattr(request, "request_id", "")


def _validation_error(exc: DjangoValidationError) -> DomainError:
    """Carry a Django model validation failure into the central error envelope."""
    details: dict[str, list[str]] | list[str]
    if hasattr(exc, "message_dict"):
        details = exc.message_dict
    elif hasattr(exc, "messages"):
        details = exc.messages
    else:
        details = [str(exc)]
    return DomainError(
        "VALIDATION_ERROR",
        message="تعذر حفظ البيانات.",
        details=details,
    )


def _owned_facilities(user: User) -> QuerySet[Facility]:
    return (
        Facility.objects.filter(memberships__user=user)
        .select_related("category", "category__capabilities", "province", "city", "neighborhood")
        .prefetch_related(
            "applications",
            "business_hours",
            # With the rows themselves: the detail leaves retired ones out.
            "specialty_links__specialty",
            "service_links__service_tag",
            "evidence",
        )
        .distinct()
    )


def _owner_config_item(switch: CategoryProvince, choices: TagChoices) -> dict[str, Any]:
    category = switch.category
    requirements = category.verification_requirements.filter(active=True).order_by("sort_order")
    return {
        "category": {
            "id": str(category.pk),
            "nameAr": category.name_ar,
            "nameEn": category.name_en or None,
            "iconKey": category.icon_key or None,
            "specialization": category.specialization,
        },
        "capabilities": category_capabilities(category),
        "verificationRequirements": [
            {
                "id": item.pk,
                "labelAr": item.label_ar,
                "labelEn": item.label_en or None,
                "instructionsAr": item.instructions_ar or None,
                "required": item.required,
                "minFiles": item.min_files,
                "maxFiles": item.max_files,
            }
            for item in requirements
        ],
        "specialties": choices["specialties"],
        "services": choices["services"],
    }


class OwnerConfigView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="ownerConfigRetrieve",
        tags=["Owner"],
        summary="List categories open for owner onboarding in a province",
        description=(
            "Returns only categories whose per-province owner switch is on and whose "
            "capability set allows onboarding, together with the safe descriptors of the "
            "verification requirements the owner will have to satisfy, and the specialties "
            "and services the owner may pick for a facility of each."
        ),
        parameters=[
            OpenApiParameter(
                "provinceId",
                str,
                OpenApiParameter.QUERY,
                required=True,
                description="Province to inspect.",
            )
        ],
        responses={
            200: OwnerConfigSerializer,
            400: DOMAIN_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        province_id = request.query_params.get("provinceId")
        if not province_id:
            raise DomainError(
                "PROVINCE_REQUIRED",
                message="معرّف المحافظة مطلوب.",
            )
        province = get_object_or_404(Province.objects.filter(active=True), pk=province_id)
        switches = list(
            CategoryProvince.objects.filter(
                province=province,
                owner_registration_enabled=True,
                category__active=True,
                category__group__active=True,
                category__capabilities__supports_owner_onboarding=True,
            )
            .select_related("category__capabilities", "category__group")
            .prefetch_related("category__verification_requirements")
            .order_by("sort_order", "category__sort_order")
        )
        choices = tag_choices(switch.category for switch in switches)
        return Response(
            {
                "province": {
                    "id": str(province.pk),
                    "nameAr": province.name_ar,
                    "mapCenter": map_center(province),
                },
                "categories": [
                    _owner_config_item(item, choices[item.category_id]) for item in switches
                ],
            }
        )


class OwnerFacilityListCreateView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="ownerFacilitiesList",
        tags=["Owner"],
        summary="List the facilities the caller belongs to",
        responses={200: OwnerFacilitySummaryListSerializer, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        items = _owned_facilities(request.user).order_by("-updated_at")
        return Response({"items": [facility_summary(item) for item in items]})

    @extend_schema(
        operation_id="ownerFacilityCreate",
        tags=["Owner"],
        summary="Create a facility draft",
        description=(
            "The caller becomes the owner of the new draft. Creation re-checks the current "
            "province and category onboarding policy rather than any cached value."
        ),
        request=FacilityCreateSerializer,
        responses={201: OwnerFacilityDetailSerializer, 400: DOMAIN_400, **protected()},
    )
    def post(self, request: AuthenticatedRequest) -> Response:
        serializer = FacilityCreateSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        try:
            facility = create_facility_draft(
                actor=request.user,
                data=serializer.validated_data,
                request_id=_request_id(request),
            )
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        facility = _owned_facilities(request.user).get(pk=facility.pk)
        return Response(facility_detail(facility), status=201)


class OwnerFacilityDetailView(APIView):
    permission_classes = [IsAuthenticated]

    def _facility(self, request: AuthenticatedRequest, facility_id: UUID) -> Facility:
        facility = get_object_or_404(_owned_facilities(request.user), pk=facility_id)
        require_facility_member(request.user, facility)
        return facility

    @extend_schema(
        operation_id="ownerFacilityRetrieve",
        tags=["Owner"],
        summary="Retrieve one facility the caller belongs to",
        responses={
            200: OwnerFacilityDetailSerializer,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def get(self, request: AuthenticatedRequest, facility_id: UUID) -> Response:
        return Response(facility_detail(self._facility(request, facility_id)))

    @extend_schema(
        operation_id="ownerFacilityUpdate",
        tags=["Owner"],
        summary="Update the core fields of a facility",
        description=(
            "On an ACTIVE facility the facility stays published: its name, address, city, "
            "neighbourhood and map point wait for an operator as a CHANGE application "
            "(`pendingChange` in the response), and every other field applies at once. A "
            "second edit while one waits is merged into it. Elsewhere the edit applies as it "
            "stands and is reviewed at the next submission."
        ),
        request=FacilityPatchSerializer,
        responses={
            200: OwnerFacilityDetailSerializer,
            400: DOMAIN_400,
            **protected(),
            404: NOT_FOUND_404,
            409: CONFLICT_409,
        },
    )
    def patch(self, request: AuthenticatedRequest, facility_id: UUID) -> Response:
        facility = self._facility(request, facility_id)
        serializer = FacilityPatchSerializer(data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        try:
            updated = update_facility_core(
                actor=request.user,
                facility=facility,
                data=serializer.validated_data,
                request_id=_request_id(request),
            )
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        return Response(facility_detail(_owned_facilities(request.user).get(pk=updated.pk)))


class OwnerFacilitySubmitView(APIView):
    throttle_classes = [OwnerSubmitThrottle]
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="ownerFacilitySubmit",
        tags=["Owner"],
        summary="Submit a facility for review",
        description=(
            "Submission re-validates the current onboarding policy and the completeness of "
            "the current evidence requirements. Only one submitted application of a given "
            "kind can exist per facility at a time. An ACTIVE facility is never taken down "
            "to be reviewed: its edits are sent as they are saved, and submitting answers with "
            "the change already waiting, or 400 when there is none."
        ),
        request=None,
        responses={
            200: OwnerSubmitResultSerializer,
            400: DOMAIN_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def post(self, request: AuthenticatedRequest, facility_id: UUID) -> Response:
        facility = get_object_or_404(_owned_facilities(request.user), pk=facility_id)
        require_facility_member(request.user, facility)
        try:
            application = submit_facility(
                actor=request.user,
                facility=facility,
                request_id=_request_id(request),
            )
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        return Response(
            {
                "applicationId": str(application.pk),
                "status": application.status,
                # submit_facility has just set it.
                "submittedAt": cast(datetime, application.submitted_at).isoformat(),
            }
        )


class OwnerFacilityLocationView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="ownerFacilityLocationReplace",
        tags=["Owner"],
        summary="Set the map point of a facility",
        description=(
            "WGS84 decimal degrees. PostGIS remains the source of truth for geo. On an ACTIVE "
            "facility the new point waits for review and the published one stays."
        ),
        request=FacilityLocationSerializer,
        responses={
            200: OwnerFacilityDetailSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
            409: CONFLICT_409,
        },
    )
    def put(self, request: AuthenticatedRequest, facility_id: UUID) -> Response:
        facility = get_object_or_404(_owned_facilities(request.user), pk=facility_id)
        require_facility_member(request.user, facility)
        serializer = FacilityLocationSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        try:
            updated = update_facility_location(
                actor=request.user,
                facility=facility,
                latitude=serializer.validated_data["latitude"],
                longitude=serializer.validated_data["longitude"],
                request_id=_request_id(request),
            )
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        return Response(facility_detail(_owned_facilities(request.user).get(pk=updated.pk)))


class OwnerFacilityImagesView(APIView):
    parser_classes = [MultiPartParser, FormParser]
    permission_classes = [IsAuthenticated]

    def _facility(self, request: AuthenticatedRequest, facility_id: UUID) -> Facility:
        facility = get_object_or_404(_owned_facilities(request.user), pk=facility_id)
        require_facility_member(request.user, facility)
        return facility

    @extend_schema(
        operation_id="ownerFacilityImagesList",
        tags=["Media"],
        summary="List the public images of a facility",
        responses={
            200: OwnerFacilityImageListSerializer,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def get(self, request: AuthenticatedRequest, facility_id: UUID) -> Response:
        facility = self._facility(request, facility_id)
        storage = PublicS3Storage()
        items = [
            {
                "id": str(item.pk),
                "url": storage.url(item.storage_key),
                "sortOrder": item.sort_order,
                "width": item.width,
                "height": item.height,
            }
            for item in facility.images.order_by("sort_order", "created_at")
        ]
        return Response({"items": items})

    @extend_schema(
        operation_id="ownerFacilityImageCreate",
        tags=["Media"],
        summary="Upload a public facility image",
        description=(
            "Sent as multipart/form-data. The server decodes the file, enforces byte and "
            "pixel limits, re-encodes to JPEG, strips metadata and stores it under a "
            "random key. The declared extension and MIME type are not trusted."
        ),
        request={"multipart/form-data": PublicImageUploadSerializer},
        responses={
            201: OwnerFacilityImageSerializer,
            400: DOMAIN_400,
            **protected(),
            404: NOT_FOUND_404,
            409: CONFLICT_409,
        },
    )
    def post(self, request: AuthenticatedRequest, facility_id: UUID) -> Response:
        facility = self._facility(request, facility_id)
        ensure_editable_by_owner(facility)
        if not facility.category.supports("supports_photos"):
            raise ConflictError(
                "PHOTOS_NOT_SUPPORTED",
                message="هذا التصنيف لا يدعم الصور.",
            )
        serializer = PublicImageUploadSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        try:
            storage, key, width, height = save_public_image(
                facility_id=facility.pk,
                upload=serializer.validated_data["file"],
            )
            try:
                image = FacilityImage.objects.create(
                    facility=facility,
                    storage_key=key,
                    width=width,
                    height=height,
                    sort_order=facility.images.count(),
                )
            except Exception:
                storage.delete(key)
                raise
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        record_audit(
            actor=request.user,
            action="facility.public_image.created",
            target=image,
            metadata={"facilityId": str(facility.pk)},
            request_id=_request_id(request),
        )
        return Response(
            {
                "id": str(image.pk),
                "url": storage.url(key),
                "sortOrder": image.sort_order,
                "width": width,
                "height": height,
            },
            status=201,
        )


class OwnerFacilityImageDeleteView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="ownerFacilityImageDelete",
        tags=["Media"],
        summary="Delete a public facility image",
        responses={204: None, **protected(), 404: NOT_FOUND_404, 409: CONFLICT_409},
    )
    def delete(self, request: AuthenticatedRequest, facility_id: UUID, image_id: UUID) -> Response:
        facility = get_object_or_404(_owned_facilities(request.user), pk=facility_id)
        require_facility_member(request.user, facility)
        ensure_editable_by_owner(facility)
        image = get_object_or_404(FacilityImage, pk=image_id, facility=facility)
        key = image.storage_key
        record_audit(
            actor=request.user,
            action="facility.public_image.deleted",
            target=image,
            metadata={"facilityId": str(facility.pk)},
            request_id=_request_id(request),
        )
        image.delete()
        transaction.on_commit(lambda: PublicS3Storage().delete(key))
        return Response(status=204)


class OwnerFacilityEvidenceView(APIView):
    throttle_classes = [EvidenceUploadThrottle]
    parser_classes = [MultiPartParser, FormParser]
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="ownerFacilityEvidenceCreate",
        tags=["Media"],
        summary="Upload private verification evidence",
        description=(
            "Sent as multipart/form-data and stored in the private namespace. The response "
            "carries identifiers only: evidence is never served through a public URL and "
            "its storage key is never returned."
        ),
        request={"multipart/form-data": EvidenceUploadSerializer},
        responses={
            201: OwnerEvidenceCreatedSerializer,
            400: DOMAIN_400,
            **protected(),
            404: NOT_FOUND_404,
            409: CONFLICT_409,
        },
    )
    def post(self, request: AuthenticatedRequest, facility_id: UUID) -> Response:
        facility = get_object_or_404(_owned_facilities(request.user), pk=facility_id)
        require_facility_member(request.user, facility)
        serializer = EvidenceUploadSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        requirement = get_object_or_404(
            VerificationRequirement.objects.filter(active=True, category=facility.category),
            pk=serializer.validated_data["requirementId"],
        )
        current = VerificationEvidence.objects.filter(
            facility=facility, requirement=requirement, application__isnull=True
        ).count()
        if current >= requirement.max_files:
            raise ConflictError(
                "EVIDENCE_MAX_FILES",
                message="تم بلوغ الحد الأقصى لعدد ملفات هذا المتطلب.",
            )
        try:
            storage, key, _, _ = save_private_evidence(
                facility_id=facility.pk,
                requirement_id=requirement.pk,
                upload=serializer.validated_data["file"],
            )
            try:
                evidence = VerificationEvidence.objects.create(
                    facility=facility,
                    requirement=requirement,
                    storage_key=key,
                    uploaded_by=request.user,
                )
            except Exception:
                storage.delete(key)
                raise
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        record_audit(
            actor=request.user,
            action="facility.evidence.created",
            target=evidence,
            metadata={
                "facilityId": str(facility.pk),
                "requirementId": str(requirement.pk),
            },
            request_id=_request_id(request),
        )
        return Response(
            {"id": str(evidence.pk), "requirementId": requirement.pk},
            status=201,
        )


class OwnerFacilityEvidenceDeleteView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="ownerFacilityEvidenceDelete",
        tags=["Media"],
        summary="Delete a piece of verification evidence",
        description="Evidence is locked while an application is under review.",
        responses={204: None, **protected(), 404: NOT_FOUND_404, 409: CONFLICT_409},
    )
    def delete(
        self, request: AuthenticatedRequest, facility_id: UUID, evidence_id: UUID
    ) -> Response:
        facility = get_object_or_404(_owned_facilities(request.user), pk=facility_id)
        require_facility_member(request.user, facility)
        if facility.applications.filter(status=FacilityApplication.Status.SUBMITTED).exists():
            raise ConflictError(
                "EVIDENCE_LOCKED_DURING_REVIEW",
                message="لا يمكن تعديل الإثباتات أثناء مراجعة الطلب.",
            )
        evidence = get_object_or_404(
            VerificationEvidence, pk=evidence_id, facility=facility, application__isnull=True
        )
        key = evidence.storage_key
        record_audit(
            actor=request.user,
            action="facility.evidence.deleted",
            target=evidence,
            metadata={"facilityId": str(facility.pk)},
            request_id=_request_id(request),
        )
        evidence.delete()
        transaction.on_commit(lambda: PrivateS3Storage().delete(key))
        return Response(status=204)


class OwnerFacilityMembersView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="ownerFacilityMembersList",
        tags=["Owner"],
        summary="List the members of a facility",
        responses={200: OwnerMemberListSerializer, **protected(), 404: NOT_FOUND_404},
    )
    def get(self, request: AuthenticatedRequest, facility_id: UUID) -> Response:
        facility = get_object_or_404(_owned_facilities(request.user), pk=facility_id)
        require_facility_member(request.user, facility)
        items = facility.memberships.select_related("user").order_by("created_at")
        return Response(
            {
                "items": [
                    {
                        "userId": str(item.user_id),
                        "name": item.user.name,
                        "phone": item.user.phone,
                        "role": item.role,
                    }
                    for item in items
                ]
            }
        )

    @extend_schema(
        operation_id="ownerFacilityMemberUpsert",
        tags=["Owner"],
        summary="Add a member or change a member role",
        description=(
            "Only an owner may call this, and the last owner cannot be demoted. Adding a new "
            "member by account id is deprecated: invite them by phone number with "
            "ownerFacilityInvitationCreate, which they accept themselves. Changing the role of "
            "an existing member stays here."
        ),
        request=FacilityMemberSerializer,
        responses={
            201: OwnerMemberUpsertedSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
            409: CONFLICT_409,
        },
    )
    @transaction.atomic
    def post(self, request: AuthenticatedRequest, facility_id: UUID) -> Response:
        facility = get_object_or_404(_owned_facilities(request.user), pk=facility_id)
        require_facility_owner(request.user, facility)
        serializer = FacilityMemberSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user = get_object_or_404(
            User,
            pk=serializer.validated_data["userId"],
            is_active=True,
        )
        requested_role = serializer.validated_data["role"]
        member = (
            FacilityMembership.objects.select_for_update()
            .filter(facility=facility, user=user)
            .first()
        )
        if (
            member is not None
            and member.role == FacilityMembership.Role.OWNER
            and requested_role != FacilityMembership.Role.OWNER
        ):
            owner_count = FacilityMembership.objects.select_for_update().filter(
                facility=facility,
                role=FacilityMembership.Role.OWNER,
            ).count()
            if owner_count <= 1:
                raise ConflictError(
                    "LAST_OWNER_PROTECTED",
                    message="لا يمكن ترك المنشأة بلا مالك.",
                )
        before_role = member.role if member is not None else None
        if member is None:
            member = FacilityMembership.objects.create(
                facility=facility,
                user=user,
                role=requested_role,
            )
        else:
            member.role = requested_role
            member.save(update_fields=["role"])
        record_audit(
            actor=request.user,
            action="facility.member.upserted",
            target=member,
            before_snapshot={
                "role": before_role,
                "facilityId": str(facility.pk),
            },
            after_snapshot={"role": member.role, "facilityId": str(facility.pk)},
            request_id=_request_id(request),
        )
        return Response(
            {"userId": str(user.pk), "name": user.name, "role": member.role},
            status=201,
        )


class OwnerFacilityMemberDeleteView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="ownerFacilityMemberDelete",
        tags=["Owner"],
        summary="Remove a member from a facility",
        description="Only an owner may call this, and the last owner cannot be removed.",
        responses={204: None, **protected(), 404: NOT_FOUND_404, 409: CONFLICT_409},
    )
    @transaction.atomic
    def delete(self, request: AuthenticatedRequest, facility_id: UUID, user_id: UUID) -> Response:
        facility = get_object_or_404(_owned_facilities(request.user), pk=facility_id)
        require_facility_owner(request.user, facility)
        member = get_object_or_404(
            FacilityMembership.objects.select_for_update(),
            facility=facility,
            user_id=user_id,
        )
        if member.role == FacilityMembership.Role.OWNER:
            owners = FacilityMembership.objects.filter(
                facility=facility, role=FacilityMembership.Role.OWNER
            ).count()
            if owners <= 1:
                raise ConflictError(
                    "LAST_OWNER_PROTECTED",
                    message="لا يمكن ترك المنشأة بلا مالك.",
                )
        record_audit(
            actor=request.user,
            action="facility.member.deleted",
            target=member,
            before_snapshot={"role": member.role, "facilityId": str(facility.pk)},
            request_id=_request_id(request),
        )
        member.delete()
        return Response(status=204)
