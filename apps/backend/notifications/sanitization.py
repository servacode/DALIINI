from __future__ import annotations

from typing import Any

SENSITIVE_KEYS = {
    "otp",
    "password",
    "token",
    "accessToken",
    "refreshToken",
    "phone",
    "storageKey",
    "evidenceId",
    "latitude",
    "longitude",
}


def safe_notification_payload(payload: dict[str, Any]) -> dict[str, Any]:
    if not isinstance(payload, dict):
        raise ValueError("Notification payload must be an object")
    unsafe = {key for key in payload if key in SENSITIVE_KEYS}
    if unsafe:
        raise ValueError(f"Unsafe notification payload keys: {sorted(unsafe)}")
    return payload
