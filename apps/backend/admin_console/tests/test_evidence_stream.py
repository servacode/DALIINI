"""An authorised operator receives the evidence bytes, typed, uncached and audited (INT-066)."""

import io
from typing import Any

import pytest
from rest_framework.test import APIClient

from accounts.models import AdminPermission, AdminRole, User, UserAdminRole
from audit.models import AuditEvent
from directory.models import VerificationRequirement
from facilities.models import Facility, VerificationEvidence

JPEG = b"\xff\xd8\xff\xe0" + b"evidence-bytes" + b"\xff\xd9"
KEY = "facilities/f/evidence/r/9f2c.jpg"


class _Storage:
    """Stands in for the private bucket; returns the bytes stored under KEY."""

    def open(self, name: str, mode: str = "rb") -> io.BytesIO:
        assert name == KEY
        stream = io.BytesIO(JPEG)
        # As an S3 file does: its name is the object key.
        stream.name = KEY
        return stream


@pytest.fixture
def evidence(facility: Facility, monkeypatch: pytest.MonkeyPatch) -> VerificationEvidence:
    monkeypatch.setattr("admin_console.views_reviews.PrivateS3Storage", _Storage)
    requirement = VerificationRequirement.objects.create(
        category=facility.category, label_ar="ترخيص", required=True
    )
    return VerificationEvidence.objects.create(
        facility=facility, requirement=requirement, storage_key=KEY
    )


def operator_with(user: User, *codes: str) -> APIClient:
    role = AdminRole.objects.create(code="evidence-" + "-".join(codes), name="r")
    for code in codes:
        role.permissions.add(AdminPermission.objects.get_or_create(code=code)[0])
    UserAdminRole.objects.create(user=user, role=role, active=True)
    client = APIClient()
    client.force_authenticate(user=user)
    return client


@pytest.mark.django_db
def test_an_authorised_operator_receives_the_bytes_typed_uncached_and_audited(
    evidence: VerificationEvidence, user: User
) -> None:
    client = operator_with(user, "admin.evidence.read")

    response: Any = client.get(f"/api/v1/admin/evidence/{evidence.pk}/content/")

    assert response.status_code == 200
    assert b"".join(response.streaming_content) == JPEG
    assert response["Content-Type"] == "image/jpeg"
    assert response["Cache-Control"] == "private, no-store"
    assert response["X-Content-Type-Options"] == "nosniff"
    # Not the key, nor any part of it: not even the object's file name.
    assert KEY.rsplit("/", 1)[-1] not in str(response.headers)
    audit = AuditEvent.objects.get(action="verification_evidence.viewed")
    assert str(audit.target_id) == str(evidence.pk)


@pytest.mark.django_db
def test_an_operator_without_the_permission_gets_nothing_and_nothing_is_audited(
    evidence: VerificationEvidence, user: User
) -> None:
    client = operator_with(user, "admin.reviews.read")

    response = client.get(f"/api/v1/admin/evidence/{evidence.pk}/content/")

    assert response.status_code == 403
    assert not AuditEvent.objects.filter(action="verification_evidence.viewed").exists()
