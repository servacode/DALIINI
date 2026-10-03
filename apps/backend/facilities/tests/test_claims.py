"""«هذه منشأتي»: a pharmacist takes over a facility the directory listed without an owner."""

import io
from typing import Any

import pytest
from django.core.files.uploadedfile import SimpleUploadedFile
from PIL import Image
from rest_framework.test import APIClient

from accounts.models import User
from directory.models import CategoryProvince, VerificationRequirement
from facilities.models import (
    Facility,
    FacilityApplication,
    FacilityMembership,
    VerificationEvidence,
)
from notifications.models import Notification

CLAIMS = "/api/v1/owner/claims/"


class _Storage:
    deleted: list[str] = []

    def delete(self, name: str) -> None:
        _Storage.deleted.append(name)


def _jpeg() -> SimpleUploadedFile:
    buffer = io.BytesIO()
    Image.new("RGB", (40, 30)).save(buffer, format="JPEG")
    return SimpleUploadedFile("licence.jpg", buffer.getvalue(), content_type="image/jpeg")


def _client(user: User) -> APIClient:
    client = APIClient()
    client.force_authenticate(user=user)
    return client


@pytest.fixture
def requirement(facility: Facility, monkeypatch: pytest.MonkeyPatch) -> VerificationRequirement:
    CategoryProvince.objects.create(
        province=facility.province,
        category=facility.category,
        public_enabled=True,
        owner_registration_enabled=True,
    )
    counter = {"n": 0}

    def save(**kwargs: Any) -> tuple[_Storage, str, int, int]:
        counter["n"] += 1
        return _Storage(), f"facilities/{kwargs['facility_id']}/evidence/{counter['n']}.jpg", 40, 30

    monkeypatch.setattr("facilities.media.save_private_evidence", save)
    monkeypatch.setattr("facilities.claims.PrivateS3Storage", _Storage)
    _Storage.deleted = []
    return VerificationRequirement.objects.create(
        category=facility.category, label_ar="ترخيص", required=True, min_files=1, max_files=2
    )


@pytest.fixture
def pharmacist(user: User) -> APIClient:
    return _client(user)


def _ready_claim(
    client: APIClient, facility: Facility, requirement: VerificationRequirement
) -> str:
    claim = client.post(CLAIMS, {"facilityId": str(facility.pk)}, format="json").json()
    uploaded = client.post(
        f"{CLAIMS}{claim['id']}/evidence/",
        {"requirementId": requirement.pk, "file": _jpeg()},
        format="multipart",
    )
    assert uploaded.status_code == 201, uploaded.content
    submitted = client.post(f"{CLAIMS}{claim['id']}/submit/")
    assert submitted.status_code == 200, submitted.content
    return str(claim["id"])


@pytest.mark.django_db
def test_a_facility_nobody_owns_can_be_found_and_one_with_an_owner_cannot(
    pharmacist: APIClient, facility: Facility, requirement: VerificationRequirement
) -> None:
    found = pharmacist.get("/api/v1/owner/claimable-facilities/", {"q": "صيدليه اختبار"})
    assert [item["id"] for item in found.json()["items"]] == [str(facility.pk)]

    owner = User.objects.create_user(phone="+963944111000", password="x" * 12, name="مالك")
    FacilityMembership.objects.create(facility=facility, user=owner, role="OWNER")

    gone = pharmacist.get("/api/v1/owner/claimable-facilities/", {"q": "صيدلية"})
    refused = pharmacist.post(CLAIMS, {"facilityId": str(facility.pk)}, format="json")
    assert gone.json()["items"] == []
    assert refused.status_code == 409
    assert refused.json()["code"] == "FACILITY_ALREADY_OWNED"
    assert pharmacist.get("/api/v1/owner/claimable-facilities/", {"q": "ص"}).status_code == 400


@pytest.mark.django_db
def test_a_claim_needs_its_own_documents(
    pharmacist: APIClient, facility: Facility, requirement: VerificationRequirement
) -> None:
    claim = pharmacist.post(CLAIMS, {"facilityId": str(facility.pk)}, format="json").json()

    assert claim["requirements"][0]["labelAr"] == "ترخيص"
    early = pharmacist.post(f"{CLAIMS}{claim['id']}/submit/")
    assert early.status_code == 400
    # Starting again returns the same open claim.
    again = pharmacist.post(CLAIMS, {"facilityId": str(facility.pk)}, format="json").json()
    assert again["id"] == claim["id"]


