from __future__ import annotations

import hashlib
import hmac
import secrets
from datetime import timedelta
from typing import Any
from uuid import UUID

from django.conf import settings
from django.contrib.auth import authenticate
from django.db import transaction
from django.utils import timezone
from rest_framework.exceptions import AuthenticationFailed, ValidationError

from audit.services import record_audit
from core.exceptions import ConflictError, DomainError
from facilities.models import Facility, FacilityMembership
from health.beacons import OTP, record_failure, record_ok
from locations.models import Province
from notifications.services import (
    deactivate_push_tokens_for_sessions,
    deactivate_push_tokens_for_user,
)
from sessions.models import UserSession

from .media import delete_profile_image
from .models import AccountDeletionRequest, OTPChallenge, User
from .otp import deliver_otp, generate_otp, otp_digest
from .providers.base import InvalidRecipient, TransientOtpError
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


def _session_payload(user: User, session: UserSession, raw_refresh: str) -> dict[str, Any]:
    return {
        "accessToken": issue_access_token(user.pk, session.pk),
        "refreshToken": raw_refresh,
        "sessionId": str(session.pk),
        "expiresAt": session.expires_at.isoformat(),
    }


def create_session(*, user: User, platform: str, device_name: str) -> dict[str, Any]:
    now = timezone.now()
    session = UserSession.objects.create(
        user=user,
        refresh_digest="pending-" + secrets.token_hex(32),
        platform=platform[:32],
        device_name=device_name[:120],
        expires_at=now + REFRESH_TTL,
    )
    # Every sign-in opens a session, so this is where "last sign-in" is true. The console's
    # account card reads it; without this it said «لم يدخل بعد» of everyone. An update, not a
    # save, so the account's own updated_at stays about the account.
    User.objects.filter(pk=user.pk).update(last_login=now)
    user.last_login = now
    raw = _new_refresh_for(session)
    session.refresh_digest = _refresh_digest(raw)
    session.save(update_fields=["refresh_digest"])
    return _session_payload(user, session, raw)


def start_challenge(
    *, phone: str, purpose: str, metadata: dict[str, Any] | None = None
) -> OTPChallenge:
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
    try:
        deliver_otp(phone=phone, code=code)
    except InvalidRecipient as exc:
        # A challenge nobody can ever answer is removed rather than left to expire, so the
        # pending list stays a list of codes that are actually on their way.
        challenge.delete()
        raise DomainError(
            "OTP_RECIPIENT_INVALID",
            message="تعذّر إرسال رمز التحقق إلى هذا الرقم. تأكد أن الرقم صحيح ومفعّل على واتساب.",
            status_code=422,
        ) from exc
    except TransientOtpError as exc:
        challenge.delete()
        # The channel is down, not the person's number: this is what the system page counts.
        record_failure(OTP, "unavailable")
        raise DomainError(
            "OTP_DELIVERY_UNAVAILABLE",
            message="تعذّر إرسال رمز التحقق الآن. حاول مرة أخرى بعد قليل.",
            status_code=503,
        ) from exc
    record_ok(OTP)
    return challenge


CONSOLE_SENT = "console"


def hand_over_console_code(phone: str) -> OTPChallenge | None:
    """The recovery code an operator sent this number, if one is still waiting — once.

    An operator who opens an account, or helps someone locked out, sends a code from the
    console (DECISION-104). The person then opens the app and asks to recover their password,
    and the app starts a recovery of its own. Without this the app sent a second code under a
    new challenge, and the first — the one the operator told them to expect — matched nothing
    anywhere: it could not be typed into any screen. So the app's first request for this
    number is answered with the code already on its way, and nothing new is sent.

    **Once.** A second request from the app is someone asking for a code again because the
    first never came, and that one sends a fresh code as usual. Nothing here tells a caller
    anything a code request did not already: the challenge id is useless without the code,
    and the code is only on the person's own phone.
    """
    now = timezone.now()
    with transaction.atomic():
        waiting = (
            OTPChallenge.objects.select_for_update()
            .filter(
                phone=phone,
                purpose=OTPChallenge.Purpose.RECOVERY,
                metadata__sentBy=CONSOLE_SENT,
                verified_at__isnull=True,
                consumed_at__isnull=True,
                expires_at__gt=now,
            )
            # Presence, not value: excluding `handedOver=True` compared a missing key to NULL,
            # which is neither true nor false, and so excluded every row — the code was never
            # handed over at all. A test caught it.
            .exclude(metadata__has_key="handedOver")
            .order_by("-created_at")
            .first()
        )
        if waiting is None:
            return None
        waiting.metadata = {**waiting.metadata, "handedOver": True}
        waiting.save(update_fields=["metadata"])
        return waiting


