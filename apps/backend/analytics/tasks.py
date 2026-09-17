from celery import shared_task

from .services import purge_expired_analytics


@shared_task
def purge_analytics_retention() -> int:
    return purge_expired_analytics()
