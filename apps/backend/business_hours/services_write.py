from django.db import transaction
from django.utils import timezone

from audit.services import record_audit

from .domain import HourInterval, validate_weekly_schedule
from .models import BusinessHour


@transaction.atomic
def replace_business_hours(*, actor, facility, rows):
    intervals = [HourInterval(**row) for row in rows]
    validate_weekly_schedule(intervals)
    BusinessHour.objects.filter(facility=facility).delete()
    created = []
    for interval in intervals:
        obj = BusinessHour(
            facility=facility,
            weekday=interval.weekday,
            opens_at=interval.opens_at,
            closes_at=interval.closes_at,
            sort_order=interval.sort_order,
        )
        obj.full_clean()
        obj.save()
        created.append(obj)
    # Writing the hours is also confirming them: the owner has just looked at every row.
    confirmed_at = timezone.now()
    type(facility).objects.filter(pk=facility.pk).update(hours_confirmed_at=confirmed_at)
    facility.hours_confirmed_at = confirmed_at
    record_audit(
        actor=actor,
        action="facility.hours.replaced",
        target=facility,
        metadata={"count": len(created)},
    )
    return created
