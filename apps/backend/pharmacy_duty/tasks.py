from celery import shared_task  # type: ignore[import-untyped]

from .nudges import nudge_duty_gaps


@shared_task  # type: ignore[untyped-decorator]
def nudge_uncovered_duty_days() -> int:
    """Daily: ask pharmacists to cover duty days nobody covers in the next three days."""
    return nudge_duty_gaps()
