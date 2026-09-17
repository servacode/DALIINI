import pytest
from django.contrib.gis.geos import Point
from django.core.exceptions import ValidationError
from rest_framework.test import APIClient

from accounts.models import User
from directory.models import CategoryProvince
from facilities.models import FacilityMembership
from facilities.services import submit_facility, update_facility_core
from locations.models import City, Neighborhood


@pytest.mark.django_db
def test_owner_detail_is_membership_scoped(facility, user):
    other = User.objects.create_user(
        phone="+963900000002",
        password="StrongPass123!",
        name="Other User",
    )
    FacilityMembership.objects.create(
        facility=facility,
        user=other,
        role=FacilityMembership.Role.OWNER,
    )
    client = APIClient()
    client.force_authenticate(user=user)

    response = client.get(f"/api/v1/owner/facilities/{facility.pk}/")

    assert response.status_code == 404


@pytest.mark.django_db
def test_cannot_downgrade_last_owner(facility, user):
    FacilityMembership.objects.create(
        facility=facility,
        user=user,
        role=FacilityMembership.Role.OWNER,
    )
    client = APIClient()
    client.force_authenticate(user=user)

    response = client.post(
        f"/api/v1/owner/facilities/{facility.pk}/members/",
        {"userId": str(user.pk), "role": FacilityMembership.Role.MANAGER},
        format="json",
    )

    assert response.status_code == 409
    assert response.json()["error"]["code"] == "LAST_OWNER_PROTECTED"
    assert FacilityMembership.objects.get(facility=facility, user=user).role == "OWNER"


@pytest.mark.django_db
def test_clearing_city_clears_existing_neighborhood(facility, user):
    city = City.objects.create(
        province=facility.province,
        code="raqqa-city-test",
        name_ar="الرقة",
    )
    neighborhood = Neighborhood.objects.create(city=city, name_ar="حي اختبار")
    facility.city = city
    facility.neighborhood = neighborhood
    facility.save(update_fields=["city", "neighborhood"])

    updated = update_facility_core(
        actor=user,
        facility=facility,
        data={"cityId": None},
    )

    assert updated.city_id is None
    assert updated.neighborhood_id is None


@pytest.mark.django_db
def test_submit_rechecks_current_owner_registration_switch(facility, user):
    FacilityMembership.objects.create(
        facility=facility,
        user=user,
        role=FacilityMembership.Role.OWNER,
    )
    switch = CategoryProvince.objects.create(
        province=facility.province,
        category=facility.category,
        owner_registration_enabled=True,
    )
    facility.location = Point(39.01, 35.95, srid=4326)
    facility.save(update_fields=["location"])
    switch.owner_registration_enabled = False
    switch.save(update_fields=["owner_registration_enabled"])

    with pytest.raises(ValidationError):
        submit_facility(actor=user, facility=facility)
