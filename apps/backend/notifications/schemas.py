"""Request contract for device push registration."""

from rest_framework import serializers

from .models import DevicePushToken


class PushTokenRegisterSerializer(serializers.Serializer):  # type: ignore[type-arg]
    """The device's current provider token. It is stored encrypted and never returned."""

    platform = serializers.ChoiceField(choices=DevicePushToken.Platform.choices)
    token = serializers.CharField(max_length=4096)


class PushTokenSerializer(serializers.Serializer):  # type: ignore[type-arg]
    token = serializers.CharField(max_length=4096)
