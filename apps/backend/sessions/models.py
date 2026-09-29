import uuid

from django.conf import settings
from django.db import models


class UserSession(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="sessions",
    )
    refresh_digest = models.CharField(max_length=128, unique=True)
    previous_refresh_digest = models.CharField(max_length=128, blank=True)
    previous_valid_until = models.DateTimeField(null=True, blank=True)
    platform = models.CharField(max_length=32, blank=True)
    device_name = models.CharField(max_length=120, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    last_seen_at = models.DateTimeField(null=True, blank=True)
    expires_at = models.DateTimeField()
    revoked_at = models.DateTimeField(null=True, blank=True)
    compromised_at = models.DateTimeField(null=True, blank=True)

    def __str__(self) -> str:
        # Never a refresh digest: a repr can reach logs and error reports.
        return f"{self.user_id} {self.id}"
