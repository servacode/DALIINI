"""An owner picks a facility's specialties and services by their integer ids.

The contract declared both lists as UUIDs while the rows are keyed by integers, so no real
request could set them. The ids an owner sends are the ones the owner configuration offers,
and the detail answers with the same kind of id.
"""

import pytest
from rest_framework.test import APIClient

from accounts.models import User
from directory.models import Category, CategoryGroup, ServiceTag, Specialty
from facilities.models import Facility, FacilityMembership, FacilityServiceTag, FacilitySpecialty
from facilities.services import application_snapshot


@pytest.fixture
def owner(facility: Facility, user: User) -> APIClient:
    FacilityMembership.objects.create(
        facility=facility, user=user, role=FacilityMembership.Role.OWNER
    )
    client = APIClient()
    client.force_authenticate(user=user)
    return client


def detail_url(facility: Facility) -> str:
    return f"/api/v1/owner/facilities/{facility.pk}/"


@pytest.mark.django_db
def test_an_owner_sets_specialties_and_services_by_integer_id(
    facility: Facility, owner: APIClient
) -> None:
    own = Specialty.objects.create(category=facility.category, name_ar="تركيب", sort_order=2)
    shared = Specialty.objects.create(specialization="PHARMACY", name_ar="سريرية", sort_order=1)
    service = ServiceTag.objects.create(category=facility.category, name_ar="قياس ضغط")

    response = owner.patch(
        detail_url(facility),
        {"specialtyIds": [own.pk, shared.pk], "serviceTagIds": [service.pk]},
        format="json",
    )

    assert response.status_code == 200, response.content
    body = response.json()
    assert body["specialtyIds"] == [shared.pk, own.pk]
    assert body["serviceTagIds"] == [service.pk]
    assert owner.get(detail_url(facility)).json()["specialtyIds"] == [shared.pk, own.pk]
    assert set(
        FacilitySpecialty.objects.filter(facility=facility).values_list("specialty_id", flat=True)
    ) == {own.pk, shared.pk}


@pytest.mark.django_db
def test_an_empty_list_clears_them(facility: Facility, owner: APIClient) -> None:
    service = ServiceTag.objects.create(category=facility.category, name_ar="قياس ضغط")
    FacilityServiceTag.objects.create(facility=facility, service_tag=service)

    response = owner.patch(detail_url(facility), {"serviceTagIds": []}, format="json")

    assert response.status_code == 200, response.content
    assert response.json()["serviceTagIds"] == []
    assert not FacilityServiceTag.objects.filter(facility=facility).exists()


@pytest.mark.django_db
@pytest.mark.parametrize(
    "value", [["00000000-0000-4000-8000-000000000000"], ["x"], [0], "3"]
)
def test_anything_but_a_list_of_ids_is_refused(
    facility: Facility, owner: APIClient, value: object
) -> None:
    response = owner.patch(detail_url(facility), {"specialtyIds": value}, format="json")

    assert response.status_code == 400, response.content
    assert "specialtyIds" in str(response.json()["details"])


@pytest.mark.django_db
def test_an_item_of_another_category_or_specialization_is_refused(
    facility: Facility, owner: APIClient
) -> None:
    group = CategoryGroup.objects.create(code="tags-other", name_ar="أخرى")
    other = Category.objects.create(group=group, code="tags-lab", slug="tags-lab", name_ar="مخابر")
    elsewhere = Specialty.objects.create(category=other, name_ar="دم")
    clinic_only = Specialty.objects.create(specialization="MEDICAL_CLINIC", name_ar="أطفال")
    foreign_service = ServiceTag.objects.create(category=other, name_ar="سحب دم")

    for body in (
        {"specialtyIds": [elsewhere.pk]},
        {"specialtyIds": [clinic_only.pk]},
        {"serviceTagIds": [foreign_service.pk]},
    ):
        response = owner.patch(detail_url(facility), body, format="json")
        assert response.status_code == 400, (body, response.content)
    assert not FacilitySpecialty.objects.filter(facility=facility).exists()


@pytest.mark.django_db
def test_a_retired_item_is_left_out_and_cannot_be_picked(
    facility: Facility, owner: APIClient
) -> None:
    kept = Specialty.objects.create(category=facility.category, name_ar="تركيب")
    retired = Specialty.objects.create(category=facility.category, name_ar="منسية")
    FacilitySpecialty.objects.create(facility=facility, specialty=kept)
    FacilitySpecialty.objects.create(facility=facility, specialty=retired)
    Specialty.objects.filter(pk=retired.pk).update(active=False)

    shown = owner.get(detail_url(facility)).json()["specialtyIds"]
    picked = owner.patch(detail_url(facility), {"specialtyIds": [retired.pk]}, format="json")

    assert shown == [kept.pk]
    assert picked.status_code == 400


@pytest.mark.django_db
def test_the_review_snapshot_lists_integer_ids(facility: Facility) -> None:
    specialty = Specialty.objects.create(category=facility.category, name_ar="تركيب")
    service = ServiceTag.objects.create(category=facility.category, name_ar="قياس ضغط")
    FacilitySpecialty.objects.create(facility=facility, specialty=specialty)
    FacilityServiceTag.objects.create(facility=facility, service_tag=service)

    snapshot = application_snapshot(facility)

    assert snapshot["specialtyIds"] == [specialty.pk]
    assert snapshot["serviceTagIds"] == [service.pk]
