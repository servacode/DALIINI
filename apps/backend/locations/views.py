from django.shortcuts import get_object_or_404
from drf_spectacular.utils import extend_schema
from rest_framework.response import Response
from rest_framework.views import APIView

from core.openapi import NOT_FOUND_404

from .models import Province
from .presenters import map_center
from .schemas import PublicCityListSerializer, PublicProvinceListSerializer


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
