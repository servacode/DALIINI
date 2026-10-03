"""Duty shifts end to end: owner scheduling, permissions and the public Duty Now query."""

from datetime import datetime, timedelta
from zoneinfo import ZoneInfo

import pytest
from django.utils import timezone
from rest_framework.test import APIClient

from accounts.models import User
from business_hours.models import TemporaryClosure
from business_hours.query import filter_for_availability_state
from business_hours.services import AvailabilityState, get_facility_availability
from directory.models import Category, CategoryCapabilities, CategoryProvince
from facilities.models import Facility, FacilityMembership
from pharmacy_duty.models import DutyShift

DAMASCUS = ZoneInfo("Asia/Damascus")


def _client(user: User) -> APIClient:
    client = APIClient()
    client.force_authenticate(user=user)
    return client


def _url(facility: Facility, shift: DutyShift | None = None) -> str:
    base = f"/api/v1/owner/facilities/{facility.pk}/duty/"
    return f"{base}{shift.pk}/" if shift else base


def _body(start: datetime, end: datetime) -> dict[str, str]:
    return {"startsAt": start.isoformat(), "endsAt": end.isoformat()}


@pytest.fixture
def owner(facility: Facility, user: User) -> APIClient:
    FacilityMembership.objects.create(
        facility=facility, user=user, role=FacilityMembership.Role.OWNER
    )
    return _client(user)


@pytest.fixture
def manager(facility: Facility) -> APIClient:
    other = User.objects.create_user(phone="+963900000002", password="x" * 12, name="M")
    FacilityMembership.objects.create(
        facility=facility, user=other, role=FacilityMembership.Role.MANAGER
    )
    return _client(other)


@pytest.mark.django_db
def test_owner_schedules_a_future_shift_and_lists_it(owner: APIClient, facility: Facility) -> None:
    start = timezone.now() + timedelta(days=1)
    created = owner.post(_url(facility), _body(start, start + timedelta(hours=8)), format="json")
    assert created.status_code == 201
    listed = owner.get(_url(facility)).json()["items"]
    assert [item["id"] for item in listed] == [created.json()["id"]]


@pytest.mark.django_db
def test_manager_starts_duty_now_and_facility_is_on_duty(
    manager: APIClient, facility: Facility
) -> None:
    now = timezone.now()
    response = manager.post(_url(facility), _body(now, now + timedelta(hours=4)), format="json")
    assert response.status_code == 201
    assert get_facility_availability(facility).state is AvailabilityState.DUTY


@pytest.mark.django_db
def test_overlap_is_rejected_with_the_shared_code(owner: APIClient, facility: Facility) -> None:
    start = timezone.now() + timedelta(hours=1)
    owner.post(_url(facility), _body(start, start + timedelta(hours=4)), format="json")
    overlap = owner.post(
        _url(facility),
        _body(start + timedelta(hours=2), start + timedelta(hours=6)),
        format="json",
    )
    assert overlap.status_code == 409
    assert overlap.json()["code"] == "DUTY_OVERLAP_OR_INVALID"
    # Back to back is not an overlap: ranges are half-open.
    adjacent = owner.post(
        _url(facility),
        _body(start + timedelta(hours=4), start + timedelta(hours=5)),
        format="json",
    )
    assert adjacent.status_code == 201


@pytest.mark.django_db
def test_end_before_start_is_rejected(owner: APIClient, facility: Facility) -> None:
    start = timezone.now() + timedelta(hours=3)
    response = owner.post(_url(facility), _body(start, start - timedelta(hours=1)), format="json")
    assert response.status_code == 400


@pytest.mark.django_db
def test_end_early_and_cancel(owner: APIClient, facility: Facility) -> None:
    now = timezone.now()
    shift = DutyShift.objects.create(
        facility=facility, starts_at=now - timedelta(hours=1), ends_at=now + timedelta(hours=5)
    )
    ended = owner.patch(_url(facility, shift), {"endsAt": now.isoformat()}, format="json")
    assert ended.status_code == 200
    assert get_facility_availability(facility).state is not AvailabilityState.DUTY

    future = DutyShift.objects.create(
        facility=facility,
        starts_at=now + timedelta(days=2),
        ends_at=now + timedelta(days=2, hours=3),
    )
    assert owner.delete(_url(facility, future)).status_code == 204
    assert not DutyShift.objects.filter(pk=future.pk).exists()


