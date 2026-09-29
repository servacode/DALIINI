from __future__ import annotations

from dataclasses import dataclass
from datetime import UTC, datetime
from enum import StrEnum
from typing import Any


class EventName(StrEnum):
    PROVINCE_CONFIGURATION_CHANGED = "public.province.configuration_changed"
    FACILITY_CHANGED = "public.facility.changed"
    FACILITY_AVAILABILITY_CHANGED = "public.facility.availability_changed"
    DUTY_CHANGED = "public.duty.changed"
    USER_APPLICATION_CHANGED = "user.application.changed"
    USER_FACILITY_CHANGED = "user.facility.changed"
    ADMIN_REVIEW_QUEUE_CHANGED = "admin.review_queue.changed"
    ADMIN_SYSTEM_CHANGED = "admin.system.changed"


class ScopeType(StrEnum):
    PROVINCE = "province"
    USER = "user"
    ADMIN = "admin"


@dataclass(frozen=True)
class RealtimeEvent:
    name: EventName
    scope_type: ScopeType
    scope_id: str
    resource_id: str | None
    version: int = 1
    occurred_at: datetime | None = None

    def payload(self) -> dict[str, Any]:
        occurred_at = self.occurred_at or datetime.now(UTC)
        return {
            "version": self.version,
            "name": self.name.value,
            "scope": {
                "type": self.scope_type.value,
                "id": self.scope_id,
            },
            "resourceId": self.resource_id,
            "occurredAt": occurred_at.isoformat(),
        }
