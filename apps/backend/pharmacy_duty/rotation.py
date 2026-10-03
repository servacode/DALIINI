"""Turning a saved rotation into the shifts of a period (`DutyRotation`)."""

from __future__ import annotations

from datetime import date, datetime, timedelta
from uuid import UUID

from django.utils import timezone

from business_hours.services import DAMASCUS
from facilities.models import Facility

from .importer import RowResult
from .models import DutyRotation

LONGEST_PERIOD = timedelta(days=93)


def on_duty(rotation: DutyRotation, day: date) -> list[str]:
    """The facility ids the rotation puts on duty on `day`."""
    ids = list(rotation.facility_ids)
    if not ids:
        return []
    offset = (day - rotation.anchor_date).days * rotation.per_day
    return [ids[(offset + k) % len(ids)] for k in range(min(rotation.per_day, len(ids)))]


def period_problem(first: date, last: date) -> str | None:
    """Why a period cannot be generated, worded for the operator; None when it can."""
    if last < first:
        return "نهاية الفترة قبل بدايتها."
    if last - first > LONGEST_PERIOD:
        return "الفترة أطول من ثلاثة أشهر؛ ولّد الجدول على مراحل."
    return None


def rows_for(rotation: DutyRotation, first: date, last: date) -> list[RowResult]:
    """One checked row per shift the rotation generates between `first` and `last`.

    The caller has checked the period with `period_problem`."""
    if period_problem(first, last):
        raise ValueError("Period not checked with period_problem().")
    names: dict[str, str] = {
        str(pk): name
        for pk, name in Facility.objects.filter(
            pk__in=[UUID(value) for value in rotation.facility_ids]
        )
        .exclude(status=Facility.Status.CLOSED)
        .values_list("pk", "name_ar")
    }
    rows: list[RowResult] = []
    line = 1
    day = first
    while day <= last:
        for facility_id in on_duty(rotation, day):
            line += 1
            starts = timezone.make_aware(datetime.combine(day, rotation.starts_at_time), DAMASCUS)
            ends = timezone.make_aware(datetime.combine(day, rotation.ends_at_time), DAMASCUS)
            if ends <= starts:
                ends += timedelta(days=1)
            row = RowResult(
                line=line,
                facility_id=facility_id,
                facility_name=names.get(facility_id),
                starts_at=starts,
                ends_at=ends,
            )
            if facility_id not in names:
                row.errors.append("هذه الصيدلية أُغلقت أو لم تعد موجودة؛ أزلها من القالب.")
            rows.append(row)
        day += timedelta(days=1)
    return rows
