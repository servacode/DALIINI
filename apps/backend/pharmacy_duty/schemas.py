"""Response contract for pharmacy duty shifts."""

from typing import Any

from rest_framework import serializers

from .serializers import DutyShiftSerializer


class DutyShiftListSerializer(serializers.Serializer[Any]):
    items = DutyShiftSerializer(many=True)
