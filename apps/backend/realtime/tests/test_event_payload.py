from datetime import UTC, datetime

from realtime.events import EventName, RealtimeEvent, ScopeType


def test_event_payload_is_minimal_invalidation_envelope() -> None:
    event = RealtimeEvent(
        name=EventName.FACILITY_CHANGED,
        scope_type=ScopeType.PROVINCE,
        scope_id="province-id",
        resource_id="facility-id",
        occurred_at=datetime(2026, 9, 17, 10, 0, tzinfo=UTC),
    )
    payload = event.payload()
    assert set(payload) == {
        "version",
        "name",
        "scope",
        "resourceId",
        "occurredAt",
    }
    serialized = str(payload).lower()
    assert "storage_key" not in serialized
    assert "evidence" not in serialized
    assert "token" not in serialized
