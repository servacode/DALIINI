"""Response contract for pharmacy duty shifts."""

from rest_framework import serializers

from .serializers import DutyShiftSerializer


class DutyShiftListSerializer(serializers.Serializer):
    items = DutyShiftSerializer(many=True)
