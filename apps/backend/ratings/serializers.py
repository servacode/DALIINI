from typing import Any

from rest_framework import serializers


class RatingWriteSerializer(serializers.Serializer[Any]):
    stars = serializers.IntegerField(min_value=1, max_value=5)
