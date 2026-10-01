from typing import Any

import pytest
from django.core.cache import cache
from rest_framework.test import APIClient

from platform_settings.maintenance import ENABLED_KEY, MESSAGE_KEY, RETRY_AFTER_KEY
from platform_settings.models import PlatformSetting


@pytest.fixture(autouse=True)
def _clear_cache() -> None:
    cache.clear()


def _enable(message: str = "صيانة", retry: int = 120) -> None:
    for key, value in ((ENABLED_KEY, True), (MESSAGE_KEY, message), (RETRY_AFTER_KEY, retry)):
        setting = PlatformSetting.objects.get(key=key)
        setting.value = value
        setting.save()


@pytest.mark.django_db
def test_settings_are_seeded_and_off_by_default() -> None:
    keys = set(PlatformSetting.objects.values_list("key", flat=True))
    assert {ENABLED_KEY, MESSAGE_KEY, RETRY_AFTER_KEY} <= keys
    body = APIClient().get("/api/v1/platform/status/").json()
    assert body["maintenance"] is False
    assert body["retryAfterSeconds"] == 600
    assert APIClient().get("/api/v1/public/provinces/").status_code == 200


@pytest.mark.django_db
def test_maintenance_refuses_public_api_with_the_envelope() -> None:
    APIClient().get("/api/v1/platform/status/")  # warm the cache: the write must invalidate it
    _enable()

    response = APIClient().get("/api/v1/public/provinces/", HTTP_X_REQUEST_ID="rid-1")

    assert response.status_code == 503
    assert response["Retry-After"] == "120"
    assert response.json() == {
        "code": "MAINTENANCE",
        "message": "صيانة",
        "details": {"retryAfterSeconds": 120},
        "requestId": "rid-1",
    }


@pytest.mark.django_db
def test_exempt_paths_keep_working_during_maintenance() -> None:
    _enable()
    client = APIClient()
    status = client.get("/api/v1/platform/status/")
    assert status.status_code == 200
    assert status.json()["maintenance"] is True
    assert client.get("/health/live/").status_code == 200
    # Admin endpoints are reachable (they answer 401, not 503).
    assert client.get("/api/v1/admin/me/").status_code == 401
    assert client.post("/api/v1/auth/login/", {}, format="json").status_code != 503


@pytest.mark.django_db
def test_admin_cannot_change_the_type_of_a_known_setting(admin_api: Any) -> None:
    admin_client = admin_api("admin.settings.read", "admin.settings.manage")
    response = admin_client.put(
        "/api/v1/admin/settings/",
        {"key": ENABLED_KEY, "type": "STRING", "value": "yes"},
        format="json",
    )
    assert response.status_code == 400
    ok = admin_client.put(
        "/api/v1/admin/settings/",
        {"key": ENABLED_KEY, "type": "BOOLEAN", "value": True},
        format="json",
    )
    assert ok.status_code == 200
    assert APIClient().get("/api/v1/public/provinces/").status_code == 503
