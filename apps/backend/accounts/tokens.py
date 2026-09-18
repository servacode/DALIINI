from __future__ import annotations

import base64
import hashlib
import hmac
import json
from datetime import UTC, datetime, timedelta
from typing import Any

from django.conf import settings


def _b64(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def _unb64(value: str) -> bytes:
    padding = "=" * (-len(value) % 4)
    return base64.urlsafe_b64decode(value + padding)


def _key() -> bytes:
    return settings.ACCESS_TOKEN_SIGNING_KEY.encode("utf-8")


def issue_access_token(user_id: object, session_id: object, ttl_seconds: int = 900) -> str:
    """Sign a short-lived access token bound to the session that issued it.

    `sid` is what lets a revoked or compromised session stop working at once. Without it
    an access token outlived its session by up to `ttl_seconds`, and a WebSocket that
    authenticated with one stayed authenticated (INT-051).
    """
    now = datetime.now(UTC)
    header = {"alg": "HS256", "typ": "JWT"}
    payload = {
        "sub": str(user_id),
        "sid": str(session_id),
        "iat": int(now.timestamp()),
        "exp": int((now + timedelta(seconds=ttl_seconds)).timestamp()),
    }
    encoded_header = _b64(json.dumps(header, separators=(",", ":")).encode())
    encoded_payload = _b64(json.dumps(payload, separators=(",", ":")).encode())
    message = f"{encoded_header}.{encoded_payload}".encode("ascii")
    signature = _b64(hmac.new(_key(), message, hashlib.sha256).digest())
    return f"{encoded_header}.{encoded_payload}.{signature}"


def decode_access_token(token: str) -> dict[str, Any] | None:
    try:
        encoded_header, encoded_payload, encoded_signature = token.split(".")
        message = f"{encoded_header}.{encoded_payload}".encode("ascii")
        expected = hmac.new(_key(), message, hashlib.sha256).digest()
        supplied = _unb64(encoded_signature)
        if not hmac.compare_digest(expected, supplied):
            return None
        header = json.loads(_unb64(encoded_header))
        claims = json.loads(_unb64(encoded_payload))
        if header != {"alg": "HS256", "typ": "JWT"} or not isinstance(claims, dict):
            return None
        if int(claims["exp"]) <= int(datetime.now(UTC).timestamp()):
            return None
        if not isinstance(claims.get("sub"), str) or not isinstance(claims.get("sid"), str):
            return None
        return claims
    except (KeyError, TypeError, ValueError, json.JSONDecodeError):
        return None
