"""Open now and on duty today are two questions, and a row answers both.

`availability.state` cannot carry this. It is one value and DUTY outranks OPEN, so a pharmacy
that is on tonight's roster and serving customers right now reports DUTY and loses the fact
that its doors are open. The two booleans beside it are independent on purpose, and the three
combinations people actually meet — open and on duty, shut and on duty, open and not on duty —
each have to come back distinguishable.
"""

from datetime import datetime, time, timedelta
from zoneinfo import ZoneInfo

import pytest
from django.utils import timezone
from rest_framework.test import APIClient

from business_hours.models import BusinessHour
from business_hours.services import is_on_duty_today, is_open_now
from directory.models import CategoryProvince
from facilities.models import Facility, FacilityImage
from pharmacy_duty.models import DutyShift

FACILITIES = "/api/v1/public/facilities/"
DAMASCUS = ZoneInfo("Asia/Damascus")


def _local_now():
    return timezone.now().astimezone(DAMASCUS)


def _open_all_day(facility):
    """Business hours that cover the whole of today, without wrapping past midnight."""
    BusinessHour.objects.create(
        facility=facility,
        weekday=_local_now().weekday(),
        opens_at=time(0, 0),
        closes_at=time(23, 59, 59),
    )


def _on_duty_all_day(facility):
    local = _local_now()
    start = datetime.combine(local.date(), time(0, 0), tzinfo=DAMASCUS)
    DutyShift.objects.create(
        facility=facility,
        starts_at=start,
        ends_at=start + timedelta(hours=23, minutes=59),
    )


@pytest.fixture
def listed(db, facility):
    CategoryProvince.objects.create(
        province=facility.province,
        category=facility.category,
        public_enabled=True,
        owner_registration_enabled=True,
    )
    return facility


def _sibling(facility, name):
    return Facility.objects.create(
        category=facility.category,
        province=facility.province,
        name_ar=name,
        status=Facility.Status.ACTIVE,
    )


def _rows(province_id, **params):
    body = APIClient().get(FACILITIES, {"provinceId": str(province_id), **params}).json()
    return {item["nameAr"]: item for item in body["items"]}


# --- the three combinations, over the wire -----------------------------------------------


@pytest.mark.django_db
def test_the_three_combinations_come_back_distinguishable(listed):
    both = _sibling(listed, "مفتوحة ومناوبة")
    _open_all_day(both)
    _on_duty_all_day(both)
    shut_on_duty = _sibling(listed, "مغلقة ومناوبة")
    _on_duty_all_day(shut_on_duty)
    open_only = _sibling(listed, "مفتوحة فقط")
    _open_all_day(open_only)

    rows = _rows(listed.province_id)

    assert (
        rows["مفتوحة ومناوبة"]["availability"]["isOpenNow"],
        rows["مفتوحة ومناوبة"]["availability"]["isOnDutyToday"],
    ) == (True, True)
    assert (
        rows["مغلقة ومناوبة"]["availability"]["isOpenNow"],
        rows["مغلقة ومناوبة"]["availability"]["isOnDutyToday"],
    ) == (False, True)
    assert (
        rows["مفتوحة فقط"]["availability"]["isOpenNow"],
        rows["مفتوحة فقط"]["availability"]["isOnDutyToday"],
    ) == (True, False)


@pytest.mark.django_db
def test_being_on_duty_no_longer_hides_being_open(listed):
    both = _sibling(listed, "مفتوحة ومناوبة")
    _open_all_day(both)
    _on_duty_all_day(both)

    row = _rows(listed.province_id)["مفتوحة ومناوبة"]

    # The legacy single value still says DUTY, which is exactly why it cannot be the source
    # for a badge: the facility is open, and only the boolean says so.
    assert row["availability"]["state"] == "DUTY"
    assert row["availability"]["isOpenNow"] is True


# --- the filters combine ------------------------------------------------------------------


@pytest.mark.django_db
def test_open_now_keeps_facilities_that_are_also_on_duty(listed):
    both = _sibling(listed, "مفتوحة ومناوبة")
    _open_all_day(both)
    _on_duty_all_day(both)

    assert "مفتوحة ومناوبة" in _rows(listed.province_id, openNow="true")


