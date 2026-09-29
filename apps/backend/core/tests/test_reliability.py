"""Beat schedule, maintenance purges and structured logging."""

import json
import logging
from datetime import timedelta

import pytest
from django.conf import settings
from django.utils import timezone

from accounts.models import OTPChallenge, User
from accounts.tasks import purge_expired_otp_challenges
from core.logging import JsonFormatter, current_request_id
from directory_backend.celery import app
from sessions.models import UserSession
from sessions.tasks import purge_ended_sessions


def test_every_scheduled_task_is_registered() -> None:
    app.loader.import_default_modules()
    for entry in settings.CELERY_BEAT_SCHEDULE.values():
        assert entry["task"] in app.tasks


@pytest.mark.django_db
def test_ended_sessions_and_old_otps_are_purged(user: User) -> None:
    old = timezone.now() - timedelta(days=60)
    UserSession.objects.create(user=user, refresh_digest="old", expires_at=old)
    UserSession.objects.create(
        user=user, refresh_digest="live", expires_at=timezone.now() + timedelta(days=1)
    )
    OTPChallenge.objects.create(phone="+963900000009", purpose="REGISTER", expires_at=old)

    assert purge_ended_sessions() == 1
    assert purge_expired_otp_challenges() == 1
    assert list(UserSession.objects.values_list("refresh_digest", flat=True)) == ["live"]


def test_json_logs_carry_request_id_and_extras_but_not_secrets() -> None:
    record = logging.LogRecord("x", logging.INFO, "", 0, "hello", None, None)
    record.facility_id = "f1"
    record.push_token = "SECRET"
    token = current_request_id.set("req-1")
    try:
        payload = json.loads(JsonFormatter().format(record))
    finally:
        current_request_id.reset(token)
    assert payload["requestId"] == "req-1"
    assert payload["facilityId"] == "f1"
    assert "SECRET" not in json.dumps(payload)
