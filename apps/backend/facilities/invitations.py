"""Inviting people to run a facility, by their phone number.

Members used to be added by account id, which no owner can know: there was no working way to
bring a pharmacist's colleague in. An owner now invites a phone number. What the owner is told
does not depend on whether that number has an account — the invitation is created and listed
the same way — so the feature cannot be used to learn who is registered. The person accepts
from their own account, which is what proves the number is theirs.
"""

from __future__ import annotations

from datetime import timedelta
from typing import TYPE_CHECKING, Any
from uuid import UUID

from django.db import transaction
from django.db.models import QuerySet
from django.utils import timezone

from audit.services import record_audit
from core.exceptions import ConflictError
from notifications.models import Notification
from notifications.services import notify

from .models import Facility, FacilityInvitation, FacilityMembership

if TYPE_CHECKING:
    from accounts.models import User

LIFETIME = timedelta(days=7)
ROLE_AR: dict[str, str] = {
    FacilityMembership.Role.OWNER.value: "مالكاً",
    FacilityMembership.Role.MANAGER.value: "مديراً",
}


def effective_status(invitation: FacilityInvitation, now: Any = None) -> str:
    """PENDING past its expiry reads as EXPIRED; nothing has to sweep the table for that."""
    now = now or timezone.now()
    if invitation.status == FacilityInvitation.Status.PENDING and invitation.expires_at <= now:
        return "EXPIRED"
    return str(invitation.status)


def invitation_payload(invitation: FacilityInvitation) -> dict[str, Any]:
    return {
        "id": str(invitation.pk),
        "phone": invitation.phone,
        "role": invitation.role,
        "status": effective_status(invitation),
        "createdAt": invitation.created_at.isoformat(),
        "expiresAt": invitation.expires_at.isoformat(),
        "respondedAt": invitation.responded_at.isoformat() if invitation.responded_at else None,
    }


def received_payload(invitation: FacilityInvitation) -> dict[str, Any]:
    facility = invitation.facility
    inviter = invitation.invited_by
    return {
        "id": str(invitation.pk),
        "role": invitation.role,
        "facility": {
            "id": str(facility.pk),
            "nameAr": facility.name_ar,
            "categoryNameAr": facility.category.name_ar,
            "provinceNameAr": facility.province.name_ar,
        },
        "invitedByName": inviter.name if inviter else None,
        "createdAt": invitation.created_at.isoformat(),
        "expiresAt": invitation.expires_at.isoformat(),
    }


@transaction.atomic
def invite(
    *, actor: User, facility: Facility, phone: str, role: str, request_id: str = ""
) -> FacilityInvitation:
    """Invite `phone` (already normalised). Re-inviting a waiting number renews it."""
    from accounts.models import User as UserModel

    if facility.memberships.filter(user__phone=phone).exists():
        # The owner already sees every member and their number, so this tells them nothing new.
        raise ConflictError("ALREADY_MEMBER", message="صاحب هذا الرقم عضو في المنشأة بالفعل.")
    now = timezone.now()
    invitation = (
        FacilityInvitation.objects.select_for_update()
        .filter(facility=facility, phone=phone, status=FacilityInvitation.Status.PENDING)
        .first()
    )
    if invitation is None:
        invitation = FacilityInvitation(facility=facility, phone=phone)
    invitation.role = role
    invitation.invited_by = actor
    invitation.expires_at = now + LIFETIME
    invitation.save()
    record_audit(
        actor=actor,
        action="facility.invitation.sent",
        target=invitation,
        # The number itself is the owner's to see on the invitation; the trail keeps its end.
        metadata={"facilityId": str(facility.pk), "role": role, "phoneEnd": phone[-3:]},
        request_id=request_id,
    )
    invitee = UserModel.objects.filter(phone=phone, is_active=True).first()
    if invitee is not None:
        _announce(invitation, invitee)
    return invitation


def _announce(invitation: FacilityInvitation, invitee: User) -> None:
    inviter = invitation.invited_by
    who = inviter.name if inviter else "مالك منشأة"
    notify(
        user=invitee,
        type="facility.invitation.received",
        title_ar="دعوة لإدارة منشأة",
        body_ar=(
            f"دعاك {who} لتكون {ROLE_AR.get(invitation.role, 'عضواً')} في "
            f"{invitation.facility.name_ar}. اقبل الدعوة من حسابك خلال سبعة أيام."
        )[:400],
        destination=Notification.Destination.OWNER_FACILITIES,
        payload={"invitationId": str(invitation.pk), "facilityId": str(invitation.facility_id)},
    )


