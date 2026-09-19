"""Owner facility responses carry their category's capabilities (INT-056).

Without them the owner app had to look capabilities up in the province's owner
configuration, which lists only categories currently open for onboarding, so a facility in a
closed category could not be told apart from one that does not support a control.
"""

import pytest
from rest_framework.test import APIClient

from accounts.models import User
from directory.models import CategoryProvince
from facilities.models import Facility, FacilityMembership

FLAGS = {
    "hours",
    "photos",
    "ratings",
    "duty",
    "specialtyFilter",
    "serviceFilter",
    "temporaryClosure",
    "ownerOnboarding",
}


def owned_by(facility: Facility, user: User) -> APIClient:
    FacilityMembership.objects.create(
        facility=facility, user=user, role=FacilityMembership.Role.OWNER
    )
    client = APIClient()
    client.force_authenticate(user=user)
    return client


@pytest.mark.django_db
def test_list_and_detail_carry_the_category_capabilities(facility: Facility, user: User) -> None:
    caps = facility.category.capabilities
    caps.supports_duty = True
    caps.supports_temporary_closure = False
    caps.save()
    client = owned_by(facility, user)

    listed = client.get("/api/v1/owner/facilities/").json()["items"][0]["capabilities"]
    detail = client.get(f"/api/v1/owner/facilities/{facility.pk}/").json()["capabilities"]

    assert set(listed) == FLAGS
    assert listed == detail
    assert listed["duty"] is True
    assert listed["temporaryClosure"] is False


@pytest.mark.django_db
def test_a_category_closed_to_onboarding_still_reports_its_capabilities(
    facility: Facility, user: User
) -> None:
    CategoryProvince.objects.create(
        province=facility.province,
        category=facility.category,
        public_enabled=True,
        owner_registration_enabled=False,
    )
    client = owned_by(facility, user)

    config = client.get("/api/v1/owner/config/", {"provinceId": str(facility.province_id)}).json()
    detail = client.get(f"/api/v1/owner/facilities/{facility.pk}/").json()

    assert config["categories"] == []
    assert detail["capabilities"]["duty"] is True


@pytest.mark.django_db
def test_the_owner_configuration_uses_the_same_flags(facility: Facility, user: User) -> None:
    CategoryProvince.objects.create(
        province=facility.province,
        category=facility.category,
        public_enabled=False,
        owner_registration_enabled=True,
    )
    client = owned_by(facility, user)

    config = client.get("/api/v1/owner/config/", {"provinceId": str(facility.province_id)}).json()

    assert set(config["categories"][0]["capabilities"]) == FLAGS
