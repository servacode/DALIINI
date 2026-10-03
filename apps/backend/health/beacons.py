"""Record that a background service worked, or did not (DECISION-073).

Called from the paths that do the work: the scheduler's heartbeat, sending a code, pushing a
notification. Recording must never be the reason that work fails, so every write runs in its own
savepoint and swallows its own database errors, logging them instead. Inside a caller's
transaction that is later rolled back (changing a phone number), the record goes with it.
"""

import logging

from django.db import DatabaseError, transaction
from django.db.models import F
from django.utils import timezone

from .models import ServiceSignal

logger = logging.getLogger(__name__)

SCHEDULER = "scheduler"
OTP = "otp"
PUSH = "push"
BACKUP = "backup"


def record_ok(name: str) -> None:
    now = timezone.now()
    try:
        with transaction.atomic():
            if not ServiceSignal.objects.filter(name=name).update(ok_at=now, failures=0):
                ServiceSignal.objects.update_or_create(
                    name=name, defaults={"ok_at": now, "failures": 0}
                )
    except DatabaseError:
        logger.warning("health.signal_write_failed", extra={"signal": name})


def record_failure(name: str, reason: str) -> None:
    now = timezone.now()
    try:
        with transaction.atomic():
            updated = ServiceSignal.objects.filter(name=name).update(
                failed_at=now, failure=reason[:60], failures=F("failures") + 1
            )
            if not updated:
                ServiceSignal.objects.update_or_create(
                    name=name, defaults={"failed_at": now, "failure": reason[:60], "failures": 1}
                )
    except DatabaseError:
        logger.warning("health.signal_write_failed", extra={"signal": name})
