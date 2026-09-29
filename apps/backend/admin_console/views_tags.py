"""Specialties and services: the choices behind two public filters, and an owner's picks.

Reads need admin.taxonomy.read, writes admin.taxonomy.manage (re-checked inside the handler
where a write shares its view with a read). Every write is audited by the service it calls.

A specialty is listed with its category, whichever scope it has: the category's own, and the
ones its specialization shares with every category of that specialization. Delete is for an
item no facility lists; one in use is refused with 409 and retired with `active = false`.
"""

from __future__ import annotations

from typing import Any
from uuid import UUID

from django.core.exceptions import ValidationError as DjangoValidationError
from django.db.models import Count
from django.shortcuts import get_object_or_404
from drf_spectacular.utils import extend_schema
from rest_framework.exceptions import ValidationError
from rest_framework.response import Response

from accounts.authentication import AuthenticatedRequest
from core.openapi import CONFLICT_409, NOT_FOUND_404, VALIDATION_400, protected
from directory.models import Category, ServiceTag, Specialty
from directory.services import (
    create_service_tag,
    create_specialty,
    delete_service_tag,
    delete_specialty,
    specialty_scope,
    update_service_tag,
    update_specialty,
)
from directory.tags import ORDER, specialties_for

from .schemas import (
    AdminServiceTagCreateRequestSerializer,
    AdminServiceTagListSerializer,
    AdminServiceTagSerializer,
    AdminSpecialtyCreateRequestSerializer,
    AdminSpecialtyListSerializer,
    AdminSpecialtySerializer,
    AdminTagUpdateRequestSerializer,
)
from .views import AdminView
from .views_smart import require_permission

READ = "admin.taxonomy.read"
MANAGE = "admin.taxonomy.manage"

def _django_error(exc: DjangoValidationError) -> ValidationError:
    if hasattr(exc, "message_dict"):
        return ValidationError(exc.message_dict)
    return ValidationError({"nonFieldErrors": exc.messages})


def _refuse_moves(request: AuthenticatedRequest, fields: tuple[str, ...]) -> None:
    """An item cannot change scope: refused visibly, never silently dropped."""
    sent = request.data if isinstance(request.data, dict) else {}
    refused = {
        field: ["Fixed at creation; add a new item instead."] for field in fields if field in sent
    }
    if refused:
        raise ValidationError(refused)


def _facility_count(row: Specialty | ServiceTag) -> int:
    counted = getattr(row, "facility_count", None)
    return int(counted) if counted is not None else row.facility_links.count()


def _specialty_payload(specialty: Specialty) -> dict[str, Any]:
    return {
        "id": specialty.pk,
        "scope": specialty_scope(specialty),
        "categoryId": str(specialty.category_id) if specialty.category_id else None,
        "specialization": specialty.specialization or None,
        "nameAr": specialty.name_ar,
        "nameEn": specialty.name_en,
        "active": specialty.active,
        "sortOrder": specialty.sort_order,
        "facilityCount": _facility_count(specialty),
    }


def _service_tag_payload(tag: ServiceTag) -> dict[str, Any]:
    return {
        "id": tag.pk,
        "categoryId": str(tag.category_id),
        "nameAr": tag.name_ar,
        "nameEn": tag.name_en,
        "active": tag.active,
        "sortOrder": tag.sort_order,
        "facilityCount": _facility_count(tag),
    }


# --------------------------------------------------------------------------- specialties


