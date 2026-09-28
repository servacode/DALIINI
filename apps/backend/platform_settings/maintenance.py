"""Maintenance mode: typed platform settings, a short-lived cache and the gate itself."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any

import logging

from django.core.cache import cache
from django.db import DatabaseError

from .models import PlatformSetting

logger = logging.getLogger(__name__)

ENABLED_KEY = "maintenance.enabled"
MESSAGE_KEY = "maintenance.messageAr"
RETRY_AFTER_KEY = "maintenance.retryAfterSeconds"

DEFAULT_MESSAGE_AR = "المنصة قيد الصيانة حالياً. يرجى المحاولة لاحقاً."
DEFAULT_RETRY_AFTER_SECONDS = 600

# Keys the platform knows, with the type each must keep. Seeded by a data migration so
# the admin Settings page lists them; the admin write endpoint refuses a type change.
TYPED_DEFAULTS: dict[str, tuple[str, Any]] = {
    ENABLED_KEY: (PlatformSetting.ValueType.BOOLEAN, False),
    MESSAGE_KEY: (PlatformSetting.ValueType.STRING, DEFAULT_MESSAGE_AR),
    RETRY_AFTER_KEY: (PlatformSetting.ValueType.INTEGER, DEFAULT_RETRY_AFTER_SECONDS),
}

CACHE_KEY = "platform:maintenance:v1"
CACHE_TTL_SECONDS = 15


@dataclass(frozen=True)
class MaintenanceState:
    enabled: bool
    message_ar: str
    retry_after_seconds: int

    def as_payload(self) -> dict[str, Any]:
        return {
            "maintenance": self.enabled,
            "messageAr": self.message_ar,
            "retryAfterSeconds": self.retry_after_seconds,
        }


def _load() -> MaintenanceState:
    values = dict(
        PlatformSetting.objects.filter(key__in=TYPED_DEFAULTS).values_list("key", "value")
    )
    enabled = values.get(ENABLED_KEY)
    message = values.get(MESSAGE_KEY)
    retry = values.get(RETRY_AFTER_KEY)
    return MaintenanceState(
        enabled=enabled is True,
        message_ar=message if isinstance(message, str) and message else DEFAULT_MESSAGE_AR,
        retry_after_seconds=(
            retry
            if isinstance(retry, int) and not isinstance(retry, bool) and retry >= 0
            else DEFAULT_RETRY_AFTER_SECONDS
        ),
    )


def get_maintenance_state() -> MaintenanceState:
    cached = cache.get(CACHE_KEY)
    if isinstance(cached, MaintenanceState):
        return cached
    try:
        state = _load()
    except DatabaseError:
        # Fail open: a database hiccup must not turn into a platform-wide 503. The request
        # itself will surface the database problem through the normal error path.
        logger.warning("maintenance.state_unavailable")
        return MaintenanceState(False, DEFAULT_MESSAGE_AR, DEFAULT_RETRY_AFTER_SECONDS)
    cache.set(CACHE_KEY, state, CACHE_TTL_SECONDS)
    return state


def invalidate_maintenance_cache() -> None:
    cache.delete(CACHE_KEY)


# Paths that keep working while the platform is in maintenance, so operators can switch it
# off again and clients can learn why they are refused.
_EXEMPT_PREFIXES = (
    "/api/v1/admin/",
    "/api/v1/platform/status/",
    "/api/v1/auth/login/",
    "/api/v1/auth/refresh/",
    "/api/v1/auth/logout/",
    "/api/v1/schema",
    "/health/",
)


def is_gated_path(path: str) -> bool:
    return path.startswith("/api/v1/") and not path.startswith(_EXEMPT_PREFIXES)
