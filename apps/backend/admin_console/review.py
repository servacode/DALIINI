"""Review-desk helpers: the previous approved state and likely duplicates of a facility."""

from __future__ import annotations

import re
from typing import Any

from django.contrib.gis.measure import D
from django.db.models import Q

from facilities.models import Facility, FacilityApplication

DUPLICATE_REASON_CHOICES = [
    ("SAME_PHONE", "Same phone"),
    ("SAME_NAME_NEARBY", "Same name nearby"),
]
DUPLICATE_RADIUS_METERS = 200
DUPLICATE_LIMIT = 5

# Arabic diacritics (harakat), tatweel and common letter variants collapse, so "صيدليّة"
# and "صيدلية" compare equal.
_DIACRITICS = re.compile(r"[\u064B-\u065F\u0670\u0640]")
_VARIANTS = str.maketrans({"أ": "ا", "إ": "ا", "آ": "ا", "ى": "ي", "ة": "ه", "ؤ": "و", "ئ": "ي"})


def normalize_name(value: str) -> str:
    text = _DIACRITICS.sub("", (value or "").strip().lower()).translate(_VARIANTS)
    return re.sub(r"\s+", " ", text)


def previous_snapshot(application: FacilityApplication) -> dict[str, Any] | None:
    """The last approved snapshot of the facility before this application, if any."""
    previous = (
        FacilityApplication.objects.filter(
            facility_id=application.facility_id, status=FacilityApplication.Status.APPROVED
        )
        .exclude(pk=application.pk)
        .order_by("-reviewed_at", "-updated_at")
        .first()
    )
    if previous is None:
        return None
    snapshot: dict[str, Any] = dict(previous.snapshot or {})
    snapshot["approvedAt"] = previous.reviewed_at.isoformat() if previous.reviewed_at else None
    return snapshot


def find_duplicates(facility: Facility) -> list[dict[str, Any]]:
    """Other facilities with the same normalized name nearby, or the same phone."""
    name = normalize_name(facility.name_ar)
    conditions = Q()
    if facility.location is not None:
        conditions |= Q(location__distance_lte=(facility.location, D(m=DUPLICATE_RADIUS_METERS)))
    if facility.phone:
        conditions |= Q(phone=facility.phone)
    if not conditions:
        return []
    candidates = (
        Facility.objects.exclude(pk=facility.pk)
        .filter(conditions)
        .only("id", "name_ar", "status", "phone", "location")
        .order_by("-updated_at")[:100]
    )
    results: list[dict[str, Any]] = []
    for other in candidates:
        reasons = []
        if facility.phone and other.phone == facility.phone:
            reasons.append("SAME_PHONE")
        if (
            facility.location is not None
            and other.location is not None
            and normalize_name(other.name_ar) == name
        ):
            reasons.append("SAME_NAME_NEARBY")
        if reasons:
            results.append(
                {
                    "id": str(other.pk),
                    "nameAr": other.name_ar,
                    "status": other.status,
                    "reasons": reasons,
                }
            )
        if len(results) >= DUPLICATE_LIMIT:
            break
    return results
