from datetime import UTC

from django.shortcuts import get_object_or_404
from django.utils import timezone
from drf_spectacular.utils import OpenApiParameter, extend_schema
from rest_framework.exceptions import ValidationError
from rest_framework.response import Response
from rest_framework.views import APIView

from business_hours.query import filter_for_flags, with_availability_flags
from business_hours.services import get_facility_availability
from content_services.selectors import active_ads
from content_services.serializers import public_ad
from core.openapi import NOT_FOUND_404, VALIDATION_400
from directory.models import CategoryProvince
from locations.models import Province

from .pagination import FacilityCursorPagination
from .schemas import (
    FacilityCursorPageSerializer,
    MapMarkerListSerializer,
    PublicFacilityDetailSerializer,
    PublicHomeSerializer,
)
from .selectors import (
    apply_text_search,
    public_facilities,
    with_distance,
    with_favorite_state,
    with_rating_summary,
    within_bbox,
)
from .serializers import compact_facility, facility_detail


def _q(name, description, required=False):
    return OpenApiParameter(name, str, OpenApiParameter.QUERY, required=required,
                            description=description)


SCOPE_PARAMS = [
    _q("provinceId", "Province to scope the query to.", required=True),
    _q("cityId", "Optional city filter."),
    _q("neighborhoodId", "Optional neighbourhood filter."),
    _q("specialtyId", "Optional specialty filter; only meaningful when the category "
       "declares specialtyFilter."),
    _q("serviceId", "Optional service-tag filter; only meaningful when the category "
       "declares serviceFilter."),
    _q("search", "Free-text term matched against facility text."),
    _q("bbox", "Viewport as west,south,east,north in WGS84 decimal degrees."),
    _q("latitude", "Caller latitude in WGS84 decimal degrees. Must be sent with longitude."),
    _q("longitude", "Caller longitude in WGS84 decimal degrees. Must be sent with latitude."),
]

PAGE_PARAMS = [
    _q("cursor", "Opaque token returned as `nextCursor` by the previous page."),
    OpenApiParameter("limit", int, OpenApiParameter.QUERY, required=False,
                     description="Page size, maximum 100, default 30."),
]


def _parse_float(value, name):
    if value in (None, ""):
        return None
    try:
        return float(value)
    except (TypeError, ValueError) as exc:
        raise ValidationError({name: "Invalid number."}) from exc


def _base_from_params(params, user=None):
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


def _orders_by_distance(params):
    """Whether this request wants the nearest first.

    Coordinates alone used to decide it, which left a client no way to ask for the whole
    province by name while still being told how far each facility is. `sort` separates the two:
    the coordinates say what to measure, this says what to order by.
    """
    located = bool(params.get("latitude") and params.get("longitude"))
    requested = params.get("sort")
    if requested == "nearest":
        return located
    if requested == "name":
        return False
    return located


def _with_flags(params, queryset):
    """Apply the availability filters, and carry the flags on every row either way.

    The annotation is unconditional so that a row can say whether it is open and whether it is
    on today's roster without the serializer going back to the database once per facility.
    """
    return filter_for_flags(
        with_availability_flags(queryset),
        open_now=params.get("openNow") == "true",
        duty_today=params.get("dutyToday") == "true",
        duty_now=params.get("dutyNow") == "true",
    )





class PublicFacilityListView(APIView):
    @extend_schema(
        operation_id="publicFacilitiesList",
        tags=["Public Discovery"],
        summary="List publicly visible facilities in a province, optionally in one category",
        description=(
            "Ordered nearest-first when coordinates are supplied, otherwise by Arabic "
            "name. Availability is computed by the backend. The filters combine: openNow "
            "and dutyToday together mean facilities that are both, which is a different "
            "question from either alone."
        ),
        parameters=[
            *SCOPE_PARAMS,
            _q("categoryId", "Optional category to list. Absent means the whole province."),
            _q("openNow", "Pass true to keep only facilities open at this moment."),
            _q("dutyNow", "Pass true to keep only facilities whose duty shift is running."),
            _q("dutyToday", "Pass true to keep only facilities on today's duty roster."),
            _q(
                "sort",
                "nearest orders by distance and needs coordinates; name orders by Arabic "
                "name. Omitted keeps the historical behaviour: nearest whenever "
                "coordinates are supplied, name otherwise. Distances are returned "
                "whenever coordinates are supplied, whichever ordering is asked for.",
            ),
            *PAGE_PARAMS,
        ],
        responses={200: FacilityCursorPageSerializer, 400: VALIDATION_400},
    )
    def get(self, request):
        base = _base_from_params(request.query_params, request.user)
        queryset = _with_flags(request.query_params, base)
        paginator = FacilityCursorPagination()
        # The paginator owns the ordering: its cursor is built from it, so setting one on the
        # queryset as well would only give the two a chance to disagree.
        if _orders_by_distance(request.query_params):
            paginator.ordering = ("distance_meters", "id")
        page = paginator.paginate_queryset(queryset, request)
        return paginator.get_paginated_response([compact_facility(row) for row in page])


