from __future__ import annotations

import hashlib
import hmac
import secrets
from datetime import timedelta

from django.conf import settings
from django.contrib.auth import authenticate
from django.db import transaction
from django.utils import timezone
from rest_framework.exceptions import AuthenticationFailed, ValidationError

from audit.services import record_audit
from facilities.models import Facility, FacilityMembership
from locations.models import Province
from sessions.models import UserSession

from .models import AccountDeletionRequest, OTPChallenge, User
from .otp import deliver_otp, generate_otp, otp_digest
from .tokens import issue_access_token

OTP_TTL = timedelta(minutes=5)
REFRESH_TTL = timedelta(days=30)
REFRESH_GRACE = timedelta(seconds=30)


def _refresh_key() -> bytes:
    return settings.REFRESH_HMAC_SECRET.encode("utf-8")


def _refresh_digest(raw: str) -> str:
    return hmac.new(_refresh_key(), raw.encode("utf-8"), hashlib.sha256).hexdigest()


def _new_refresh_for(session: UserSession) -> str:
    return f"{session.pk}.{secrets.token_urlsafe(48)}"


def _session_payload(user: User, session: UserSession, raw_refresh: str) -> dict:
    return {
        "accessToken": issue_access_token(user.pk),
        "refreshToken": raw_refresh,
        "sessionId": str(session.pk),
        "expiresAt": session.expires_at.isoformat(),
    }


def create_session(*, user: User, platform: str, device_name: str) -> dict:
    session = UserSession.objects.create(
        user=user,
        refresh_digest="pending-" + secrets.token_hex(32),
        platform=platform[:32],
        device_name=device_name[:120],
        expires_at=timezone.now() + REFRESH_TTL,
    )
    raw = _new_refresh_for(session)
    session.refresh_digest = _refresh_digest(raw)
    session.save(update_fields=["refresh_digest"])
    return _session_payload(user, session, raw)


def start_challenge(*, phone: str, purpose: str, metadata: dict | None = None) -> OTPChallenge:
    challenge = OTPChallenge.objects.create(
        phone=phone,
        purpose=purpose,
        otp_digest="pending",
        expires_at=timezone.now() + OTP_TTL,
        metadata=metadata or {},
    )
    code = generate_otp()
    challenge.otp_digest = otp_digest(challenge_id=challenge.pk, code=code)
    challenge.save(update_fields=["otp_digest"])
    deliver_otp(phone=phone, code=code)
    return challenge


@transaction.atomic
def verify_challenge(*, challenge_id, code: str, purpose: str) -> OTPChallenge:
    challenge = OTPChallenge.objects.select_for_update().filter(pk=challenge_id).first()
    if challenge is None or challenge.purpose != purpose:
        raise ValidationError({"challengeId": "Invalid or expired challenge."})
    now = timezone.now()
    if challenge.consumed_at or challenge.expires_at <= now:
        raise ValidationError({"challengeId": "Invalid or expired challenge."})
    if challenge.attempt_count >= challenge.max_attempts:
        raise ValidationError({"challengeId": "Invalid or expired challenge."})
    expected = otp_digest(challenge_id=challenge.pk, code=code)
    if not hmac.compare_digest(challenge.otp_digest, expected):
        challenge.attempt_count += 1
        challenge.save(update_fields=["attempt_count"])
        raise ValidationError({"code": "Invalid verification code."})
    challenge.verified_at = now
    challenge.save(update_fields=["verified_at"])
    return challenge


def _verified_challenge(*, challenge_id, purpose: str) -> OTPChallenge:
    challenge = OTPChallenge.objects.select_for_update().filter(pk=challenge_id).first()
    now = timezone.now()
    if (
        challenge is None
        or challenge.purpose != purpose
        or challenge.verified_at is None
        or challenge.consumed_at is not None
        or challenge.expires_at <= now
    ):
        raise ValidationError({"challengeId": "Invalid or expired challenge."})
    return challenge


@transaction.atomic
def complete_registration(*, challenge_id, password: str, platform: str, device_name: str) -> dict:
    challenge = _verified_challenge(
        challenge_id=challenge_id,
        purpose=OTPChallenge.Purpose.REGISTER,
    )
    if User.objects.filter(phone=challenge.phone).exists():
        raise ValidationError({"challengeId": "Registration cannot be completed."})
    province = Province.objects.filter(
        pk=challenge.metadata.get("provinceId"),
        active=True,
    ).first()
    if province is None:
        raise ValidationError({"provinceId": "Province is unavailable."})
    user = User.objects.create_user(
        phone=challenge.phone,
        password=password,
        name=challenge.metadata.get("displayName", "")[:120],
        province=province,
        phone_verified_at=timezone.now(),
    )
    challenge.consumed_at = timezone.now()
    challenge.save(update_fields=["consumed_at"])
    return create_session(user=user, platform=platform, device_name=device_name)


def login(*, phone: str, password: str, platform: str, device_name: str) -> dict:
    user = authenticate(phone=phone, password=password)
    if user is None or not user.is_active:
        raise AuthenticationFailed("Invalid credentials.")
    return create_session(user=user, platform=platform, device_name=device_name)


