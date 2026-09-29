from uuid import UUID

from django.db.models import Exists, OuterRef, QuerySet
from django.shortcuts import get_object_or_404
from drf_spectacular.utils import extend_schema
from rest_framework.request import Request
from rest_framework.response import Response
from rest_framework.views import APIView

from core.openapi import NOT_FOUND_404
from locations.models import Province

from .models import Category, CategoryProvince
from .presenters import category_capabilities
from .schemas import PublicCategoryListSerializer, PublicCategoryTagsSerializer
from .tags import category_tag_choices

#: Taxonomy changes rarely and is the same for everyone, as the public content reads are.
PUBLIC_CACHE = "public, max-age=300"


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
    def get(self, request: Request, province_id: UUID) -> Response:
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
                "capabilities": category_capabilities(switch.category),
            }
            for switch in switches
        ]
        return Response({"items": items})


def public_categories() -> QuerySet[Category]:
    """Categories the public can see somewhere: the rule of the province list, any province.

    Active, in an active group, and switched on for the public in at least one active
    province. Anything else answers 404, as an unknown category would.
    """
    switched_on = CategoryProvince.objects.filter(
        category=OuterRef("pk"), public_enabled=True, province__active=True
    )
    return Category.objects.filter(Exists(switched_on), active=True, group__active=True)


class PublicCategoryTagsView(APIView):
    authentication_classes = []
    permission_classes = []

    @extend_schema(
        operation_id="publicCategoryTagsRetrieve",
        tags=["Public Taxonomy"],
        summary="List the specialties and services a category offers",
        description=(
            "The choices behind the specialty and service filters: active items only, in "
            "the operators' order. Specialties are the category's own plus those shared by "
            "its specialization. Whether to offer each filter is still decided by the "
            "category's `specialtyFilter` and `serviceFilter` capabilities. A category that "
            "is not public in any province is 404. Cacheable for five minutes "
            "(`Cache-Control: public, max-age=300`)."
        ),
        responses={200: PublicCategoryTagsSerializer, 404: NOT_FOUND_404},
    )
    def get(self, request: Request, category_id: UUID) -> Response:
        category = get_object_or_404(public_categories(), pk=category_id)
        response = Response(category_tag_choices(category))
        response["Cache-Control"] = PUBLIC_CACHE
        return response
