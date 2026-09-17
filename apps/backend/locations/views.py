from django.shortcuts import get_object_or_404
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import Province


class PublicProvinceListView(APIView):
    authentication_classes = []
    permission_classes = []

    def get(self, request):
        items = [
            {
                "id": str(province.id),
                "code": province.code,
                "nameAr": province.name_ar,
                "nameEn": province.name_en or None,
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
