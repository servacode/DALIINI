"""Every way a duty shift reaches the roster goes through here.

The owner endpoints, the admin roster and any future bulk import (a CSV or a partner API)
share one validation path, so a rule added here holds for all of them:

- the facility's category must support duty (only pharmacies do);
- the shift must end after it starts;
- it must not overlap another shift of the same facility (a PostgreSQL exclusion
  constraint, so two concurrent writers cannot both win);
- it must not overlap a temporary closure of the facility, which would advertise a closed
  pharmacy as the one on duty.

`upsert_duty_shifts` is the entry point for anything that writes more than one shift or
writes on someone else's behalf. It records where each shift came from (`source`) and is
idempotent for imports: a row identical to an existing shift is reported as unchanged.
"""

from __future__ import annotations

from collections.abc import Iterable, Mapping
from dataclasses import dataclass
from datetime import datetime
from typing import Any

from django.core.exceptions import ValidationError as DjangoValidationError
from django.db import IntegrityError, transaction

from business_hours.models import TemporaryClosure
from core.exceptions import ConflictError
from facilities.models import Facility

from .models import DutyShift


def duty_error() -> ConflictError:
    """The shared duty rejection.

    Overlap and an invalid range report the same code on purpose: telling the caller which of
    the two it was would disclose that another facility already holds the slot.
    """
    return ConflictError(
        "DUTY_OVERLAP_OR_INVALID",
        message="الوردية تتعارض مع وردية أخرى أو أن بياناتها غير صالحة.",
    )


def require_duty_capability(facility: Facility) -> None:
    capabilities = getattr(facility.category, "capabilities", None)
    if capabilities is None or not capabilities.supports_duty:
        raise ConflictError(
            "DUTY_NOT_SUPPORTED",
            message="هذا التصنيف لا يدعم ورديات المناوبة.",
        )


def save_shift(shift: DutyShift) -> DutyShift:
    """Validate and store one shift. Raises ConflictError with a stable code."""
    require_duty_capability(shift.facility)
    if shift.starts_at and shift.ends_at and shift.ends_at > shift.starts_at:
        closed = TemporaryClosure.objects.filter(
            facility_id=shift.facility_id,
            starts_at__lt=shift.ends_at,
            ends_at__gt=shift.starts_at,
        ).exists()
        if closed:
            raise ConflictError(
                "DUTY_DURING_CLOSURE",
                message="لا يمكن جدولة مناوبة خلال إغلاق مؤقت للمنشأة.",
            )
    try:
        with transaction.atomic():
            shift.full_clean()
            shift.save()
    except (DjangoValidationError, IntegrityError) as exc:
        raise duty_error() from exc
    return shift


@dataclass(frozen=True)
class DutyUpsertResult:
    shift: DutyShift
    # CREATED, UPDATED or UNCHANGED.
    outcome: str
    before: dict[str, Any] | None


def shift_snapshot(shift: DutyShift) -> dict[str, Any]:
    return {
        "facilityId": str(shift.facility_id),
        "startsAt": shift.starts_at.isoformat(),
        "endsAt": shift.ends_at.isoformat(),
        "source": shift.source,
    }


@transaction.atomic
def upsert_duty_shifts(rows: Iterable[Mapping[str, Any]], *, source: str) -> list[DutyUpsertResult]:
    """Create or change shifts, all or nothing.

    Each row carries `facility_id`, `starts_at` and `ends_at`, and optionally `id` to change
    an existing shift of that facility. A row without `id` that matches an existing shift of
    the facility exactly is UNCHANGED, which makes re-running an import harmless. `source`
    (OWNER, ADMIN or IMPORT) is recorded on created shifts; a changed shift keeps the source
    it was created with. The first invalid row raises and nothing is written.
    """
    if source not in DutyShift.Source.values:
        raise ValueError(f"Unknown duty source: {source}")
    results: list[DutyUpsertResult] = []
    for row in rows:
        facility = Facility.objects.select_related("category__capabilities").get(
            pk=row["facility_id"]
        )
        starts_at: datetime = row["starts_at"]
        ends_at: datetime = row["ends_at"]
        if row.get("id"):
            shift = DutyShift.objects.select_for_update().get(pk=row["id"], facility=facility)
            before = shift_snapshot(shift)
            if shift.starts_at == starts_at and shift.ends_at == ends_at:
                results.append(DutyUpsertResult(shift, "UNCHANGED", before))
                continue
            shift.starts_at, shift.ends_at = starts_at, ends_at
            shift.facility = facility
            results.append(DutyUpsertResult(save_shift(shift), "UPDATED", before))
            continue
        existing = DutyShift.objects.filter(
            facility=facility, starts_at=starts_at, ends_at=ends_at
        ).first()
        if existing is not None:
            results.append(DutyUpsertResult(existing, "UNCHANGED", None))
            continue
        shift = DutyShift(facility=facility, starts_at=starts_at, ends_at=ends_at, source=source)
        results.append(DutyUpsertResult(save_shift(shift), "CREATED", None))
    return results
