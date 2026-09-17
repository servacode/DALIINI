"""Response contract for the public advertisement endpoint."""

from rest_framework import serializers

from .models import Advertisement


class AdvertisementActionSerializer(serializers.Serializer):
    type = serializers.ChoiceField(choices=Advertisement.ActionType.choices)
    payload = serializers.DictField(
        help_text="Action arguments, validated server-side per action type."
    )


class PublicAdvertisementSerializer(serializers.Serializer):
    id = serializers.UUIDField()
    imageUrl = serializers.CharField(
        help_text="Service-issued media route. The raw object storage key is never returned."
    )
    titleAr = serializers.CharField(allow_null=True)
    titleEn = serializers.CharField(allow_null=True)
    subtitleAr = serializers.CharField(allow_null=True)
    subtitleEn = serializers.CharField(allow_null=True)
    action = AdvertisementActionSerializer()
    slideDurationMs = serializers.IntegerField()


class PublicAdvertisementListSerializer(serializers.Serializer):
    items = PublicAdvertisementSerializer(many=True)
