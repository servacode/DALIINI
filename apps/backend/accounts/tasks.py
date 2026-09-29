from __future__ import annotations

from datetime import timedelta

from celery import shared_task
from django.utils import timezone

from .models import OTPChallenge

# Kept a day past expiry so per-phone OTP rate checks still see recent challenges.
OTP_RETENTION = timedelta(days=1)


@shared_task  # type: ignore[untyped-decorator]
def purge_expired_otp_challenges() -> int:
    deleted, _ = OTPChallenge.objects.filter(expires_at__lt=timezone.now() - OTP_RETENTION).delete()
    return deleted
