import pytest

from directory.models import CategoryProvince
from facilities.models import Facility
from search.selectors import public_facilities


@pytest.mark.django_db
def test_public_facility_requires_public_category_switch(facility: Facility) -> None:
    CategoryProvince.objects.create(
        province=facility.province,
        category=facility.category,
        public_enabled=False,
        owner_registration_enabled=True,
    )
    assert not public_facilities().filter(pk=facility.pk).exists()


@pytest.mark.django_db
def test_owner_onboarding_does_not_imply_public_visibility(facility: Facility) -> None:
    switch = CategoryProvince.objects.create(
        province=facility.province,
        category=facility.category,
        public_enabled=False,
        owner_registration_enabled=True,
    )
    assert switch.owner_registration_enabled is True
    assert switch.public_enabled is False
