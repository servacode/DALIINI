from celery import shared_task  # type: ignore[import-untyped]

from .hours_confirmation import remind_owners_to_confirm_hours


@shared_task  # type: ignore[untyped-decorator]
def remind_hours_confirmation() -> int:
    """Weekly: ask owners whose hours were not confirmed in a week to confirm them."""
    return remind_owners_to_confirm_hours()
