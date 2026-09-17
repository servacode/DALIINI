from celery import shared_task

from .models import Notification
from .services import push_notification


@shared_task(autoretry_for=(Exception,), retry_backoff=True, retry_jitter=True, max_retries=5)
def deliver_notification_push(notification_id: str, title: str, body: str) -> None:
    notification = Notification.objects.select_related("user").get(pk=notification_id)
    push_notification(notification, title=title, body=body)
