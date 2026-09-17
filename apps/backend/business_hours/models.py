import uuid

from django.core.exceptions import ValidationError
from django.db import models


class BusinessHour(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    facility = models.ForeignKey(
        "facilities.Facility",
        on_delete=models.CASCADE,
        related_name="business_hours",
    )
    weekday = models.PositiveSmallIntegerField()
    opens_at = models.TimeField()
    closes_at = models.TimeField()
    sort_order = models.PositiveSmallIntegerField(default=0)

    class Meta:
        ordering = ["weekday", "opens_at", "sort_order"]
        constraints = [
            models.CheckConstraint(
                condition=models.Q(weekday__gte=0, weekday__lte=6),
                name="business_hour_valid_weekday",
            ),
            models.UniqueConstraint(
                fields=["facility", "weekday", "opens_at", "closes_at"],
                name="uniq_facility_business_hour",
            ),
        ]

    def clean(self):
        if self.opens_at == self.closes_at:
            raise ValidationError("Opening and closing time cannot be equal.")


class TemporaryClosure(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    facility = models.ForeignKey(
        "facilities.Facility",
        on_delete=models.CASCADE,
        related_name="temporary_closures",
    )
    starts_at = models.DateTimeField()
    ends_at = models.DateTimeField()
    reason = models.CharField(max_length=240, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        indexes = [models.Index(fields=["facility", "starts_at", "ends_at"])]

    def clean(self):
        if self.ends_at <= self.starts_at:
            raise ValidationError({"ends_at": "Must be after starts_at."})
