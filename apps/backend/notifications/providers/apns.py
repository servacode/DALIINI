from django.core.exceptions import ImproperlyConfigured

from .base import PushMessage


class ApnsPushProvider:
    def send(self, message: PushMessage) -> None:
        raise ImproperlyConfigured("APNs transport is deferred until the iOS release phases")
