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


@pytest.mark.django_db
@override_settings(ALLOWED_HOSTS=["api.example.test"], SECURE_SSL_REDIRECT=True)
def test_a_probe_from_inside_the_machine_is_answered() -> None:
    # Docker asks 127.0.0.1 over plain HTTP; production allows only its own name and HTTPS.
    client = Client(HTTP_HOST="127.0.0.1:8000")

    assert client.get("/health/live/").status_code == 200
    assert client.head("/health/live/").status_code == 200
    # Everything else still refuses that host.
    assert client.get("/api/v1/public/provinces/").status_code == 400


@pytest.mark.django_db
def test_a_probe_does_not_accept_writes() -> None:
    assert Client().post("/health/live/").status_code == 405
