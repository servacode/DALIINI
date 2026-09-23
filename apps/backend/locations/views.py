from django.shortcuts import get_object_or_404
from drf_spectacular.utils import OpenApiParameter, extend_schema
from rest_framework.exceptions import ValidationError
from rest_framework.request import Request
from rest_framework.response import Response
from rest_framework.views import APIView

from core.openapi import NOT_FOUND_404, VALIDATION_400

from .models import Province
from .presenters import map_center, place, province_payload
from .resolver import resolve
from .schemas import (
    PublicCityListSerializer,
    PublicLocationResolveSerializer,
    PublicProvinceListSerializer,
)


class PublicProvinceListView(APIView):
    authentication_classes = []
    permission_classes = []

    @extend_schema(
        operation_id="publicProvincesList",
        tags=["Public Taxonomy"],
        summary="List active provinces",
        description=(
            "Every province is seeded, but only active ones are publicly visible. "
            "Ordered by sort order then Arabic name."
        ),
        responses={200: PublicProvinceListSerializer},
    )
    def get(self, request):
        items = [
            {
                "id": str(province.id),
                "code": province.code,
                "nameAr": province.name_ar,
                "nameEn": province.name_en or None,
                "mapCenter": map_center(province),
            }
            for province in Province.objects.filter(active=True).order_by(
                "sort_order",
                "name_ar",
            )
        ]
        return Response({"items": items})


class PublicProvinceCitiesView(APIView):
    authentication_classes = []
    permission_classes = []

    @extend_schema(
        operation_id="publicProvinceCitiesList",
        tags=["Public Taxonomy"],
        summary="List active cities in a province",
        responses={200: PublicCityListSerializer, 404: NOT_FOUND_404},
    )
    def get(self, request, province_id):
        province = get_object_or_404(Province.objects.filter(active=True), pk=province_id)
        items = [
            {
                "id": str(city.id),
                "code": city.code,
                "nameAr": city.name_ar,
                "nameEn": city.name_en or None,
            }
            for city in province.cities.filter(active=True).order_by("name_ar")
        ]
        return Response({"items": items})


class PublicLocationResolveView(APIView):
    """Turn a coordinate into the place this platform calls it.

    The app asks once, when it has a position, so that a first-time user is not made to pick
    a province from a list before seeing anything. A point outside every province the
    platform serves is not an error: the answer is simply empty, and the app falls back to
    whatever province it already had.
    """

    authentication_classes = []
    permission_classes = []

    @extend_schema(
        operation_id="publicLocationResolve",
        tags=["Public Taxonomy"],
        summary="Resolve a coordinate to a province, city and neighbourhood",
        description=(
            "Point-in-polygon against the seeded city and neighbourhood boundaries, then the "
            "nearest active province centre within 200 km. No external geocoder is called and "
            "the coordinate is not stored."
        ),
        parameters=[
            OpenApiParameter("latitude", float, OpenApiParameter.QUERY, required=True),
            OpenApiParameter("longitude", float, OpenApiParameter.QUERY, required=True),
        ],
        responses={200: PublicLocationResolveSerializer, 400: VALIDATION_400},
    )
    def get(self, request: Request) -> Response:
        latitude = _coordinate(request, "latitude", -90.0, 90.0)
        longitude = _coordinate(request, "longitude", -180.0, 180.0)
        found = resolve(latitude, longitude)
        return Response(
            {
                "province": province_payload(found.province),
                "city": place(found.city),
                "neighborhood": place(found.neighborhood),
                "label": found.label_ar,
                "resolvedBy": found.resolved_by,
            }
        )


def _coordinate(request: Request, name: str, low: float, high: float) -> float:
    raw = request.query_params.get(name)
    if raw is None:
        raise ValidationError({name: "This query parameter is required."})
    try:
        value = float(raw)
    except (TypeError, ValueError):
        raise ValidationError({name: "Must be a number."}) from None
    if not low <= value <= high:
        raise ValidationError({name: f"Must be between {low} and {high}."})
    return value
