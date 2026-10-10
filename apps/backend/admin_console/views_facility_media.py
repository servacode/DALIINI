"""A facility's opening hours and public photos, set from the console (DECISION-115).

The directory opens empty and the first facilities are the operator's to add. The console could
create one with its name, contacts and pin, but not its week or its picture — so a pharmacy the
platform added itself could never show «مفتوح الآن» and always wore a placeholder.

These are the owner's own operations behind the operator's permission: the same weekly-schedule
validation, the same image pipeline (decoded, bounded, re-encoded, stripped, random key), the same
capability checks and the same audit actions, with the operator as the actor.
"""

from __future__ import annotations

from typing import Any
from uuid import UUID

from django.core.exceptions import ValidationError as DjangoValidationError
from django.db import transaction
from django.shortcuts import get_object_or_404
from drf_spectacular.utils import extend_schema
from rest_framework.parsers import FormParser, MultiPartParser
from rest_framework.response import Response

from accounts.authentication import AuthenticatedRequest
from audit.services import record_audit
from business_hours.models import BusinessHour
from business_hours.schemas import BusinessHoursListSerializer
from business_hours.serializers import BusinessHourInputSerializer, serialize_hours
from business_hours.services_write import replace_business_hours
from core.exceptions import ConflictError, DomainError
from core.openapi import CONFLICT_409, DOMAIN_400, NOT_FOUND_404, protected
from facilities.media import save_public_image
from facilities.models import Facility, FacilityImage
from facilities.schemas import OwnerFacilityImageListSerializer, OwnerFacilityImageSerializer
from facilities.serializers import PublicImageUploadSerializer
from storage.backends import PublicS3Storage

from .views import AdminView, _request_id, _validation_error
from .views_smart import require_permission

EDIT_PERMISSION = "admin.facilities.edit"


def _image_payload(image: FacilityImage, storage: PublicS3Storage) -> dict[str, Any]:
    return {
        "id": str(image.pk),
        "url": storage.url(image.storage_key),
        "sortOrder": image.sort_order,
        "width": image.width,
        "height": image.height,
    }


class AdminFacilityHoursView(AdminView):
    required_permission = "admin.facilities.read"

    @extend_schema(
        operation_id="adminFacilityHoursList",
        tags=["Admin Facilities"],
        summary="A facility's weekly opening hours",
        responses={200: BusinessHoursListSerializer, **protected(), 404: NOT_FOUND_404},
    )
    def get(self, request: AuthenticatedRequest, facility_id: UUID) -> Response:
        facility = get_object_or_404(Facility, pk=facility_id)
        rows = BusinessHour.objects.filter(facility=facility).order_by(
            "weekday", "sort_order", "opens_at"
        )
        return Response({"items": serialize_hours(rows)})

    @extend_schema(
        operation_id="adminFacilityHoursReplace",
        tags=["Admin Facilities"],
        summary="Replace a facility's weekly opening hours",
        description=(
            "The whole week in one call, as the owner's own route: overnight spans are "
            "allowed and same-day overlaps refused. Requires `admin.facilities.edit`."
        ),
        request=BusinessHourInputSerializer(many=True),
        responses={
            200: BusinessHoursListSerializer,
            400: DOMAIN_400,
            **protected(),
            404: NOT_FOUND_404,
            409: CONFLICT_409,
        },
    )
    def put(self, request: AuthenticatedRequest, facility_id: UUID) -> Response:
        require_permission(self, request, EDIT_PERMISSION)
        facility = get_object_or_404(Facility, pk=facility_id)
        if not facility.category.supports("supports_hours"):
            raise ConflictError(
                "HOURS_NOT_SUPPORTED", message="هذا التصنيف لا يدعم أوقات الدوام."
            )
        items = BusinessHourInputSerializer(data=request.data, many=True)
        items.is_valid(raise_exception=True)
        try:
            created = replace_business_hours(
                actor=request.user, facility=facility, rows=list(items.validated_data)
            )
        except DjangoValidationError as exc:
            raise DomainError(
                "INVALID_HOURS",
                message="أوقات الدوام المرسلة غير صالحة.",
                details=getattr(exc, "message_dict", None) or exc.messages,
            ) from exc
        return Response({"items": serialize_hours(created)})


class AdminFacilityImagesView(AdminView):
    required_permission = "admin.facilities.read"
    parser_classes = [MultiPartParser, FormParser]

    @extend_schema(
        operation_id="adminFacilityImagesList",
        tags=["Admin Facilities"],
        summary="A facility's public photos",
        responses={200: OwnerFacilityImageListSerializer, **protected(), 404: NOT_FOUND_404},
    )
    def get(self, request: AuthenticatedRequest, facility_id: UUID) -> Response:
        facility = get_object_or_404(Facility, pk=facility_id)
        storage = PublicS3Storage()
        return Response(
            {
                "items": [
                    _image_payload(image, storage)
                    for image in facility.images.order_by("sort_order", "created_at")
                ]
            }
        )

    @extend_schema(
        operation_id="adminFacilityImageCreate",
        tags=["Admin Facilities"],
        summary="Add a public photo to a facility",
        description=(
            "Multipart, through the same pipeline as the owner's upload: the file is decoded, "
            "bounded, re-encoded to JPEG, stripped and stored under a random key. Requires "
            "`admin.facilities.edit`."
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
        require_permission(self, request, EDIT_PERMISSION)
        facility = get_object_or_404(Facility, pk=facility_id)
        if not facility.category.supports("supports_photos"):
            raise ConflictError("PHOTOS_NOT_SUPPORTED", message="هذا التصنيف لا يدعم الصور.")
        upload = PublicImageUploadSerializer(data=request.data)
        upload.is_valid(raise_exception=True)
        try:
            storage, key, width, height = save_public_image(
                facility_id=facility.pk, upload=upload.validated_data["file"]
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
        return Response(_image_payload(image, storage), status=201)


class AdminFacilityImageDeleteView(AdminView):
    required_permission = "admin.facilities.read"

    @extend_schema(
        operation_id="adminFacilityImageDelete",
        tags=["Admin Facilities"],
        summary="Remove a public photo from a facility",
        description="Requires `admin.facilities.edit`. The stored file goes once the row does.",
        responses={204: None, **protected(), 404: NOT_FOUND_404},
    )
    def delete(
        self, request: AuthenticatedRequest, facility_id: UUID, image_id: UUID
    ) -> Response:
        require_permission(self, request, EDIT_PERMISSION)
        image = get_object_or_404(FacilityImage, pk=image_id, facility_id=facility_id)
        key = image.storage_key
        record_audit(
            actor=request.user,
            action="facility.public_image.deleted",
            target=image,
            metadata={"facilityId": str(facility_id)},
            request_id=_request_id(request),
        )
        image.delete()
        transaction.on_commit(lambda: PublicS3Storage().delete(key))
        return Response(status=204)
