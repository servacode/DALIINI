"""What the public facility list will and will not do.

A home screen asks for the whole province — everything open, or everyone on duty — while a
category page asks for one category. Both are the same list with the same ordering rules; the
province is what is always required, because every list in this product is scoped by one.
"""

import pytest
from directory.models import CategoryProvince
from facilities.models import Facility
from rest_framework.test import APIClient

FACILITIES = "/api/v1/public/facilities/"


@pytest.fixture
def listed(db, facility):
    CategoryProvince.objects.create(
        province=facility.province,
        category=facility.category,
        public_enabled=True,
        owner_registration_enabled=True,
    )
    return facility


@pytest.mark.django_db
def test_a_province_can_be_listed_without_a_category(listed):
    body = APIClient().get(FACILITIES, {"provinceId": str(listed.province_id)}).json()

    assert [item["id"] for item in body["items"]] == [str(listed.id)]


@pytest.mark.django_db
def test_a_category_still_narrows_the_same_list(listed):
    client = APIClient()

    inside = client.get(
        FACILITIES,
        {"provinceId": str(listed.province_id), "categoryId": str(listed.category_id)},
    ).json()
    elsewhere = client.get(
        FACILITIES,
        {
            "provinceId": str(listed.province_id),
            "categoryId": "00000000-0000-4000-8000-000000000000",
        },
    ).json()

    assert [item["id"] for item in inside["items"]] == [str(listed.id)]
    assert elsewhere["items"] == []


@pytest.mark.django_db
def test_the_province_is_still_required(listed):
    assert APIClient().get(FACILITIES).status_code == 400


@pytest.mark.django_db
def test_a_facility_that_is_not_active_is_not_listed(listed):
    listed.status = Facility.Status.SUSPENDED
    listed.save(update_fields=["status"])

    body = APIClient().get(FACILITIES, {"provinceId": str(listed.province_id)}).json()

    assert body["items"] == []
