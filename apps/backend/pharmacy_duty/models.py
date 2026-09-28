import uuid

from django.contrib.postgres.constraints import ExclusionConstraint
from django.contrib.postgres.fields import DateTimeRangeField, RangeOperators
from django.core.exceptions import ValidationError
from django.db import models
from django.db.models import F, Func


class TstzRange(Func):
    function = "TSTZRANGE"
    output_field = DateTimeRangeField()

    def __init__(self, start, end):
        super().__init__(start, end, models.Value("[)"))


class DutyShift(models.Model):
    class Source(models.TextChoices):
        """Who put the shift on the roster. Kept as provenance; it never changes afterwards."""

        OWNER = "OWNER", "Pharmacy owner or manager"
        ADMIN = "ADMIN", "Platform operator"
        IMPORT = "IMPORT", "Bulk import"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    facility = models.ForeignKey(
        "facilities.Facility",
        on_delete=models.CASCADE,
        related_name="duty_shifts",
    )
    starts_at = models.DateTimeField()
    ends_at = models.DateTimeField()
    source = models.CharField(max_length=8, choices=Source.choices, default=Source.OWNER)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        constraints = [
            models.CheckConstraint(
                condition=models.Q(ends_at__gt=F("starts_at")),
                name="duty_shift_positive_duration",
            ),
            ExclusionConstraint(
                name="prevent_overlapping_duty_for_facility",
                expressions=[
                    (F("facility"), RangeOperators.EQUAL),
                    (
                        TstzRange(F("starts_at"), F("ends_at")),
                        RangeOperators.OVERLAPS,
                    ),
                ],
            ),
        ]
        indexes = [
            models.Index(
                fields=["facility", "starts_at", "ends_at"],
                name="pharmacy_du_facilit_idx",
            )
        ]

    def clean(self):
        if self.ends_at <= self.starts_at:
            raise ValidationError({"ends_at": "Must be after starts_at."})
        capabilities = getattr(self.facility.category, "capabilities", None)
        if capabilities is None or not capabilities.supports_duty:
            raise ValidationError("Facility category does not support duty.")


class DutyGapNudge(models.Model):
    """One uncovered duty day in one province that owners were already asked to cover.

    The daily nudge task writes the row in the same transaction as the notifications, so a
    rerun on the same day, or on the next day while the gap is still open, never asks about
    the same day twice.
    """

    province = models.ForeignKey(
        "locations.Province", on_delete=models.CASCADE, related_name="duty_gap_nudges"
    )
    gap_date = models.DateField()
    recipient_count = models.PositiveIntegerField(default=0)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        constraints = [
            models.UniqueConstraint(
                fields=["province", "gap_date"], name="uniq_duty_gap_nudge_per_day"
            )
        ]

    def __str__(self) -> str:
        return f"{self.province_id} {self.gap_date}"
