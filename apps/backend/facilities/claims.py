"""«هذه منشأتي»: claiming a facility that nobody owns.

The directory lists some facilities itself (an operator adds them, DECISION-062) and will import
others from the pharmacists' syndicate. Those have no owner, and the pharmacist behind one must
be able to take it over without registering it a second time. A claim is that request: the
claimant finds the facility, uploads the category's verification documents as their own, and
an operator decides. Approval makes the claimant its owner; the facility stays published
throughout and its details do not change.

A claimant's documents belong to the claim (`VerificationEvidence.application`) until it is
approved, when they become the facility's. A withdrawn or rejected claim's documents are deleted.
"""

from __future__ import annotations

from typing import TYPE_CHECKING, Any
from uuid import UUID

from django.core.exceptions import ValidationError
from django.db import IntegrityError, transaction
from django.db.models import Exists, OuterRef, QuerySet
from django.utils import timezone

from audit.services import record_audit
from core.exceptions import ConflictError
from directory.models import CategoryProvince, VerificationRequirement
from storage.backends import PrivateS3Storage

from .models import Facility, FacilityApplication, FacilityMembership, VerificationEvidence

if TYPE_CHECKING:
    from accounts.models import User

OPEN_CLAIMS_PER_ACCOUNT = 5
OPEN = (FacilityApplication.Status.DRAFT, FacilityApplication.Status.SUBMITTED)


def _owned() -> Exists:
    return Exists(
        FacilityMembership.objects.filter(
            facility_id=OuterRef("pk"), role=FacilityMembership.Role.OWNER
        )
    )


def _onboarding_open() -> Exists:
    return Exists(
        CategoryProvince.objects.filter(
            province_id=OuterRef("province_id"),
            category_id=OuterRef("category_id"),
            owner_registration_enabled=True,
            category__active=True,
            category__capabilities__supports_owner_onboarding=True,
            province__active=True,
        )
    )


def claimable() -> QuerySet[Facility]:
    """Published facilities with no owner, where owners may register."""
    return (
        Facility.objects.filter(status=Facility.Status.ACTIVE)
        .filter(~_owned())
        .filter(_onboarding_open())
        .select_related("category", "province", "city")
    )


def _claims(user: User) -> QuerySet[FacilityApplication]:
    return FacilityApplication.objects.filter(kind=FacilityApplication.Kind.CLAIM, applicant=user)


def requirements(facility: Facility) -> list[VerificationRequirement]:
    return list(
        VerificationRequirement.objects.filter(category=facility.category, active=True).order_by(
            "sort_order", "id"
        )
    )


def own_evidence_complete(application: FacilityApplication) -> bool:
    """Every required document of the category, from the claim's own uploads only."""
    uploaded: dict[int, int] = {}
    for requirement_id in application.claim_evidence.values_list("requirement_id", flat=True):
        uploaded[requirement_id] = uploaded.get(requirement_id, 0) + 1
    return all(
        uploaded.get(item.pk, 0) >= item.min_files
        for item in requirements(application.facility)
        if item.required
    )


def claim_payload(application: FacilityApplication) -> dict[str, Any]:
    facility = application.facility
    return {
        "id": str(application.pk),
        "status": application.status,
        "rejectionReason": application.rejection_reason or None,
        "submittedAt": application.submitted_at.isoformat() if application.submitted_at else None,
        "reviewedAt": application.reviewed_at.isoformat() if application.reviewed_at else None,
        "facility": {
            "id": str(facility.pk),
            "nameAr": facility.name_ar,
            "categoryNameAr": facility.category.name_ar,
            "provinceNameAr": facility.province.name_ar,
            "addressAr": facility.address_ar or None,
        },
        "requirements": [
            {
                "id": item.pk,
                "labelAr": item.label_ar,
                "required": item.required,
                "minFiles": item.min_files,
                "maxFiles": item.max_files,
            }
            for item in requirements(facility)
        ],
        "evidence": [
            {
                "id": str(row.pk),
                "requirementId": row.requirement_id,
                "createdAt": row.created_at.isoformat(),
            }
            for row in application.claim_evidence.order_by("created_at")
        ],
    }


def my_claims(user: User) -> QuerySet[FacilityApplication]:
    return (
        _claims(user)
        .select_related("facility__category", "facility__province")
        .order_by("-created_at")
    )


def _mine(user: User, application_id: UUID, *, lock: bool = True) -> FacilityApplication:
    queryset = _claims(user).select_related("facility__category", "facility__province")
    if lock:
        queryset = queryset.select_for_update(of=("self",))
    application = queryset.filter(pk=application_id).first()
    if application is None:
        raise FacilityApplication.DoesNotExist
    return application


def _editable(application: FacilityApplication) -> None:
    if application.status != FacilityApplication.Status.DRAFT:
        raise ConflictError(
            "CLAIM_LOCKED",
            message="لا يمكن تعديل المطالبة بعد إرسالها. اسحبها وابدأ من جديد إن احتجت.",
        )


@transaction.atomic
def start(*, user: User, facility_id: UUID, request_id: str = "") -> FacilityApplication:
    facility = claimable().filter(pk=facility_id).first()
    if facility is None:
        if Facility.objects.filter(pk=facility_id).filter(_owned()).exists():
            raise ConflictError(
                "FACILITY_ALREADY_OWNED", message="لهذه المنشأة مالك مسجّل في الدليل."
            )
        raise Facility.DoesNotExist
    existing = _claims(user).filter(facility=facility, status__in=OPEN).first()
    if existing is not None:
        return existing
    if _claims(user).filter(status__in=OPEN).count() >= OPEN_CLAIMS_PER_ACCOUNT:
        raise ConflictError(
            "TOO_MANY_CLAIMS",
            message="لديك مطالبات مفتوحة كثيرة. أكملها أو اسحبها قبل أن تبدأ غيرها.",
        )
    application = FacilityApplication.objects.create(
        facility=facility,
        kind=FacilityApplication.Kind.CLAIM,
        status=FacilityApplication.Status.DRAFT,
        applicant=user,
    )
    record_audit(
        actor=user,
        action="facility.claim.started",
        target=application,
        metadata={"facilityId": str(facility.pk)},
        request_id=request_id,
    )
    return application


