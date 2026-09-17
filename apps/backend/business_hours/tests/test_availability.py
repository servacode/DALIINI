from datetime import UTC, datetime, time, timedelta
from zoneinfo import ZoneInfo

import pytest

from business_hours.models import BusinessHour, TemporaryClosure
from business_hours.services import AvailabilityState, get_facility_availability
from pharmacy_duty.models import DutyShift


@pytest.mark.django_db
def test_temporary_closure_wins(facility):
    now = datetime(2026, 9, 17, 9, 0, tzinfo=UTC)
    local = now.astimezone(ZoneInfo("Asia/Damascus"))
    BusinessHour.objects.create(
        facility=facility,
        weekday=local.weekday(),
        opens_at=time(0, 1),
        closes_at=time(23, 59),
    )
    DutyShift.objects.create(
        facility=facility,
        starts_at=now - timedelta(hours=1),
        ends_at=now + timedelta(hours=1),
    )
    TemporaryClosure.objects.create(
        facility=facility,
        starts_at=now - timedelta(minutes=1),
        ends_at=now + timedelta(minutes=30),
    )
    result = get_facility_availability(facility, now)
    assert result.state is AvailabilityState.TEMP_CLOSED


@pytest.mark.django_db
def test_duty_wins_over_regular_hours(facility):
    now = datetime(2026, 9, 17, 9, 0, tzinfo=UTC)
    DutyShift.objects.create(
        facility=facility,
        starts_at=now - timedelta(minutes=1),
        ends_at=now + timedelta(minutes=30),
    )
    result = get_facility_availability(facility, now)
    assert result.state is AvailabilityState.DUTY


@pytest.mark.django_db
def test_overnight_business_hour_is_open_from_prior_day(facility):
    now = datetime(2026, 9, 17, 22, 30, tzinfo=UTC)
    local = now.astimezone(ZoneInfo("Asia/Damascus"))
    BusinessHour.objects.create(
        facility=facility,
        weekday=(local.weekday() - 1) % 7,
        opens_at=time(20),
        closes_at=time(2),
    )
    result = get_facility_availability(facility, now)
    assert result.state is AvailabilityState.OPEN
