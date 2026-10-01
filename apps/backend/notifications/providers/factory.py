from django.conf import settings
from django.core.exceptions import ImproperlyConfigured

from .apns import ApnsPushProvider
from .base import PushProvider
from .development import DevelopmentPushProvider
from .fcm import FcmPushProvider


def get_push_provider(platform: str) -> PushProvider:
    provider = getattr(settings, "PUSH_PROVIDER", "development").lower()
    if provider == "development":
        return DevelopmentPushProvider()
    if provider == "fcm" and platform == "ANDROID":
        return FcmPushProvider()
    if provider == "apns" and platform == "IOS":
        return ApnsPushProvider()
    raise ImproperlyConfigured(f"Unsupported push provider/platform: {provider}/{platform}")
