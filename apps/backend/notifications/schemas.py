"""Request contract for device push registration."""

from rest_framework import serializers

from .models import DevicePushToken


class PushTokenRegisterSerializer(serializers.Serializer):  # type: ignore[type-arg]
    """The device's current provider token. It is stored encrypted and never returned."""

    platform = serializers.ChoiceField(choices=DevicePushToken.Platform.choices)
    token = serializers.CharField(max_length=4096)


class PushTokenSerializer(serializers.Serializer):  # type: ignore[type-arg]
    token = serializers.CharField(max_length=4096)


class NotificationSerializer(serializers.Serializer):  # type: ignore[type-arg]
    """One message in the account's inbox, as the app reads it."""

    id = serializers.UUIDField()
    type = serializers.CharField(help_text="What happened, as a stable code.")
    titleAr = serializers.CharField()
    bodyAr = serializers.CharField(allow_blank=True)
    destination = serializers.ChoiceField(
        choices=["NONE", "FACILITY", "OWNER_FACILITIES"],
        help_text=(
            "Where opening this message takes the reader. The set is closed on purpose: a "
            "notification can never carry an arbitrary link."
        ),
    )
    facilityId = serializers.UUIDField(
        allow_null=True, help_text="Set only when the destination is FACILITY."
    )
    isRead = serializers.BooleanField()
    createdAt = serializers.DateTimeField()


class NotificationPageSerializer(serializers.Serializer):  # type: ignore[type-arg]
    items = NotificationSerializer(many=True)
    nextCursor = serializers.CharField(allow_null=True)
    hasMore = serializers.BooleanField()
    unreadCount = serializers.IntegerField(
        help_text="Unread messages in the whole inbox, not only on this page."
    )


class UnreadCountSerializer(serializers.Serializer):  # type: ignore[type-arg]
    unreadCount = serializers.IntegerField()
