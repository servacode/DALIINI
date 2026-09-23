import uuid

from django.conf import settings
from django.db import models


class Notification(models.Model):
    """One message in an account's own inbox.

    The inbox is the record; a push is only a way of announcing it. The two carry the same
    words, so an account that never granted the notification permission still finds
    everything the platform told it, in the app, in order.
    """

    class Destination(models.TextChoices):
        """Where a notification may send the reader, restricted on purpose.

        Anything outside this list is not stored: a notification must never be able to
        carry an arbitrary deep link into the app or out of it.
        """

        NONE = "NONE", "None"
        FACILITY = "FACILITY", "Facility"
        OWNER_FACILITIES = "OWNER_FACILITIES", "The owner's facilities"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="notifications"
    )
    type = models.CharField(max_length=80)
    title_ar = models.CharField(max_length=180, default="")
    body_ar = models.CharField(max_length=400, blank=True, default="")
    destination = models.CharField(
        max_length=24, choices=Destination.choices, default=Destination.NONE
    )
    payload = models.JSONField(default=dict, blank=True)
    read_at = models.DateTimeField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        indexes = [
            models.Index(
                fields=["user", "read_at", "-created_at"],
                name="notificatio_user_id_9c4029_idx",
            ),
            # The inbox itself: one account's messages, newest first.
            models.Index(fields=["user", "-created_at"], name="notifications_inbox_idx"),
        ]


class DevicePushToken(models.Model):
    class Platform(models.TextChoices):
        ANDROID = "ANDROID", "Android"
        IOS = "IOS", "iOS"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="push_tokens"
    )
    session = models.ForeignKey(
        "directory_sessions.UserSession",
        null=True,
        blank=True,
        on_delete=models.SET_NULL,
        related_name="push_tokens",
    )
    platform = models.CharField(max_length=16, choices=Platform.choices)
    token_digest = models.CharField(max_length=64, unique=True)
    token_ciphertext = models.TextField()
    active = models.BooleanField(default=True)
    last_seen_at = models.DateTimeField(auto_now=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        indexes = [
            models.Index(
                fields=["user", "active", "platform"],
                name="notificatio_user_id_67bc12_idx",
            )
        ]
