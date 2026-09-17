from __future__ import annotations

from django.contrib.gis.db.models.functions import Distance
from django.contrib.gis.geos import Point, Polygon
from django.db.models import Avg, Count, Q

from facilities.models import Facility


def public_facilities():
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
    ).distinct()


def models_f(name):
    from django.db.models import F

    return F(name)


def with_rating_summary(queryset):
    return queryset.annotate(
        rating_average=Avg("ratings__stars"),
        rating_count=Count("ratings", distinct=True),
    )


def with_distance(queryset, latitude=None, longitude=None):
    if latitude is None or longitude is None:
        return queryset
    point = Point(float(longitude), float(latitude), srid=4326)
    return queryset.filter(location__isnull=False).annotate(
        distance=Distance("location", point)
    ).order_by("distance", "name_ar", "id")


def within_bbox(queryset, bbox: str | None):
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


def apply_text_search(queryset, term: str | None):
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
