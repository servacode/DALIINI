import os

from celery import Celery

os.environ.setdefault("DJANGO_SETTINGS_MODULE", "directory_backend.settings.development")

app = Celery("directory_backend")
app.config_from_object("django.conf:settings", namespace="CELERY")
app.autodiscover_tasks()
