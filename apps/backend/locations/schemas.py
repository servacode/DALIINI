"""Response contract for the public locations endpoints.

These serializers describe what the views already return. They are not used to build the
responses, only to describe them, so the runtime payload stays the single source of truth.
"""

from typing import Any

from rest_framework import serializers

from core.openapi import CoordinatesSerializer


class PublicProvinceSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    code = serializers.CharField()
    nameAr = serializers.CharField()
    nameEn = serializers.CharField(allow_null=True)
    mapCenter = CoordinatesSerializer(
        allow_null=True,
        help_text=(
            "Where a map opens for this province when the user's own position is unknown. "
            "Null when no centre has been set."
        ),
    )


class PublicProvinceListSerializer(serializers.Serializer[Any]):
    items = PublicProvinceSerializer(many=True)


class PublicCitySerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    code = serializers.CharField()
    nameAr = serializers.CharField()
    nameEn = serializers.CharField(allow_null=True)


class PublicCityListSerializer(serializers.Serializer[Any]):
    items = PublicCitySerializer(many=True)


class PublicPlaceSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    nameAr = serializers.CharField()
    nameEn = serializers.CharField(allow_null=True)


class PublicLocationResolveSerializer(serializers.Serializer[Any]):
    province = PublicProvinceSerializer(allow_null=True)
    city = PublicPlaceSerializer(allow_null=True)
    neighborhood = PublicPlaceSerializer(allow_null=True)
    # The stubs see Field.label, but the serializer metaclass collects this declaration
    # as the wire field "label" and removes it from the class; the name is the contract.
    label = serializers.CharField(  # type: ignore[assignment]
        allow_null=True,
        help_text="What the app shows the user: the province, and the finer place when known.",
    )
    resolvedBy = serializers.ChoiceField(
        choices=["BOUNDARY", "NEAREST_PROVINCE", "NONE"],
        help_text=(
            "BOUNDARY when the point falls inside a seeded city or neighbourhood, "
            "NEAREST_PROVINCE when only the closest province centre could be used, "
            "NONE when the point is outside every province this platform serves."
        ),
    )