class CategorySpecialtiesView(AdminView):
    required_permission = READ

    @extend_schema(
        operation_id="adminCategorySpecialtiesList",
        tags=["Admin Taxonomy"],
        summary="List the specialties a category offers, in both scopes",
        description=(
            "The category's own specialties and those its specialization shares, retired "
            "ones included, in the order the public sees them."
        ),
        responses={200: AdminSpecialtyListSerializer, **protected(), 404: NOT_FOUND_404},
    )
    def get(self, request: AuthenticatedRequest, category_id: UUID) -> Response:
        category = get_object_or_404(Category, pk=category_id)
        rows = (
            specialties_for(category)
            .annotate(facility_count=Count("facility_links"))
            .order_by(*ORDER)
        )
        return Response({"items": [_specialty_payload(row) for row in rows]})

    @extend_schema(
        operation_id="adminCategorySpecialtyCreate",
        tags=["Admin Taxonomy"],
        summary="Add a specialty to a category or to its specialization",
        description=(
            "Requires admin.taxonomy.manage, re-checked inside the handler. `scope` CATEGORY "
            "scopes it to this category; SPECIALIZATION shares it with every category of "
            "this category's specialization and is refused for a GENERIC category. A name "
            "already used in the same scope is refused, retired items included."
        ),
        request=AdminSpecialtyCreateRequestSerializer,
        responses={
            201: AdminSpecialtySerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def post(self, request: AuthenticatedRequest, category_id: UUID) -> Response:
        require_permission(self, request, MANAGE)
        category = get_object_or_404(Category, pk=category_id)
        payload = AdminSpecialtyCreateRequestSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        try:
            specialty = create_specialty(
                request=request, category=category, data=payload.validated_data
            )
        except DjangoValidationError as exc:
            raise _django_error(exc) from exc
        return Response(_specialty_payload(specialty), status=201)


class SpecialtyDetailView(AdminView):
    required_permission = MANAGE

    @extend_schema(
        operation_id="adminSpecialtyUpdate",
        tags=["Admin Taxonomy"],
        summary="Rename, reorder, retire or bring back a specialty",
        description=(
            "Omitted fields keep their value. The scope is fixed: `scope`, `categoryId` and "
            "`specialization` are refused. A specialization's specialty changes for every "
            "category of that specialization."
        ),
        request=AdminTagUpdateRequestSerializer,
        responses={
            200: AdminSpecialtySerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def put(self, request: AuthenticatedRequest, specialty_id: int) -> Response:
        get_object_or_404(Specialty, pk=specialty_id)
        _refuse_moves(request, ("scope", "categoryId", "specialization"))
        payload = AdminTagUpdateRequestSerializer(data=request.data, partial=True)
        payload.is_valid(raise_exception=True)
        try:
            specialty = update_specialty(
                request=request, specialty_id=specialty_id, data=payload.validated_data
            )
        except DjangoValidationError as exc:
            raise _django_error(exc) from exc
        return Response(_specialty_payload(specialty))

    @extend_schema(
        operation_id="adminSpecialtyDelete",
        tags=["Admin Taxonomy"],
        summary="Delete a specialty no facility lists",
        description=(
            "One that a facility lists answers 409 `SPECIALTY_IN_USE`; retire it with "
            "`active = false` instead."
        ),
        responses={204: None, 409: CONFLICT_409, **protected(), 404: NOT_FOUND_404},
    )
    def delete(self, request: AuthenticatedRequest, specialty_id: int) -> Response:
        get_object_or_404(Specialty, pk=specialty_id)
        delete_specialty(request=request, specialty_id=specialty_id)
        return Response(status=204)


# ------------------------------------------------------------------------------ services


class CategoryServiceTagsView(AdminView):
    required_permission = READ

    @extend_schema(
        operation_id="adminCategoryServiceTagsList",
        tags=["Admin Taxonomy"],
        summary="List the services of a category",
        description="Retired ones included, in the order the public sees them.",
        responses={200: AdminServiceTagListSerializer, **protected(), 404: NOT_FOUND_404},
    )
    def get(self, request: AuthenticatedRequest, category_id: UUID) -> Response:
        category = get_object_or_404(Category, pk=category_id)
        rows = (
            ServiceTag.objects.filter(category=category)
            .annotate(facility_count=Count("facility_links"))
            .order_by(*ORDER)
        )
        return Response({"items": [_service_tag_payload(row) for row in rows]})

    @extend_schema(
        operation_id="adminCategoryServiceTagCreate",
        tags=["Admin Taxonomy"],
        summary="Add a service to a category",
        description=(
            "Requires admin.taxonomy.manage, re-checked inside the handler. A name already "
            "used in the category is refused, retired services included."
        ),
        request=AdminServiceTagCreateRequestSerializer,
        responses={
            201: AdminServiceTagSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def post(self, request: AuthenticatedRequest, category_id: UUID) -> Response:
        require_permission(self, request, MANAGE)
        category = get_object_or_404(Category, pk=category_id)
        payload = AdminServiceTagCreateRequestSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        try:
            tag = create_service_tag(
                request=request, category=category, data=payload.validated_data
            )
        except DjangoValidationError as exc:
            raise _django_error(exc) from exc
        return Response(_service_tag_payload(tag), status=201)


class ServiceTagDetailView(AdminView):
    required_permission = MANAGE

    @extend_schema(
        operation_id="adminServiceTagUpdate",
        tags=["Admin Taxonomy"],
        summary="Rename, reorder, retire or bring back a service",
        description=(
            "Omitted fields keep their value. The category is fixed: `categoryId` is refused."
        ),
        request=AdminTagUpdateRequestSerializer,
        responses={
            200: AdminServiceTagSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def put(self, request: AuthenticatedRequest, service_tag_id: int) -> Response:
        get_object_or_404(ServiceTag, pk=service_tag_id)
        _refuse_moves(request, ("categoryId",))
        payload = AdminTagUpdateRequestSerializer(data=request.data, partial=True)
        payload.is_valid(raise_exception=True)
        try:
            tag = update_service_tag(
                request=request, service_tag_id=service_tag_id, data=payload.validated_data
            )
        except DjangoValidationError as exc:
            raise _django_error(exc) from exc
        return Response(_service_tag_payload(tag))

    @extend_schema(
        operation_id="adminServiceTagDelete",
        tags=["Admin Taxonomy"],
        summary="Delete a service no facility lists",
        description=(
            "One that a facility lists answers 409 `SERVICE_TAG_IN_USE`; retire it with "
            "`active = false` instead."
        ),
        responses={204: None, 409: CONFLICT_409, **protected(), 404: NOT_FOUND_404},
    )
    def delete(self, request: AuthenticatedRequest, service_tag_id: int) -> Response:
        get_object_or_404(ServiceTag, pk=service_tag_id)
        delete_service_tag(request=request, service_tag_id=service_tag_id)
        return Response(status=204)
