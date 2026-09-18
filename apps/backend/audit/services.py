from typing import Any

from .models import AuditEvent

SENSITIVE_PARTS = (
    "password",
    "token",
    "secret",
    "database_url",
    "databaseurl",
    "otp",
    "refresh",
    "authorization",
    "cookie",
    "storage_key",
    "ciphertext",
)


def _redact(value: Any) -> Any:
    if isinstance(value, dict):
        output = {}
        for key, item in value.items():
            normalized = str(key).lower().replace("-", "_")
            output[key] = (
                "[REDACTED]"
                if any(part in normalized for part in SENSITIVE_PARTS)
                else _redact(item)
            )
        return output
    if isinstance(value, list):
        return [_redact(item) for item in value]
    return value


def record_audit(
    *,
    actor: Any,
    action: str,
    target: Any,
    metadata: dict[str, Any] | None = None,
    before_snapshot: dict[str, Any] | None = None,
    after_snapshot: dict[str, Any] | None = None,
    request_id: str = "",
) -> AuditEvent:
    return AuditEvent.objects.create(
        actor=actor,
        action=action,
        target_type=target.__class__.__name__,
        target_id=str(target.pk),
        before_snapshot=_redact(before_snapshot or {}),
        after_snapshot=_redact(after_snapshot or {}),
        request_id=request_id or "",
        metadata=_redact(metadata or {}),
    )
