"""A verification requirement's id is the model's integer, on the wire and in the contract.

INT-068.

The owner and review contracts declared it a UUID while the model keys it with an integer.
The launch baseline configures no requirement, so nothing noticed until the first one was:
the generated Kotlin client could not decode the owner configuration at all, and the upload
serializer refused the only id an owner could send, so no evidence could ever be uploaded.
"""

import io
from typing import Any

import pytest
import yaml
from django.core.files.uploadedfile import SimpleUploadedFile
from django.core.management import call_command
from PIL import Image
from rest_framework.test import APIClient

from accounts.models import AdminPermission, AdminRole, User, UserAdminRole
from directory.models import CategoryProvince, VerificationRequirement
from facilities.models import Facility, FacilityApplication, FacilityMembership


class _Storage:
    def delete(self, name: str) -> None:
        pass


def _jpeg() -> SimpleUploadedFile:
    buffer = io.BytesIO()
    Image.new("RGB", (40, 30)).save(buffer, format="JPEG")
    return SimpleUploadedFile("licence.jpg", buffer.getvalue(), content_type="image/jpeg")


@pytest.fixture
def requirement(facility: Facility) -> VerificationRequirement:
    CategoryProvince.objects.create(
        province=facility.province,
        category=facility.category,
        public_enabled=True,
        owner_registration_enabled=True,
    )
    return VerificationRequirement.objects.create(
        category=facility.category, label_ar="ترخيص", required=True, min_files=1, max_files=2
    )


@pytest.fixture
def owner(facility: Facility, user: User, monkeypatch: pytest.MonkeyPatch) -> APIClient:
    monkeypatch.setattr(
        "facilities.views.save_private_evidence",
        lambda **kwargs: (_Storage(), f"facilities/{kwargs['facility_id']}/evidence/x.jpg", 40, 30),
    )
    FacilityMembership.objects.create(
        facility=facility, user=user, role=FacilityMembership.Role.OWNER
    )
    client = APIClient()
    client.force_authenticate(user=user)
    return client


@pytest.mark.django_db
def test_the_owner_receives_the_integer_id_and_uploads_evidence_with_it(
    facility: Facility, requirement: VerificationRequirement, owner: APIClient
) -> None:
    config = owner.get("/api/v1/owner/config/", {"provinceId": str(facility.province_id)})
    served = config.json()["categories"][0]["verificationRequirements"][0]
    assert served["id"] == requirement.pk

    upload: Any = owner.post(
        f"/api/v1/owner/facilities/{facility.pk}/evidence/",
        {"requirementId": served["id"], "file": _jpeg()},
        format="multipart",
    )
    assert upload.status_code == 201, upload.json()
    assert upload.json()["requirementId"] == requirement.pk

    detail = owner.get(f"/api/v1/owner/facilities/{facility.pk}/").json()
    assert [item["requirementId"] for item in detail["evidence"]] == [requirement.pk]


@pytest.mark.django_db
def test_an_unknown_requirement_is_not_found(
    facility: Facility, requirement: VerificationRequirement, owner: APIClient
) -> None:
    upload = owner.post(
        f"/api/v1/owner/facilities/{facility.pk}/evidence/",
        {"requirementId": requirement.pk + 1000, "file": _jpeg()},
        format="multipart",
    )

    assert upload.status_code == 404


@pytest.mark.django_db
def test_the_reviewer_receives_the_same_integer_id(
    facility: Facility, requirement: VerificationRequirement, owner: APIClient
) -> None:
    owner.post(
        f"/api/v1/owner/facilities/{facility.pk}/evidence/",
        {"requirementId": requirement.pk, "file": _jpeg()},
        format="multipart",
    )
    application = FacilityApplication.objects.create(
        facility=facility, status=FacilityApplication.Status.SUBMITTED
    )
    reviewer = User.objects.create_user(phone="+963900000002", password="x", name="r")
    role = AdminRole.objects.create(code="reviewer-ids", name="r")
    role.permissions.add(AdminPermission.objects.get_or_create(code="admin.reviews.read")[0])
    UserAdminRole.objects.create(user=reviewer, role=role, active=True)
    client = APIClient()
    client.force_authenticate(user=reviewer)

    review = client.get(f"/api/v1/admin/applications/{application.pk}/").json()

    assert [item["requirementId"] for item in review["evidence"]] == [requirement.pk]


def test_the_contract_types_every_requirement_id_as_the_model_does() -> None:
    buffer = io.StringIO()
    call_command("spectacular", "--format", "openapi", stdout=buffer)
    components = yaml.safe_load(buffer.getvalue())["components"]["schemas"]

    declared = {
        f"{name}.id": components[name]["properties"]["id"]
        for name in ("OwnerVerificationRequirement", "AdminVerificationRequirement")
    }
    for name, schema in components.items():
        if "requirementId" in schema.get("properties", {}):
            declared[f"{name}.requirementId"] = schema["properties"]["requirementId"]

    assert len(declared) >= 6, sorted(declared)
    assert {name: value["type"] for name, value in declared.items()} == {
        name: "integer" for name in declared
    }
