"""An owner's changes to duty and closures leave the same trail an operator's do."""

from datetime import timedelta

import pytest
from django.utils import timezone
from rest_framework.test import APIClient

from accounts.models import User
from audit.models import AuditEvent
from directory.models import CategoryCapabilities
from facilities.models import Facility, FacilityMembership

DAY = timedelta(days=1)


@pytest.fixture
def owner(facility: Facility, user: User) -> APIClient:
    FacilityMembership.objects.create(
        facility=facility, user=user, role=FacilityMembership.Role.OWNER
    )
    CategoryCapabilities.objects.filter(category=facility.category).update(
        supports_temporary_closure=True
    )
    client = APIClient()
    client.force_authenticate(user=user)
    return client


def _actions(user: User) -> list[str]:
    events = AuditEvent.objects.filter(actor=user).order_by("created_at")
    return list(events.values_list("action", flat=True))


@pytest.mark.django_db
def test_a_shift_scheduled_moved_and_cancelled_is_audited(
    owner: APIClient, facility: Facility, user: User
) -> None:
    base = f"/api/v1/owner/facilities/{facility.pk}/duty/"
    start = timezone.now() + DAY
    created = owner.post(
        base,
        {"startsAt": start.isoformat(), "endsAt": (start + timedelta(hours=8)).isoformat()},
        format="json",
    )
    assert created.status_code == 201
    shift_url = f"{base}{created.json()['id']}/"

    later = (start + timedelta(hours=10)).isoformat()
    assert owner.patch(shift_url, {"endsAt": later}, format="json").status_code == 200
    # Sending the same times again changes nothing, and records nothing.
    assert owner.patch(shift_url, {"endsAt": later}, format="json").status_code == 200
    assert owner.delete(shift_url).status_code == 204

    assert _actions(user) == ["duty_shift.created", "duty_shift.updated", "duty_shift.deleted"]
    moved = AuditEvent.objects.get(action="duty_shift.updated")
    assert moved.before_snapshot["endsAt"] != moved.after_snapshot["endsAt"]
    assert moved.after_snapshot["source"] == "OWNER"


@pytest.mark.django_db
def test_a_closure_opened_and_cancelled_is_audited(
    owner: APIClient, facility: Facility, user: User
) -> None:
    base = f"/api/v1/owner/facilities/{facility.pk}/temporary-closures/"
    start = timezone.now() + DAY
    created = owner.post(
        base,
        {
            "startsAt": start.isoformat(),
            "endsAt": (start + DAY).isoformat(),
            "reason": "جرد سنوي",
        },
        format="json",
    )
    assert created.status_code == 201
    assert owner.delete(f"{base}{created.json()['id']}/").status_code == 204

    assert _actions(user) == ["facility.closure.created", "facility.closure.cancelled"]
    cancelled = AuditEvent.objects.get(action="facility.closure.cancelled")
    assert cancelled.before_snapshot["reason"] == "جرد سنوي"
    assert cancelled.metadata["facilityId"] == str(facility.pk)
