from datetime import UTC

from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework.exceptions import ValidationError
from rest_framework.response import Response
from rest_framework.views import APIView

from business_hours.query import filter_for_availability_state
from business_hours.services import AvailabilityState, get_facility_availability
from directory.models import CategoryProvince
from locations.models import Province

from .pagination import FacilityCursorPagination
from .selectors import (
    apply_text_search,
    public_facilities,
    with_distance,
    with_rating_summary,
    within_bbox,
)
from .serializers import compact_facility, facility_detail


def _parse_float(value, name):
    if value in (None, ""):
        return None
    try:
        return float(value)
    except (TypeError, ValueError) as exc:
        raise ValidationError({name: "Invalid number."}) from exc


def _base_from_params(params):
    province_id = params.get("provinceId")
    category_id = params.get("categoryId")
    if not province_id:
        raise ValidationError({"provinceId": "Required."})
    queryset = public_facilities().filter(province_id=province_id)
    if category_id:
        queryset = queryset.filter(category_id=category_id)
    if params.get("cityId"):
        queryset = queryset.filter(city_id=params["cityId"])
    if params.get("neighborhoodId"):
        queryset = queryset.filter(neighborhood_id=params["neighborhoodId"])
    if params.get("specialtyId"):
        queryset = queryset.filter(specialty_links__specialty_id=params["specialtyId"])
    if params.get("serviceId"):
        queryset = queryset.filter(service_links__service_tag_id=params["serviceId"])
    queryset = apply_text_search(queryset, params.get("search"))
    try:
        queryset = within_bbox(queryset, params.get("bbox"))
    except (TypeError, ValueError) as exc:
        raise ValidationError({"bbox": str(exc)}) from exc
    latitude = _parse_float(params.get("latitude"), "latitude")
    longitude = _parse_float(params.get("longitude"), "longitude")
    if (latitude is None) != (longitude is None):
        raise ValidationError("latitude and longitude must be supplied together.")
    if latitude is not None and not (-90 <= latitude <= 90):
        raise ValidationError({"latitude": "Out of range."})
    if longitude is not None and not (-180 <= longitude <= 180):
        raise ValidationError({"longitude": "Out of range."})
    queryset = with_distance(queryset, latitude, longitude)
    return with_rating_summary(queryset)





class PublicFacilityListView(APIView):
    def get(self, request):
        if not request.query_params.get("categoryId"):
            raise ValidationError({"categoryId": "Required."})
        queryset = _base_from_params(request.query_params)
        if request.query_params.get("openNow") == "true":
            queryset = filter_for_availability_state(queryset, AvailabilityState.OPEN)
        if request.query_params.get("dutyNow") == "true":
            queryset = filter_for_availability_state(queryset, AvailabilityState.DUTY)
        paginator = FacilityCursorPagination()
        if request.query_params.get("latitude") and request.query_params.get("longitude"):
            paginator.ordering = ("distance", "id")
        page = paginator.paginate_queryset(queryset, request)
        return paginator.get_paginated_response([compact_facility(row) for row in page])


class PublicFacilityDetailView(APIView):
    def get(self, request, facility_id):
        queryset = with_rating_summary(public_facilities())
        facility = get_object_or_404(queryset, pk=facility_id)
        return Response(facility_detail(facility))


class PublicMapFacilitiesView(APIView):
    def get(self, request):
        queryset = _base_from_params(request.query_params)
        if not request.query_params.get("bbox"):
            raise ValidationError({"bbox": "Required for map queries."})
        markers = []
        for facility in queryset[:500]:
            if not facility.location:
                continue
            markers.append(
                {
                    "id": str(facility.id),
                    "nameAr": facility.name_ar,
                    "latitude": facility.location.y,
                    "longitude": facility.location.x,
                    "availability": get_facility_availability(facility).state.value,
                }
            )
        return Response({"items": markers})


class PublicSearchView(APIView):
    def get(self, request):
        term = (request.query_params.get("q") or "").strip()
        if len(term) < 2:
            raise ValidationError({"q": "At least 2 characters are required."})
        queryset = _base_from_params(request.query_params)
        queryset = apply_text_search(queryset, term)
        paginator = FacilityCursorPagination()
        page = paginator.paginate_queryset(queryset, request)
        return paginator.get_paginated_response([compact_facility(row) for row in page])


class PublicHomeView(APIView):
    def get(self, request):
        province_id = request.query_params.get("provinceId")
        if not province_id:
            raise ValidationError({"provinceId": "Required."})
        get_object_or_404(Province.objects.filter(active=True), pk=province_id)
        categories = CategoryProvince.objects.filter(
            province_id=province_id,
            province__active=True,
            public_enabled=True,
            category__active=True,
        ).select_related("category", "category__capabilities").order_by(
            "sort_order",
            "category__sort_order",
        )
        base_params = request.query_params.copy()
        base_params["provinceId"] = province_id
        base = _base_from_params(base_params)
        nearby = list(base[:10])
        open_nearby = list(filter_for_availability_state(base, AvailabilityState.OPEN)[:10])
        duty_now = list(filter_for_availability_state(base, AvailabilityState.DUTY)[:10])
        category_items = []
        for switch in categories:
            category = switch.category
            caps = category.capabilities
            category_items.append(
                {
                    "id": str(category.id),
                    "nameAr": category.name_ar,
                    "nameEn": category.name_en or None,
                    "iconKey": category.icon_key or None,
                    "capabilities": {
                        "hours": caps.supports_hours,
                        "ratings": caps.supports_ratings,
                        "duty": caps.supports_duty,
                        "specialtyFilter": caps.supports_specialty_filter,
                        "serviceFilter": caps.supports_service_filter,
                    },
                }
            )
        return Response(
            {
                "ads": [],
                "categories": category_items,
                "nearby": [compact_facility(row) for row in nearby],
                "openNearby": [compact_facility(row) for row in open_nearby],
                "dutyNow": [compact_facility(row) for row in duty_now],
                "serverTime": timezone.now().astimezone(UTC).isoformat(),
            }
        )
