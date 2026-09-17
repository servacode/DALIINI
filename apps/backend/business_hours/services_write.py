from django.db import transaction

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
    record_audit(
        actor=actor,
        action="facility.hours.replaced",
        target=facility,
        metadata={"count": len(created)},
    )
    return created
