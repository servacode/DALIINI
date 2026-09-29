"""Response contract for opening hours and temporary closures."""

from typing import Any

from rest_framework import serializers

from .serializers import TemporaryClosureSerializer


class BusinessHourSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    weekday = serializers.IntegerField(
        min_value=0, max_value=6, help_text="0 is Monday, matching Python weekday numbering."
    )
    opensAt = serializers.TimeField()
    closesAt = serializers.TimeField(
        help_text="A value earlier than opensAt denotes an overnight span."
    )
    sequence = serializers.IntegerField(
        help_text="Ordering within a weekday, for categories that open in several spans."
    )


class BusinessHoursListSerializer(serializers.Serializer[Any]):
    items = BusinessHourSerializer(many=True)


class TemporaryClosureListSerializer(serializers.Serializer[Any]):
    items = TemporaryClosureSerializer(many=True)
