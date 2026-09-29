"""How complete and current a facility's public record is, computed in SQL.

The score is 100 minus a fixed penalty per issue, so an operator can read it back: a
facility at 70 is missing something worth 30. Every signal is an annotation on the same
query that lists facilities, so sorting and filtering by quality never loads rows into
Python and a page of 250 facilities costs one query, not one per row.

What each issue means:

- NO_PHOTOS / NO_HOURS: none recorded, and only when the category supports photos / hours
  (a category without a capability row gets the model defaults, which support both).
- NO_LOCATION / NO_PHONE: the field is empty.
- STALE: nothing changed or was confirmed for `STALE_AFTER_DAYS`. "Changed or confirmed" is
  the latest of the record's `updated_at` (any edit, submission or status change),
  `hours_confirmed_at` (the owner confirmed or replaced the hours) and `last_verified_at` (an
  operator approved it).
- OPEN_REPORTS: at least one problem report is still OPEN.
- NOT_VERIFIED_RECENTLY: never approved, or last approved over `VERIFIED_WITHIN_DAYS` ago.
"""

from __future__ import annotations

from datetime import datetime, timedelta
from typing import Any

from django.db.models import (
    BooleanField,
    Case,
    Count,
    Exists,
    F,
    IntegerField,
    OuterRef,
    Q,
    QuerySet,
    Subquery,
    Value,
    When,
)
from django.db.models.functions import Coalesce, Greatest
from django.utils import timezone

from business_hours.models import BusinessHour
from facilities.models import Facility, FacilityImage, FacilityReport

STALE_AFTER_DAYS = 90
VERIFIED_WITHIN_DAYS = 180

# Code -> penalty. The penalties add up to exactly 100.
ISSUE_WEIGHTS: dict[str, int] = {
    "NO_LOCATION": 20,
    "NO_PHONE": 20,
    "NO_HOURS": 15,
    "OPEN_REPORTS": 15,
    "NO_PHOTOS": 10,
    "STALE": 10,
    "NOT_VERIFIED_RECENTLY": 10,
}
QUALITY_ISSUE_CHOICES = [
    ("NO_PHOTOS", "No photos"),
    ("NO_HOURS", "No opening hours"),
    ("NO_LOCATION", "No map location"),
    ("NO_PHONE", "No phone number"),
    ("STALE", "Not updated or confirmed recently"),
    ("OPEN_REPORTS", "Open problem reports"),
    ("NOT_VERIFIED_RECENTLY", "Not verified recently"),
]
QUALITY_ISSUES = [code for code, _ in QUALITY_ISSUE_CHOICES]


def _flag(code: str) -> str:
    return f"q_issue_{code.lower()}"


def _conditions(now: datetime) -> dict[str, Q]:
    return {
        "NO_PHOTOS": Q(q_supports_photos=True, q_has_photos=False),
        "NO_HOURS": Q(q_supports_hours=True, q_has_hours=False),
        "NO_LOCATION": Q(location__isnull=True),
        "NO_PHONE": Q(phone=""),
        "STALE": Q(q_fresh_at__lt=now - timedelta(days=STALE_AFTER_DAYS)),
        "OPEN_REPORTS": Q(q_open_reports__gt=0),
        "NOT_VERIFIED_RECENTLY": Q(last_verified_at__isnull=True)
        | Q(last_verified_at__lt=now - timedelta(days=VERIFIED_WITHIN_DAYS)),
    }


def with_quality(queryset: QuerySet[Facility], now: datetime | None = None) -> QuerySet[Facility]:
    """Annotate `quality_score`, one boolean per issue and the open report count."""
    now = now or timezone.now()
    open_reports = (
        FacilityReport.objects.filter(facility=OuterRef("pk"), status=FacilityReport.Status.OPEN)
        .order_by()
        .values("facility")
        .annotate(total=Count("id"))
        .values("total")
    )
    annotated = queryset.annotate(
        q_open_reports=Coalesce(Subquery(open_reports, output_field=IntegerField()), 0),
        q_has_photos=Exists(FacilityImage.objects.filter(facility=OuterRef("pk"))),
        q_has_hours=Exists(BusinessHour.objects.filter(facility=OuterRef("pk"))),
        q_supports_photos=Coalesce(
            F("category__capabilities__supports_photos"), Value(True), output_field=BooleanField()
        ),
        q_supports_hours=Coalesce(
            F("category__capabilities__supports_hours"), Value(True), output_field=BooleanField()
        ),
        # PostgreSQL's GREATEST skips NULLs, so an unset confirmation or verification is ignored.
        q_fresh_at=Greatest("updated_at", "hours_confirmed_at", "last_verified_at"),
    )
    flags = {
        _flag(code): Case(When(condition, then=Value(True)), default=Value(False))
        for code, condition in _conditions(now).items()
    }
    annotated = annotated.annotate(**flags)
    penalty: Any = Value(0)
    for code, weight in ISSUE_WEIGHTS.items():
        penalty = penalty + Case(
            When(**{_flag(code): True}, then=Value(weight)),
            default=Value(0),
            output_field=IntegerField(),
        )
    return annotated.annotate(quality_score=Value(100) - penalty)


def filter_issue(queryset: QuerySet[Facility], code: str) -> QuerySet[Facility]:
    """Keep facilities that have `code`. The queryset must come from `with_quality`."""
    return queryset.filter(**{_flag(code): True})


def quality_payload(facility: Any) -> dict[str, Any]:
    """`qualityScore` and `qualityIssues` of a facility annotated by `with_quality`."""
    return {
        "qualityScore": max(0, min(100, int(facility.quality_score))),
        "qualityIssues": [code for code in QUALITY_ISSUES if getattr(facility, _flag(code))],
    }
