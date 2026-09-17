from __future__ import annotations

import hashlib
import hmac
import secrets
from dataclasses import dataclass

from django.conf import settings


@dataclass(frozen=True)
class OtpDelivery:
    provider: str
    accepted: bool


def generate_otp() -> str:
    return f"{secrets.randbelow(1_000_000):06d}"


def otp_digest(*, challenge_id, code: str) -> str:
    key = settings.RECOVERY_HMAC_SECRET.encode("utf-8")
    message = f"otp:{challenge_id}:{code}".encode("utf-8")
    return hmac.new(key, message, hashlib.sha256).hexdigest()


def deliver_otp(*, phone: str, code: str) -> OtpDelivery:
    provider = settings.OTP_PROVIDER.lower()
    if provider in {"development", "test"}:
        # Deliberately do not log or return the raw OTP. Automated tests set the
        # digest directly; connected environments must provide a real provider.
        return OtpDelivery(provider=provider, accepted=True)
    raise RuntimeError(f"OTP provider adapter is not configured: {provider}")
