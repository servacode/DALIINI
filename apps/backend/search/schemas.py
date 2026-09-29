"""Response contract for public discovery.

Listings use the cursor envelope of `08-API-CONTRACT.md`, `{items, nextCursor, hasMore}`,
produced centrally by `core.pagination.CursorPage`.
"""

from typing import Any

from rest_framework import serializers

from business_hours.services import AvailabilityState
from content_services.schemas import PublicAdvertisementSerializer
from core.openapi import BilingualRefSerializer, CoordinatesSerializer, NamedRefSerializer


class AvailabilitySerializer(serializers.Serializer[Any]):
    """Computed server-side by the availability engine; clients must not recompute it."""

    state = serializers.ChoiceField(choices=[s.value for s in AvailabilityState])
    nextOpenAt = serializers.DateTimeField(allow_null=True)
    isOpenNow = serializers.BooleanField(
        help_text=(
            "Whether the doors are open at this moment, by the facility's own business "
            "hours and temporary closures. Independent of duty: unlike `state`, which "
            "collapses both into one value and lets DUTY win, this stays true for a "
            "facility that is open while its duty shift runs."
        ),
    )
    isOnDutyToday = serializers.BooleanField(
        help_text=(
            "Whether the facility appears on today's duty roster, taking today to be the "
            "local day in Asia/Damascus. A different question from being open: a pharmacy "
            "on tonight's roster is on duty today from midnight, hours before it opens. "
            "False simply means it is not on the roster; clients must not render that as "
            "a badge of its own."
        ),
    )


class CompactFacilitySerializer(serializers.Serializer[Any]):
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
    imageUrl = serializers.URLField(
        allow_null=True,
        help_text=(
            "The facility's first photograph, in the order its owner arranged them, or "
            "null when it has none. A public media URL; clients never build one."
        ),
    )
    lastVerifiedAt = serializers.DateTimeField(
        allow_null=True,
        help_text="When an operator last approved this facility's details (trust signal).",
    )
    infoConfirmedAt = serializers.DateTimeField(
        allow_null=True,
        help_text=(
            "The most recent of `lastVerifiedAt` and the owner's own confirmation that the "
            "opening hours are still right. Null when neither ever happened."
        ),
    )
    updatedAt = serializers.DateTimeField(help_text="Last change to the facility record.")


class FacilityImageSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    url = serializers.CharField(
        help_text="Service-issued media route. Raw object storage keys are never returned."
    )


class PublicHoursEntrySerializer(serializers.Serializer[Any]):
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
    whatsapp = serializers.CharField(
        allow_null=True, help_text="WhatsApp contact, E.164 Syrian mobile (+9639XXXXXXXX)."
    )
    addressAr = serializers.CharField(allow_null=True)
    addressEn = serializers.CharField(allow_null=True)
    neighborhood = NamedRefSerializer(allow_null=True)
    location = CoordinatesSerializer(allow_null=True)
    images = FacilityImageSerializer(many=True)
    specialties = NamedRefSerializer(many=True)
    services = NamedRefSerializer(many=True)
    hours = PublicHoursEntrySerializer(many=True)


class FacilityCursorPageSerializer(serializers.Serializer[Any]):
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


class MapMarkerSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    nameAr = serializers.CharField()
    latitude = serializers.FloatField()
    longitude = serializers.FloatField()
    availability = serializers.ChoiceField(choices=[s.value for s in AvailabilityState])
    categoryIconKey = serializers.CharField(
        allow_null=True,
        help_text="Which mark the pin wears. A map of identical pins cannot be read.",
    )


class MapMarkerListSerializer(serializers.Serializer[Any]):
    items = MapMarkerSerializer(many=True)


class HomeCategoryCapabilitiesSerializer(serializers.Serializer[Any]):
    """The home screen carries a reduced capability set compared with the category list."""

    hours = serializers.BooleanField()
    ratings = serializers.BooleanField()
    duty = serializers.BooleanField()
    specialtyFilter = serializers.BooleanField()
    serviceFilter = serializers.BooleanField()


class HomeCategorySerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    nameAr = serializers.CharField()
    nameEn = serializers.CharField(allow_null=True)
    iconKey = serializers.CharField(allow_null=True)
    capabilities = HomeCategoryCapabilitiesSerializer()


class PublicHomeSerializer(serializers.Serializer[Any]):
    ads = PublicAdvertisementSerializer(many=True)
    categories = HomeCategorySerializer(many=True)
    nearby = CompactFacilitySerializer(many=True)
    openNearby = CompactFacilitySerializer(many=True)
    dutyNow = CompactFacilitySerializer(many=True)
    serverTime = serializers.DateTimeField(
        help_text="Authoritative server time in UTC, so clients do not trust the device clock."
    )