@pytest.mark.django_db
def test_an_approved_claim_makes_the_claimant_the_owner_and_the_papers_the_facilitys(
    pharmacist: APIClient,
    facility: Facility,
    requirement: VerificationRequirement,
    admin_api: Any,
    user: User,
) -> None:
    claim_id = _ready_claim(pharmacist, facility, requirement)
    operator = admin_api("admin.reviews.read", "admin.reviews.decide")

    review = operator.get(f"/api/v1/admin/applications/{claim_id}/").json()
    assert review["kind"] == "CLAIM"
    assert review["applicantName"] == user.name
    assert review["ownerName"] is None
    assert len(review["evidence"]) == 1

    decided = operator.post(f"/api/v1/admin/applications/{claim_id}/approve/", {}, format="json")

    assert decided.status_code == 200, decided.content
    assert FacilityMembership.objects.filter(facility=facility, user=user, role="OWNER").exists()
    facility.refresh_from_db()
    assert facility.status == Facility.Status.ACTIVE
    assert (
        VerificationEvidence.objects.filter(facility=facility, application__isnull=True).count()
        == 1
    )
    assert Notification.objects.filter(user=user, type="facility.claim.approved").exists()
    # It is the claimant's facility now, with its documents.
    detail = pharmacist.get(f"/api/v1/owner/facilities/{facility.pk}/").json()
    assert len(detail["evidence"]) == 1


@pytest.mark.django_db
def test_a_rejected_claim_deletes_the_papers_and_changes_nothing(
    pharmacist: APIClient,
    facility: Facility,
    requirement: VerificationRequirement,
    admin_api: Any,
    user: User,
    django_capture_on_commit_callbacks: Any,
) -> None:
    claim_id = _ready_claim(pharmacist, facility, requirement)
    operator = admin_api("admin.reviews.read", "admin.reviews.decide")

    with django_capture_on_commit_callbacks(execute=True):
        decided = operator.post(
            f"/api/v1/admin/applications/{claim_id}/reject/",
            {"reason": "الترخيص باسم شخص آخر"},
            format="json",
        )

    assert decided.status_code == 200
    assert not FacilityMembership.objects.filter(facility=facility).exists()
    assert not VerificationEvidence.objects.filter(facility=facility).exists()
    assert len(_Storage.deleted) == 1
    mine = pharmacist.get(CLAIMS).json()["items"][0]
    assert mine["status"] == "REJECTED"
    assert mine["rejectionReason"] == "الترخيص باسم شخص آخر"


@pytest.mark.django_db
def test_one_claim_is_reviewed_at_a_time_and_nobody_reads_anothers(
    pharmacist: APIClient, facility: Facility, requirement: VerificationRequirement
) -> None:
    claim_id = _ready_claim(pharmacist, facility, requirement)
    rival = _client(User.objects.create_user(phone="+963944222000", password="x" * 12, name="آخر"))
    second = rival.post(CLAIMS, {"facilityId": str(facility.pk)}, format="json").json()
    rival.post(
        f"{CLAIMS}{second['id']}/evidence/",
        {"requirementId": requirement.pk, "file": _jpeg()},
        format="multipart",
    )

    blocked = rival.post(f"{CLAIMS}{second['id']}/submit/")

    assert blocked.status_code == 409
    assert blocked.json()["code"] == "CLAIM_PENDING"
    assert rival.get(f"{CLAIMS}{claim_id}/").status_code == 404
    assert rival.delete(f"{CLAIMS}{claim_id}/").status_code == 404
    # The rival's papers are not the facility's, and an owner would not see them either.
    assert not VerificationEvidence.objects.filter(
        facility=facility, application__isnull=True
    ).exists()


@pytest.mark.django_db
def test_withdrawing_deletes_the_claim_and_its_papers(
    pharmacist: APIClient,
    facility: Facility,
    requirement: VerificationRequirement,
    django_capture_on_commit_callbacks: Any,
) -> None:
    claim_id = _ready_claim(pharmacist, facility, requirement)

    with django_capture_on_commit_callbacks(execute=True):
        response = pharmacist.delete(f"{CLAIMS}{claim_id}/")

    assert response.status_code == 204
    assert not FacilityApplication.objects.filter(pk=claim_id).exists()
    assert not VerificationEvidence.objects.exists()
    assert len(_Storage.deleted) == 1


@pytest.mark.django_db
def test_the_task_board_counts_claims(
    pharmacist: APIClient, facility: Facility, requirement: VerificationRequirement, admin_api: Any
) -> None:
    _ready_claim(pharmacist, facility, requirement)

    tasks = admin_api("admin.dashboard.read").get("/api/v1/admin/tasks/").json()

    assert tasks["applications"]["claim"]["count"] == 1
