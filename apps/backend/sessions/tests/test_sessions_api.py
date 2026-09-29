from typing import Any

import pytest
from rest_framework.test import APIClient

from accounts.models import User
from accounts.services import create_session
from sessions.models import UserSession


def _signed_in(user: User, device: str) -> tuple[APIClient, dict[str, Any]]:
    session = create_session(user=user, platform="ANDROID", device_name=device)
    client = APIClient()
    client.credentials(HTTP_AUTHORIZATION=f"Bearer {session['accessToken']}")
    return client, session


@pytest.mark.django_db
def test_list_shows_every_device_without_secrets(user: User) -> None:
    client, _ = _signed_in(user, "phone")
    _signed_in(user, "tablet")
    items = client.get("/api/v1/auth/sessions/").json()["items"]
    assert sorted(item["deviceName"] for item in items) == ["phone", "tablet"]
    for item in items:
        assert set(item) == {"id", "platform", "deviceName", "createdAt", "lastSeenAt", "revoked"}


@pytest.mark.django_db
def test_revoking_another_device_ends_it(user: User) -> None:
    client, _ = _signed_in(user, "phone")
    other_client, other = _signed_in(user, "tablet")
    assert client.delete(f"/api/v1/auth/sessions/{other['sessionId']}/").status_code == 204
    assert UserSession.objects.get(pk=other["sessionId"]).revoked_at is not None
    assert other_client.get("/api/v1/auth/sessions/").status_code == 401


@pytest.mark.django_db
def test_a_session_of_someone_else_cannot_be_revoked(user: User) -> None:
    client, _ = _signed_in(user, "phone")
    stranger = User.objects.create_user(phone="+963900000055", password="x" * 12, name="S")
    _, theirs = _signed_in(stranger, "theirs")
    response = client.delete(f"/api/v1/auth/sessions/{theirs['sessionId']}/")
    assert response.status_code in (400, 404)
    assert UserSession.objects.get(pk=theirs["sessionId"]).revoked_at is None


@pytest.mark.django_db
def test_anonymous_cannot_list_sessions() -> None:
    assert APIClient().get("/api/v1/auth/sessions/").status_code == 401
