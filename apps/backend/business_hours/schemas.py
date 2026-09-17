"""Response contract for opening hours and temporary closures."""

from rest_framework import serializers

from .serializers import TemporaryClosureSerializer


class BusinessHourSerializer(serializers.Serializer):
    id = serializers.UUIDField()
    weekday = serializers.IntegerField(
        min_value=0, max_value=6, help_text="0 is Monday, matching Python weekday numbering."
    )
    opensAt = serializers.TimeField()
    closesAt = serializers.TimeField(
        help_text="A value earlier than opensAt denotes an overnight span."
    )
    sortOrder = serializers.IntegerField()


class BusinessHoursListSerializer(serializers.Serializer):
    items = BusinessHourSerializer(many=True)


class TemporaryClosureListSerializer(serializers.Serializer):
    items = TemporaryClosureSerializer(many=True)
