from datetime import UTC, datetime, time, timedelta
from zoneinfo import ZoneInfo

import pytest

from business_hours.models import BusinessHour, TemporaryClosure
from business_hours.services import AvailabilityState, get_facility_availability
from facilities.models import Facility
from pharmacy_duty.models import DutyShift


@pytest.mark.django_db
def test_temporary_closure_wins(facility: Facility) -> None:
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
def test_duty_wins_over_regular_hours(facility: Facility) -> None:
    now = datetime(2026, 9, 17, 9, 0, tzinfo=UTC)
    DutyShift.objects.create(
        facility=facility,
        starts_at=now - timedelta(minutes=1),
        ends_at=now + timedelta(minutes=30),
    )
    result = get_facility_availability(facility, now)
    assert result.state is AvailabilityState.DUTY


@pytest.mark.django_db
def test_overnight_business_hour_is_open_from_prior_day(facility: Facility) -> None:
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


@pytest.mark.django_db
@pytest.mark.parametrize("case", ["closed", "duty", "open", "shut"])
def test_the_flags_alone_give_the_engine_s_state(facility: Facility, case: str) -> None:
    """Map markers take their state from the query's flags (DECISION-083); it must be the
    state the facility's own page shows."""
    from business_hours.query import with_availability_flags
    from business_hours.services import state_from_flags

    now = datetime(2026, 9, 17, 9, 0, tzinfo=UTC)
    local = now.astimezone(ZoneInfo("Asia/Damascus"))
    if case in ("open", "closed"):
        BusinessHour.objects.create(
            facility=facility, weekday=local.weekday(), opens_at=time(0, 1), closes_at=time(23, 59)
        )
    if case in ("duty", "closed"):
        DutyShift.objects.create(
            facility=facility, starts_at=now - timedelta(hours=1), ends_at=now + timedelta(hours=1)
        )
    if case == "closed":
        TemporaryClosure.objects.create(
            facility=facility,
            starts_at=now - timedelta(minutes=1),
            ends_at=now + timedelta(minutes=30),
        )
    row = with_availability_flags(Facility.objects.filter(pk=facility.pk), now).get()
    flags = (row._availability_closed, row._availability_duty, row._availability_scheduled)
    assert state_from_flags(*flags) is get_facility_availability(facility, now).state
