import uuid

from django.conf import settings
from django.db import models


class Notification(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="notifications"
    )
    type = models.CharField(max_length=80)
    payload = models.JSONField(default=dict, blank=True)
    read_at = models.DateTimeField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        indexes = [models.Index(fields=["user", "read_at", "-created_at"])]


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
        indexes = [models.Index(fields=["user", "active", "platform"])]