@pytest.mark.django_db
def test_open_now_and_duty_today_together_mean_both(listed):
    both = _sibling(listed, "مفتوحة ومناوبة")
    _open_all_day(both)
    _on_duty_all_day(both)
    shut_on_duty = _sibling(listed, "مغلقة ومناوبة")
    _on_duty_all_day(shut_on_duty)
    open_only = _sibling(listed, "مفتوحة فقط")
    _open_all_day(open_only)

    rows = _rows(listed.province_id, openNow="true", dutyToday="true")

    assert set(rows) == {"مفتوحة ومناوبة"}


@pytest.mark.django_db
def test_duty_today_alone_keeps_the_shut_one(listed):
    shut_on_duty = _sibling(listed, "مغلقة ومناوبة")
    _on_duty_all_day(shut_on_duty)
    open_only = _sibling(listed, "مفتوحة فقط")
    _open_all_day(open_only)

    assert set(_rows(listed.province_id, dutyToday="true")) == {"مغلقة ومناوبة"}


# --- today is not this minute --------------------------------------------------------------


@pytest.mark.django_db
def test_a_shift_later_today_counts_as_on_duty_today_but_not_on_duty_now(listed):
    day = datetime(2026, 9, 24, 0, 0, tzinfo=DAMASCUS)
    DutyShift.objects.create(
        facility=listed,
        starts_at=day + timedelta(hours=20),
        ends_at=day + timedelta(hours=23),
    )
    morning = day + timedelta(hours=9)

    assert is_on_duty_today(listed, morning) is True
    assert is_open_now(listed, morning) is False


@pytest.mark.django_db
def test_a_shift_that_runs_past_midnight_belongs_to_both_days(listed):
    day = datetime(2026, 9, 24, 0, 0, tzinfo=DAMASCUS)
    DutyShift.objects.create(
        facility=listed,
        starts_at=day + timedelta(hours=22),
        ends_at=day + timedelta(hours=30),
    )

    # The night it was published for, and the morning it ran into: on the roster for both.
    assert is_on_duty_today(listed, day + timedelta(hours=23)) is True
    assert is_on_duty_today(listed, day + timedelta(hours=25)) is True
    assert is_on_duty_today(listed, day + timedelta(hours=40)) is True
    # The day after it ended is not its day.
    assert is_on_duty_today(listed, day + timedelta(hours=60)) is False


@pytest.mark.django_db
def test_the_local_day_is_what_bounds_today_not_utc(listed):
    """Damascus runs ahead of UTC, so a late-evening shift would fall on the wrong UTC date."""
    day = datetime(2026, 9, 24, 0, 0, tzinfo=DAMASCUS)
    DutyShift.objects.create(
        facility=listed,
        starts_at=day + timedelta(hours=22, minutes=30),
        ends_at=day + timedelta(hours=23, minutes=30),
    )

    assert is_on_duty_today(listed, day + timedelta(hours=10)) is True


# --- the picture a row shows ----------------------------------------------------------------


@pytest.mark.django_db
def test_a_row_carries_the_owners_first_photograph(listed):
    FacilityImage.objects.create(facility=listed, storage_key="facilities/second.jpg", sort_order=2)
    FacilityImage.objects.create(facility=listed, storage_key="facilities/first.jpg", sort_order=1)

    url = _rows(listed.province_id)[listed.name_ar]["imageUrl"]

    assert url is not None
    assert url.endswith("facilities/first.jpg")


@pytest.mark.django_db
def test_a_row_without_a_photograph_says_so_rather_than_inventing_one(listed):
    assert _rows(listed.province_id)[listed.name_ar]["imageUrl"] is None


# --- nearest is an ordering, not a filter -----------------------------------------------------


@pytest.mark.django_db
def test_sort_name_keeps_the_distances_it_no_longer_orders_by(listed):
    """Asking for the whole province by name must not cost the reader the distances."""
    Facility.objects.filter(pk=listed.pk).update(location="SRID=4326;POINT(39.01 35.95)")

    rows = _rows(listed.province_id, latitude="35.95", longitude="39.02", sort="name")

    assert rows[listed.name_ar]["distanceMeters"] is not None


@pytest.mark.django_db
def test_sort_nearest_without_coordinates_falls_back_rather_than_failing(listed):
    body = APIClient().get(
        FACILITIES, {"provinceId": str(listed.province_id), "sort": "nearest"}
    )

    assert body.status_code == 200