def verify_challenge(*, challenge_id: UUID, code: str, purpose: str) -> OTPChallenge:
    """Prove a code, counting every wrong one against the challenge.

    The wrong attempt is committed before the refusal is raised: raised inside the same
    transaction, the refusal would roll the count back with it, and a challenge would take
    guesses without end. So this must not be called inside another transaction either.
    """
    with transaction.atomic():
        challenge = OTPChallenge.objects.select_for_update().filter(pk=challenge_id).first()
        if challenge is None or challenge.purpose != purpose:
            raise ValidationError({"challengeId": "Invalid or expired challenge."})
        now = timezone.now()
        if challenge.consumed_at or challenge.expires_at <= now:
            raise ValidationError({"challengeId": "Invalid or expired challenge."})
        if challenge.attempt_count >= challenge.max_attempts:
            raise ValidationError({"challengeId": "Invalid or expired challenge."})
        expected = otp_digest(challenge_id=challenge.pk, code=code)
        wrong = not hmac.compare_digest(challenge.otp_digest, expected)
        if wrong:
            challenge.attempt_count += 1
            challenge.save(update_fields=["attempt_count"])
        else:
            challenge.verified_at = now
            challenge.save(update_fields=["verified_at"])
    if wrong:
        raise ValidationError({"code": "Invalid verification code."})
    return challenge


def _verified_challenge(*, challenge_id: UUID, purpose: str) -> OTPChallenge:
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
def complete_registration(
    *,
    challenge_id: UUID,
    display_name: str,
    password: str,
    platform: str,
    device_name: str,
) -> dict[str, Any]:
    """Open the account, now that the number has been shown to be theirs.

    The name arrives here rather than with the code, so nothing about a person is
    written down until they have proved the number and chosen a password.
    """
    challenge = _verified_challenge(
        challenge_id=challenge_id,
        purpose=OTPChallenge.Purpose.REGISTER,
    )
    if User.objects.filter(phone=challenge.phone).exists():
        # Said here rather than when the code was asked for: telling an anonymous caller that a
        # number has an account is telling them whose numbers are registered. By this point the
        # caller has proved they receive on it, so the only person this tells is its owner.
        raise ConflictError(
            "PHONE_ALREADY_REGISTERED",
            message="هذا الرقم له حساب بالفعل.",
        )
    province = Province.objects.filter(
        pk=challenge.metadata.get("provinceId"),
        active=True,
    ).first()
    if province is None:
        raise ValidationError({"provinceId": "Province is unavailable."})
    user = User.objects.create_user(
        phone=challenge.phone,
        password=password,
        name=display_name[:120],
        province=province,
        phone_verified_at=timezone.now(),
    )
    challenge.consumed_at = timezone.now()
    challenge.save(update_fields=["consumed_at"])
    _welcome(user)
    # Invitations sent to this number before it had an account are announced now. After the
    # welcome, so that an invitation — the one thing here that asks the reader to act — is the
    # first message in an inbox that shows the newest at the top.
    from facilities.invitations import announce_waiting

    announce_waiting(user)
    return create_session(user=user, platform=platform, device_name=device_name)


WELCOME_TYPE = "account.welcome"


