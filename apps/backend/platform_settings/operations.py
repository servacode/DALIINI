"""Operational thresholds operators tune from the Settings page.

Typed and seeded like the maintenance keys, so the Settings page lists them and the admin
write endpoint refuses a change of type. Read on demand: these drive admin views only, so a
database read per request is cheaper than a cache that could go stale.
"""

from __future__ import annotations

from typing import Any

from .maintenance import TYPED_DEFAULTS as MAINTENANCE_DEFAULTS
from .models import PlatformSetting

REVIEW_SLA_HOURS_KEY = "review.slaHours"
READINESS_MIN_FACILITIES_KEY = "readiness.minActiveFacilities"

OPERATIONS_DEFAULTS: dict[str, tuple[str, Any]] = {
    # An application, report or re-verification older than this is flagged `overdue`.
    REVIEW_SLA_HOURS_KEY: (PlatformSetting.ValueType.INTEGER, 48),
    # Launch readiness wants at least this many ACTIVE facilities in a province.
    READINESS_MIN_FACILITIES_KEY: (PlatformSetting.ValueType.INTEGER, 5),
}

# Every key the platform knows, with the type it must keep.
TYPED_SETTINGS: dict[str, tuple[str, Any]] = {**MAINTENANCE_DEFAULTS, **OPERATIONS_DEFAULTS}


def get_int_setting(key: str) -> int:
    """The stored positive integer, or the default when unset or not a positive integer."""
    default = int(OPERATIONS_DEFAULTS[key][1])
    value = PlatformSetting.objects.filter(key=key).values_list("value", flat=True).first()
    if isinstance(value, int) and not isinstance(value, bool) and value > 0:
        return value
    return default
