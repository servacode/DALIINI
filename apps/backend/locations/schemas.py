"""Response contract for the public locations endpoints.

These serializers describe what the views already return. They are not used to build the
responses, only to describe them, so the runtime payload stays the single source of truth.
"""

from rest_framework import serializers

from core.openapi import CoordinatesSerializer


class PublicProvinceSerializer(serializers.Serializer):
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


class PublicProvinceListSerializer(serializers.Serializer):
    items = PublicProvinceSerializer(many=True)


class PublicCitySerializer(serializers.Serializer):
    id = serializers.UUIDField()
    code = serializers.CharField()
    nameAr = serializers.CharField()
    nameEn = serializers.CharField(allow_null=True)


class PublicCityListSerializer(serializers.Serializer):
    items = PublicCitySerializer(many=True)
