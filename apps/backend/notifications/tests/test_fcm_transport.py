"""The FCM HTTP v1 transport: what goes on the wire, and what each refusal means."""

from __future__ import annotations

import base64
import json
import urllib.parse
from typing import Any

import pytest
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa
from django.core.exceptions import ImproperlyConfigured
from django.test import override_settings

from notifications.providers import fcm_http
from notifications.providers.base import InvalidPushToken, PushMessage, TransientPushError
from notifications.providers.fcm import FcmPushProvider
from notifications.providers.fcm_http import (
    FcmHttpTransport,
    ServiceAccount,
    signed_assertion,
)

KEY = rsa.generate_private_key(public_exponent=65537, key_size=2048)
PEM = KEY.private_bytes(
    serialization.Encoding.PEM,
    serialization.PrivateFormat.PKCS8,
    serialization.NoEncryption(),
).decode("ascii")
ACCOUNT_JSON = json.dumps(
    {
        "type": "service_account",
        "project_id": "daliini-test",
        "client_email": "push@daliini-test.iam.gserviceaccount.com",
        "private_key": PEM,
        "token_uri": "https://oauth2.googleapis.com/token",
    }
)
MESSAGE = PushMessage(
    token="device-token-1",
    title="عنوان لا يغادر الخادم",
    body="نص لا يغادر الخادم",
    data={"notificationId": "n-1", "type": "facility.application.approved"},
)


def _b64decode(part: str) -> bytes:
    return base64.urlsafe_b64decode(part + "=" * (-len(part) % 4))


class Wire:
    """Answers the token endpoint and the send endpoint from queues, and records calls."""

    def __init__(
        self,
        sends: list[tuple[int, dict[str, Any]] | Exception] | None = None,
        tokens: list[tuple[int, dict[str, Any]] | Exception] | None = None,
    ) -> None:
        self.sends = list(sends or [(200, {"name": "projects/p/messages/1"})])
        self.tokens = list(tokens or [])
        self.calls: list[tuple[str, bytes, dict[str, str]]] = []
        self.issued = 0

    def __call__(self, url: str, body: bytes, headers: dict[str, str]) -> tuple[int, bytes]:
        self.calls.append((url, body, headers))
        if url.startswith("https://oauth2.googleapis.com/"):
            answer = self.tokens.pop(0) if self.tokens else None
            if answer is None:
                self.issued += 1
                answer = (200, {"access_token": f"at-{self.issued}", "expires_in": 3600})
        else:
            answer = self.sends.pop(0) if self.sends else (200, {})
        if isinstance(answer, Exception):
            raise answer
        status, payload = answer
        return status, json.dumps(payload).encode("utf-8")

    def sent(self) -> list[tuple[str, bytes, dict[str, str]]]:
        return [call for call in self.calls if call[0].startswith("https://fcm.googleapis.com/")]

    def token_requests(self) -> int:
        return len(self.calls) - len(self.sent())


class Clock:
    def __init__(self, now: float = 1_800_000_000.0) -> None:
        self.now = now

    def __call__(self) -> float:
        return self.now


def transport(wire: Wire, clock: Clock | None = None) -> FcmHttpTransport:
    return FcmHttpTransport(
        ServiceAccount.from_setting(ACCOUNT_JSON), opener=wire, clock=clock or Clock()
    )


def fcm_error(
    status: int, code: str, message: str = "", **extra: Any
) -> tuple[int, dict[str, Any]]:
    details: list[dict[str, Any]] = [
        {"@type": "type.googleapis.com/google.firebase.fcm.v1.FcmError", "errorCode": code}
    ]
    if extra.get("field"):
        details.append(
            {
                "@type": "type.googleapis.com/google.rpc.BadRequest",
                "fieldViolations": [{"field": extra["field"], "description": "bad"}],
            }
        )
    error = {"code": status, "message": message, "status": code, "details": details}
    return status, {"error": error}


def test_the_key_is_read_as_pasted_or_as_base64() -> None:
    pasted = ServiceAccount.from_setting(ACCOUNT_JSON)
    encoded = ServiceAccount.from_setting(base64.b64encode(ACCOUNT_JSON.encode()).decode())

    assert pasted.client_email == "push@daliini-test.iam.gserviceaccount.com"
    assert encoded.client_email == pasted.client_email
    assert pasted.token_uri == "https://oauth2.googleapis.com/token"


@pytest.mark.parametrize(
    "raw",
    [
        "not json and not base64 !",
        json.dumps({"client_email": "a@b"}),
        json.dumps({"client_email": "a@b", "private_key": "-----BEGIN PRIVATE KEY-----\nxx"}),
        json.dumps({"client_email": "a@b", "private_key": PEM, "token_uri": "http://x/token"}),
    ],
)
def test_a_wrong_key_is_a_configuration_error_that_never_echoes_it(raw: str) -> None:
    with pytest.raises(ImproperlyConfigured) as refused:
        ServiceAccount.from_setting(raw)

    assert "PRIVATE KEY" not in str(refused.value)
    assert PEM[40:80] not in str(refused.value)


def test_the_assertion_is_an_rs256_jwt_signed_by_the_key() -> None:
    account = ServiceAccount.from_setting(ACCOUNT_JSON)

    token = signed_assertion(account, now=1_800_000_000)

    header, claims, signature = token.split(".")
    assert json.loads(_b64decode(header)) == {"alg": "RS256", "typ": "JWT"}
    assert json.loads(_b64decode(claims)) == {
        "iss": "push@daliini-test.iam.gserviceaccount.com",
        "scope": "https://www.googleapis.com/auth/firebase.messaging",
        "aud": "https://oauth2.googleapis.com/token",
        "iat": 1_800_000_000,
        "exp": 1_800_003_600,
    }
    KEY.public_key().verify(
        _b64decode(signature),
        f"{header}.{claims}".encode("ascii"),
        padding.PKCS1v15(),
        hashes.SHA256(),
    )


