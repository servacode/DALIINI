"""Categories and provinces as their cards in the console show them (DECISION-113).

The window used to open with every capability unticked and no province chosen, whatever the
category actually had, so saving it unseen could switch things off. The list row now carries
the flags and the switches as they are, and how many facilities the category holds.
"""

from typing import Any

import pytest
from django.db import connection
from django.test.utils import CaptureQueriesContext

from directory.models import Category, CategoryProvince
from facilities.models import Facility

READ = "admin.taxonomy.read"


def _row(client: Any, category: Category) -> dict[str, Any]:
    items = client.get("/api/v1/admin/categories/").json()["items"]
    return next(item for item in items if item["id"] == str(category.pk))


@pytest.mark.django_db
def test_a_row_carries_its_flags_switches_and_count(admin_api: Any, facility: Facility) -> None:
    CategoryProvince.objects.create(
        category=facility.category,
        province=facility.province,
        public_enabled=True,
        owner_registration_enabled=False,
    )
    client = admin_api(READ)

    row = _row(client, facility.category)

    assert row["capabilities"]["supportsDuty"] is True
    assert row["capabilities"]["supportsSpecialtyFilter"] is False
    assert row["switches"] == [
        {
            "provinceId": str(facility.province_id),
            "provinceNameAr": "الرقة",
            "publicEnabled": True,
            "ownerRegistrationEnabled": False,
        }
    ]
    assert row["facilityCount"] == 1


@pytest.mark.django_db
def test_a_category_without_flags_reads_as_all_off(admin_api: Any, facility: Facility) -> None:
    bare = Category.objects.create(
        group=facility.category.group, code="bare-test", slug="bare-test", name_ar="بلا قدرات"
    )
    client = admin_api(READ)

    row = _row(client, bare)

    assert set(row["capabilities"].values()) == {False}
    assert row["switches"] == []
    assert row["facilityCount"] == 0


@pytest.mark.django_db
def test_the_list_costs_the_same_however_many_categories(
    admin_api: Any, facility: Facility
) -> None:
    client = admin_api(READ)
    client.get("/api/v1/admin/categories/")
    with CaptureQueriesContext(connection) as one:
        client.get("/api/v1/admin/categories/")
    for n in range(4):
        category = Category.objects.create(
            group=facility.category.group, code=f"more-{n}", slug=f"more-{n}", name_ar=f"ت{n}"
        )
        CategoryProvince.objects.create(category=category, province=facility.province)
    with CaptureQueriesContext(connection) as five:
        client.get("/api/v1/admin/categories/")

    assert len(five) == len(one)


@pytest.mark.django_db
def test_a_province_card_counts_its_active_facilities_and_cities(
    admin_api: Any, facility: Facility
) -> None:
    from locations.models import City

    City.objects.create(province=facility.province, code="c-on", name_ar="مدينة", active=True)
    City.objects.create(province=facility.province, code="c-off", name_ar="أخرى", active=False)
    Facility.objects.create(
        category=facility.category,
        province=facility.province,
        name_ar="مسودة",
        status=Facility.Status.DRAFT,
    )
    client = admin_api("admin.provinces.read")

    items = client.get("/api/v1/admin/provinces/").json()["items"]
    row = next(item for item in items if item["id"] == str(facility.province_id))

    assert row["activeFacilityCount"] == 1
    assert row["cityCount"] == 2
    assert row["activeCityCount"] == 1
