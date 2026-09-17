import uuid

from django.conf import settings
from django.db import models


class ProductAnalyticsEvent(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        null=True,
        blank=True,
        on_delete=models.SET_NULL,
        related_name="+",
    )
    anonymous_id_hash = models.CharField(max_length=64, blank=True)
    name = models.CharField(max_length=80)
    properties = models.JSONField(default=dict, blank=True)
    occurred_at = models.DateTimeField()
    received_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        indexes = [
            models.Index(fields=["name", "-occurred_at"]),
            models.Index(fields=["-received_at"]),
        ]