def test_a_push_is_data_only_high_priority_and_carries_no_words() -> None:
    wire = Wire()

    transport(wire).send(project_id="daliini-test", message=MESSAGE)

    token_url, form, _ = wire.calls[0]
    grant = urllib.parse.parse_qs(form.decode("ascii"))
    assert token_url == "https://oauth2.googleapis.com/token"
    assert grant["grant_type"] == ["urn:ietf:params:oauth:grant-type:jwt-bearer"]
    [(url, body, headers)] = wire.sent()
    assert url == "https://fcm.googleapis.com/v1/projects/daliini-test/messages:send"
    assert headers["Authorization"] == "Bearer at-1"
    assert json.loads(body) == {
        "message": {
            "token": "device-token-1",
            "data": {"notificationId": "n-1", "type": "facility.application.approved"},
            "android": {"priority": "HIGH", "ttl": "86400s"},
        }
    }
    assert MESSAGE.title not in body.decode("utf-8")
    assert MESSAGE.body not in body.decode("utf-8")


def test_the_access_token_is_shared_until_it_nears_its_end() -> None:
    wire = Wire(sends=[(200, {})] * 3)
    clock = Clock()
    sender = transport(wire, clock)

    sender.send(project_id="p", message=MESSAGE)
    clock.now += 3000  # 50 minutes: still more than five left
    sender.send(project_id="p", message=MESSAGE)
    clock.now += 400  # under five minutes left: replaced before sending
    sender.send(project_id="p", message=MESSAGE)

    assert wire.token_requests() == 2
    assert [call[2]["Authorization"] for call in wire.sent()] == [
        "Bearer at-1",
        "Bearer at-1",
        "Bearer at-2",
    ]


def test_a_refused_access_token_is_replaced_once() -> None:
    wire = Wire(sends=[(401, {"error": {"status": "UNAUTHENTICATED"}}), (200, {})])

    transport(wire).send(project_id="p", message=MESSAGE)

    assert wire.token_requests() == 2
    assert [call[2]["Authorization"] for call in wire.sent()] == ["Bearer at-1", "Bearer at-2"]


@pytest.mark.parametrize(
    "answer",
    [
        fcm_error(404, "UNREGISTERED", "Requested entity was not found."),
        fcm_error(403, "SENDER_ID_MISMATCH", "SenderId mismatch"),
        fcm_error(400, "INVALID_ARGUMENT", "The registration token is not a valid FCM token"),
        fcm_error(400, "INVALID_ARGUMENT", "Invalid value", field="message.token"),
    ],
)
def test_a_token_fcm_disowns_is_invalid(answer: tuple[int, dict[str, Any]]) -> None:
    with pytest.raises(InvalidPushToken):
        transport(Wire(sends=[answer])).send(project_id="p", message=MESSAGE)


@pytest.mark.parametrize(
    "answer",
    [
        fcm_error(429, "QUOTA_EXCEEDED"),
        fcm_error(503, "UNAVAILABLE"),
        fcm_error(500, "INTERNAL"),
        OSError("timed out"),
    ],
)
def test_quota_outages_and_timeouts_are_retried(answer: Any) -> None:
    with pytest.raises(TransientPushError):
        transport(Wire(sends=[answer])).send(project_id="p", message=MESSAGE)


@pytest.mark.parametrize(
    "answer",
    [
        fcm_error(400, "INVALID_ARGUMENT", "Invalid JSON payload received.", field="message.data"),
        fcm_error(403, "PERMISSION_DENIED", "Permission denied"),
        (404, {"error": {"code": 404, "status": "NOT_FOUND", "message": "no such project"}}),
    ],
)
def test_a_refusal_of_our_setup_keeps_the_token(answer: tuple[int, dict[str, Any]]) -> None:
    with pytest.raises(ImproperlyConfigured):
        transport(Wire(sends=[answer])).send(project_id="p", message=MESSAGE)


def test_the_token_endpoint_refusing_the_key_is_configuration_and_its_outage_is_transient() -> None:
    with pytest.raises(ImproperlyConfigured):
        transport(Wire(tokens=[(400, {"error": "invalid_grant"})])).send(
            project_id="p", message=MESSAGE
        )
    with pytest.raises(TransientPushError):
        transport(Wire(tokens=[(503, {})])).send(project_id="p", message=MESSAGE)
    with pytest.raises(TransientPushError):
        transport(Wire(tokens=[OSError("unreachable")])).send(project_id="p", message=MESSAGE)


@override_settings(FCM_PROJECT_ID="daliini-test", FCM_SERVICE_ACCOUNT_JSON=ACCOUNT_JSON)
def test_the_provider_signs_in_with_the_configured_key_once_per_process() -> None:
    fcm_http._cached = None

    first, second = FcmPushProvider(), FcmPushProvider()

    assert isinstance(first.transport, FcmHttpTransport)
    assert first.transport is second.transport


@override_settings(FCM_PROJECT_ID="daliini-test", FCM_SERVICE_ACCOUNT_JSON="")
def test_without_a_key_a_send_is_a_configuration_error() -> None:
    with pytest.raises(ImproperlyConfigured, match="FCM_SERVICE_ACCOUNT_JSON"):
        FcmPushProvider().send(MESSAGE)
