"""Response contract for public discovery.

The cursor envelope described here is DRF's `CursorPagination` default,
`{next, previous, results}`, because that is what the runtime returns.
`08-API-CONTRACT.md` specifies `{items, nextCursor, hasMore}` instead. The divergence is
recorded as a defect rather than hidden by documenting an envelope nobody emits.
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


class FacilityImageSerializer(serializers.Serializer):
    id = serializers.UUIDField()
    url = serializers.CharField(
        help_text="Service-issued media route. Raw object storage keys are never returned."
    )


class PublicHoursEntrySerializer(serializers.Serializer):
    weekday = serializers.IntegerField(min_value=0, max_value=6)
    opensAt = serializers.TimeField()
    closesAt = serializers.TimeField()


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
    """DRF cursor pagination envelope as emitted by `FacilityCursorPagination`."""

    next = serializers.CharField(
        allow_null=True, help_text="Absolute URL of the next page, or null on the last page."
    )
    previous = serializers.CharField(allow_null=True)
    results = CompactFacilitySerializer(many=True)


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
