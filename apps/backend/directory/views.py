from django.shortcuts import get_object_or_404
from drf_spectacular.utils import extend_schema
from rest_framework.response import Response
from rest_framework.views import APIView

from core.openapi import NOT_FOUND_404
from locations.models import Province

from .models import CategoryProvince
from .schemas import PublicCategoryListSerializer


def _capabilities(category):
    caps = category.capabilities
    return {
        "hours": caps.supports_hours,
        "photos": caps.supports_photos,
        "ratings": caps.supports_ratings,
        "duty": caps.supports_duty,
        "specialtyFilter": caps.supports_specialty_filter,
        "serviceFilter": caps.supports_service_filter,
        "temporaryClosure": caps.supports_temporary_closure,
        "ownerOnboarding": caps.supports_owner_onboarding,
    }


class PublicProvinceCategoriesView(APIView):
    authentication_classes = []
    permission_classes = []

    @extend_schema(
        operation_id="publicProvinceCategoriesList",
        tags=["Public Taxonomy"],
        summary="List categories publicly enabled for a province",
        description=(
            "A category is listed only when the province is active, the group and the "
            "category are active, and the per-province public switch is on. Clients drive "
            "their UI from the returned capability flags, never from the category name."
        ),
        responses={200: PublicCategoryListSerializer, 404: NOT_FOUND_404},
    )
    def get(self, request, province_id):
        get_object_or_404(Province.objects.filter(active=True), pk=province_id)
        switches = CategoryProvince.objects.filter(
            province_id=province_id,
            public_enabled=True,
            category__active=True,
            category__group__active=True,
        ).select_related(
            "category",
            "category__group",
            "category__capabilities",
        ).order_by("sort_order", "category__sort_order")
        items = [
            {
                "id": str(switch.category_id),
                "nameAr": switch.category.name_ar,
                "nameEn": switch.category.name_en or None,
                "iconKey": switch.category.icon_key or None,
                "group": {
                    "id": str(switch.category.group_id),
                    "nameAr": switch.category.group.name_ar,
                },
                "capabilities": _capabilities(switch.category),
            }
            for switch in switches
        ]
        return Response({"items": items})
