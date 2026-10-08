"""The console's facility list and record: read, create and correct.

Moved out of `views.py`, which had grown past 1,800 lines; the lifecycle transitions (suspend,
reactivate, close) still live there and move in the same way later.
"""

from __future__ import annotations

from typing import Any
from uuid import UUID

from django.core.exceptions import ObjectDoesNotExist
from django.core.exceptions import ValidationError as DjangoValidationError
from django.shortcuts import get_object_or_404
from drf_spectacular.utils import OpenApiParameter, extend_schema
from rest_framework.exceptions import NotFound
from rest_framework.response import Response

from accounts.authentication import AuthenticatedRequest
from core.openapi import NOT_FOUND_404, VALIDATION_400, protected
from core.pagination import QueryOrderedCursorPage, page_parameters
from facilities.models import Facility

from .facility_editor import (
    AdminFacilityCreateSerializer,
    AdminFacilityWriteSerializer,
    create_facility,
    update_facility,
)
from .quality import QUALITY_ISSUES, with_quality
from .schemas import (
    AdminFacilityDetailSerializer,
    AdminFacilityListSerializer,
    AdminFacilityMapSerializer,
)
from .serializers import (
    facility_card_payload,
    facility_detail_payload,
    with_facility_cards,
    with_facility_names,
)
from .views import (
    FACILITY_ORDERINGS,
    AdminView,
    _filter,
    _page,
    _request_id,
    _validation_error,
    filtered_facilities,
)
from .views_smart import require_permission

EDIT_PERMISSION = "admin.facilities.edit"


def _detail(facility_id: UUID) -> Response:
    facility = get_object_or_404(
        with_quality(with_facility_names(Facility.objects.all())), pk=facility_id
    )
    return Response(facility_detail_payload(facility))


class FacilityListView(AdminView):
    required_permission = "admin.facilities.read"

    @extend_schema(
        operation_id="adminFacilitiesList",
        tags=["Admin Facilities"],
        summary="List facilities for operations",
        description=(
            "In cursor pages. Every filter is optional and combines with the rest. Each row "
            "carries `qualityScore` (0-100) and `qualityIssues`, computed in the same query."
        ),
        parameters=[
            *page_parameters(QueryOrderedCursorPage),
            _filter(
                "id",
                "One facility by id: what a link written before the console had cards resolves "
                "to.",
            ),
            _filter("status", "Facility status, for example ACTIVE or SUSPENDED."),
            _filter("province", "Province id."),
            _filter("city", "City id."),
            _filter("category", "Category id."),
            _filter("q", "Free text matched against the Arabic and English facility names."),
            OpenApiParameter(
                "issue",
                str,
                OpenApiParameter.QUERY,
                required=False,
                enum=QUALITY_ISSUES,
                description="Keep facilities that have this quality issue.",
            ),
            OpenApiParameter(
                "ordering",
                str,
                OpenApiParameter.QUERY,
                required=False,
                enum=list(FACILITY_ORDERINGS),
                description="Sort order; the default is `-updatedAt` (most recently changed).",
            ),
        ],
        responses={200: AdminFacilityListSerializer, 400: VALIDATION_400, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        qs = with_facility_cards(filtered_facilities(request.query_params))
        return _page(request, qs, facility_card_payload)

    @extend_schema(
        operation_id="adminFacilityCreate",
        tags=["Admin Facilities"],
        summary="Add a facility to the directory",
        description=(
            "Listed by the directory itself, with no owner; an owner can claim it later. "
            "ACTIVE (the default) publishes it at once and counts as verified. The same "
            "validation as an owner's edit applies. Requires `admin.facilities.edit`; audited."
        ),
        request=AdminFacilityCreateSerializer,
        responses={201: AdminFacilityDetailSerializer, 400: VALIDATION_400, **protected()},
    )
    def post(self, request: AuthenticatedRequest) -> Response:
        require_permission(self, request, EDIT_PERMISSION)
        payload = AdminFacilityCreateSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        try:
            facility = create_facility(
                actor=request.user,
                data=dict(payload.validated_data),
                request_id=_request_id(request),
            )
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        response = _detail(facility.pk)
        response.status_code = 201
        return response


class FacilityDetailView(AdminView):
    required_permission = "admin.facilities.read"

    @extend_schema(
        operation_id="adminFacilityRetrieve",
        tags=["Admin Facilities"],
        summary="Retrieve one facility",
        description="Everything the console shows and edits, with the quality score.",
        responses={200: AdminFacilityDetailSerializer, **protected(), 404: NOT_FOUND_404},
    )
    def get(self, request: AuthenticatedRequest, facility_id: UUID) -> Response:
        return _detail(facility_id)

    @extend_schema(
        operation_id="adminFacilityUpdate",
        tags=["Admin Facilities"],
        summary="Correct a facility's details",
        description=(
            "Only the fields sent change. The status is left as it is: an operator's "
            "correction does not send a live facility back for re-verification. Moving it to "
            "another province clears its city unless one is sent; another category clears its "
            "specialties and services unless they are sent. Requires `admin.facilities.edit`; "
            "audited with both snapshots, and the owners are notified."
        ),
        request=AdminFacilityWriteSerializer,
        responses={
            200: AdminFacilityDetailSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def patch(self, request: AuthenticatedRequest, facility_id: UUID) -> Response:
        require_permission(self, request, EDIT_PERMISSION)
        payload = AdminFacilityWriteSerializer(data=request.data, partial=True)
        payload.is_valid(raise_exception=True)
        data: dict[str, Any] = dict(payload.validated_data)
        try:
            update_facility(
                actor=request.user,
                facility_id=facility_id,
                data=data,
                request_id=_request_id(request),
            )
        except ObjectDoesNotExist as exc:
            raise NotFound() from exc
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        return _detail(facility_id)


# Enough for every facility of a launch province many times over; past it the map says so
# rather than drawing a sample that looks complete.
MAP_LIMIT = 5000


class FacilityMapView(AdminView):
    """Every located facility the filters select, as points (DECISION-075).

    The same filters as the list, so "the map of what I am looking at" is one click. Only what
    a pin needs travels: the name, the state and the coordinates. A facility without a location
    is counted rather than dropped silently, so the operator can go and fix it.
    """

    required_permission = "admin.facilities.read"

    @extend_schema(
        operation_id="adminFacilitiesMap",
        tags=["Admin Facilities"],
        summary="Located facilities as map points, with the same filters as the list",
        parameters=[
            _filter("status", "Facility status."),
            _filter("province", "Province id."),
            _filter("city", "City id."),
            _filter("category", "Category id."),
            _filter("q", "Free text matched against the facility names."),
            _filter("issue", f"One of {', '.join(QUALITY_ISSUES)}."),
        ],
        responses={200: AdminFacilityMapSerializer, 400: VALIDATION_400, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        qs = filtered_facilities(request.query_params)
        located = qs.exclude(location__isnull=True)
        points = [
            {
                "id": str(pk),
                "nameAr": name,
                "status": status,
                "categoryId": str(category_id),
                "latitude": round(point.y, 6),
                "longitude": round(point.x, 6),
            }
            for pk, name, status, category_id, point in located.values_list(
                "id", "name_ar", "status", "category_id", "location"
            )[: MAP_LIMIT + 1]
        ]
        return Response(
            {
                "items": points[:MAP_LIMIT],
                "truncated": len(points) > MAP_LIMIT,
                "withoutLocation": qs.filter(location__isnull=True).count(),
            }
        )
