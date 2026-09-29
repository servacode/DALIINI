from __future__ import annotations

from typing import TYPE_CHECKING, cast

from django.contrib.gis.db.models.functions import Distance
from django.contrib.gis.geos import Point, Polygon
from django.db.models import (
    Avg,
    Count,
    Exists,
    ExpressionWrapper,
    FloatField,
    OuterRef,
    Prefetch,
    Q,
)

from facilities.models import Facility, FacilityImage

if TYPE_CHECKING:
    from django.contrib.auth.models import AnonymousUser
    from django.db.models import F, QuerySet

    from accounts.models import User


def public_facilities() -> QuerySet[Facility]:
    return Facility.objects.filter(
        status=Facility.Status.ACTIVE,
        province__active=True,
        category__active=True,
        category__province_switches__province_id=models_f("province_id"),
        category__province_switches__public_enabled=True,
    ).select_related(
        "province",
        "city",
        "neighborhood",
        "category",
        "category__capabilities",
    ).prefetch_related(
        # A list row shows one photograph. Prefetching in the owner's own order means the
        # whole page's pictures cost one query instead of one per facility.
        Prefetch(
            "images",
            queryset=FacilityImage.objects.order_by("sort_order", "created_at"),
        ),
    ).distinct()


def models_f(name: str) -> F:
    from django.db.models import F

    return F(name)


def with_favorite_state(
    queryset: QuerySet[Facility], user: User | AnonymousUser | None
) -> QuerySet[Facility]:
    """Whether the caller has saved each facility, as one subquery for the whole page.

    Anonymous callers get nothing annotated and the presenter reads false, so a public
    response never depends on who is asking beyond this one flag.
    """
    if user is None or not getattr(user, "is_authenticated", False):
        return queryset
    from favorites.models import Favorite

    saved = Favorite.objects.filter(user=cast("User", user), facility=OuterRef("pk"))
    return queryset.annotate(is_favorite=Exists(saved))


def with_rating_summary(queryset: QuerySet[Facility]) -> QuerySet[Facility]:
    return queryset.annotate(
        rating_average=Avg("ratings__stars"),
        rating_count=Count("ratings", distinct=True),
    )


def with_distance(
    queryset: QuerySet[Facility], latitude: float | None = None, longitude: float | None = None
) -> QuerySet[Facility]:
    if latitude is None or longitude is None:
        return queryset
    point = Point(float(longitude), float(latitude), srid=4326)
    # A plain float, in metres, rather than GeoDjango's Distance measure. The cursor paginator
    # stores the ordering value of the last row as text and filters on it for the next page;
    # a measure serialises as "123.4 m", which the database cannot compare, so the second page
    # of any nearest-first list failed with a 500 (INT-058).
    return queryset.filter(location__isnull=False).annotate(
        distance_meters=ExpressionWrapper(Distance("location", point), output_field=FloatField())
    ).order_by("distance_meters", "name_ar", "id")


def within_bbox(queryset: QuerySet[Facility], bbox: str | None) -> QuerySet[Facility]:
    if not bbox:
        return queryset
    parts = [float(value) for value in bbox.split(",")]
    if len(parts) != 4:
        raise ValueError("bbox must be minLon,minLat,maxLon,maxLat")
    min_lon, min_lat, max_lon, max_lat = parts
    if min_lon >= max_lon or min_lat >= max_lat:
        raise ValueError("bbox bounds are invalid")
    polygon = Polygon.from_bbox((min_lon, min_lat, max_lon, max_lat))
    polygon.srid = 4326
    return queryset.filter(location__within=polygon)


def apply_text_search(queryset: QuerySet[Facility], term: str | None) -> QuerySet[Facility]:
    if not term:
        return queryset
    normalized = term.strip()
    if not normalized:
        return queryset
    return queryset.filter(
        Q(name_ar__icontains=normalized)
        | Q(name_en__icontains=normalized)
        | Q(address_ar__icontains=normalized)
        | Q(address_en__icontains=normalized)
        | Q(city__name_ar__icontains=normalized)
        | Q(neighborhood__name_ar__icontains=normalized)
        | Q(category__name_ar__icontains=normalized)
        | Q(specialty_links__specialty__name_ar__icontains=normalized)
        | Q(service_links__service_tag__name_ar__icontains=normalized)
    ).distinct()
