"""The public discovery responses match the contract the mobile clients were generated from.

All three faults here were found by the Android connected suite, which decodes these responses
with the generated Kotlin client rather than reading them as loose JSON: a nearest-first list
failed on its second page (INT-058), a facility with opening hours could not be decoded at all
(INT-059), and every public image pointed at a route that does not exist (INT-060).
"""

from datetime import time
from typing import Any

import pytest
from django.contrib.gis.geos import Point
from rest_framework.test import APIClient

from business_hours.models import BusinessHour
from directory.models import CategoryProvince
from facilities.models import Facility, FacilityImage

LIST = "/api/v1/public/facilities/"


def listed(facility: Facility) -> None:
    CategoryProvince.objects.get_or_create(
        province=facility.province,
        category=facility.category,
        defaults={"public_enabled": True, "owner_registration_enabled": False},
    )


def neighbours(facility: Facility, count: int) -> list[Facility]:
    """`count` public facilities north of `facility`, each a little further away."""
    rows = []
    for index in range(count):
        rows.append(
            Facility.objects.create(
                category=facility.category,
                province=facility.province,
                name_ar=f"جار {index}",
                status=Facility.Status.ACTIVE,
                location=Point(39.0, 35.95 + 0.001 * (index + 1), srid=4326),
            )
        )
    return rows


@pytest.mark.django_db
def test_a_nearest_first_list_pages_to_the_end_in_distance_order(facility: Facility) -> None:
    listed(facility)
    facility.location = Point(39.0, 35.95, srid=4326)
    facility.save()
    neighbours(facility, 4)
    client = APIClient()
    params: dict[str, Any] = {
        "provinceId": str(facility.province_id),
        "categoryId": str(facility.category_id),
        "latitude": "35.95",
        "longitude": "39.0",
        "limit": 2,
    }

    seen: list[float] = []
    ids: list[str] = []
    cursor = None
    for _ in range(5):
        response = client.get(LIST, {**params, **({"cursor": cursor} if cursor else {})})
        assert response.status_code == 200, response.content
        body = response.json()
        seen += [row["distanceMeters"] for row in body["items"]]
        ids += [row["id"] for row in body["items"]]
        cursor = body["nextCursor"]
        if not cursor:
            break

    assert len(ids) == 5
    assert len(set(ids)) == 5
    assert seen == sorted(seen)
    assert seen[0] == 0.0


@pytest.mark.django_db
def test_detail_hours_carry_the_id_and_sequence_the_contract_requires(facility: Facility) -> None:
    listed(facility)
    BusinessHour.objects.create(
        facility=facility, weekday=0, opens_at=time(16), closes_at=time(22), sort_order=1
    )
    BusinessHour.objects.create(
        facility=facility, weekday=0, opens_at=time(8), closes_at=time(12), sort_order=0
    )

    hours = APIClient().get(f"/api/v1/public/facilities/{facility.pk}/").json()["hours"]

    assert [set(row) for row in hours] == [{"id", "weekday", "opensAt", "closesAt", "sequence"}] * 2
    assert [(row["weekday"], row["sequence"], row["opensAt"]) for row in hours] == [
        (0, 0, "08:00:00"),
        (0, 1, "16:00:00"),
    ]


@pytest.mark.django_db
def test_detail_images_point_at_storage_not_at_a_missing_route(facility: Facility) -> None:
    listed(facility)
    FacilityImage.objects.create(
        facility=facility, storage_key="facilities/x/public/a.jpg", width=10, height=10
    )

    images = APIClient().get(f"/api/v1/public/facilities/{facility.pk}/").json()["images"]

    assert len(images) == 1
    assert "/api/v1/public/media/" not in images[0]["url"]
    assert "facilities/x/public/a.jpg" in images[0]["url"]
