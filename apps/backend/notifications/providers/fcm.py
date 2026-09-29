from __future__ import annotations

from typing import Protocol

from django.conf import settings
from django.core.exceptions import ImproperlyConfigured

from .base import PushMessage
from .fcm_http import transport_from_settings


class FcmTransport(Protocol):
    def send(self, *, project_id: str, message: PushMessage) -> None: ...


class FcmPushProvider:
    """FCM provider boundary.

    The transport is the only part that speaks HTTP: FcmHttpTransport (fcm_http.py) from
    FCM_SERVICE_ACCOUNT_JSON by default, or whatever a test hands in. Without either, a send
    is refused as a misconfiguration, which the delivery loop logs and does not retry.
    """

    def __init__(self, transport: FcmTransport | None = None) -> None:
        self.project_id = getattr(settings, "FCM_PROJECT_ID", "")
        if not self.project_id:
            raise ImproperlyConfigured("FCM_PROJECT_ID is required for FCM push")
        self.transport = transport if transport is not None else transport_from_settings()

    def send(self, message: PushMessage) -> None:
        if self.transport is None:
            raise ImproperlyConfigured(
                "FCM transport is not configured: set FCM_SERVICE_ACCOUNT_JSON"
            )
        self.transport.send(project_id=self.project_id, message=message)
