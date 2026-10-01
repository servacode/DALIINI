"""Ask pharmacists to cover a day nobody is on duty, before it arrives.

Once a day the beat task looks `NUDGE_WINDOW_DAYS` ahead in every province that keeps a duty
roster (see `coverage.duty_provinces`). For each province with an uncovered day, the owners of
its ACTIVE pharmacies are asked, in the inbox and by push, whether they can cover it.

- Idempotent per gap-day: a `DutyGapNudge` row is written in the same transaction as the
  notifications, and the unique (province, day) constraint makes a second attempt a no-op,
  whether it is a rerun today, a concurrent worker, or tomorrow's run while the gap is still
  open.
- One day per province per run: the earliest uncovered day that was not asked about yet.
  A later gap is asked about on a later run, if it is still open then.
- Capped per owner: at most `MAX_NUDGES_PER_OWNER_PER_DAY` nudges per owner per Damascus day,
  however many provinces or pharmacies the owner has. A day nobody could be asked about
  (every owner already nudged today, or no owners) is not recorded, so it is asked later.
"""

from __future__ import annotations

from datetime import date, datetime

from django.db import IntegrityError, transaction
from django.utils import timezone

from accounts.models import User
from facilities.models import FacilityMembership
from notifications.models import Notification
from notifications.services import notify

from .coverage import day_bounds, duty_facilities, duty_gaps, duty_provinces, local_today, window
from .models import DutyGapNudge

NUDGE_TYPE = "duty.gap_nudge"
NUDGE_WINDOW_DAYS = 3
MAX_NUDGES_PER_OWNER_PER_DAY = 1
NUDGE_TITLE_AR = "مناوبة غير مغطاة"

WEEKDAYS_AR = ["الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد"]


def day_label_ar(day: date) -> str:
    return f"{WEEKDAYS_AR[day.weekday()]} {day.day}/{day.month}"


def nudge_body_ar(day: date) -> str:
    return f"لا توجد صيدلية مناوبة يوم {day_label_ar(day)} في منطقتك. هل يمكنك تغطية المناوبة؟"


def _owners_in(province_id: str) -> list[User]:
    facilities = duty_facilities().filter(province_id=province_id).values("pk")
    user_ids = FacilityMembership.objects.filter(
        facility_id__in=facilities, role=FacilityMembership.Role.OWNER
    ).values("user_id")
    return list(User.objects.filter(pk__in=user_ids, is_active=True).order_by("pk"))


def nudge_duty_gaps(now: datetime | None = None) -> int:
    """Send today's gap nudges. Returns how many notifications were sent."""
    now = now or timezone.now()
    today = local_today(now)
    today_start, _ = day_bounds(today)
    days = window(today, NUDGE_WINDOW_DAYS)
    gaps = duty_gaps(days, province_ids=duty_provinces().values_list("pk", flat=True))
    sent = 0
    for province_id, gap_days in gaps.items():
        asked = set(
            DutyGapNudge.objects.filter(province_id=province_id, gap_date__in=gap_days).values_list(
                "gap_date", flat=True
            )
        )
        pending = [day for day in gap_days if day not in asked]
        if not pending:
            continue
        gap_day = pending[0]
        eligible = [
            owner
            for owner in _owners_in(province_id)
            if Notification.objects.filter(
                user=owner, type=NUDGE_TYPE, created_at__gte=today_start
            ).count()
            < MAX_NUDGES_PER_OWNER_PER_DAY
        ]
        if not eligible:
            # Nobody could be asked today; leave the day unrecorded so a later run asks.
            continue
        try:
            with transaction.atomic():
                DutyGapNudge.objects.create(
                    province_id=province_id, gap_date=gap_day, recipient_count=len(eligible)
                )
                for owner in eligible:
                    notify(
                        user=owner,
                        type=NUDGE_TYPE,
                        title_ar=NUDGE_TITLE_AR,
                        body_ar=nudge_body_ar(gap_day),
                        destination=Notification.Destination.OWNER_FACILITIES,
                        payload={"provinceId": province_id, "gapDate": gap_day.isoformat()},
                    )
        except IntegrityError:
            # Another run recorded this day first; its owners were asked there.
            continue
        sent += len(eligible)
    return sent
