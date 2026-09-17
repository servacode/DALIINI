from unittest.mock import Mock, patch

import pytest
from django.test import Client, override_settings


@pytest.mark.django_db
def test_liveness_is_process_only() -> None:
    response = Client().get("/health/live/")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


@pytest.mark.django_db
@override_settings(REDIS_URL="redis://unused:6379/0")
@patch("health.views.Redis.from_url")
def test_readiness_checks_database_and_redis(redis_from_url: Mock) -> None:
    redis_from_url.return_value.ping.return_value = True
    response = Client().get("/health/ready/")
    assert response.status_code == 200
    assert response.json() == {
        "status": "ready",
        "checks": {"database": "ok", "redis": "ok"},
    }


@pytest.mark.django_db
@override_settings(REDIS_URL="redis://unused:6379/0")
@patch("health.views.Redis.from_url")
def test_readiness_fails_when_redis_is_unavailable(redis_from_url: Mock) -> None:
    redis_from_url.return_value.ping.side_effect = OSError("offline")
    response = Client().get("/health/ready/")
    assert response.status_code == 503
    assert response.json()["checks"]["redis"] == "failed"
