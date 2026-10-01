"""Facility lifecycle: owner submission, operator decisions and status transitions."""

from types import SimpleNamespace
from typing import Any

import pytest
from django.contrib.gis.geos import Point
from django.core.exceptions import ValidationError

from accounts.models import User
from admin_console.services import decide_application, transition_facility
from audit.models import AuditEvent
from audit.services import record_audit
from directory.models import CategoryProvince
from facilities.models import Facility, FacilityApplication, FacilityMembership
from facilities.services import submit_facility, update_facility_core, update_facility_location


@pytest.fixture
def operator() -> Any:
    admin = User.objects.create_user(phone="+963900000077", password="x" * 12, name="Op")
    return SimpleNamespace(user=admin, request_id="req-1")


@pytest.fixture
def draft(facility: Facility, user: User) -> Facility:
    CategoryProvince.objects.create(
        category=facility.category,
        province=facility.province,
        public_enabled=True,
        owner_registration_enabled=True,
    )
    FacilityMembership.objects.create(facility=facility, user=user, role="OWNER")
    facility.status = Facility.Status.DRAFT
    facility.location = Point(39.01, 35.95, srid=4326)
    facility.save()
    return facility


def _submit(facility: Facility, user: User) -> FacilityApplication:
    return submit_facility(actor=user, facility=facility)


@pytest.mark.django_db
def test_draft_submitted_approved_becomes_active(
    draft: Facility, user: User, operator: Any
) -> None:
    application = _submit(draft, user)
    draft.refresh_from_db()
    assert draft.status == Facility.Status.SUBMITTED
    assert application.kind == FacilityApplication.Kind.INITIAL

    decide_application(request=operator, application_id=application.pk, approve=True)
    draft.refresh_from_db()
    application.refresh_from_db()
    assert draft.status == Facility.Status.ACTIVE
    assert draft.activated_at is not None
    assert application.status == FacilityApplication.Status.APPROVED
    assert application.reviewed_at is not None
    assert AuditEvent.objects.filter(action="facility_application.approved").exists()


@pytest.mark.django_db
def test_reject_needs_a_reason_and_returns_to_draft(
    draft: Facility, user: User, operator: Any
) -> None:
    application = _submit(draft, user)
    with pytest.raises(ValidationError):
        decide_application(request=operator, application_id=application.pk, approve=False)
    decide_application(
        request=operator, application_id=application.pk, approve=False, reason="صور غير واضحة"
    )
    draft.refresh_from_db()
    application.refresh_from_db()
    assert draft.status == Facility.Status.DRAFT
    assert application.rejection_reason == "صور غير واضحة"
    # A decided application cannot be decided again.
    with pytest.raises(ValidationError):
        decide_application(request=operator, application_id=application.pk, approve=True)
    # The owner can fix and resubmit, reusing the rejected application.
    again = _submit(draft, user)
    assert again.pk == application.pk


@pytest.mark.django_db
def test_sensitive_edit_requires_reverification(draft: Facility, user: User, operator: Any) -> None:
    decide_application(request=operator, application_id=_submit(draft, user).pk, approve=True)
    update_facility_core(actor=user, facility=draft, data={"nameAr": "اسم جديد"})
    draft.refresh_from_db()
    assert draft.status == Facility.Status.REVERIFICATION_REQUIRED

    application = _submit(draft, user)
    assert application.kind == FacilityApplication.Kind.REVERIFICATION
    decide_application(request=operator, application_id=application.pk, approve=True)
    draft.refresh_from_db()
    assert draft.status == Facility.Status.ACTIVE


@pytest.mark.django_db
def test_location_edit_requires_reverification_and_rejection_keeps_it_there(
    draft: Facility, user: User, operator: Any
) -> None:
    decide_application(request=operator, application_id=_submit(draft, user).pk, approve=True)
    update_facility_location(actor=user, facility=draft, latitude=35.9, longitude=39.0)
    draft.refresh_from_db()
    assert draft.status == Facility.Status.REVERIFICATION_REQUIRED
    application = _submit(draft, user)
    decide_application(request=operator, application_id=application.pk, approve=False, reason="x")
    draft.refresh_from_db()
    # A previously live facility is not demoted to a never-published draft.
    assert draft.status == Facility.Status.REVERIFICATION_REQUIRED


@pytest.mark.django_db
def test_suspend_reactivate_close(draft: Facility, user: User, operator: Any) -> None:
    decide_application(request=operator, application_id=_submit(draft, user).pk, approve=True)
    transition_facility(
        request=operator, facility_id=draft.pk, target_status="SUSPENDED", reason="r"
    )
    draft.refresh_from_db()
    assert draft.status == Facility.Status.SUSPENDED
    with pytest.raises(ValidationError):
        transition_facility(request=operator, facility_id=draft.pk, target_status="SUSPENDED")
    transition_facility(request=operator, facility_id=draft.pk, target_status="ACTIVE")
    transition_facility(request=operator, facility_id=draft.pk, target_status="CLOSED")
    draft.refresh_from_db()
    assert draft.status == Facility.Status.CLOSED
    with pytest.raises(ValidationError):
        transition_facility(request=operator, facility_id=draft.pk, target_status="ACTIVE")
    assert AuditEvent.objects.filter(action="facility.suspended").exists()


@pytest.mark.django_db
def test_suspended_facility_cannot_self_reactivate(
    draft: Facility, user: User, operator: Any
) -> None:
    decide_application(request=operator, application_id=_submit(draft, user).pk, approve=True)
    transition_facility(request=operator, facility_id=draft.pk, target_status="SUSPENDED")
    # Editing keeps it suspended...
    update_facility_core(actor=user, facility=draft, data={"descriptionAr": "وصف"})
    draft.refresh_from_db()
    assert draft.status == Facility.Status.SUSPENDED
    # ...and resubmitting is refused, so an approval can never undo the suspension.
    with pytest.raises(ValidationError):
        _submit(draft, user)


@pytest.mark.django_db
def test_submit_requires_location(draft: Facility, user: User) -> None:
    draft.location = None
    draft.save()
    with pytest.raises(ValidationError):
        _submit(draft, user)


@pytest.mark.django_db
def test_record_audit_redacts_secrets_in_any_spelling(facility: Facility, user: User) -> None:
    event = record_audit(
        actor=user,
        action="test.redaction",
        target=facility,
        metadata={
            "storageKey": "private/evidence/1.jpg",
            "storage_key": "private/evidence/2.jpg",
            "accessToken": "a",
            "nested": {"refresh-token": "r", "Password": "p", "keep": "ok"},
            "items": [{"otpCode": "123456"}],
        },
        before_snapshot={"secretValue": "s"},
        after_snapshot={"nameAr": "visible"},
    )
    stored = AuditEvent.objects.get(pk=event.pk)
    assert stored.metadata["storageKey"] == "[REDACTED]"
    assert stored.metadata["storage_key"] == "[REDACTED]"
    assert stored.metadata["accessToken"] == "[REDACTED]"
    assert stored.metadata["nested"] == {
        "refresh-token": "[REDACTED]",
        "Password": "[REDACTED]",
        "keep": "ok",
    }
    assert stored.metadata["items"] == [{"otpCode": "[REDACTED]"}]
    assert stored.before_snapshot == {"secretValue": "[REDACTED]"}
    assert stored.after_snapshot == {"nameAr": "visible"}