def _welcome(user: User) -> None:
    """The platform's first word to a new account, in the inbox.

    In the inbox and not over WhatsApp, deliberately. The code must go out over that channel;
    nothing else has to, and every extra message from an unofficial account raises the chance
    of the number being blocked, which would stop every registration at once (DECISION-052).

    No push is expected to leave with it either: the device registers its token after the
    session exists, which is after this. The inbox is the record, and it is open in front of
    the reader at this exact moment.
    """
    from notifications.services import notify

    from .tasks import WELCOME_DELAY_SECONDS, send_whatsapp_welcome

    # And on WhatsApp, after the code's one-minute gap has passed, once the account is committed
    # (DECISION-101). Queued, so a WhatsApp failure can never fail a registration.
    user_id = str(user.pk)
    transaction.on_commit(
        lambda: send_whatsapp_welcome.apply_async(args=[user_id], countdown=WELCOME_DELAY_SECONDS)
    )

    notify(
        user=user,
        type=WELCOME_TYPE,
        title_ar="أهلًا بك في دليني",
        body_ar=(
            "حسابك جاهز. تستطيع الآن تقييم المنشآت وحفظ ما يهمّك، "
            "وإن كانت لك منشأة فأضفها من «حسابي» لتظهر للناس بعد المراجعة."
        ),
    )


def login(*, phone: str, password: str, platform: str, device_name: str) -> dict[str, Any]:
    user = authenticate(phone=phone, password=password)
    if user is None or not user.is_active:
        raise AuthenticationFailed("Invalid credentials.")
    return create_session(user=user, platform=platform, device_name=device_name)


def rotate_refresh(*, raw_refresh: str) -> dict[str, Any]:
    """Rotate a refresh secret, or record a replay and refuse it.

    A replay has to revoke every session of the user, and that write has to survive the
    refusal. Raising inside the atomic block rolled it back, so a replayed secret was
    refused and nothing was revoked (INT-054). The refusal is therefore raised only after
    the transaction that records the compromise has committed.
    """
    outcome = _rotate_refresh(raw_refresh=raw_refresh)
    if outcome is None:
        raise AuthenticationFailed("Invalid refresh token.")
    return outcome


@transaction.atomic
def _rotate_refresh(*, raw_refresh: str) -> dict[str, Any] | None:
    """Rotate inside one transaction; None means a replay was detected and recorded."""
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
        deactivate_push_tokens_for_user(session.user)
        return None
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


def revoke_session(*, user: User, session_id: UUID | str) -> None:
    updated = UserSession.objects.filter(pk=session_id, user=user, revoked_at__isnull=True).update(
        revoked_at=timezone.now()
    )
    if not updated:
        raise ValidationError({"sessionId": "Session not found."})
    deactivate_push_tokens_for_sessions([session_id])


def revoke_all_sessions(*, user: User) -> None:
    UserSession.objects.filter(user=user, revoked_at__isnull=True).update(revoked_at=timezone.now())
    deactivate_push_tokens_for_user(user)


@transaction.atomic
def reset_password(*, challenge_id: UUID, password: str) -> None:
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


@transaction.atomic
def change_password(*, user: User, current_password: str, new_password: str) -> None:
    """Replace a password the caller can prove they already know.

    Every session ends, this one included: `02-BASELINE-DECISIONS.md` makes a password change
    a revocation, because the reason to change a password is usually that someone else may
    have had it. The caller signs in again with the new one.
    """
    locked = User.objects.select_for_update().get(pk=user.pk)
    if not locked.check_password(current_password):
        raise ValidationError({"currentPassword": "Current password is incorrect."})
    if current_password == new_password:
        raise ValidationError({"newPassword": "The new password must differ from the old one."})
    locked.set_password(new_password)
    locked.save(update_fields=["password", "updated_at"])
    revoke_all_sessions(user=locked)


@transaction.atomic
def start_phone_change(*, user: User, phone: str) -> OTPChallenge:
    """Send a code to the number the account is to move to.

    The code goes to the *new* number, not the old one: that is what proves the caller can
    receive on it, which is the only thing worth proving here. The account it belongs to is
    written into the challenge so that a code sent for one person cannot be spent by another.
    """
    if phone == user.phone:
        raise ValidationError({"phone": "This is already the number of this account."})
    if User.objects.filter(phone=phone).exclude(pk=user.pk).exists():
        raise ValidationError({"phone": "This number belongs to another account."})
    return start_challenge(
        phone=phone,
        purpose=OTPChallenge.Purpose.PHONE_CHANGE,
        metadata={"userId": str(user.pk)},
    )