@transaction.atomic
def add_evidence(
    *, user: User, application_id: UUID, requirement_id: int, upload: Any, request_id: str = ""
) -> VerificationEvidence:
    from facilities.media import save_private_evidence

    application = _mine(user, application_id)
    _editable(application)
    requirement = VerificationRequirement.objects.filter(
        pk=requirement_id, category=application.facility.category, active=True
    ).first()
    if requirement is None:
        raise ValidationError({"requirementId": "Not a document this category asks for."})
    if application.claim_evidence.filter(requirement=requirement).count() >= requirement.max_files:
        raise ConflictError(
            "EVIDENCE_MAX_FILES", message="تم بلوغ الحد الأقصى لعدد ملفات هذا المتطلب."
        )
    storage, key, _, _ = save_private_evidence(
        facility_id=application.facility_id, requirement_id=requirement.pk, upload=upload
    )
    try:
        evidence = VerificationEvidence.objects.create(
            facility_id=application.facility_id,
            application=application,
            requirement=requirement,
            storage_key=key,
            uploaded_by=user,
        )
    except Exception:
        storage.delete(key)
        raise
    record_audit(
        actor=user,
        action="facility.claim.evidence_added",
        target=evidence,
        metadata={"facilityId": str(application.facility_id), "claimId": str(application.pk)},
        request_id=request_id,
    )
    return evidence


def _delete_files(rows: QuerySet[VerificationEvidence]) -> None:
    keys = list(rows.values_list("storage_key", flat=True))
    rows.delete()

    def remove() -> None:
        storage = PrivateS3Storage()
        for key in keys:
            storage.delete(key)

    transaction.on_commit(remove)


@transaction.atomic
def remove_evidence(
    *, user: User, application_id: UUID, evidence_id: UUID, request_id: str = ""
) -> None:
    application = _mine(user, application_id)
    _editable(application)
    rows = application.claim_evidence.filter(pk=evidence_id)
    if not rows.exists():
        raise VerificationEvidence.DoesNotExist
    _delete_files(rows)
    record_audit(
        actor=user,
        action="facility.claim.evidence_removed",
        target=application,
        metadata={"facilityId": str(application.facility_id)},
        request_id=request_id,
    )


@transaction.atomic
def submit(*, user: User, application_id: UUID, request_id: str = "") -> FacilityApplication:
    from .services import application_snapshot

    application = _mine(user, application_id)
    _editable(application)
    if not claimable().filter(pk=application.facility_id).exists():
        raise ConflictError(
            "FACILITY_ALREADY_OWNED", message="لهذه المنشأة مالك مسجّل في الدليل الآن."
        )
    if not own_evidence_complete(application):
        raise ValidationError({"evidence": "Required verification documents are missing."})
    application.status = FacilityApplication.Status.SUBMITTED
    application.submitted_at = timezone.now()
    application.rejection_reason = ""
    application.snapshot = application_snapshot(application.facility)
    try:
        with transaction.atomic():
            application.save()
    except IntegrityError as exc:
        raise ConflictError(
            "CLAIM_PENDING",
            message="هناك مطالبة أخرى بهذه المنشأة قيد المراجعة. حاول بعد البت فيها.",
        ) from exc
    record_audit(
        actor=user,
        action="facility.claim.submitted",
        target=application,
        metadata={"facilityId": str(application.facility_id)},
        request_id=request_id,
    )
    return application


@transaction.atomic
def withdraw(*, user: User, application_id: UUID, request_id: str = "") -> None:
    application = _mine(user, application_id)
    if application.status not in OPEN:
        raise ConflictError("CLAIM_CLOSED", message="هذه المطالبة لم تعد مفتوحة.")
    _delete_files(application.claim_evidence.all())
    record_audit(
        actor=user,
        action="facility.claim.withdrawn",
        target=application.facility,
        metadata={"facilityId": str(application.facility_id)},
        request_id=request_id,
    )
    application.delete()


def decide(
    facility: Facility, application: FacilityApplication, *, approve: bool, reason: str
) -> None:
    """An operator's decision on a submitted claim. Both rows are locked by the caller."""
    if approve:
        if facility.memberships.filter(role=FacilityMembership.Role.OWNER).exists():
            raise ConflictError(
                "FACILITY_ALREADY_OWNED",
                message="صار لهذه المنشأة مالك منذ إرسال المطالبة. ارفضها مع ذكر السبب.",
            )
        if not own_evidence_complete(application):
            raise ValidationError("Current verification evidence is incomplete.")
        assert application.applicant_id is not None
        member, created = FacilityMembership.objects.get_or_create(
            facility=facility,
            user_id=application.applicant_id,
            defaults={"role": FacilityMembership.Role.OWNER},
        )
        if not created:
            member.role = FacilityMembership.Role.OWNER
            member.save(update_fields=["role"])
        # The claimant's papers are now the facility's.
        application.claim_evidence.update(application=None)
        facility.last_verified_at = timezone.now()
        application.status = FacilityApplication.Status.APPROVED
        application.rejection_reason = ""
    else:
        _delete_files(application.claim_evidence.all())
        application.status = FacilityApplication.Status.REJECTED
        application.rejection_reason = reason.strip()
