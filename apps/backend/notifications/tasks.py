from __future__ import annotations

import logging
import random

from celery import Task, shared_task

from .models import Notification
from .providers.base import TransientPushError
from .services import push_notification

logger = logging.getLogger(__name__)

MAX_RETRIES = 5
BACKOFF_BASE_SECONDS = 30
BACKOFF_CAP_SECONDS = 1800


def retry_countdown(retries: int) -> int:
    """Exponential backoff with full jitter, capped."""
    ceiling = min(BACKOFF_CAP_SECONDS, BACKOFF_BASE_SECONDS * (2**retries))
    return random.randint(1, max(1, ceiling))


@shared_task(bind=True, acks_late=True, reject_on_worker_lost=True, max_retries=MAX_RETRIES)
def deliver_notification_push(self: Task, notification_id: str, title: str, body: str) -> None:
    """Push a notification to every active device of its user.

    Delivery is idempotent per device (NotificationPushDelivery), so a retry after a partial
    failure only re-attempts the devices that were not delivered. Permanent failures (an
    unregistered token, a misconfigured provider) never retry; transient ones retry with
    exponential backoff and jitter.
    """
    notification = Notification.objects.select_related("user").filter(pk=notification_id).first()
    if notification is None:
        logger.warning("push.notification_missing", extra={"notification_id": notification_id})
        return
    try:
        push_notification(notification, title=title, body=body)
    except TransientPushError as exc:
        logger.warning(
            "push.transient_failure",
            extra={"notification_id": notification_id, "retries": self.request.retries},
        )
        raise self.retry(exc=exc, countdown=retry_countdown(self.request.retries)) from exc
