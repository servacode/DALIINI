"""Advertisement lifecycle, as `Cycle K` describes it.

`04-OPERATING-CYCLES.md` Cycle K is: create, upload and validate the image, set targeting
and schedule, preview, activate, and let the public API filter by time and scope. Editing an
existing advertisement is part of running that loop — a schedule that needs moving or copy
that needs correcting should not force an operator to delete and recreate, which would lose
the audit thread and the id the analytics reference.

The field mapping lives in one place so create and update cannot drift. Validation is the
model's: `Advertisement.clean()` already decides that the end must follow the start, that
the slide duration is bounded, that a global advertisement targets nothing and a scoped one
targets something, and that the action payload matches its action type.

No new field is invented here. `06-DATA-MODEL.md` fixes the set.
"""

from typing import Any

from django.db import transaction

from audit.services import record_audit

from .models import Advertisement

# Wire name -> model field. The whole editable surface of an advertisement, and the only
# surface: anything absent from this map cannot be set through the Admin.
EDITABLE_FIELDS = (
    ("imageKey", "image_key", str),
    ("titleAr", "title_ar", str),
    ("titleEn", "title_en", str),
    ("subtitleAr", "subtitle_ar", str),
    ("subtitleEn", "subtitle_en", str),
    ("actionType", "action_type", str),
    ("actionPayload", "action_payload", None),
    ("targetScope", "target_scope", str),
    ("provinceId", "province_id", None),
    ("categoryId", "category_id", None),
    ("startsAt", "starts_at", None),
    ("endsAt", "ends_at", None),
    ("enabled", "enabled", bool),
    ("sortOrder", "sort_order", int),
    ("slideDurationMs", "slide_duration_ms", int),
)


def _snapshot(advertisement: Advertisement) -> dict[str, Any]:
    """Audit snapshot. Copy and payload are omitted; what matters operationally is reach."""
    return {
        "titleAr": advertisement.title_ar,
        "targetScope": advertisement.target_scope,
        "provinceId": str(advertisement.province_id) if advertisement.province_id else None,
        "categoryId": str(advertisement.category_id) if advertisement.category_id else None,
        "enabled": advertisement.enabled,
        "startsAt": advertisement.starts_at.isoformat() if advertisement.starts_at else None,
        "endsAt": advertisement.ends_at.isoformat() if advertisement.ends_at else None,
        "sortOrder": advertisement.sort_order,
    }


def apply_fields(advertisement: Advertisement, data: dict[str, Any]) -> Advertisement:
    """Copy the editable wire fields the caller actually sent onto the instance.

    Only keys present in `data` are touched, on create and on update alike. The model
    supplies the defaults for everything else, and an editor that loads one tab and saves it
    cannot blank the fields it never showed.
    """
    for wire, field, cast in EDITABLE_FIELDS:
        if wire not in data:
            continue
        value = data[wire]
        if cast is not None and value is not None:
            value = cast(value)
        setattr(advertisement, field, value)
    return advertisement


@transaction.atomic
def save_advertisement(*, actor: Any, advertisement: Advertisement) -> Advertisement:
    creating = advertisement._state.adding
    before = None if creating else _snapshot(Advertisement.objects.get(pk=advertisement.pk))
    advertisement.full_clean()
    advertisement.save()
    record_audit(
        actor=actor,
        action="advertisement.created" if creating else "advertisement.updated",
        target=advertisement,
        before_snapshot=before,
        after_snapshot=_snapshot(advertisement),
        metadata={
            "enabled": advertisement.enabled,
            "targetScope": advertisement.target_scope,
            "actionType": advertisement.action_type,
        },
    )
    return advertisement


@transaction.atomic
def update_advertisement(
    *, actor: Any, advertisement: Advertisement, data: dict[str, Any]
) -> Advertisement:
    apply_fields(advertisement, data)
    return save_advertisement(actor=actor, advertisement=advertisement)


@transaction.atomic
def delete_advertisement(*, actor: Any, advertisement: Advertisement) -> None:
    record_audit(
        actor=actor,
        action="advertisement.deleted",
        target=advertisement,
        before_snapshot=_snapshot(advertisement),
        metadata={"enabled": advertisement.enabled},
    )
    advertisement.delete()
