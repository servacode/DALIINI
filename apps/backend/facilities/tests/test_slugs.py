"""Readable link words for a facility, from its Arabic name."""

import pytest
from rest_framework.test import APIClient

from directory.models import CategoryProvince
from facilities.models import Facility
from facilities.slugs import facility_slug


def test_the_slug_keeps_the_words_and_drops_the_rest() -> None:
    assert facility_slug("صيدلية الأمل (الفرع ٢)") == "صيدلية-الأمل-الفرع-٢"
    assert facility_slug("صَيْدَلِيّة  النور") == "صيدلية-النور"
    assert facility_slug("Al-Amal Pharmacy") == "al-amal-pharmacy"
    assert len(facility_slug("صيدلية " * 40)) <= 80


@pytest.mark.django_db
def test_public_facilities_carry_their_slug(facility: Facility) -> None:
    CategoryProvince.objects.create(
        province=facility.province, category=facility.category, public_enabled=True
    )

    listed = APIClient().get(
        "/api/v1/public/facilities/", {"provinceId": str(facility.province_id)}
    )
    detail = APIClient().get(f"/api/v1/public/facilities/{facility.pk}/")

    assert listed.json()["items"][0]["slug"] == "صيدلية-اختبار"
    assert detail.json()["slug"] == "صيدلية-اختبار"
