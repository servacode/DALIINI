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
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    facility = models.ForeignKey(
        "facilities.Facility",
        on_delete=models.CASCADE,
        related_name="duty_shifts",
    )
    starts_at = models.DateTimeField()
    ends_at = models.DateTimeField()
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
        if not self.facility.category.capabilities.supports_duty:
            raise ValidationError("Facility category does not support duty.")
