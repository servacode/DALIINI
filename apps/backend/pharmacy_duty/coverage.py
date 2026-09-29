"""Which days of the duty roster nobody covers. The one definition of a duty gap.

The admin alerts (DUTY_GAP), the admin roster (`gap: true`), province launch readiness and
the daily nudge to pharmacists all ask this module, so they cannot disagree.

A day is a Damascus calendar day, midnight to midnight, and it is covered when at least one
duty shift of an ACTIVE facility whose category supports duty overlaps it. A night shift
from 22:00 to 06:00 therefore covers both days it touches, as the public "on duty today"
roster already counts it.

Duty is scheduled per facility and a facility belongs to a province (and optionally a city),
so gaps are computed per province, or per city when one is asked for.
"""

from __future__ import annotations

from collections import defaultdict
from collections.abc import Iterable
from datetime import UTC, date, datetime, time, timedelta
from typing import Any

from django.db.models import QuerySet
from django.utils import timezone

from business_hours.services import DAMASCUS
from directory.models import CategoryProvince
from facilities.models import Facility
from locations.models import Province

from .models import DutyShift

DEFAULT_WINDOW_DAYS = 14


def local_today(now: datetime | None = None) -> date:
    return (now or timezone.now()).astimezone(DAMASCUS).date()


def window(start: date, days: int) -> list[date]:
    return [start + timedelta(days=offset) for offset in range(days)]


def day_bounds(day: date) -> tuple[datetime, datetime]:
    """A Damascus day as a UTC half-open interval. Combined per day, so DST is respected."""
    start = datetime.combine(day, time(0, 0), tzinfo=DAMASCUS)
    end = datetime.combine(day + timedelta(days=1), time(0, 0), tzinfo=DAMASCUS)
    return start.astimezone(UTC), end.astimezone(UTC)


def duty_facilities() -> QuerySet[Facility]:
    """Facilities whose shifts count: ACTIVE, in a category that supports duty."""
    return Facility.objects.filter(
        status=Facility.Status.ACTIVE, category__capabilities__supports_duty=True
    )


def duty_provinces() -> QuerySet[Province]:
    """Active provinces where a duty category is offered to the public.

    A province that offers no duty category has no roster to keep, so it has no gaps.
    """
    offered = CategoryProvince.objects.filter(
        public_enabled=True,
        category__active=True,
        category__capabilities__supports_duty=True,
    ).values("province_id")
    return Province.objects.filter(active=True, pk__in=offered).order_by("sort_order", "name_ar")


def shifts_in(
    days: list[date], *, province_ids: Iterable[Any], city_id: Any = None
) -> QuerySet[DutyShift]:
    """Counting shifts that overlap any of `days` (which must be consecutive)."""
    first, _ = day_bounds(days[0])
    _, last = day_bounds(days[-1])
    facilities = duty_facilities().filter(province_id__in=list(province_ids))
    if city_id:
        facilities = facilities.filter(city_id=city_id)
    return DutyShift.objects.filter(facility__in=facilities, starts_at__lt=last, ends_at__gt=first)


def duty_gaps(
    days: list[date], *, province_ids: Iterable[Any], city_id: Any = None
) -> dict[str, list[date]]:
    """Province id -> the days in `days` with no counting shift, in date order."""
    ids = [str(value) for value in province_ids]
    if not days or not ids:
        return {}
    bounds = [day_bounds(day) for day in days]
    covered: dict[str, set[int]] = defaultdict(set)
    rows = shifts_in(days, province_ids=ids, city_id=city_id).values_list(
        "facility__province_id", "starts_at", "ends_at"
    )
    for province_id, starts_at, ends_at in rows:
        marks = covered[str(province_id)]
        for index, (day_start, day_end) in enumerate(bounds):
            if starts_at < day_end and ends_at > day_start:
                marks.add(index)
    return {
        province_id: [day for index, day in enumerate(days) if index not in covered[province_id]]
        for province_id in ids
    }
