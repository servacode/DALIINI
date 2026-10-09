from __future__ import annotations

import hashlib
import hmac
import secrets
from dataclasses import dataclass
from uuid import UUID

from django.conf import settings

from .providers.base import OtpMessage
from .providers.factory import get_otp_sender


@dataclass(frozen=True)
class OtpDelivery:
    provider: str
    accepted: bool


def generate_otp() -> str:
    return f"{secrets.randbelow(1_000_000):06d}"


def otp_digest(*, challenge_id: UUID, code: str) -> str:
    key = settings.RECOVERY_HMAC_SECRET.encode("utf-8")
    message = f"otp:{challenge_id}:{code}".encode()
    return hmac.new(key, message, hashlib.sha256).hexdigest()


def deliver_welcome(phone: str) -> bool:
    """Send the WhatsApp welcome where this deployment's channel can (DECISION-101).

    Only the bot can: the official Cloud API sends a business-initiated message only from an
    approved template, and there is none for this yet; the development sender sends nothing.
    Returns whether a send was attempted, so the caller can tell «not this channel» from «sent».
    Raises as `send` does, so the task retries a transient failure.
    """
    sender = get_otp_sender()
    welcome = getattr(sender, "send_welcome", None)
    if welcome is None:
        return False
    welcome(phone)
    return True


def deliver_otp(*, phone: str, code: str) -> OtpDelivery:
    """Hand the code to whichever sender this deployment is configured for.

    The raw code is never logged or returned, by any provider: a code that reaches a log file
    is a code anyone with the log can use. Automated tests set the stored digest directly
    instead of reading one back.
    """
    provider = str(settings.OTP_PROVIDER).lower()
    get_otp_sender().send(OtpMessage(phone=phone, code=code))
    return OtpDelivery(provider=provider, accepted=True)
