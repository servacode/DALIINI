"""The admin duty roster, the shared upsert service and the daily gap nudges."""

from __future__ import annotations

from datetime import datetime, timedelta
from typing import Any

import pytest
from django.utils import timezone
from rest_framework.test import APIClient

from accounts.models import User
from audit.models import AuditEvent
from business_hours.models import TemporaryClosure
from directory.models import Category, CategoryCapabilities, CategoryProvince
from facilities.models import Facility, FacilityMembership
from notifications.models import Notification
from pharmacy_duty.coverage import day_bounds, local_today
from pharmacy_duty.models import DutyGapNudge, DutyShift
from pharmacy_duty.nudges import NUDGE_TYPE, nudge_duty_gaps
from pharmacy_duty.services import upsert_duty_shifts

ROSTER = "/api/v1/admin/duty/"


@pytest.fixture
def pharmacy(facility: Facility, user: User) -> Facility:
    CategoryProvince.objects.create(
        category=facility.category, province=facility.province, public_enabled=True
    )
    FacilityMembership.objects.create(facility=facility, user=user, role="OWNER")
    return facility


def _tomorrow_evening() -> tuple[datetime, datetime]:
    start, _ = day_bounds(local_today() + timedelta(days=1))
    return start + timedelta(hours=20), start + timedelta(hours=30)


@pytest.mark.django_db
def test_roster_lists_shifts_per_day_and_flags_gaps(admin_api: Any, pharmacy: Facility) -> None:
    starts, ends = _tomorrow_evening()
    DutyShift.objects.create(facility=pharmacy, starts_at=starts, ends_at=ends)
    client = admin_api("admin.duty.read")

    body = client.get(ROSTER, {"provinceId": str(pharmacy.province_id)}).json()

    assert len(body["days"]) == 14
    assert body["from"] == local_today().isoformat()
    today, tomorrow, after = body["days"][:3]
    assert today["gap"] is True and today["shifts"] == []
    # 20:00 to 06:00 covers both days it touches.
    assert tomorrow["gap"] is False and after["gap"] is False
    shift = tomorrow["shifts"][0]
    assert (shift["facilityId"], shift["createdBy"], shift["status"]) == (
        str(pharmacy.pk),
        "OWNER",
        "UPCOMING",
    )
    ranged = client.get(
        ROSTER,
        {
            "provinceId": str(pharmacy.province_id),
            "from": "2026-01-01",
            "to": "2026-01-03",
        },
    ).json()
    assert [day["date"] for day in ranged["days"]] == ["2026-01-01", "2026-01-02", "2026-01-03"]
    assert client.get(ROSTER).status_code == 400
    assert client.get(ROSTER, {"provinceId": "bad"}).status_code == 400
    assert (
        client.get(
            ROSTER,
            {"provinceId": str(pharmacy.province_id), "from": "2026-01-01", "to": "2026-06-01"},
        ).status_code
        == 400
    )
    assert (
        admin_api("admin.facilities.read")
        .get(ROSTER, {"provinceId": str(pharmacy.province_id)})
        .status_code
        == 403
    )


@pytest.mark.django_db
def test_admin_creates_moves_and_cancels_a_shift_for_the_pharmacy(
    admin_api: Any, pharmacy: Facility, user: User
) -> None:
    client = admin_api("admin.duty.read", "admin.duty.manage")
    starts, ends = _tomorrow_evening()
    payload = {
        "facilityId": str(pharmacy.pk),
        "startsAt": starts.isoformat(),
        "endsAt": ends.isoformat(),
    }

    created = client.post(ROSTER, payload, format="json")
    assert created.status_code == 201
    body = created.json()
    assert body["createdBy"] == "ADMIN"
    shift_id = body["id"]
    assert client.post(ROSTER, payload, format="json").status_code == 200  # identical: unchanged
    overlapping = client.post(
        ROSTER,
        {**payload, "startsAt": (starts + timedelta(hours=1)).isoformat()},
        format="json",
    )
    assert (overlapping.status_code, overlapping.json()["code"]) == (409, "DUTY_OVERLAP_OR_INVALID")

    moved = client.patch(
        f"{ROSTER}{shift_id}/",
        {"endsAt": (ends + timedelta(hours=2)).isoformat()},
        format="json",
    )
    assert moved.status_code == 200
    assert moved.json()["createdBy"] == "ADMIN"
    assert client.delete(f"{ROSTER}{shift_id}/").status_code == 204
    assert not DutyShift.objects.filter(pk=shift_id).exists()

    actions = list(
        AuditEvent.objects.filter(target_id=shift_id)
        .order_by("created_at")
        .values_list("action", flat=True)
    )
    assert actions == ["duty_shift.created", "duty_shift.updated", "duty_shift.deleted"]
    messages = Notification.objects.filter(user=user, type="duty.shift.admin_changed")
    assert messages.count() == 3
    assert all(message.payload["facilityId"] == str(pharmacy.pk) for message in messages)
    assert admin_api("admin.duty.read").post(ROSTER, payload, format="json").status_code == 403


