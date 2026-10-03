"""Changes to a live facility, reviewed without taking it out of the directory.

An owner's edit to a published facility used to send it back to re-verification, and a
facility in re-verification is not public: a pharmacy that fixed a typo in its address
vanished from the directory until an operator got to it. Now the facility stays published as
it was approved, and the edit is split:

* **Reviewed fields** — what the facility is called and where it is (`REVIEWED_FIELDS`). These
  are what a hijacked or fake listing would change, so they wait for an operator. They are kept
  on a CHANGE application as `proposed_changes` and applied when it is approved.
* **Everything else** — phone, WhatsApp, description, specialties, services, hours, photos —
  takes effect at once, as it does for any owner edit.

The owner sees their proposal as saved: the owner's own view of the facility shows the proposed
values, marked as waiting (`overlay_pending`). Further edits while it waits are merged into
the same proposal and bump its `revision`; an approval names the revision the operator saw, so a
proposal changed after the operator opened it is refused rather than published unseen.
"""

from __future__ import annotations

from typing import TYPE_CHECKING, Any
from uuid import UUID

from django.contrib.gis.geos import Point
from django.core.exceptions import ValidationError
from django.utils import timezone

from audit.services import record_audit
from core.exceptions import ConflictError

from .models import Facility, FacilityApplication

if TYPE_CHECKING:
    from accounts.models import User

REVIEWED_FIELDS = (
    "nameAr",
    "nameEn",
    "addressAr",
    "addressEn",
    "cityId",
    "neighborhoodId",
    "location",
)
_TEXT = {
    "nameAr": "name_ar",
    "nameEn": "name_en",
    "addressAr": "address_ar",
    "addressEn": "address_en",
}


def takes_changes_for_review(facility: Facility) -> bool:
    """Whether an owner's edit to this facility is reviewed while it stays published."""
    return facility.status == Facility.Status.ACTIVE


def pending_change(facility: Facility, *, lock: bool = False) -> FacilityApplication | None:
    queryset = FacilityApplication.objects.filter(
        facility=facility,
        kind=FacilityApplication.Kind.CHANGE,
        status=FacilityApplication.Status.SUBMITTED,
    )
    if lock:
        queryset = queryset.select_for_update()
    return queryset.first()


def split(data: dict[str, Any]) -> tuple[dict[str, Any], dict[str, Any]]:
    """The reviewed part of an edit, and the part that takes effect at once."""
    reviewed = {key: value for key, value in data.items() if key in REVIEWED_FIELDS}
    live = {key: value for key, value in data.items() if key not in REVIEWED_FIELDS}
    return reviewed, live


def live_values(facility: Facility) -> dict[str, Any]:
    """The reviewed fields as the public sees them, in wire names and JSON values."""
    point = facility.location
    return {
        "nameAr": facility.name_ar,
        "nameEn": facility.name_en,
        "addressAr": facility.address_ar,
        "addressEn": facility.address_en,
        "cityId": str(facility.city_id) if facility.city_id else None,
        "neighborhoodId": str(facility.neighborhood_id) if facility.neighborhood_id else None,
        "location": {"latitude": point.y, "longitude": point.x} if point else None,
    }


def _wire(key: str, value: Any) -> Any:
    if key in _TEXT:
        return str(value).strip()
    if key in {"cityId", "neighborhoodId"}:
        return str(value) if value else None
    if key == "location":
        if value is None:
            return None
        return {"latitude": float(value["latitude"]), "longitude": float(value["longitude"])}
    raise KeyError(key)


def _apply_to(facility: Facility, values: dict[str, Any]) -> None:
    """Set reviewed values on `facility` in memory, checking places against each other."""
    from .services import resolve_city, resolve_neighborhood

    for key, column in _TEXT.items():
        if key in values:
            setattr(facility, column, values[key])
    if "cityId" in values:
        city_id = UUID(values["cityId"]) if values["cityId"] else None
        facility.city = resolve_city(facility=facility, city_id=city_id)
        if "neighborhoodId" not in values:
            facility.neighborhood = None
    if "neighborhoodId" in values:
        neighborhood_id = UUID(values["neighborhoodId"]) if values["neighborhoodId"] else None
        facility.neighborhood = resolve_neighborhood(
            city=facility.city if facility.city_id else None, neighborhood_id=neighborhood_id
        )
    if "location" in values:
        point = values["location"]
        facility.location = (
            Point(point["longitude"], point["latitude"], srid=4326) if point else None
        )


