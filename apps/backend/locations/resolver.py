"""Where a coordinate is, in this platform's own words.

The app should not make a user pick their province from a list when the phone already knows
where it is. This resolves a point against the geography the platform itself holds — the
neighbourhood and city boundaries seeded for each province — and falls back to the nearest
province centre when the point is outside every boundary the platform knows.

Nothing here calls an external geocoder. The answer is the platform's own taxonomy, which is
what every list, filter and map in the app is scoped by, so the name the user reads is the
same name the data is organised under.
"""

from dataclasses import dataclass

from django.contrib.gis.db.models.functions import Distance
from django.contrib.gis.geos import Point
from django.contrib.gis.measure import D

from .models import City, Neighborhood, Province

#: How far from a province centre a point may be and still be called that province. Syria's
#: largest province is roughly 250 km across, so a point beyond this is not in any province
#: the platform serves rather than merely far from its centre.
NEAREST_PROVINCE_LIMIT_KM = 200


class ResolvedBy:
    """How the answer was arrived at, so the client can tell a fact from an estimate."""

    BOUNDARY = "BOUNDARY"
    NEAREST_PROVINCE = "NEAREST_PROVINCE"
    NONE = "NONE"


@dataclass(frozen=True)
class ResolvedLocation:
    province: Province | None
    city: City | None
    neighborhood: Neighborhood | None
    resolved_by: str

    @property
    def label_ar(self) -> str | None:
        """What the user reads: the province, and the finer place when one is known."""
        if self.province is None:
            return None
        finer = self.neighborhood.name_ar if self.neighborhood else None
        if finer is None and self.city is not None and self.city.name_ar != self.province.name_ar:
            finer = self.city.name_ar
        return f"{self.province.name_ar} — {finer}" if finer else self.province.name_ar


def resolve(latitude: float, longitude: float) -> ResolvedLocation:
    """The place a coordinate falls in, as precisely as the seeded geography allows."""
    point = Point(longitude, latitude, srid=4326)

    neighborhood = (
        Neighborhood.objects.filter(
            active=True,
            city__active=True,
            city__province__active=True,
            boundary__contains=point,
        )
        .select_related("city", "city__province")
        .first()
    )
    if neighborhood is not None:
        return ResolvedLocation(
            province=neighborhood.city.province,
            city=neighborhood.city,
            neighborhood=neighborhood,
            resolved_by=ResolvedBy.BOUNDARY,
        )

    city = (
        City.objects.filter(active=True, province__active=True, boundary__contains=point)
        .select_related("province")
        .first()
    )
    if city is not None:
        return ResolvedLocation(
            province=city.province,
            city=city,
            neighborhood=None,
            resolved_by=ResolvedBy.BOUNDARY,
        )

    province = (
        Province.objects.filter(
            active=True,
            map_center__isnull=False,
            map_center__distance_lte=(point, D(km=NEAREST_PROVINCE_LIMIT_KM)),
        )
        .annotate(distance=Distance("map_center", point))
        .order_by("distance")
        .first()
    )
    if province is not None:
        return ResolvedLocation(
            province=province,
            city=None,
            neighborhood=None,
            resolved_by=ResolvedBy.NEAREST_PROVINCE,
        )

    return ResolvedLocation(
        province=None, city=None, neighborhood=None, resolved_by=ResolvedBy.NONE
    )
