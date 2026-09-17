from rest_framework import serializers


class RatingWriteSerializer(serializers.Serializer):
    stars = serializers.IntegerField(min_value=1, max_value=5)