@transaction.atomic
def rotate_refresh(*, raw_refresh: str) -> dict:
    try:
        session_id, _ = raw_refresh.split(".", 1)
    except ValueError as exc:
        raise AuthenticationFailed("Invalid refresh token.") from exc
    session = (
        UserSession.objects.select_for_update()
        .select_related("user")
        .filter(pk=session_id)
        .first()
    )
    if session is None:
        raise AuthenticationFailed("Invalid refresh token.")
    now = timezone.now()
    supplied = _refresh_digest(raw_refresh)
    if (
        session.revoked_at
        or session.compromised_at
        or session.expires_at <= now
        or not session.user.is_active
    ):
        raise AuthenticationFailed("Invalid refresh token.")
    current = hmac.compare_digest(session.refresh_digest, supplied)
    previous = (
        bool(session.previous_refresh_digest)
        and hmac.compare_digest(session.previous_refresh_digest, supplied)
    )
    previous_in_grace = (
        previous
        and session.previous_valid_until is not None
        and session.previous_valid_until > now
    )
    if not current and not previous_in_grace:
        session.compromised_at = now
        session.revoked_at = now
        session.save(update_fields=["compromised_at", "revoked_at"])
        UserSession.objects.filter(
            user=session.user,
            revoked_at__isnull=True,
        ).update(revoked_at=now)
        raise AuthenticationFailed("Invalid refresh token.")
    raw_new = _new_refresh_for(session)
    session.previous_refresh_digest = session.refresh_digest
    session.previous_valid_until = now + REFRESH_GRACE
    session.refresh_digest = _refresh_digest(raw_new)
    session.last_seen_at = now
    session.save(
        update_fields=[
            "previous_refresh_digest",
            "previous_valid_until",
            "refresh_digest",
            "last_seen_at",
        ]
    )
    return _session_payload(session.user, session, raw_new)


def revoke_session(*, user: User, session_id) -> None:
    updated = UserSession.objects.filter(pk=session_id, user=user, revoked_at__isnull=True).update(
        revoked_at=timezone.now()
    )
    if not updated:
        raise ValidationError({"sessionId": "Session not found."})


def revoke_all_sessions(*, user: User) -> None:
    UserSession.objects.filter(user=user, revoked_at__isnull=True).update(revoked_at=timezone.now())


@transaction.atomic
def reset_password(*, challenge_id, password: str) -> None:
    challenge = _verified_challenge(
        challenge_id=challenge_id,
        purpose=OTPChallenge.Purpose.RECOVERY,
    )
    user = User.objects.select_for_update().filter(phone=challenge.phone, is_active=True).first()
    if user is None:
        challenge.consumed_at = timezone.now()
        challenge.save(update_fields=["consumed_at"])
        return
    user.set_password(password)
    user.save(update_fields=["password", "updated_at"])
    revoke_all_sessions(user=user)
    challenge.consumed_at = timezone.now()
    challenge.save(update_fields=["consumed_at"])


def _identity_digest(phone: str) -> str:
    key = settings.RECOVERY_HMAC_SECRET.encode("utf-8")
    return hmac.new(key, f"deletion:{phone}".encode(), hashlib.sha256).hexdigest()


@transaction.atomic
def request_account_deletion(*, user: User, request_id: str = "") -> AccountDeletionRequest:
    locked = User.objects.select_for_update().get(pk=user.pk)
    sole_owned = []
    owner_links = FacilityMembership.objects.select_related("facility").filter(
        user=locked,
        role=FacilityMembership.Role.OWNER,
    )
    for link in owner_links:
        if link.facility.status == Facility.Status.CLOSED:
            continue
        another_owner = FacilityMembership.objects.filter(
            facility=link.facility,
            role=FacilityMembership.Role.OWNER,
        ).exclude(user=locked).exists()
        if not another_owner:
            sole_owned.append(str(link.facility_id))
    if sole_owned:
        raise ValidationError(
            {
                "ownedFacilities": (
                    "Transfer ownership or close facilities before deleting the account."
                )
            }
        )
    deletion = AccountDeletionRequest.objects.create(
        user=locked,
        identity_digest=_identity_digest(locked.phone),
        channel=AccountDeletionRequest.Channel.IN_APP,
        status=AccountDeletionRequest.Status.COMPLETED,
        verified_at=timezone.now(),
        completed_at=timezone.now(),
        retention_notes="Audit/security records retained under documented retention policy.",
    )
    before = {"userId": str(locked.pk), "active": locked.is_active}
    revoke_all_sessions(user=locked)
    FacilityMembership.objects.filter(user=locked).delete()
    locked.admin_role_links.all().delete()
    locked.phone = "d" + locked.pk.hex[:15]
    locked.name = "Deleted user"
    locked.province = None
    locked.profile_image_key = ""
    locked.is_active = False
    locked.set_unusable_password()
    locked.save()
    record_audit(
        actor=locked,
        action="account.deleted",
        target=deletion,
        before_snapshot=before,
        after_snapshot={"userId": str(locked.pk), "active": False},
        request_id=request_id,
    )
    return deletion
