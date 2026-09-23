"""Response contract for public discovery.

Listings use the cursor envelope of `08-API-CONTRACT.md`, `{items, nextCursor, hasMore}`,
produced centrally by `core.pagination.CursorPage`.
"""

from rest_framework import serializers

from business_hours.services import AvailabilityState
from content_services.schemas import PublicAdvertisementSerializer
from core.openapi import BilingualRefSerializer, CoordinatesSerializer, NamedRefSerializer


class AvailabilitySerializer(serializers.Serializer):
    """Computed server-side by the availability engine; clients must not recompute it."""

    state = serializers.ChoiceField(choices=[s.value for s in AvailabilityState])
    nextOpenAt = serializers.DateTimeField(allow_null=True)


class CompactFacilitySerializer(serializers.Serializer):
    id = serializers.UUIDField()
    nameAr = serializers.CharField()
    nameEn = serializers.CharField(allow_null=True)
    category = BilingualRefSerializer()
    city = NamedRefSerializer(allow_null=True)
    distanceMeters = serializers.FloatField(
        allow_null=True,
        help_text="Great-circle distance in metres from the supplied coordinates. "
        "Null when no coordinates were supplied; clients never compute it locally.",
    )
    ratingAverage = serializers.FloatField(allow_null=True)
    ratingCount = serializers.IntegerField()
    availability = AvailabilitySerializer()
    isFavorite = serializers.BooleanField(
        help_text=(
            "Whether the caller has saved this facility. False for anonymous callers; "
            "resolved for a whole page in one subquery."
        ),
    )


class FacilityImageSerializer(serializers.Serializer):
    id = serializers.UUIDField()
    url = serializers.CharField(
        help_text="Service-issued media route. Raw object storage keys are never returned."
    )


class PublicHoursEntrySerializer(serializers.Serializer):
    # Public detail shares `serialize_hours` with the owner API, so it carries the
    # same four keys plus the row id. The schema said otherwise before this batch.
    id = serializers.UUIDField()
    weekday = serializers.IntegerField(min_value=0, max_value=6)
    opensAt = serializers.TimeField()
    closesAt = serializers.TimeField()
    sequence = serializers.IntegerField()


class PublicFacilityDetailSerializer(CompactFacilitySerializer):
    descriptionAr = serializers.CharField(allow_null=True)
    descriptionEn = serializers.CharField(allow_null=True)
    phone = serializers.CharField(allow_null=True)
    addressAr = serializers.CharField(allow_null=True)
    addressEn = serializers.CharField(allow_null=True)
    neighborhood = NamedRefSerializer(allow_null=True)
    location = CoordinatesSerializer(allow_null=True)
    images = FacilityImageSerializer(many=True)
    specialties = NamedRefSerializer(many=True)
    services = NamedRefSerializer(many=True)
    hours = PublicHoursEntrySerializer(many=True)


class FacilityCursorPageSerializer(serializers.Serializer):
    """The cursor envelope of `08-API-CONTRACT.md`, emitted by `core.pagination.CursorPage`."""

    items = CompactFacilitySerializer(many=True)
    nextCursor = serializers.CharField(
        allow_null=True,
        help_text=(
            "Opaque token for the next page, or null on the last page. Send it back "
            "unchanged as the `cursor` query parameter; never parse it."
        ),
    )
    hasMore = serializers.BooleanField(help_text="True when `nextCursor` is set.")


class MapMarkerSerializer(serializers.Serializer):
    id = serializers.UUIDField()
    nameAr = serializers.CharField()
    latitude = serializers.FloatField()
    longitude = serializers.FloatField()
    availability = serializers.ChoiceField(choices=[s.value for s in AvailabilityState])


class MapMarkerListSerializer(serializers.Serializer):
    items = MapMarkerSerializer(many=True)


class HomeCategoryCapabilitiesSerializer(serializers.Serializer):
    """The home screen carries a reduced capability set compared with the category list."""

    hours = serializers.BooleanField()
    ratings = serializers.BooleanField()
    duty = serializers.BooleanField()
    specialtyFilter = serializers.BooleanField()
    serviceFilter = serializers.BooleanField()


class HomeCategorySerializer(serializers.Serializer):
    id = serializers.UUIDField()
    nameAr = serializers.CharField()
    nameEn = serializers.CharField(allow_null=True)
    iconKey = serializers.CharField(allow_null=True)
    capabilities = HomeCategoryCapabilitiesSerializer()


class PublicHomeSerializer(serializers.Serializer):
    ads = PublicAdvertisementSerializer(many=True)
    categories = HomeCategorySerializer(many=True)
    nearby = CompactFacilitySerializer(many=True)
    openNearby = CompactFacilitySerializer(many=True)
    dutyNow = CompactFacilitySerializer(many=True)
    serverTime = serializers.DateTimeField(
        help_text="Authoritative server time in UTC, so clients do not trust the device clock."
    )
