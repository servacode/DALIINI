"""Review-desk helpers: the previous approved state, likely duplicates, and missing documents."""

from __future__ import annotations

import re
from typing import Any

from django.contrib.gis.measure import D
from django.db.models import Count, Exists, F, IntegerField, OuterRef, Q, QuerySet, Subquery, Value
from django.db.models.functions import Coalesce

from directory.models import VerificationRequirement
from facilities.models import Facility, FacilityApplication, VerificationEvidence

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


def missing_evidence() -> Exists:
    """True for an application whose facility lacks a required document.

    The rule submission enforces: every active, required document of the facility's category
    has at least its minimum number of files. A requirement added after submission therefore
    marks an application already in the queue, which is what a reviewer needs to see. One
    EXISTS per row, usable in `filter()` and, negated, as an annotation.
    """
    uploaded = (
        VerificationEvidence.objects.filter(
            Q(application__isnull=True) | Q(application_id=OuterRef(OuterRef("pk"))),
            facility_id=OuterRef(OuterRef("facility_id")),
            requirement_id=OuterRef("pk"),
        )
        .order_by()
        .values("requirement_id")
        .annotate(files=Count("id"))
        .values("files")[:1]
    )
    short = (
        VerificationRequirement.objects.filter(
            category_id=OuterRef("facility__category_id"), active=True, required=True
        )
        .annotate(files=Coalesce(Subquery(uploaded, output_field=IntegerField()), Value(0)))
        .filter(files__lt=F("min_files"))
    )
    return Exists(short)


def with_evidence_state(
    applications: QuerySet[FacilityApplication],
) -> QuerySet[FacilityApplication]:
    """Annotate `evidence_complete` on each application, in the query itself."""
    return applications.annotate(evidence_complete=~missing_evidence())


def evidence_complete(application: FacilityApplication) -> bool:
    """The annotated answer when the row has one, otherwise the same rule asked for this row."""
    annotated = getattr(application, "evidence_complete", None)
    if annotated is not None:
        return bool(annotated)
    return not (
        FacilityApplication.objects.filter(pk=application.pk).filter(missing_evidence()).exists()
    )