def announce_waiting(user: User) -> None:
    """A new account learns of invitations sent to its number before it existed."""
    for invitation in received_by(user).select_related("facility", "invited_by"):
        _announce(invitation, user)


def received_by(user: User) -> QuerySet[FacilityInvitation]:
    return FacilityInvitation.objects.filter(
        phone=user.phone,
        status=FacilityInvitation.Status.PENDING,
        expires_at__gt=timezone.now(),
    ).exclude(facility__status=Facility.Status.CLOSED)


def _own(user: User, invitation_id: UUID) -> FacilityInvitation:
    invitation = (
        # Lock the invitation row only: `invited_by` is nullable, and PostgreSQL will not lock
        # the nullable side of the outer join that fetches it.
        FacilityInvitation.objects.select_for_update(of=("self",))
        .select_related("facility", "invited_by")
        .filter(pk=invitation_id, phone=user.phone)
        .first()
    )
    if invitation is None:
        # Somebody else's invitation is as absent as one that never existed.
        raise FacilityInvitation.DoesNotExist
    if invitation.status != FacilityInvitation.Status.PENDING:
        raise ConflictError("INVITATION_CLOSED", message="هذه الدعوة لم تعد قائمة.")
    if invitation.expires_at <= timezone.now():
        raise ConflictError(
            "INVITATION_EXPIRED", message="انتهت صلاحية الدعوة. اطلب من المالك إرسالها مجدداً."
        )
    if invitation.facility.status == Facility.Status.CLOSED:
        raise ConflictError("INVITATION_CLOSED", message="أُغلقت هذه المنشأة.")
    return invitation


@transaction.atomic
def accept(*, user: User, invitation_id: UUID, request_id: str = "") -> FacilityMembership:
    invitation = _own(user, invitation_id)
    member, created = FacilityMembership.objects.select_for_update().get_or_create(
        facility=invitation.facility, user=user, defaults={"role": invitation.role}
    )
    if not created and invitation.role == FacilityMembership.Role.OWNER:
        # An invitation can raise a manager to owner; it never lowers anyone.
        member.role = FacilityMembership.Role.OWNER
        member.save(update_fields=["role"])
    invitation.status = FacilityInvitation.Status.ACCEPTED
    invitation.accepted_by = user
    invitation.responded_at = timezone.now()
    invitation.save(update_fields=["status", "accepted_by", "responded_at"])
    record_audit(
        actor=user,
        action="facility.invitation.accepted",
        target=invitation,
        metadata={"facilityId": str(invitation.facility_id), "role": member.role},
        request_id=request_id,
    )
    if invitation.invited_by is not None:
        notify(
            user=invitation.invited_by,
            type="facility.invitation.accepted",
            title_ar="قُبلت دعوتك",
            body_ar=f"انضم {user.name} إلى {invitation.facility.name_ar}."[:400],
            destination=Notification.Destination.OWNER_FACILITIES,
            payload={"facilityId": str(invitation.facility_id)},
        )
    return member


@transaction.atomic
def decline(*, user: User, invitation_id: UUID, request_id: str = "") -> FacilityInvitation:
    invitation = _own(user, invitation_id)
    invitation.status = FacilityInvitation.Status.DECLINED
    invitation.responded_at = timezone.now()
    invitation.save(update_fields=["status", "responded_at"])
    record_audit(
        actor=user,
        action="facility.invitation.declined",
        target=invitation,
        metadata={"facilityId": str(invitation.facility_id)},
        request_id=request_id,
    )
    return invitation


@transaction.atomic
def revoke(
    *, actor: User, facility: Facility, invitation_id: UUID, request_id: str = ""
) -> FacilityInvitation:
    invitation = (
        FacilityInvitation.objects.select_for_update()
        .filter(pk=invitation_id, facility=facility)
        .first()
    )
    if invitation is None:
        raise FacilityInvitation.DoesNotExist
    if invitation.status != FacilityInvitation.Status.PENDING:
        raise ConflictError("INVITATION_CLOSED", message="هذه الدعوة لم تعد قائمة.")
    invitation.status = FacilityInvitation.Status.REVOKED
    invitation.responded_at = timezone.now()
    invitation.save(update_fields=["status", "responded_at"])
    record_audit(
        actor=actor,
        action="facility.invitation.revoked",
        target=invitation,
        metadata={"facilityId": str(facility.pk)},
        request_id=request_id,
    )
    return invitation
