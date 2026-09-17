from __future__ import annotations

from django.conf import settings
from django.core.exceptions import ImproperlyConfigured

from .base import PushMessage


class FcmPushProvider:
    """FCM provider boundary.

    Network transport is deliberately adapter-driven so production credentials and
    Google auth libraries are not coupled to domain code. P19/P21 may bind the
    concrete transport after credentials exist.
    """

    def __init__(self, transport=None):
        self.transport = transport
        self.project_id = getattr(settings, "FCM_PROJECT_ID", "")
        if not self.project_id:
            raise ImproperlyConfigured("FCM_PROJECT_ID is required for FCM push")

    def send(self, message: PushMessage) -> None:
        if self.transport is None:
            raise ImproperlyConfigured("FCM transport is not configured")
        self.transport.send(project_id=self.project_id, message=message)
