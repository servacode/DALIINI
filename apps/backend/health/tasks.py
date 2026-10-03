from celery import shared_task

from .beacons import SCHEDULER, record_ok


@shared_task  # type: ignore[untyped-decorator]
def heartbeat() -> None:
    """Scheduled by beat and run by a worker, so one fresh row proves both are alive."""
    record_ok(SCHEDULER)
