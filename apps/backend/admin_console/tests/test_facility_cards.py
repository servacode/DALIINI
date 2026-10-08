"""A facility as its card in the console shows it (DECISION-109).

The card holds the whole facility and never fetches anything else, so the list row carries how
to reach the place, where it is and what it looks like — and carrying it must not cost a query
per card.
"""

from typing import Any

import pytest
from django.db import connection
from django.test.utils import CaptureQueriesContext

from facilities.models import Facility, FacilityImage
from locations.models import City

READ = "admin.facilities.read"


def _row(client: Any, facility: Facility) -> dict[str, Any]:
    items = client.get("/api/v1/admin/facilities/").json()["items"]
    return next(item for item in items if item["id"] == str(facility.pk))


@pytest.mark.django_db
def test_a_row_carries_what_the_card_shows(admin_api: Any, facility: Facility) -> None:
    city = City.objects.create(province=facility.province, code="t-city", name_ar="مدينة الاختبار")
    facility.city = city
    facility.phone = "0933000111"
    facility.whatsapp = "0933000222"
    facility.address_ar = "شارع الاختبار"
    facility.save()
    for key, order in (("facilities/f/second.jpg", 1), ("facilities/f/first.jpg", 0)):
        FacilityImage.objects.create(facility=facility, storage_key=key, sort_order=order)
    client = admin_api(READ)

    row = _row(client, facility)

    assert row["phone"] == "0933000111"
    assert row["whatsapp"] == "0933000222"
    assert row["addressAr"] == "شارع الاختبار"
    assert row["cityNameAr"] == "مدينة الاختبار"
    assert row["imageUrl"].endswith("facilities/f/first.jpg")
    assert row["createdAt"]
    assert "categoryIconKey" in row


@pytest.mark.django_db
def test_a_facility_with_nothing_to_show_says_so_rather_than_failing(
    admin_api: Any, facility: Facility
) -> None:
    client = admin_api(READ)

    row = _row(client, facility)

    assert row["phone"] is None
    assert row["cityNameAr"] is None
    assert row["imageUrl"] is None


@pytest.mark.django_db
def test_the_cards_cost_the_same_number_of_queries_however_many(
    admin_api: Any, facility: Facility
) -> None:
    client = admin_api(READ)

    def load(more: int) -> int:
        for index in range(more):
            extra = Facility.objects.create(
                category=facility.category,
                province=facility.province,
                name_ar=f"منشأة {index} {Facility.objects.count()}",
                status=Facility.Status.ACTIVE,
            )
            FacilityImage.objects.create(
                facility=extra, storage_key=f"facilities/{extra.pk}/a.jpg", sort_order=0
            )
        with CaptureQueriesContext(connection) as captured:
            assert client.get("/api/v1/admin/facilities/").status_code == 200
        return len(captured)

    one = load(1)
    many = load(9)

    assert many == one, f"{one} queries for 2 facilities, {many} for 11"


@pytest.mark.django_db
def test_a_link_to_one_facility_lists_that_facility_alone(
    admin_api: Any, facility: Facility
) -> None:
    Facility.objects.create(
        category=facility.category,
        province=facility.province,
        name_ar="منشأة أخرى",
        status=Facility.Status.ACTIVE,
    )
    client = admin_api(READ)

    items = client.get(f"/api/v1/admin/facilities/?id={facility.pk}").json()["items"]

    assert [item["id"] for item in items] == [str(facility.pk)]
    assert client.get("/api/v1/admin/facilities/?id=not-an-id").status_code == 400
