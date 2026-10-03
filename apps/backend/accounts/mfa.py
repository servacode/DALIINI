"""Two-step sign-in for the people who run the console (DECISION-065).

A password alone opens a session, as it always has: the app, the owner screens and the account
all work as before. What a password alone no longer opens is the console. An operator who has
an authenticator app must also give its six-digit code once per session, and the session
remembers it (`UserSession.mfa_verified_at`). Where `STAFF_MFA_REQUIRED` is on — production — an
operator without one is sent to set it up before the console answers anything else.

TOTP is RFC 6238 as every authenticator app implements it: HMAC-SHA1, 30-second steps, six
digits, one step of drift either way. A code is accepted once: the step it matched is recorded,
and a code from that step or an earlier one is refused. Ten one-time recovery codes are issued
when the authenticator is confirmed; only their digests are stored.
"""

from __future__ import annotations

import base64
import hashlib
import hmac
import secrets
import struct
import time
from typing import TYPE_CHECKING, Any
from urllib.parse import quote

from cryptography.fernet import Fernet, InvalidToken
from django.conf import settings
from django.core.exceptions import ImproperlyConfigured
from django.db import transaction
from django.utils import timezone

from audit.services import record_audit
from core.exceptions import ConflictError, DomainError

from .models import StaffRecoveryCode, StaffTotpDevice

if TYPE_CHECKING:
    from sessions.models import UserSession

    from .models import User

STEP_SECONDS = 30
DIGITS = 6
DRIFT_STEPS = 1
ISSUER = "Daliini"
RECOVERY_CODES = 10


# -------------------------------------------------------------------------------- TOTP


def new_secret() -> str:
    """160 random bits in base32, the form authenticator apps expect."""
    return base64.b32encode(secrets.token_bytes(20)).decode("ascii").rstrip("=")


def _key(secret: str) -> bytes:
    padded = secret.upper() + "=" * (-len(secret) % 8)
    return base64.b32decode(padded)


def code_at(secret: str, step: int) -> str:
    digest = hmac.new(_key(secret), struct.pack(">Q", step), hashlib.sha1).digest()
    offset = digest[-1] & 0x0F
    value = struct.unpack(">I", digest[offset : offset + 4])[0] & 0x7FFFFFFF
    return str(value % 10**DIGITS).zfill(DIGITS)


