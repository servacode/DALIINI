from __future__ import annotations

from django.conf import settings
from django.core.exceptions import ImproperlyConfigured

from .base import OtpSender
from .development import DevelopmentOtpSender
from .whatsapp import WhatsAppOtpSender


def get_otp_sender() -> OtpSender:
    """The sender this deployment is configured for.

    An unknown name raises rather than falling back: a deployment that names a provider we do
    not have must fail at the first send, loudly, instead of quietly delivering nothing while
    appearing to work.
    """
    provider = str(getattr(settings, "OTP_PROVIDER", "development")).lower()
    if provider in {"development", "test", "console"}:
        return DevelopmentOtpSender()
    if provider == "whatsapp":
        return WhatsAppOtpSender()
    raise ImproperlyConfigured(f"Unsupported OTP provider: {provider}")
