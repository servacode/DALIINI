from collections import Counter
from typing import Any
from uuid import UUID

from django.core.exceptions import ValidationError
from django.db import transaction
from django.utils import timezone

from accounts.models import User, UserAdminRole
from audit.services import record_audit
from facilities.models import Facility, FacilityApplication, FacilityReport, VerificationEvidence
from notifications.models import Notification
from notifications.services import create_notification
from sessions.models import UserSession


def _request_id(request: Any) -> str:
    return getattr(request, "request_id", "")


def decide_report(
    *, report: FacilityReport, target_status: str, actor: Any, note: str = "", request_id: str = ""
) -> FacilityReport:
    """Resolve or dismiss one OPEN report the caller has already locked. Audited."""
    if report.status != FacilityReport.Status.OPEN:
        raise ValidationError({"status": "Only open reports can be decided."})
    report.status = target_status
    report.resolved_by = actor
    report.resolved_at = timezone.now()
    report.save(update_fields=["status", "resolved_by", "resolved_at"])
    record_audit(
        actor=actor,
        action=f"facility_report.{target_status.lower()}",
        target=report,
        before_snapshot={"status": FacilityReport.Status.OPEN},
        after_snapshot={"status": report.status},
        metadata={"facilityId": str(report.facility_id), "note": (note or "").strip()},
        request_id=request_id,
    )
    return report


def _facility_snapshot(facility: Facility) -> dict[str, Any]:
    return {
        "status": facility.status,
        "nameAr": facility.name_ar,
        "categoryId": str(facility.category_id),
        "provinceId": str(facility.province_id),
    }


def _required_evidence_is_complete(facility: Facility) -> bool:
    requirements = list(
        facility.category.verification_requirements.filter(active=True, required=True)
    )
    counts = Counter(
        VerificationEvidence.objects.filter(facility=facility).values_list(
            "requirement_id", flat=True
        )
    )
    return all(counts[item.id] >= item.min_files for item in requirements)


@transaction.atomic
def decide_application(
    *, request: Any, application_id: UUID, approve: bool, reason: str = ""
) -> FacilityApplication:
    application = (
        FacilityApplication.objects.select_for_update()
        .select_related("facility__category")
        .get(pk=application_id)
    )
    if application.status != FacilityApplication.Status.SUBMITTED:
        raise ValidationError("Only submitted applications can be reviewed.")
    if not approve and not reason.strip():
        raise ValidationError({"reason": "Rejection reason is required."})
    facility = Facility.objects.select_for_update().get(pk=application.facility_id)
    before = _facility_snapshot(facility)
    if approve:
        if not _required_evidence_is_complete(facility):
            raise ValidationError("Current verification evidence is incomplete.")
        application.status = FacilityApplication.Status.APPROVED
        application.rejection_reason = ""
        facility.status = Facility.Status.ACTIVE
        if facility.activated_at is None:
            facility.activated_at = timezone.now()
        facility.last_verified_at = timezone.now()
    else:
        application.status = FacilityApplication.Status.REJECTED
        application.rejection_reason = reason.strip()
        # A facility that was live before goes back to needing re-verification rather than
        # being demoted to a never-published draft.
        facility.status = (
            Facility.Status.REVERIFICATION_REQUIRED
            if facility.activated_at is not None
            else Facility.Status.DRAFT
        )
    application.reviewed_by = request.user
    application.reviewed_at = timezone.now()
    application.save(
        update_fields=[
            "status",
            "rejection_reason",
            "reviewed_by",
            "reviewed_at",
            "updated_at",
        ]
    )
    facility.save(update_fields=["status", "activated_at", "last_verified_at", "updated_at"])
    record_audit(
        actor=request.user,
        action="facility_application.approved" if approve else "facility_application.rejected",
        target=application,
        before_snapshot=before,
        after_snapshot=_facility_snapshot(facility),
        metadata={"reason": reason.strip() if not approve else ""},
        request_id=_request_id(request),
    )
    _tell_the_owners(facility, approve=approve, reason=reason.strip())
    return application


def _tell_the_owners(facility: Facility, *, approve: bool, reason: str) -> None:
    """A review decision reaches the people responsible for the facility.

    The message goes to the account's own inbox, which is the record; whether a push also
    reaches a device depends on a permission the owner may never have granted. The rejection
    reason is the reviewer's own words and is shown to the owner, who is the one asked to act
    on it.
    """
    title = "تمت الموافقة على منشأتك" if approve else "طلب منشأتك يحتاج تعديلاً"
    body = (
        f"{facility.name_ar} صارت ظاهرة في الدليل."
        if approve
        else f"سبب الرفض: {reason}" if reason else f"راجِع طلب {facility.name_ar} وأعد إرساله."
    )
    for membership in facility.memberships.select_related("user"):
        create_notification(
            user=membership.user,
            type=(
                "facility.application.approved" if approve else "facility.application.rejected"
            ),
            title_ar=title,
            body_ar=body[:400],
            destination=Notification.Destination.FACILITY,
            payload={"facilityId": str(facility.id)},
        )


@transaction.atomic
def transition_facility(
    *, request: Any, facility_id: UUID, target_status: str, reason: str = ""
) -> Facility:
    facility = Facility.objects.select_for_update().get(pk=facility_id)
    allowed: dict[str, set[str]] = {
        Facility.Status.SUSPENDED: {Facility.Status.ACTIVE},
        Facility.Status.ACTIVE: {Facility.Status.SUSPENDED},
        Facility.Status.CLOSED: {
            Facility.Status.ACTIVE,
            Facility.Status.SUSPENDED,
            Facility.Status.REVERIFICATION_REQUIRED,
        },
    }
    if facility.status not in allowed.get(target_status, set()):
        raise ValidationError("Invalid facility status transition.")
    before = _facility_snapshot(facility)
    facility.status = target_status
    facility.save(update_fields=["status", "updated_at"])
    record_audit(
        actor=request.user,
        action=f"facility.{target_status.lower()}",
        target=facility,
        before_snapshot=before,
        after_snapshot=_facility_snapshot(facility),
        metadata={"reason": reason.strip()},
        request_id=_request_id(request),
    )
    return facility


@transaction.atomic
def set_user_blocked(*, request: Any, user: User, blocked: bool) -> User:
    before = {"active": user.is_active}
    user.is_active = not blocked
    user.save(update_fields=["is_active", "updated_at"])
    if blocked:
        UserSession.objects.filter(user=user, revoked_at__isnull=True).update(
            revoked_at=timezone.now()
        )
    record_audit(
        actor=request.user,
        action="user.blocked" if blocked else "user.unblocked",
        target=user,
        before_snapshot=before,
        after_snapshot={"active": user.is_active},
        request_id=_request_id(request),
    )
    return user


@transaction.atomic
def replace_user_roles(*, request: Any, user: User, role_ids: list[Any]) -> None:
    before = list(
        UserAdminRole.objects.filter(user=user, active=True).values_list("role_id", flat=True)
    )
    UserAdminRole.objects.filter(user=user).update(active=False)
    for role_id in set(role_ids):
        link, _ = UserAdminRole.objects.get_or_create(user=user, role_id=role_id)
        if not link.active:
            link.active = True
            link.save(update_fields=["active"])
    after = list(
        UserAdminRole.objects.filter(user=user, active=True).values_list("role_id", flat=True)
    )
    record_audit(
        actor=request.user,
        action="user.admin_roles.replaced",
        target=user,
        before_snapshot={"roleIds": [str(value) for value in before]},
        after_snapshot={"roleIds": [str(value) for value in after]},
        request_id=_request_id(request),
    )
