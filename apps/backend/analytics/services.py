from __future__ import annotations

import hashlib
from datetime import timedelta

from django.conf import settings
from django.utils import timezone

from .models import ProductAnalyticsEvent
from .registry import validate_event


def _anonymous_hash(value: str) -> str:
    if not value:
        return ""
    salt = getattr(settings, "ANALYTICS_HASH_SALT", settings.SECRET_KEY)
    return hashlib.sha256(f"{salt}:{value}".encode()).hexdigest()


def record_product_event(
    *, name: str, properties: dict, occurred_at=None, user=None, anonymous_id=""
):
    safe = validate_event(name, properties)
    return ProductAnalyticsEvent.objects.create(
        user=user if getattr(user, "is_authenticated", False) else None,
        anonymous_id_hash=_anonymous_hash(anonymous_id),
        name=name,
        properties=safe,
        occurred_at=occurred_at or timezone.now(),
    )


def purge_expired_analytics(*, retention_days: int | None = None) -> int:
    days = retention_days or getattr(settings, "ANALYTICS_RETENTION_DAYS", 180)
    cutoff = timezone.now() - timedelta(days=days)
    deleted, _ = ProductAnalyticsEvent.objects.filter(received_at__lt=cutoff).delete()
    return deleted
