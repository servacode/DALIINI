"""FCM HTTP v1: the transport behind PUSH_PROVIDER=fcm.

What leaves the server is a data-only message: identifiers, never words. The app shows a
neutral notice and reads the substance over REST, so `title` and `body` stay here. It goes
out at high priority, so a duty reminder is not held back while the phone dozes, and lives
one day; after that the inbox still has it.

Authentication is a Google service account key (FCM_SERVICE_ACCOUNT_JSON, the JSON itself or
its base64), exchanged through the signed-JWT grant for a one-hour access token that every
send in the process shares until shortly before it expires. Only the standard library and
`cryptography` are used, so no Google SDK sits between the domain and the wire.
"""

from __future__ import annotations

import base64
import json
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from collections.abc import Callable
from dataclasses import dataclass
from typing import Any

from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa
from django.conf import settings
from django.core.exceptions import ImproperlyConfigured

from .base import InvalidPushToken, PushMessage, TransientPushError

SCOPE = "https://www.googleapis.com/auth/firebase.messaging"
DEFAULT_TOKEN_URI = "https://oauth2.googleapis.com/token"
SEND_URL = "https://fcm.googleapis.com/v1/projects/{project}/messages:send"
JWT_BEARER = "urn:ietf:params:oauth:grant-type:jwt-bearer"
TIMEOUT_SECONDS = 10
ASSERTION_LIFETIME_SECONDS = 3600
# A token this close to its end is replaced rather than sent and refused mid-flight.
REFRESH_MARGIN_SECONDS = 300
MESSAGE_TTL = "86400s"

# FCM's own verdicts that the token will never work again for this project.
TOKEN_GONE = {"UNREGISTERED", "SENDER_ID_MISMATCH"}
RETRYABLE = {"QUOTA_EXCEEDED", "UNAVAILABLE", "INTERNAL"}

# POST (url, body, headers) -> (status, body). Raises OSError when nothing came back.
Opener = Callable[[str, bytes, dict[str, str]], tuple[int, bytes]]


