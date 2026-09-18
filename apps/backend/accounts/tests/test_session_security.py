"""A session stops working the moment it is revoked, and a replayed refresh secret ends them all.

Until this module the refresh and revocation paths were covered only by tests that read the
source text. Executed, they showed two faults. A replayed refresh secret was refused, but the
revocation it triggered was rolled back together with the refusal (INT-054). And an access
token kept working for up to fifteen minutes after its session was revoked, including on a
WebSocket that had already authenticated (INT-051).

Sessions are created through the service rather than the login endpoint, so that these tests
do not spend the login throttle that the rest of the suite and the e2e runs share.
"""

import base64
import hashlib
import hmac
import json
from datetime import UTC, datetime, timedelta
from typing import Any

import pytest
from asgiref.sync import async_to_sync, sync_to_async
from channels.layers import get_channel_layer  # type: ignore[import-untyped]  # no stubs exist
from channels.testing import WebsocketCommunicator  # type: ignore[import-untyped]
from django.conf import settings
from django.utils import timezone
from rest_framework.test import APIClient

from accounts.models import User
from accounts.services import create_session
from realtime.consumers import DirectoryConsumer
from realtime.groups import user_group
from sessions.models import UserSession

PROFILE = "/api/v1/account/profile/"
REFRESH = "/api/v1/auth/refresh/"
LOGOUT = "/api/v1/auth/logout/"


def bearer(token: str) -> APIClient:
    client = APIClient()
    client.credentials(HTTP_AUTHORIZATION=f"Bearer {token}")
    return client


def new_session(user: User) -> dict[str, Any]:
    return create_session(user=user, platform="ANDROID", device_name="test device")


def refresh(secret: str) -> Any:
    return APIClient().post(REFRESH, {"refreshToken": secret}, format="json")


def expire_grace(session_id: str) -> None:
    """Move the previous secret's concurrency grace window into the past."""
    UserSession.objects.filter(pk=session_id).update(
        previous_valid_until=timezone.now() - timedelta(seconds=1)
    )


@pytest.mark.django_db
def test_an_access_token_works_while_its_session_is_live(user: User) -> None:
    session = new_session(user)

    assert bearer(session["accessToken"]).get(PROFILE).status_code == 200


@pytest.mark.django_db
def test_logout_ends_the_access_token_at_once(user: User) -> None:
    session = new_session(user)
    client = bearer(session["accessToken"])

    response = client.post(LOGOUT, {"sessionId": session["sessionId"]}, format="json")

    assert response.status_code == 204
    after = client.get(PROFILE)
    assert after.status_code == 401
    assert after.json()["code"] == "AUTHENTICATION_FAILED"


@pytest.mark.django_db
def test_rotation_issues_a_working_pair_and_retires_the_old_secret(user: User) -> None:
    first = new_session(user)

    rotated = refresh(first["refreshToken"])

    assert rotated.status_code == 200
    body = rotated.json()
    assert body["sessionId"] == first["sessionId"]
    assert body["refreshToken"] != first["refreshToken"]
    assert bearer(body["accessToken"]).get(PROFILE).status_code == 200


@pytest.mark.django_db
def test_a_replay_inside_the_grace_window_is_still_accepted(user: User) -> None:
    """Two requests racing on one secret must not log the user out."""
    first = new_session(user)
    assert refresh(first["refreshToken"]).status_code == 200

    assert refresh(first["refreshToken"]).status_code == 200


@pytest.mark.django_db
def test_a_replay_revokes_every_session_and_the_revocation_survives(user: User) -> None:
    stolen = new_session(user)
    other_device = new_session(user)
    rotated = refresh(stolen["refreshToken"]).json()
    expire_grace(stolen["sessionId"])

    replay = refresh(stolen["refreshToken"])

    assert replay.status_code == 401
    compromised = UserSession.objects.get(pk=stolen["sessionId"])
    assert compromised.compromised_at is not None
    assert compromised.revoked_at is not None
    assert UserSession.objects.get(pk=other_device["sessionId"]).revoked_at is not None
    # Nothing issued before the replay acts any more: not the rotated secret, not the
    # rotated access token, not the other device.
    assert refresh(rotated["refreshToken"]).status_code == 401
    assert bearer(rotated["accessToken"]).get(PROFILE).status_code == 401
    assert bearer(other_device["accessToken"]).get(PROFILE).status_code == 401


@pytest.mark.django_db
def test_a_token_without_a_session_claim_is_refused(user: User) -> None:
    now = datetime.now(UTC)

    def b64(data: bytes) -> str:
        return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")

    header = b64(json.dumps({"alg": "HS256", "typ": "JWT"}, separators=(",", ":")).encode())
    claims = {"sub": str(user.pk), "iat": int(now.timestamp()),
              "exp": int((now + timedelta(minutes=5)).timestamp())}
    payload = b64(json.dumps(claims, separators=(",", ":")).encode())
    key = settings.ACCESS_TOKEN_SIGNING_KEY.encode("utf-8")
    signature = b64(hmac.new(key, f"{header}.{payload}".encode(), hashlib.sha256).digest())

    assert bearer(f"{header}.{payload}.{signature}").get(PROFILE).status_code == 401


async def _authenticate(token: str) -> Any:
    communicator = WebsocketCommunicator(DirectoryConsumer.as_asgi(), "/ws/v1/directory/")
    connected, _ = await communicator.connect()
    assert connected
    await communicator.send_json_to({"action": "authenticate", "accessToken": token})
    reply = await communicator.receive_json_from(timeout=5)
    await communicator.disconnect()
    return reply


@pytest.mark.django_db(transaction=True)
def test_the_websocket_refuses_a_revoked_session(user: User) -> None:
    session = new_session(user)
    assert async_to_sync(_authenticate)(session["accessToken"]) == {"type": "auth", "ok": True}

    UserSession.objects.filter(pk=session["sessionId"]).update(revoked_at=timezone.now())

    assert async_to_sync(_authenticate)(session["accessToken"]) == {"type": "auth", "ok": False}


@pytest.mark.django_db(transaction=True)
def test_an_open_socket_is_closed_when_its_session_is_revoked(user: User) -> None:
    session = new_session(user)

    async def scenario() -> Any:
        communicator = WebsocketCommunicator(DirectoryConsumer.as_asgi(), "/ws/v1/directory/")
        await communicator.connect()
        await communicator.send_json_to(
            {"action": "authenticate", "accessToken": session["accessToken"]}
        )
        assert await communicator.receive_json_from(timeout=5) == {"type": "auth", "ok": True}
        await communicator.send_json_to({"action": "subscribeUser"})
        assert (await communicator.receive_json_from(timeout=5))["scope"] == "user"

        await sync_to_async(
            UserSession.objects.filter(pk=session["sessionId"]).update
        )(revoked_at=timezone.now())
        await get_channel_layer().group_send(
            user_group(str(user.pk)),
            {
                "type": "directory.event",
                "payload": {
                    "version": 1,
                    "name": "user.application.changed",
                    "scope": {"type": "user", "id": "opaque"},
                    "occurredAt": timezone.now().isoformat(),
                },
            },
        )
        closing = await communicator.receive_output(timeout=5)
        await communicator.wait()
        return closing

    closing = async_to_sync(scenario)()

    assert closing == {"type": "websocket.close", "code": 4401}
