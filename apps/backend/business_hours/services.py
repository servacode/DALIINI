from __future__ import annotations

from dataclasses import dataclass
from datetime import UTC, datetime, time, timedelta
from enum import StrEnum
from typing import TYPE_CHECKING
from zoneinfo import ZoneInfo

from django.utils import timezone

from .models import BusinessHour, TemporaryClosure

if TYPE_CHECKING:
    from facilities.models import Facility

DAMASCUS = ZoneInfo("Asia/Damascus")


class AvailabilityState(StrEnum):
    TEMP_CLOSED = "TEMP_CLOSED"
    DUTY = "DUTY"
    OPEN = "OPEN"
    CLOSED = "CLOSED"


@dataclass(frozen=True)
class AvailabilityResult:
    state: AvailabilityState
    next_open_at: datetime | None


def _damascus(now: datetime | None) -> datetime:
    value = now or timezone.now()
    if timezone.is_naive(value):
        value = timezone.make_aware(value, DAMASCUS)
    return value.astimezone(DAMASCUS)


def _window_for(
    now_local: datetime,
    opens_at: time,
    closes_at: time,
    day_offset: int = 0,
) -> tuple[datetime, datetime]:
    base = now_local.date() + timedelta(days=day_offset)
    start = datetime.combine(base, opens_at, tzinfo=DAMASCUS)
    end_day = base if closes_at > opens_at else base + timedelta(days=1)
    end = datetime.combine(end_day, closes_at, tzinfo=DAMASCUS)
    return start, end


def _hours(facility: Facility) -> list[BusinessHour]:
    """The facility's weekly hours, from the prefetch cache when a list loaded them.

    A list page prefetches `business_hours` once for every row; a single facility reads its own
    handful of rows. Either way the engine below works on the rows in memory, so serialising a
    page of thirty facilities does not cost thirty more queries per question it asks.
    """
    return sorted(facility.business_hours.all(), key=lambda row: (row.weekday, row.opens_at))


def _is_scheduled_open(facility: Facility, now_local: datetime) -> bool:
    weekdays = {now_local.weekday(), (now_local.weekday() - 1) % 7}
    candidates = [row for row in _hours(facility) if row.weekday in weekdays]
    for row in candidates:
        offset = 0 if row.weekday == now_local.weekday() else -1
        start, end = _window_for(
            now_local,
            row.opens_at,
            row.closes_at,
            offset,
        )
        if start <= now_local < end:
            return True
    return False


def _is_temporarily_closed(facility: Facility, now_utc: datetime) -> bool:
    return TemporaryClosure.objects.filter(
        facility=facility,
        starts_at__lte=now_utc,
        ends_at__gt=now_utc,
    ).exists()


def _is_on_duty(facility: Facility, now_utc: datetime) -> bool:
    return facility.duty_shifts.filter(
        starts_at__lte=now_utc,
        ends_at__gt=now_utc,
    ).exists()


def local_day_bounds(now: datetime | None = None) -> tuple[datetime, datetime]:
    """Today, as the country lives it: midnight to midnight in Damascus, returned in UTC.

    A duty roster is published for a *day*, and the day people mean is the local one. Taking
    the bounds from UTC would move the boundary by three hours and put a shift that begins at
    22:00 on the wrong date for a third of its life.
    """
    local = _damascus(now)
    start = datetime.combine(local.date(), time(0, 0), tzinfo=DAMASCUS)
    return start.astimezone(UTC), (start + timedelta(days=1)).astimezone(UTC)


def is_on_duty_today(facility: Facility, now: datetime | None = None) -> bool:
    """Whether this facility appears on today's duty roster at all.

    Deliberately not the same question as `_is_on_duty`, which asks whether a shift is running
    at this instant. A pharmacy whose shift begins at 20:00 is on today's roster from the
    moment the day starts, and someone planning their evening needs to see that at nine in the
    morning. Any overlap with the local day counts, so a shift running past midnight belongs to
    both days it touches.
    """
    day_start, day_end = local_day_bounds(now)
    return facility.duty_shifts.filter(
        starts_at__lt=day_end,
        ends_at__gt=day_start,
    ).exists()


def is_open_now(facility: Facility, now: datetime | None = None) -> bool:
    """Whether the doors are open at this moment, whatever the duty roster says.

    Independent of duty on purpose. `get_facility_availability` collapses the two into one
    value and lets DUTY win, which cannot express a pharmacy that is on duty *and* open, nor
    one that is on duty and shut until the evening. Both are ordinary, and both have to be
    tellable apart.
    """
    now_local = _damascus(now)
    if _is_temporarily_closed(facility, now_local.astimezone(UTC)):
        return False
    return _is_scheduled_open(facility, now_local)


def get_next_open(facility: Facility, now: datetime | None = None) -> datetime | None:
    now_local = _damascus(now)
    rows = _hours(facility)
    best = None
    for delta in range(0, 8):
        weekday = (now_local.weekday() + delta) % 7
        for row in rows:
            if row.weekday != weekday:
                continue
            start, _ = _window_for(
                now_local,
                row.opens_at,
                row.closes_at,
                delta,
            )
            if start <= now_local:
                continue
            if best is None or start < best:
                best = start
    return best.astimezone(UTC) if best else None


def state_from_flags(closed: bool, duty: bool, scheduled: bool) -> AvailabilityState:
    """The state alone, from the three flags `with_availability_flags` annotates, in the same
    order of precedence as `get_facility_availability`. For callers that do not need the next
    opening time, such as map markers, which then need no hours at all."""
    if closed:
        return AvailabilityState.TEMP_CLOSED
    if duty:
        return AvailabilityState.DUTY
    if scheduled:
        return AvailabilityState.OPEN
    return AvailabilityState.CLOSED


def availability_from_flags(facility: Facility, now: datetime | None = None) -> AvailabilityResult:
    """The same answer as `get_facility_availability`, from flags a list query already computed.

    `with_availability_flags` annotates every row with whether it is temporarily closed, on duty
    and scheduled open at this moment, in the one query that fetched the page. Only the next
    opening time is left to work out, and that reads the prefetched hours.
    """
    now_local = _damascus(now)
    if getattr(facility, "_availability_closed", False):
        return AvailabilityResult(AvailabilityState.TEMP_CLOSED, get_next_open(facility, now_local))
    if getattr(facility, "_availability_duty", False):
        return AvailabilityResult(AvailabilityState.DUTY, get_next_open(facility, now_local))
    if getattr(facility, "_availability_scheduled", False):
        return AvailabilityResult(AvailabilityState.OPEN, None)
    return AvailabilityResult(AvailabilityState.CLOSED, get_next_open(facility, now_local))


def get_facility_availability(
    facility: Facility,
    now: datetime | None = None,
) -> AvailabilityResult:
    now_local = _damascus(now)
    now_utc = now_local.astimezone(UTC)
    if _is_temporarily_closed(facility, now_utc):
        return AvailabilityResult(
            AvailabilityState.TEMP_CLOSED,
            get_next_open(facility, now_local),
        )
    if _is_on_duty(facility, now_utc):
        return AvailabilityResult(
            AvailabilityState.DUTY,
            get_next_open(facility, now_local),
        )
    if _is_scheduled_open(facility, now_local):
        return AvailabilityResult(AvailabilityState.OPEN, None)
    return AvailabilityResult(
        AvailabilityState.CLOSED,
        get_next_open(facility, now_local),
    )
