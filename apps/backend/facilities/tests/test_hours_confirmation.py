# mypy: disable-error-code="no-untyped-call"
"""Owners confirming their hours, the public `infoConfirmedAt`, and the weekly reminder."""

from __future__ import annotations

from datetime import UTC, datetime, timedelta

import pytest
from django.utils import timezone
from rest_framework.test import APIClient

from accounts.models import User
from audit.models import AuditEvent
from directory.models import CategoryProvince
from facilities.hours_confirmation import (
    REMINDER_TITLE_AR,
    REMINDER_TYPE,
    remind_owners_to_confirm_hours,
)
from facilities.models import Facility, FacilityMembership
from notifications.models import Notification


@pytest.fixture
def owned(facility: Facility, user: User) -> Facility:
    CategoryProvince.objects.create(
        category=facility.category, province=facility.province, public_enabled=True
    )
    FacilityMembership.objects.create(facility=facility, user=user, role="OWNER")
    return facility


def _client(user: User) -> APIClient:
    client = APIClient()
    client.force_authenticate(user=user)
    return client


@pytest.mark.django_db
def test_member_confirms_hours_and_the_public_sees_info_confirmed_at(
    owned: Facility, user: User
) -> None:
    verified = timezone.now() - timedelta(days=30)
    Facility.objects.filter(pk=owned.pk).update(last_verified_at=verified)
    public = f"/api/v1/public/facilities/{owned.pk}/"
    before = APIClient().get(public).json()
    assert before["infoConfirmedAt"] == before["lastVerifiedAt"]

    response = _client(user).post(f"/api/v1/owner/facilities/{owned.pk}/confirm-hours/")

    assert response.status_code == 200
    body = response.json()
    assert body["infoConfirmedAt"] == body["hoursConfirmedAt"]
    owned.refresh_from_db()
    assert owned.hours_confirmed_at is not None
    assert owned.last_verified_at == verified  # an approval still means an approval
    after = APIClient().get(public).json()
    assert after["infoConfirmedAt"] == owned.hours_confirmed_at.isoformat()
    assert after["lastVerifiedAt"] == before["lastVerifiedAt"]
    assert AuditEvent.objects.filter(action="facility.hours.confirmed").exists()
    detail = _client(user).get(f"/api/v1/owner/facilities/{owned.pk}/").json()
    assert detail["hoursConfirmedAt"] == owned.hours_confirmed_at.isoformat()


@pytest.mark.django_db
def test_only_members_confirm(owned: Facility) -> None:
    stranger = User.objects.create_user(phone="+963900000077", name="Stranger")
    response = _client(stranger).post(f"/api/v1/owner/facilities/{owned.pk}/confirm-hours/")
    assert response.status_code == 404


@pytest.mark.django_db
def test_replacing_hours_confirms_them(owned: Facility, user: User) -> None:
    response = _client(user).put(
        f"/api/v1/owner/facilities/{owned.pk}/hours/",
        [{"weekday": 0, "opensAt": "09:00", "closesAt": "17:00"}],
        format="json",
    )
    assert response.status_code == 200
    owned.refresh_from_db()
    assert owned.hours_confirmed_at is not None


@pytest.mark.django_db
def test_weekly_reminder_is_idempotent_per_week(owned: Facility, user: User) -> None:
    # A Wednesday noon, so "an hour later" stays in the same ISO week.
    now = datetime(2026, 9, 30, 9, 0, tzinfo=UTC)
    fresh = Facility.objects.create(
        category=owned.category,
        province=owned.province,
        name_ar="مؤكدة",
        status="ACTIVE",
        hours_confirmed_at=now - timedelta(days=2),
    )
    FacilityMembership.objects.create(facility=fresh, user=user, role="OWNER")

    assert remind_owners_to_confirm_hours(now) == 1
    (reminder,) = Notification.objects.filter(user=user, type=REMINDER_TYPE)
    assert reminder.title_ar == REMINDER_TITLE_AR == "هل أوقات دوامك ما زالت صحيحة؟"
    assert reminder.payload == {"facilityId": str(owned.pk)}

    assert remind_owners_to_confirm_hours(now + timedelta(hours=1)) == 0
    assert remind_owners_to_confirm_hours(now + timedelta(days=7)) == 2  # a new week
    assert Notification.objects.filter(user=user, type=REMINDER_TYPE).count() == 3