class PublicFacilityDetailView(APIView):
    @extend_schema(
        operation_id="publicFacilityRetrieve",
        tags=["Public Discovery"],
        summary="Retrieve one publicly visible facility",
        description=(
            "Returns the public projection only. Verification evidence, reviewer notes, "
            "memberships, internal policy fields and raw storage keys are never included."
        ),
        responses={200: PublicFacilityDetailSerializer, 404: NOT_FOUND_404},
    )
    def get(self, request, facility_id):
        queryset = with_availability_flags(
            with_favorite_state(with_rating_summary(public_facilities()), request.user)
        )
        facility = get_object_or_404(queryset, pk=facility_id)
        return Response(facility_detail(facility))


class PublicMapFacilitiesView(APIView):
    @extend_schema(
        operation_id="publicMapFacilitiesList",
        tags=["Public Discovery"],
        summary="List compact map markers inside a viewport",
        description=(
            "Capped at 500 markers. Facilities without coordinates are omitted. The filters "
            "behave exactly as they do on the list endpoint and combine the same way, so a "
            "map and a list asked the same question answer the same."
        ),
        parameters=[
            *SCOPE_PARAMS,
            _q("categoryId", "Optional category filter."),
            _q("openNow", "Pass true to keep only facilities open at this moment."),
            _q("dutyNow", "Pass true to keep only facilities whose duty shift is running."),
            _q("dutyToday", "Pass true to keep only facilities on today's duty roster."),
        ],
        responses={200: MapMarkerListSerializer, 400: VALIDATION_400},
    )
    def get(self, request):
        if not request.query_params.get("bbox"):
            raise ValidationError({"bbox": "Required for map queries."})
        base = _base_from_params(request.query_params, request.user)
        queryset = _with_flags(request.query_params, base)
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
    @extend_schema(
        operation_id="publicSearchList",
        tags=["Public Discovery"],
        summary="Search facilities within a province",
        parameters=[
            *SCOPE_PARAMS,
            _q("categoryId", "Optional category filter."),
            _q("q", "Search term, at least two characters.", required=True),
            *PAGE_PARAMS,
        ],
        responses={200: FacilityCursorPageSerializer, 400: VALIDATION_400},
    )
    def get(self, request):
        term = (request.query_params.get("q") or "").strip()
        if len(term) < 2:
            raise ValidationError({"q": "At least 2 characters are required."})
        queryset = _base_from_params(request.query_params, request.user)
        queryset = apply_text_search(queryset, term)
        paginator = FacilityCursorPagination()
        page = paginator.paginate_queryset(queryset, request)
        return paginator.get_paginated_response([compact_facility(row) for row in page])


class PublicHomeView(APIView):
    @extend_schema(
        operation_id="publicHomeRetrieve",
        tags=["Public Discovery"],
        summary="Retrieve the home composition for a province",
        description=(
            "Bundles advertisements, the active category grid and three facility strips "
            "so the first screen needs one round trip. serverTime is authoritative."
        ),
        parameters=[*SCOPE_PARAMS, _q("categoryId", "Optional category filter.")],
        responses={200: PublicHomeSerializer, 400: VALIDATION_400, 404: NOT_FOUND_404},
    )
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
        base = with_availability_flags(_base_from_params(base_params, request.user))
        nearby = list(base[:10])
        # Open means open, whether or not a duty shift happens to be running: the two are
        # independent questions and a row now carries both answers.
        open_nearby = list(filter_for_flags(base, open_now=True)[:10])
        duty_now = list(filter_for_flags(base, duty_now=True)[:10])
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
                "ads": [public_ad(row) for row in active_ads(province_id=province_id)[:20]],
                "categories": category_items,
                "nearby": [compact_facility(row) for row in nearby],
                "openNearby": [compact_facility(row) for row in open_nearby],
                "dutyNow": [compact_facility(row) for row in duty_now],
                "serverTime": timezone.now().astimezone(UTC).isoformat(),
            }
        )
