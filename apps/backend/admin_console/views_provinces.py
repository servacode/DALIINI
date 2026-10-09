"""Provinces and their cities, as the console manages them."""

from typing import Any
from uuid import UUID

from django.db.models import Count, Q
from django.shortcuts import get_object_or_404
from drf_spectacular.utils import (
    extend_schema,
)
from rest_framework.response import Response

from accounts.authentication import AuthenticatedRequest
from audit.services import record_audit
from core.openapi import NOT_FOUND_404, VALIDATION_400, protected
from facilities.models import Facility
from locations.models import City, Province

from .schemas import (
    AdminCityAdminListSerializer,
    AdminCityAdminSerializer,
    AdminCityUpdateRequestSerializer,
    AdminProvinceCardSerializer,
    AdminProvinceListSerializer,
    AdminProvinceUpdatedSerializer,
    AdminProvinceUpdateRequestSerializer,
)
from .views import (
    AdminView,
    _request_id,
)


class ProvinceListView(AdminView):
    required_permission = "admin.provinces.read"

    @extend_schema(
        operation_id="adminProvincesList",
        tags=["Admin Provinces"],
        summary="List every province",
        responses={200: AdminProvinceListSerializer, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        # Counted apart and joined in Python: two counts over two relations in one query
        # would multiply each other.
        rows = Province.objects.order_by("sort_order", "name_ar").annotate(
            city_count=Count("cities"),
            active_city_count=Count("cities", filter=Q(cities__active=True)),
        )
        facilities = dict(
            Facility.objects.filter(status=Facility.Status.ACTIVE)
            .values_list("province_id")
            .annotate(n=Count("id"))
            .values_list("province_id", "n")
        )
        items = [
            {
                "id": row.pk,
                "code": row.code,
                "name_ar": row.name_ar,
                "name_en": row.name_en,
                "active": row.active,
                "sort_order": row.sort_order,
                "city_count": row.city_count,
                "active_city_count": row.active_city_count,
                "active_facility_count": facilities.get(row.pk, 0),
            }
            for row in rows
        ]
        return Response({"items": AdminProvinceCardSerializer(items, many=True).data})


class ProvinceDetailView(AdminView):
    required_permission = "admin.provinces.manage"

    @extend_schema(
        operation_id="adminProvinceUpdate",
        tags=["Admin Provinces"],
        summary="Activate a province or change its order",
        request=AdminProvinceUpdateRequestSerializer,
        responses={200: AdminProvinceUpdatedSerializer, **protected(), 404: NOT_FOUND_404},
    )
    def put(self, request: AuthenticatedRequest, province_id: UUID) -> Response:
        province = get_object_or_404(Province, pk=province_id)
        before = {"active": province.active, "sortOrder": province.sort_order}
        if "active" in request.data:
            province.active = bool(request.data["active"])
        if "sortOrder" in request.data:
            province.sort_order = int(request.data["sortOrder"])
        province.save(update_fields=["active", "sort_order"])
        record_audit(
            actor=request.user,
            action="province.updated",
            target=province,
            before_snapshot=before,
            after_snapshot={"active": province.active, "sortOrder": province.sort_order},
            request_id=_request_id(request),
        )
        return Response({"active": province.active, "sortOrder": province.sort_order})


def _city_payload(city: Any) -> dict[str, Any]:
    return {
        "id": str(city.pk),
        "code": city.code,
        "nameAr": city.name_ar,
        "nameEn": city.name_en or None,
        "active": city.active,
    }


class ProvinceCityListView(AdminView):
    required_permission = "admin.provinces.read"

    @extend_schema(
        operation_id="adminProvinceCitiesList",
        tags=["Admin Provinces"],
        summary="List every city of a province, active or not",
        responses={200: AdminCityAdminListSerializer, **protected(), 404: NOT_FOUND_404},
    )
    def get(self, request: Any, province_id: Any) -> Response:
        province = get_object_or_404(Province, pk=province_id)
        cities = City.objects.filter(province=province).order_by("name_ar")
        return Response({"items": [_city_payload(city) for city in cities]})


class ProvinceCityDetailView(AdminView):
    required_permission = "admin.provinces.manage"

    @extend_schema(
        operation_id="adminProvinceCityUpdate",
        tags=["Admin Provinces"],
        summary="Activate or deactivate a city",
        request=AdminCityUpdateRequestSerializer,
        responses={
            200: AdminCityAdminSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def put(self, request: Any, province_id: Any, city_id: Any) -> Response:
        payload = AdminCityUpdateRequestSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        city = get_object_or_404(City, pk=city_id, province_id=province_id)
        before = {"active": city.active}
        city.active = payload.validated_data["active"]
        city.save(update_fields=["active"])
        record_audit(
            actor=request.user,
            action="city.updated",
            target=city,
            before_snapshot=before,
            after_snapshot={"active": city.active},
            request_id=_request_id(request),
        )
        return Response(_city_payload(city))
