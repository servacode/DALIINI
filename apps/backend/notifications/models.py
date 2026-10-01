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

    def __str__(self) -> str:
        return f"{self.type} {self.id}"


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

    def __str__(self) -> str:
        # Never the token: a repr can reach logs and error reports.
        return f"{self.platform} {self.id}"


class NotificationPushDelivery(models.Model):
    """One successful push of a notification to one device; makes retries idempotent."""

    notification = models.ForeignKey(
        Notification, on_delete=models.CASCADE, related_name="push_deliveries"
    )
    device = models.ForeignKey(DevicePushToken, on_delete=models.CASCADE, related_name="deliveries")
    delivered_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        constraints = [
            models.UniqueConstraint(
                fields=["notification", "device"], name="uniq_notification_push_delivery"
            )
        ]

    def __str__(self) -> str:
        return f"{self.notification_id}->{self.device_id}"


class Broadcast(models.Model):
    """An operator's message to many accounts at once; each recipient gets a Notification."""

    class Audience(models.TextChoices):
        ALL = "ALL", "Every active account"
        OWNERS = "OWNERS", "Facility owners and managers"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    actor = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        null=True,
        blank=True,
        on_delete=models.SET_NULL,
        related_name="+",
    )
    title_ar = models.CharField(max_length=180)
    body_ar = models.CharField(max_length=400)
    audience = models.CharField(max_length=8, choices=Audience.choices)
    province = models.ForeignKey(
        "locations.Province", null=True, blank=True, on_delete=models.SET_NULL, related_name="+"
    )
    recipient_count = models.PositiveIntegerField(default=0)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        indexes = [models.Index(fields=["actor", "-created_at"], name="notifications_bcast_idx")]

    def __str__(self) -> str:
        return self.title_ar
