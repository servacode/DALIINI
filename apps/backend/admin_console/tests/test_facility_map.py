"""The console's facility map: located facilities as points, with the list's filters."""

from typing import Any

import pytest
from django.contrib.gis.geos import Point

from facilities.models import Facility

MAP = "/api/v1/admin/facilities/map/"


@pytest.mark.django_db
def test_located_facilities_are_points_and_the_rest_are_counted(
    admin_api: Any, facility: Facility
) -> None:
    facility.location = Point(39.0106, 35.9528, srid=4326)
    facility.save(update_fields=["location"])
    Facility.objects.create(
        category=facility.category,
        province=facility.province,
        name_ar="صيدلية بلا موقع",
        status=Facility.Status.ACTIVE,
    )
    Facility.objects.create(
        category=facility.category,
        province=facility.province,
        name_ar="صيدلية موقوفة",
        status=Facility.Status.SUSPENDED,
        location=Point(39.02, 35.95, srid=4326),
    )
    client = admin_api("admin.facilities.read")

    body = client.get(MAP).json()
    assert body["truncated"] is False
    assert body["withoutLocation"] == 1
    points = {point["nameAr"]: point for point in body["items"]}
    assert set(points) == {"صيدلية اختبار", "صيدلية موقوفة"}
    assert points["صيدلية اختبار"]["latitude"] == pytest.approx(35.9528)
    assert points["صيدلية اختبار"]["longitude"] == pytest.approx(39.0106)
    assert set(points["صيدلية اختبار"]) == {
        "id",
        "nameAr",
        "status",
        "categoryId",
        "latitude",
        "longitude",
    }

    active = client.get(MAP, {"status": "ACTIVE"}).json()
    assert [point["nameAr"] for point in active["items"]] == ["صيدلية اختبار"]
    assert active["withoutLocation"] == 1


@pytest.mark.django_db
def test_the_map_needs_the_facility_read_permission(admin_api: Any) -> None:
    assert admin_api("admin.dashboard.read").get(MAP).status_code == 403
    assert admin_api("admin.facilities.read").get(MAP, {"issue": "NOPE"}).status_code == 400
