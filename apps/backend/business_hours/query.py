from datetime import UTC
from zoneinfo import ZoneInfo

from django.db.models import Exists, OuterRef, Q
from django.utils import timezone

from pharmacy_duty.models import DutyShift

from .models import BusinessHour, TemporaryClosure
from .services import AvailabilityState

DAMASCUS = ZoneInfo("Asia/Damascus")


def _availability_annotations(now=None):
    value = now or timezone.now()
    if timezone.is_naive(value):
        value = timezone.make_aware(value, DAMASCUS)
    local = value.astimezone(DAMASCUS)
    current_utc = local.astimezone(UTC)
    current_time = local.time().replace(tzinfo=None)
    weekday = local.weekday()
    prior_weekday = (weekday - 1) % 7

    closures = TemporaryClosure.objects.filter(
        facility_id=OuterRef("pk"),
        starts_at__lte=current_utc,
        ends_at__gt=current_utc,
    )
    duties = DutyShift.objects.filter(
        facility_id=OuterRef("pk"),
        starts_at__lte=current_utc,
        ends_at__gt=current_utc,
    )
    schedule = BusinessHour.objects.filter(facility_id=OuterRef("pk")).filter(
        Q(
            weekday=weekday,
            opens_at__lt=models_f("closes_at"),
            opens_at__lte=current_time,
            closes_at__gt=current_time,
        )
        | Q(
            weekday=weekday,
            opens_at__gt=models_f("closes_at"),
            opens_at__lte=current_time,
        )
        | Q(
            weekday=prior_weekday,
            opens_at__gt=models_f("closes_at"),
            closes_at__gt=current_time,
        )
    )
    return {
        "_availability_closed": Exists(closures),
        "_availability_duty": Exists(duties),
        "_availability_scheduled": Exists(schedule),
    }


def filter_for_availability_state(queryset, state, now=None):
    queryset = queryset.annotate(**_availability_annotations(now))
    if state is AvailabilityState.TEMP_CLOSED:
        return queryset.filter(_availability_closed=True)
    if state is AvailabilityState.DUTY:
        return queryset.filter(
            _availability_closed=False,
            _availability_duty=True,
        )
    if state is AvailabilityState.OPEN:
        return queryset.filter(
            _availability_closed=False,
            _availability_duty=False,
            _availability_scheduled=True,
        )
    if state is AvailabilityState.CLOSED:
        return queryset.filter(
            _availability_closed=False,
            _availability_duty=False,
            _availability_scheduled=False,
        )
    raise ValueError(f"Unknown availability state: {state}")


def models_f(name):
    from django.db.models import F

    return F(name)
