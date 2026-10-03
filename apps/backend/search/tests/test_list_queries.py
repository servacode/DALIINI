"""A page of facilities costs the same number of queries whatever it holds.

Each row used to ask the availability engine its state and next opening time on its own,
three or four queries a row: thirty pharmacies on Home were a hundred queries. The list query
now carries the flags and the hours are prefetched once for the page.
"""

from datetime import time

import pytest
from django.db import connection
from django.test.utils import CaptureQueriesContext
from rest_framework.test import APIClient

from business_hours.models import BusinessHour
from directory.models import CategoryProvince
from facilities.models import Facility

FACILITIES = "/api/v1/public/facilities/"


def _add(facility: Facility, count: int) -> None:
    for index in range(count):
        extra = Facility.objects.create(
            category=facility.category,
            province=facility.province,
            name_ar=f"صيدلية {index}",
            status=Facility.Status.ACTIVE,
        )
        for weekday in range(7):
            BusinessHour.objects.create(
                facility=extra, weekday=weekday, opens_at=time(9), closes_at=time(17)
            )


def _queries_for_page(facility: Facility) -> int:
    client = APIClient()
    with CaptureQueriesContext(connection) as captured:
        response = client.get(FACILITIES, {"provinceId": str(facility.province_id)})
    assert response.status_code == 200
    return len(captured.captured_queries)


@pytest.mark.django_db
def test_query_count_does_not_grow_with_the_page(facility: Facility) -> None:
    CategoryProvince.objects.create(
        province=facility.province,
        category=facility.category,
        public_enabled=True,
        owner_registration_enabled=True,
    )
    _add(facility, 3)
    # The first request also fills caches (maintenance mode and the like) that later ones read
    # from memory; it is not part of the comparison.
    _queries_for_page(facility)
    small = _queries_for_page(facility)
    _add(facility, 12)
    large = _queries_for_page(facility)

    assert large == small


@pytest.mark.django_db
def test_a_closed_row_still_says_when_it_opens(facility: Facility) -> None:
    CategoryProvince.objects.create(
        province=facility.province,
        category=facility.category,
        public_enabled=True,
        owner_registration_enabled=True,
    )
    # Open for one minute a day at a time nobody is testing: always closed, always reopening.
    for weekday in range(7):
        BusinessHour.objects.create(
            facility=facility, weekday=weekday, opens_at=time(3, 0), closes_at=time(3, 1)
        )

    body = APIClient().get(FACILITIES, {"provinceId": str(facility.province_id)}).json()
    row = next(item for item in body["items"] if item["id"] == str(facility.id))

    assert row["availability"]["nextOpenAt"] is not None or row["availability"]["state"] == "OPEN"