@pytest.mark.django_db
def test_only_members_of_that_pharmacy_may_manage_duty(
    facility: Facility, owner: APIClient
) -> None:
    stranger = User.objects.create_user(phone="+963900000003", password="x" * 12, name="S")
    start = timezone.now() + timedelta(hours=1)
    body = _body(start, start + timedelta(hours=1))
    # Not found, not forbidden: the answer must not confirm the facility exists.
    assert _client(stranger).post(_url(facility), body, format="json").status_code == 404
    assert _client(stranger).get(_url(facility)).status_code == 404
    assert APIClient().post(_url(facility), body, format="json").status_code == 401

    shift = DutyShift.objects.create(
        facility=facility, starts_at=start, ends_at=start + timedelta(hours=1)
    )
    assert _client(stranger).delete(_url(facility, shift)).status_code == 404
    assert DutyShift.objects.filter(pk=shift.pk).exists()


@pytest.mark.django_db
def test_a_shift_of_another_facility_is_not_reachable(facility: Facility, owner: APIClient) -> None:
    other = Facility.objects.create(
        category=facility.category, province=facility.province, name_ar="أخرى", status="ACTIVE"
    )
    start = timezone.now() + timedelta(hours=1)
    shift = DutyShift.objects.create(
        facility=other, starts_at=start, ends_at=start + timedelta(hours=1)
    )
    assert owner.delete(_url(facility, shift)).status_code == 404


@pytest.mark.django_db
def test_non_pharmacy_category_is_rejected(user: User, facility: Facility) -> None:
    clinic = Category.objects.create(
        group=facility.category.group, code="clinic-test", slug="clinic-test", name_ar="عيادة"
    )
    CategoryCapabilities.objects.create(category=clinic, supports_duty=False)
    place = Facility.objects.create(
        category=clinic, province=facility.province, name_ar="عيادة", status="ACTIVE"
    )
    FacilityMembership.objects.create(facility=place, user=user, role="OWNER")
    start = timezone.now() + timedelta(hours=1)
    response = _client(user).post(
        _url(place), _body(start, start + timedelta(hours=1)), format="json"
    )
    assert response.status_code == 409
    assert response.json()["code"] == "DUTY_NOT_SUPPORTED"


@pytest.mark.django_db
def test_category_without_capabilities_row_is_not_a_server_error(
    user: User, facility: Facility
) -> None:
    bare = Category.objects.create(
        group=facility.category.group, code="bare-test", slug="bare-test", name_ar="بلا"
    )
    place = Facility.objects.create(
        category=bare, province=facility.province, name_ar="بلا", status="ACTIVE"
    )
    FacilityMembership.objects.create(facility=place, user=user, role="OWNER")
    start = timezone.now() + timedelta(hours=1)
    response = _client(user).post(
        _url(place), _body(start, start + timedelta(hours=1)), format="json"
    )
    assert response.status_code == 409


@pytest.mark.django_db
def test_duty_now_uses_damascus_time_and_closure_overrides(facility: Facility) -> None:
    CategoryProvince.objects.create(
        category=facility.category, province=facility.province, public_enabled=True
    )
    # A night shift written in Damascus local time: 22:00 to 06:00 next day.
    night_start = datetime(2026, 3, 10, 22, 0, tzinfo=DAMASCUS)
    DutyShift.objects.create(
        facility=facility, starts_at=night_start, ends_at=night_start + timedelta(hours=8)
    )
    base = Facility.objects.filter(pk=facility.pk)
    at_2am_local = datetime(2026, 3, 11, 2, 0, tzinfo=DAMASCUS)
    before_local = datetime(2026, 3, 10, 21, 59, tzinfo=DAMASCUS)
    assert filter_for_availability_state(base, AvailabilityState.DUTY, at_2am_local).exists()
    assert not filter_for_availability_state(base, AvailabilityState.DUTY, before_local).exists()
    # 19:30 UTC is 22:30 in Damascus (UTC+3): on duty.
    utc_equivalent = datetime(2026, 3, 10, 19, 30, tzinfo=ZoneInfo("UTC"))
    assert filter_for_availability_state(base, AvailabilityState.DUTY, utc_equivalent).exists()

    TemporaryClosure.objects.create(
        facility=facility,
        starts_at=at_2am_local - timedelta(minutes=30),
        ends_at=at_2am_local + timedelta(hours=1),
    )
    assert not filter_for_availability_state(base, AvailabilityState.DUTY, at_2am_local).exists()
    assert filter_for_availability_state(base, AvailabilityState.TEMP_CLOSED, at_2am_local).exists()


@pytest.mark.django_db
def test_public_duty_now_filter(facility: Facility) -> None:
    CategoryProvince.objects.create(
        category=facility.category, province=facility.province, public_enabled=True
    )
    now = timezone.now()
    url = (
        f"/api/v1/public/facilities/?provinceId={facility.province_id}"
        f"&categoryId={facility.category_id}&dutyNow=true"
    )
    assert APIClient().get(url).json()["items"] == []
    DutyShift.objects.create(
        facility=facility, starts_at=now - timedelta(hours=1), ends_at=now + timedelta(hours=1)
    )
    items = APIClient().get(url).json()["items"]
    assert [item["id"] for item in items] == [str(facility.pk)]