def _urlopen(url: str, body: bytes, headers: dict[str, str]) -> tuple[int, bytes]:
    request = urllib.request.Request(url, data=body, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(request, timeout=TIMEOUT_SECONDS) as response:
            return int(response.status), response.read()
    except urllib.error.HTTPError as error:
        return int(error.code), error.read()


def _b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


@dataclass(frozen=True)
class ServiceAccount:
    client_email: str
    private_key: rsa.RSAPrivateKey
    token_uri: str

    @classmethod
    def from_setting(cls, raw: str) -> ServiceAccount:
        """Read the key file's JSON, as pasted or base64-encoded. Never echoes the secret."""
        text = raw.strip()
        if not text.startswith("{"):
            try:
                text = base64.b64decode(text, validate=True).decode("utf-8")
            except (ValueError, UnicodeDecodeError) as exc:
                raise ImproperlyConfigured(
                    "FCM_SERVICE_ACCOUNT_JSON is neither JSON nor base64-encoded JSON"
                ) from exc
        try:
            info = json.loads(text)
            client_email = str(info["client_email"])
            pem = str(info["private_key"]).encode("utf-8")
            token_uri = str(info.get("token_uri") or DEFAULT_TOKEN_URI)
        except (ValueError, KeyError, TypeError) as exc:
            raise ImproperlyConfigured(
                "FCM_SERVICE_ACCOUNT_JSON is not a service account key "
                "(client_email and private_key are required)"
            ) from exc
        try:
            key = serialization.load_pem_private_key(pem, password=None)
        except (ValueError, TypeError) as exc:
            raise ImproperlyConfigured(
                "FCM_SERVICE_ACCOUNT_JSON has an unreadable private key"
            ) from exc
        if not isinstance(key, rsa.RSAPrivateKey):
            raise ImproperlyConfigured("FCM_SERVICE_ACCOUNT_JSON must hold an RSA key")
        if urllib.parse.urlsplit(token_uri).scheme != "https":
            raise ImproperlyConfigured("FCM_SERVICE_ACCOUNT_JSON token_uri must be https")
        return cls(client_email=client_email, private_key=key, token_uri=token_uri)


def signed_assertion(account: ServiceAccount, now: int) -> str:
    """The RS256 JWT the token endpoint trades for an access token."""
    header = {"alg": "RS256", "typ": "JWT"}
    claims = {
        "iss": account.client_email,
        "scope": SCOPE,
        "aud": account.token_uri,
        "iat": now,
        "exp": now + ASSERTION_LIFETIME_SECONDS,
    }
    signing_input = ".".join(
        _b64url(json.dumps(part, separators=(",", ":")).encode("utf-8"))
        for part in (header, claims)
    )
    signature = account.private_key.sign(
        signing_input.encode("ascii"), padding.PKCS1v15(), hashes.SHA256()
    )
    return f"{signing_input}.{_b64url(signature)}"


def fcm_message(message: PushMessage) -> dict[str, Any]:
    """The v1 `message`: the device token and the identifiers; the words never leave."""
    return {
        "token": message.token,
        "data": {str(key): str(value) for key, value in message.data.items()},
        "android": {"priority": "HIGH", "ttl": MESSAGE_TTL},
    }


def _error(answer: bytes) -> dict[str, Any]:
    try:
        body = json.loads(answer or b"{}")
    except ValueError:
        return {}
    error = body.get("error") if isinstance(body, dict) else None
    return error if isinstance(error, dict) else {}


def _error_code(error: dict[str, Any]) -> str:
    for detail in error.get("details") or []:
        if isinstance(detail, dict) and detail.get("errorCode"):
            return str(detail["errorCode"])
    return str(error.get("status") or "")


def _names_the_token(error: dict[str, Any]) -> bool:
    """An INVALID_ARGUMENT about the registration token, not about our message."""
    for detail in error.get("details") or []:
        if not isinstance(detail, dict):
            continue
        for violation in detail.get("fieldViolations") or []:
            if isinstance(violation, dict) and violation.get("field") == "message.token":
                return True
    return "registration token" in str(error.get("message") or "").lower()


def classify(status: int, answer: bytes) -> Exception:
    """What a refused send means for the device and for a retry."""
    error = _error(answer)
    code = _error_code(error)
    if code in TOKEN_GONE:
        return InvalidPushToken()
    if code == "INVALID_ARGUMENT" and _names_the_token(error):
        return InvalidPushToken()
    if status == 429 or status >= 500 or code in RETRYABLE:
        return TransientPushError(f"FCM answered {status} {code}".strip())
    if status in (401, 403):
        return ImproperlyConfigured(f"FCM refused the service account ({status} {code})")
    return ImproperlyConfigured(f"FCM refused the message ({status} {code})")


class FcmHttpTransport:
    def __init__(
        self,
        account: ServiceAccount,
        *,
        opener: Opener | None = None,
        clock: Callable[[], float] = time.time,
    ) -> None:
        self.account = account
        self._open = opener or _urlopen
        self._clock = clock
        self._lock = threading.Lock()
        self._token: str | None = None
        self._expires_at = 0.0

    def send(self, *, project_id: str, message: PushMessage) -> None:
        body = json.dumps({"message": fcm_message(message)}).encode("utf-8")
        status, answer = self._post(project_id, body)
        if status == 401:
            # Our access token was refused (revoked, or the clock drifted): one fresh try.
            self._forget_token()
            status, answer = self._post(project_id, body)
        if 200 <= status < 300:
            return
        raise classify(status, answer)

    def _post(self, project_id: str, body: bytes) -> tuple[int, bytes]:
        url = SEND_URL.format(project=urllib.parse.quote(project_id, safe=""))
        headers = {
            "Authorization": f"Bearer {self._access_token()}",
            "Content-Type": "application/json; charset=utf-8",
        }
        try:
            return self._open(url, body, headers)
        except OSError as exc:
            raise TransientPushError("FCM could not be reached") from exc

    def _forget_token(self) -> None:
        with self._lock:
            self._token = None
            self._expires_at = 0.0

    def _access_token(self) -> str:
        with self._lock:
            now = self._clock()
            if self._token and now < self._expires_at - REFRESH_MARGIN_SECONDS:
                return self._token
            form = urllib.parse.urlencode(
                {"grant_type": JWT_BEARER, "assertion": signed_assertion(self.account, int(now))}
            ).encode("ascii")
            try:
                status, answer = self._open(
                    self.account.token_uri,
                    form,
                    {"Content-Type": "application/x-www-form-urlencoded"},
                )
            except OSError as exc:
                raise TransientPushError("The Google token endpoint could not be reached") from exc
            if status == 429 or status >= 500:
                raise TransientPushError(f"The Google token endpoint answered {status}")
            if status != 200:
                raise ImproperlyConfigured(f"Google refused the FCM service account ({status})")
            try:
                granted = json.loads(answer)
                token = str(granted["access_token"])
                lifetime = int(granted.get("expires_in", ASSERTION_LIFETIME_SECONDS))
            except (ValueError, KeyError, TypeError) as exc:
                raise TransientPushError("The Google token endpoint gave no token") from exc
            self._token, self._expires_at = token, now + lifetime
            return token


_cache_lock = threading.Lock()
_cached: tuple[str, FcmHttpTransport] | None = None


def transport_from_settings() -> FcmHttpTransport | None:
    """One transport per process and key, so its access token outlives a single send."""
    global _cached
    raw = str(getattr(settings, "FCM_SERVICE_ACCOUNT_JSON", "") or "")
    if not raw.strip():
        return None
    with _cache_lock:
        if _cached is None or _cached[0] != raw:
            _cached = (raw, FcmHttpTransport(ServiceAccount.from_setting(raw)))
        return _cached[1]
