from __future__ import annotations

from datetime import timedelta

from celery import shared_task  # type: ignore[import-untyped]
from django.db.models import Q
from django.utils import timezone

from .models import UserSession

# Ended sessions are kept this long so reuse detection and support lookups still work.
ENDED_SESSION_RETENTION = timedelta(days=30)


@shared_task  # type: ignore[untyped-decorator]
def purge_ended_sessions() -> int:
    cutoff = timezone.now() - ENDED_SESSION_RETENTION
    deleted, _ = UserSession.objects.filter(
        Q(expires_at__lt=cutoff) | Q(revoked_at__lt=cutoff)
    ).delete()
    return deleted
