from __future__ import annotations

import logging
from datetime import timedelta

from celery import shared_task
from django.utils import timezone

from .models import OTPChallenge, User
from .providers.base import InvalidRecipient, TransientOtpError

logger = logging.getLogger(__name__)

# Long enough after the code that the bot's one-minute gap per number has passed: it refuses a
# second message to a number sooner than that, which is what keeps the account from looking like
# a machine. A refusal is retried, twice, a little later each time.
WELCOME_DELAY_SECONDS = 90
WELCOME_RETRY_SECONDS = 120

# Kept a day past expiry so per-phone OTP rate checks still see recent challenges.
OTP_RETENTION = timedelta(days=1)


@shared_task(bind=True, max_retries=2)  # type: ignore[untyped-decorator]
def send_whatsapp_welcome(self: object, user_id: str) -> str:
    """Welcome a new account on WhatsApp, a while after its code (DECISION-101).

    Never part of registration itself: the account exists and the inbox welcome is written
    whatever happens here. A number that cannot receive WhatsApp is let go; a busy or
    disconnected bot is tried again later. Neither the number nor the outcome per person is
    logged — only that a welcome went or why it did not.
    """
    from .otp import deliver_welcome

    user = User.objects.filter(pk=user_id, is_active=True).first()
    if user is None:
        return "gone"
    try:
        sent = deliver_welcome(user.phone)
    except InvalidRecipient:
        return "not_on_whatsapp"
    except TransientOtpError as exc:
        logger.warning("welcome.whatsapp_retry")
        raise self.retry(exc=exc, countdown=WELCOME_RETRY_SECONDS) from exc  # type: ignore[attr-defined]
    return "sent" if sent else "not_this_channel"


@shared_task  # type: ignore[untyped-decorator]
def purge_expired_otp_challenges() -> int:
    deleted, _ = OTPChallenge.objects.filter(expires_at__lt=timezone.now() - OTP_RETENTION).delete()
    return deleted
