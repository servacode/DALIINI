"""Device push registration (INT-057): stored protected, tied to a session, never echoed."""

import logging
from typing import Any

import pytest
from rest_framework.test import APIClient

from accounts.models import User
from accounts.services import create_session, revoke_session
from notifications.models import DevicePushToken, Notification
from notifications.providers.base import InvalidPushToken, PushMessage
from notifications.services import push_notification

REGISTER = "/api/v1/account/push-token/"
UNREGISTER = "/api/v1/account/push-token/unregister/"
TOKEN = "fcm-token-" + "a" * 140


def signed_in(user: User) -> tuple[APIClient, dict[str, Any]]:
    session = create_session(user=user, platform="ANDROID", device_name="test")
    client = APIClient()
    client.credentials(HTTP_AUTHORIZATION=f"Bearer {session['accessToken']}")
    return client, session


@pytest.mark.django_db
def test_a_token_is_stored_protected_and_never_echoed(user: User) -> None:
    client, session = signed_in(user)

    response = client.put(REGISTER, {"platform": "ANDROID", "token": TOKEN}, format="json")

    assert response.status_code == 204
    assert response.content == b""
    row = DevicePushToken.objects.get(user=user)
    assert row.active
    assert str(row.session_id) == session["sessionId"]
    assert TOKEN not in row.token_ciphertext
    assert TOKEN not in row.token_digest


@pytest.mark.django_db
def test_registering_again_is_idempotent_and_a_new_token_replaces_the_old(user: User) -> None:
    client, _ = signed_in(user)
    client.put(REGISTER, {"platform": "ANDROID", "token": TOKEN}, format="json")
    client.put(REGISTER, {"platform": "ANDROID", "token": TOKEN}, format="json")
    assert DevicePushToken.objects.filter(user=user).count() == 1

    client.put(REGISTER, {"platform": "ANDROID", "token": TOKEN + "-rotated"}, format="json")

    assert DevicePushToken.objects.filter(user=user, active=True).count() == 1
    assert DevicePushToken.objects.filter(user=user, active=False).count() == 1


@pytest.mark.django_db
def test_unregister_stops_pushes_and_an_unknown_token_is_not_an_error(user: User) -> None:
    client, _ = signed_in(user)
    client.put(REGISTER, {"platform": "ANDROID", "token": TOKEN}, format="json")

    assert client.post(UNREGISTER, {"token": TOKEN}, format="json").status_code == 204
    assert client.post(UNREGISTER, {"token": "never-registered"}, format="json").status_code == 204
    assert not DevicePushToken.objects.get(user=user).active


@pytest.mark.django_db
def test_ending_a_session_stops_pushes_to_its_device_only(user: User) -> None:
    phone, phone_session = signed_in(user)
    laptop, _ = signed_in(user)
    phone.put(REGISTER, {"platform": "ANDROID", "token": TOKEN}, format="json")
    laptop.put(REGISTER, {"platform": "ANDROID", "token": TOKEN + "-laptop"}, format="json")

    revoke_session(user=user, session_id=phone_session["sessionId"])

    active = list(
        DevicePushToken.objects.filter(user=user, active=True).values_list("session_id", flat=True)
    )
    assert len(active) == 1
    assert str(active[0]) != phone_session["sessionId"]


@pytest.mark.django_db
def test_a_token_the_provider_rejects_is_deactivated(
    user: User, monkeypatch: pytest.MonkeyPatch
) -> None:
    client, _ = signed_in(user)
    client.put(REGISTER, {"platform": "ANDROID", "token": TOKEN}, format="json")

    class Rejecting:
        def send(self, message: PushMessage) -> None:
            raise InvalidPushToken()

    monkeypatch.setattr("notifications.services.get_push_provider", lambda platform: Rejecting())
    notification = Notification.objects.create(user=user, type="application.changed")

    push_notification(notification, title="t", body="b")

    assert not DevicePushToken.objects.get(user=user).active


@pytest.mark.django_db
def test_registration_requires_a_session_and_never_logs_the_token(
    user: User, caplog: pytest.LogCaptureFixture
) -> None:
    anonymous = APIClient().put(REGISTER, {"platform": "ANDROID", "token": TOKEN}, format="json")
    assert anonymous.status_code == 401

    client, _ = signed_in(user)
    with caplog.at_level(logging.DEBUG):
        client.put(REGISTER, {"platform": "ANDROID", "token": TOKEN}, format="json")
        client.put(REGISTER, {"platform": "WINDOWS", "token": TOKEN}, format="json")

    assert TOKEN not in caplog.text