@pytest.mark.django_db
def test_admin_shift_follows_the_owner_rules(admin_api: Any, pharmacy: Facility) -> None:
    client = admin_api("admin.duty.read", "admin.duty.manage")
    starts, ends = _tomorrow_evening()
    TemporaryClosure.objects.create(
        facility=pharmacy,
        starts_at=starts - timedelta(hours=1),
        ends_at=starts + timedelta(hours=1),
    )
    closed = client.post(
        ROSTER,
        {
            "facilityId": str(pharmacy.pk),
            "startsAt": starts.isoformat(),
            "endsAt": ends.isoformat(),
        },
        format="json",
    )
    assert (closed.status_code, closed.json()["code"]) == (409, "DUTY_DURING_CLOSURE")

    group = pharmacy.category.group
    clinic = Category.objects.create(group=group, code="clinic-t", slug="clinic-t", name_ar="عيادة")
    CategoryCapabilities.objects.create(category=clinic, supports_duty=False)
    other = Facility.objects.create(
        category=clinic, province=pharmacy.province, name_ar="عيادة", status="ACTIVE"
    )
    unsupported = client.post(
        ROSTER,
        {"facilityId": str(other.pk), "startsAt": starts.isoformat(), "endsAt": ends.isoformat()},
        format="json",
    )
    assert (unsupported.status_code, unsupported.json()["code"]) == (409, "DUTY_NOT_SUPPORTED")
    backwards = client.post(
        ROSTER,
        {
            "facilityId": str(pharmacy.pk),
            "startsAt": ends.isoformat(),
            "endsAt": starts.isoformat(),
        },
        format="json",
    )
    assert backwards.status_code == 400


@pytest.mark.django_db
def test_owner_cannot_schedule_duty_during_a_closure(pharmacy: Facility, user: User) -> None:
    starts, ends = _tomorrow_evening()
    TemporaryClosure.objects.create(facility=pharmacy, starts_at=starts, ends_at=ends)
    client = APIClient()
    client.force_authenticate(user=user)
    response = client.post(
        f"/api/v1/owner/facilities/{pharmacy.pk}/duty/",
        {"startsAt": starts.isoformat(), "endsAt": ends.isoformat()},
        format="json",
    )
    assert (response.status_code, response.json()["code"]) == (409, "DUTY_DURING_CLOSURE")


@pytest.mark.django_db
def test_upsert_is_idempotent_for_imports(pharmacy: Facility) -> None:
    starts, ends = _tomorrow_evening()
    row = {"facility_id": pharmacy.pk, "starts_at": starts, "ends_at": ends}
    (first,) = upsert_duty_shifts([row], source="IMPORT")
    (again,) = upsert_duty_shifts([row], source="IMPORT")
    assert (first.outcome, again.outcome) == ("CREATED", "UNCHANGED")
    assert again.shift.pk == first.shift.pk
    assert first.shift.source == "IMPORT"
    (moved,) = upsert_duty_shifts(
        [{**row, "id": first.shift.pk, "ends_at": ends + timedelta(hours=1)}], source="ADMIN"
    )
    assert moved.outcome == "UPDATED"
    assert moved.shift.source == "IMPORT"  # provenance never changes
    with pytest.raises(ValueError):
        upsert_duty_shifts([row], source="ROBOT")


@pytest.mark.django_db
def test_gap_nudges_once_per_gap_day_and_once_per_owner_per_day(
    pharmacy: Facility, user: User
) -> None:
    second = Facility.objects.create(
        category=pharmacy.category, province=pharmacy.province, name_ar="ثانية", status="ACTIVE"
    )
    FacilityMembership.objects.create(facility=second, user=user, role="OWNER")
    now = timezone.now()

    sent = nudge_duty_gaps(now)

    messages = Notification.objects.filter(user=user, type=NUDGE_TYPE)
    assert sent >= 1 and messages.count() == 1  # two pharmacies, still one nudge
    assert "لا توجد صيدلية مناوبة يوم" in messages.get().body_ar
    assert "هل يمكنك تغطية المناوبة؟" in messages.get().body_ar
    nudged = DutyGapNudge.objects.get(province=pharmacy.province)
    assert nudged.gap_date == local_today(now)

    assert nudge_duty_gaps(now + timedelta(minutes=5)) == 0
    assert messages.count() == 1
    # Tomorrow the next uncovered day is asked about, once.
    nudge_duty_gaps(now + timedelta(days=1))
    assert messages.count() == 2
    assert DutyGapNudge.objects.filter(province=pharmacy.province).count() == 2