def current_step(now: float | None = None) -> int:
    return int((now if now is not None else time.time()) // STEP_SECONDS)


def matching_step(secret: str, code: str, *, after: int, now: float | None = None) -> int | None:
    """The step `code` belongs to, if it is current (± drift) and later than `after`."""
    code = "".join(ch for ch in code if ch.isdigit())
    if len(code) != DIGITS:
        return None
    here = current_step(now)
    for step in range(here - DRIFT_STEPS, here + DRIFT_STEPS + 1):
        if step > after and hmac.compare_digest(code_at(secret, step), code):
            return step
    return None


def provisioning_uri(secret: str, account: str) -> str:
    label = quote(f"{ISSUER}:{account}")
    return (
        f"otpauth://totp/{label}?secret={secret}&issuer={quote(ISSUER)}"
        f"&algorithm=SHA1&digits={DIGITS}&period={STEP_SECONDS}"
    )


# ---------------------------------------------------------------------- secret storage


def _fernet() -> Fernet:
    secret = getattr(settings, "MFA_ENCRYPTION_KEY", "")
    if not secret:
        raise ImproperlyConfigured("MFA_ENCRYPTION_KEY is required")
    key = base64.urlsafe_b64encode(hashlib.sha256(secret.encode("utf-8")).digest())
    return Fernet(key)


def _seal(secret: str) -> str:
    return _fernet().encrypt(secret.encode("ascii")).decode("ascii")


def _open(ciphertext: str) -> str:
    try:
        return _fernet().decrypt(ciphertext.encode("ascii")).decode("ascii")
    except InvalidToken as exc:
        raise ImproperlyConfigured("MFA secret cannot be decrypted with this key") from exc


def _recovery_digest(code: str) -> str:
    normalized = "".join(ch for ch in code.upper() if ch.isalnum())
    key = getattr(settings, "MFA_ENCRYPTION_KEY", "").encode("utf-8")
    return hmac.new(key, normalized.encode("ascii"), hashlib.sha256).hexdigest()


def _new_recovery_code() -> str:
    raw = base64.b32encode(secrets.token_bytes(5)).decode("ascii")
    return f"{raw[:4]}-{raw[4:8]}"


# ---------------------------------------------------------------------------- policy


def confirmed_device(user: User) -> StaffTotpDevice | None:
    return StaffTotpDevice.objects.filter(user=user, confirmed_at__isnull=False).first()


def required_for_staff() -> bool:
    return bool(getattr(settings, "STAFF_MFA_REQUIRED", False))


def status(user: User, session: UserSession | None) -> dict[str, Any]:
    device = confirmed_device(user)
    return {
        "enabled": device is not None,
        "required": required_for_staff(),
        "verified": bool(session is not None and session.mfa_verified_at is not None),
        "recoveryCodesLeft": (
            StaffRecoveryCode.objects.filter(user=user, used_at__isnull=True).count()
            if device
            else 0
        ),
    }


def console_block(user: User, session: UserSession | None) -> str | None:
    """Why an operator's request may not reach the console now, or None if it may."""
    device = confirmed_device(user)
    if device is None:
        return "MFA_ENROLLMENT_REQUIRED" if required_for_staff() else None
    if session is None or session.mfa_verified_at is None:
        return "MFA_REQUIRED"
    return None


BLOCK_MESSAGES = {
    "MFA_REQUIRED": "أدخل رمز تطبيق المصادقة لمتابعة العمل في لوحة التحكم.",
    "MFA_ENROLLMENT_REQUIRED": "فعّل التحقق بخطوتين قبل استخدام لوحة التحكم.",
}


def refuse(code: str) -> DomainError:
    return DomainError(code, message=BLOCK_MESSAGES[code], status_code=403)


# ------------------------------------------------------------------------- lifecycle


@transaction.atomic
def begin_setup(*, user: User) -> dict[str, str]:
    """A fresh secret for an operator's authenticator, shown once until it is confirmed."""
    import segno

    if confirmed_device(user) is not None:
        raise ConflictError(
            "MFA_ALREADY_ENABLED", message="التحقق بخطوتين مفعّل بالفعل لهذا الحساب."
        )
    secret = new_secret()
    StaffTotpDevice.objects.update_or_create(
        user=user,
        defaults={"secret_ciphertext": _seal(secret), "confirmed_at": None, "last_used_step": 0},
    )
    uri = provisioning_uri(secret, user.phone)
    qr = segno.make(uri, error="m").svg_data_uri(scale=5, border=2)
    return {"secret": secret, "otpauthUri": uri, "qrSvgDataUri": qr}


def _accept_code(device: StaffTotpDevice, code: str) -> bool:
    step = matching_step(_open(device.secret_ciphertext), code, after=device.last_used_step)
    if step is None:
        return False
    device.last_used_step = step
    device.save(update_fields=["last_used_step"])
    return True


def _mark(session: UserSession) -> None:
    session.mfa_verified_at = timezone.now()
    session.save(update_fields=["mfa_verified_at"])


@transaction.atomic
def confirm_setup(
    *, user: User, session: UserSession, code: str, request_id: str = ""
) -> list[str]:
    device = (
        StaffTotpDevice.objects.select_for_update()
        .filter(user=user, confirmed_at__isnull=True)
        .first()
    )
    if device is None:
        raise ConflictError("MFA_SETUP_NOT_STARTED", message="ابدأ الإعداد من جديد.")
    if not _accept_code(device, code):
        raise DomainError("MFA_CODE_INVALID", message="الرمز غير صحيح أو انتهت صلاحيته.")
    device.confirmed_at = timezone.now()
    device.save(update_fields=["confirmed_at"])
    StaffRecoveryCode.objects.filter(user=user).delete()
    codes = [_new_recovery_code() for _ in range(RECOVERY_CODES)]
    StaffRecoveryCode.objects.bulk_create(
        StaffRecoveryCode(user=user, digest=_recovery_digest(code)) for code in codes
    )
    _mark(session)
    record_audit(actor=user, action="mfa.enabled", target=user, request_id=request_id)
    return codes


@transaction.atomic
def verify(*, user: User, session: UserSession, code: str, request_id: str = "") -> None:
    """Pass the second step for this session, with an authenticator or a recovery code."""
    device = (
        StaffTotpDevice.objects.select_for_update()
        .filter(user=user, confirmed_at__isnull=False)
        .first()
    )
    if device is None:
        raise ConflictError("MFA_NOT_ENABLED", message="التحقق بخطوتين غير مفعّل لهذا الحساب.")
    if _accept_code(device, code):
        _mark(session)
        record_audit(actor=user, action="mfa.verified", target=user, request_id=request_id)
        return
    recovery = (
        StaffRecoveryCode.objects.select_for_update()
        .filter(user=user, digest=_recovery_digest(code), used_at__isnull=True)
        .first()
    )
    if recovery is None:
        raise DomainError("MFA_CODE_INVALID", message="الرمز غير صحيح أو انتهت صلاحيته.")
    recovery.used_at = timezone.now()
    recovery.save(update_fields=["used_at"])
    _mark(session)
    record_audit(
        actor=user,
        action="mfa.recovery_code_used",
        target=user,
        metadata={
            "left": StaffRecoveryCode.objects.filter(user=user, used_at__isnull=True).count()
        },
        request_id=request_id,
    )


@transaction.atomic
def disable(*, user: User, code: str, request_id: str = "") -> None:
    """Switch it off, which needs a current code: a stolen session alone cannot."""
    device = (
        StaffTotpDevice.objects.select_for_update()
        .filter(user=user, confirmed_at__isnull=False)
        .first()
    )
    if device is None:
        raise ConflictError("MFA_NOT_ENABLED", message="التحقق بخطوتين غير مفعّل لهذا الحساب.")
    if not _accept_code(device, code):
        raise DomainError("MFA_CODE_INVALID", message="الرمز غير صحيح أو انتهت صلاحيته.")
    _forget(user)
    record_audit(actor=user, action="mfa.disabled", target=user, request_id=request_id)


def _forget(user: User) -> None:
    from sessions.models import UserSession

    StaffTotpDevice.objects.filter(user=user).delete()
    StaffRecoveryCode.objects.filter(user=user).delete()
    UserSession.objects.filter(user=user).update(mfa_verified_at=None)


@transaction.atomic
def reset_for(*, actor: User, user: User, request_id: str = "") -> None:
    """An administrator clears a colleague's authenticator after they lost it."""
    _forget(user)
    record_audit(actor=actor, action="mfa.reset", target=user, request_id=request_id)