def complete_phone_change(*, user: User, challenge_id: UUID, code: str) -> User:
    """Move the account to the number whose code has just been proved.

    Every session ends, this one included. The phone is how this account signs in, so
    changing it changes the identity — and a session issued to the old identity should not
    outlive it. This is the same rule a password change follows, for the same reason.
    """
    challenge = verify_challenge(
        challenge_id=challenge_id,
        code=code,
        purpose=OTPChallenge.Purpose.PHONE_CHANGE,
    )
    # A verified code is only good for the account it was started for.
    if challenge.metadata.get("userId") != str(user.pk):
        raise ValidationError({"challengeId": "Invalid or expired challenge."})
    # The code is proved outside this transaction, so a wrong one stays counted.
    with transaction.atomic():
        locked = User.objects.select_for_update().get(pk=user.pk)
        if User.objects.filter(phone=challenge.phone).exclude(pk=locked.pk).exists():
            raise ValidationError({"phone": "This number belongs to another account."})
        now = timezone.now()
        locked.phone = challenge.phone
        locked.phone_verified_at = now
        locked.updated_at = now
        locked.save(update_fields=["phone", "phone_verified_at", "updated_at"])
        challenge.consumed_at = now
        challenge.save(update_fields=["consumed_at"])
        revoke_all_sessions(user=locked)
        # The number itself is not recorded: an audit trail of who moved to which number is
        # a directory of people, and this one only needs to know that it happened.
        record_audit(actor=locked, action="account.phone.changed", target=locked)
    return locked


def _identity_digest(phone: str) -> str:
    key = settings.RECOVERY_HMAC_SECRET.encode("utf-8")
    return hmac.new(key, f"deletion:{phone}".encode(), hashlib.sha256).hexdigest()


@transaction.atomic
def request_account_deletion(
    *,
    user: User,
    request_id: str = "",
    channel: str = AccountDeletionRequest.Channel.IN_APP,
    actor: Any = None,
) -> AccountDeletionRequest:
    """Delete an account at its owner's request: from the app, or by an operator (DECISION-111).

    The same rules either way — the sole owner of a live facility is refused — and the same
    anonymisation. What differs is who is recorded as having done it: the person themselves in
    the app, or the operator who acted on their request from the site's deletion page.
    """
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
        channel=channel,
        status=AccountDeletionRequest.Status.COMPLETED,
        verified_at=timezone.now(),
        completed_at=timezone.now(),
        retention_notes="Audit/security records retained under documented retention policy.",
    )
    before = {"userId": str(locked.pk), "active": locked.is_active}
    revoke_all_sessions(user=locked)
    FacilityMembership.objects.filter(user=locked).delete()
    locked.admin_role_links.all().delete()
    # What was only ever the person's own goes with them: the places they saved and the
    # messages addressed to them. Ratings stay, unattributed, because they are part of a
    # facility's public score and no longer say who gave them.
    locked.favorites.all().delete()
    locked.notifications.all().delete()
    avatar_key = locked.profile_image_key
    locked.phone = "d" + locked.pk.hex[:15]
    locked.name = "Deleted user"
    locked.address = ""
    locked.province = None
    locked.profile_image_key = ""
    locked.is_active = False
    locked.set_unusable_password()
    locked.save()
    if avatar_key:
        # The picture sits in the public bucket under a permanent address. Blanking the key
        # alone would leave it reachable by anyone who had seen it, so the object is removed
        # once the anonymisation has committed.
        transaction.on_commit(lambda: delete_profile_image(avatar_key))
    record_audit(
        actor=actor or locked,
        action="account.deleted",
        target=deletion,
        before_snapshot=before,
        after_snapshot={"userId": str(locked.pk), "active": False},
        request_id=request_id,
    )
    return deletion