def propose(
    *, actor: User, facility: Facility, reviewed: dict[str, Any], request_id: str = ""
) -> FacilityApplication | None:
    """Merge `reviewed` into the facility's waiting proposal, or open one.

    `facility` must be locked by the caller. Values equal to what is already public are
    dropped; a proposal left with nothing in it is withdrawn, since nothing remains to review.
    """
    from .services import application_snapshot

    waiting = pending_change(facility, lock=True)
    current = live_values(facility)
    merged = {**(waiting.proposed_changes if waiting else {})}
    for key, value in reviewed.items():
        merged[key] = _wire(key, value)
    merged = {key: value for key, value in merged.items() if value != current[key]}
    if "nameAr" in merged and not merged["nameAr"]:
        raise ValidationError({"nameAr": "Facility Arabic name is required."})

    # Check the proposal as a whole now, on a copy, so the owner hears about a city outside
    # the province when they save rather than when an operator approves.
    trial = Facility.objects.get(pk=facility.pk)
    _apply_to(trial, merged)
    trial.full_clean(exclude=["location"])

    if not merged:
        if waiting is not None:
            waiting.delete()
            record_audit(
                actor=actor,
                action="facility.owner_change.withdrawn",
                target=facility,
                metadata={"facilityId": str(facility.pk)},
                request_id=request_id,
            )
        return None

    now = timezone.now()
    if waiting is None:
        waiting = FacilityApplication(
            facility=facility,
            kind=FacilityApplication.Kind.CHANGE,
            status=FacilityApplication.Status.SUBMITTED,
        )
    waiting.proposed_changes = merged
    waiting.revision += 1
    waiting.submitted_at = now
    waiting.snapshot = application_snapshot(trial)
    waiting.save()
    record_audit(
        actor=actor,
        action="facility.owner_change.submitted",
        target=waiting,
        before_snapshot={key: current[key] for key in merged},
        after_snapshot=merged,
        metadata={"facilityId": str(facility.pk), "revision": waiting.revision},
        request_id=request_id,
    )
    return waiting


def apply_approved(
    facility: Facility, application: FacilityApplication, revision: int | None
) -> None:
    """Publish an approved proposal. The caller holds both rows locked."""
    if revision is not None and revision != application.revision:
        raise ConflictError(
            "APPLICATION_CHANGED",
            message="عدّل المالك طلبه بعد أن فتحته. أعد فتح الطلب وراجع النسخة الأخيرة.",
            details={"revision": [str(application.revision)]},
        )
    try:
        _apply_to(facility, application.proposed_changes or {})
    except ValidationError as exc:
        # The places it names changed since it was proposed (a city retired, say).
        raise ConflictError(
            "APPLICATION_NO_LONGER_VALID",
            message=(
                "لم يعد التعديل المطلوب صالحاً، فقد تغيّرت المدن أو الأحياء منذ إرساله. "
                "ارفضه مع ذكر السبب."
            ),
        ) from exc
    facility.full_clean(exclude=["location"])


def review_snapshots(application: FacilityApplication) -> tuple[dict[str, Any], dict[str, Any]]:
    """What the operator compares for a CHANGE: the public facility now, and it with the
    proposal applied. Computed at read time, so what changed live meanwhile (a phone number)
    is the same on both sides and only the proposal shows as a difference."""
    from .services import application_snapshot

    facility = application.facility
    before = application_snapshot(facility)
    before["approvedAt"] = (
        facility.last_verified_at.isoformat() if facility.last_verified_at else None
    )
    # The approval date stays on both sides, so only the proposal reads as a difference.
    after = dict(before)
    for key, value in (application.proposed_changes or {}).items():
        after[key] = value
        if key == "location":
            after["hasLocation"] = value is not None
    return after, before


def overlay_pending(facility: Facility, payload: dict[str, Any]) -> dict[str, Any]:
    """The owner's view: their waiting proposal shown as saved, and marked as waiting."""
    waiting = pending_change(facility)
    if waiting is None:
        return {**payload, "pendingChange": None}
    proposed = waiting.proposed_changes or {}
    shown = dict(payload)
    for key, value in proposed.items():
        if key in shown:
            shown[key] = value if value != "" else None
    return {
        **shown,
        "pendingChange": {
            "id": str(waiting.pk),
            "proposedFields": sorted(proposed),
            "submittedAt": waiting.submitted_at.isoformat() if waiting.submitted_at else None,
        },
    }
