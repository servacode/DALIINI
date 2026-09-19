"""INT-092: a map opens on the province, not on the world, when the user's position is unknown.

The centre is reference data (`province_map_centers_v1`), written by migration 0005 and by
`seed_launch_baseline` through the same `apply_map_centers`, and served as `mapCenter` by the
two endpoints a map starts from: the public province list and the owner onboarding config.
"""

from io import StringIO

import pytest
from django.apps import apps as django_apps
from django.contrib.gis.geos import Point
from django.core.management import call_command
from django.core.management.base import CommandError
from rest_framework.test import APIClient

from accounts.models import User
from directory.reference_data import launch_v1, province_map_centers_v1
from directory.reference_data.apply import apply_map_centers
from locations.models import Province

RAQQA_LATITUDE, RAQQA_LONGITUDE = province_map_centers_v1.CENTERS["raqqa"]


def _seed(*args: str) -> str:
    out = StringIO()
    call_command("seed_launch_baseline", *args, stdout=out)
    return out.getvalue()


@pytest.mark.django_db
def test_the_migration_gives_raqqa_its_centre_and_no_other_province_one() -> None:
    raqqa = Province.objects.get(code="raqqa")

    assert raqqa.map_center is not None
    assert raqqa.map_center.srid == 4326
    assert (raqqa.map_center.y, raqqa.map_center.x) == (RAQQA_LATITUDE, RAQQA_LONGITUDE)
    others = Province.objects.filter(code__in=[code for code, *_ in launch_v1.PROVINCES])
    assert others.exclude(code="raqqa").filter(map_center__isnull=False).count() == 0


def test_the_raqqa_centre_lies_in_raqqa_city() -> None:
    """A swapped latitude and longitude would put the map in Iraq's desert; guard the order."""
    assert 35.9 < RAQQA_LATITUDE < 36.0
    assert 38.9 < RAQQA_LONGITUDE < 39.1


@pytest.mark.django_db
def test_the_seed_never_moves_a_centre_an_operator_set() -> None:
    raqqa = Province.objects.get(code="raqqa")
    raqqa.map_center = Point(39.02, 35.95, srid=4326)
    raqqa.save()

    output = _seed()

    raqqa.refresh_from_db()
    assert (raqqa.map_center.x, raqqa.map_center.y) == (39.02, 35.95)
    assert "Nothing to do" in output


@pytest.mark.django_db
def test_a_cleared_centre_is_reported_by_check_and_restored_by_the_seed() -> None:
    Province.objects.filter(code="raqqa").update(map_center=None)

    with pytest.raises(CommandError, match="reference rows are missing"):
        _seed("--check")
    assert Province.objects.get(code="raqqa").map_center is None

    _seed()

    restored = Province.objects.get(code="raqqa").map_center
    assert restored is not None
    assert (restored.y, restored.x) == (RAQQA_LATITUDE, RAQQA_LONGITUDE)


@pytest.mark.django_db
def test_a_centre_for_a_province_that_does_not_exist_is_skipped() -> None:
    class Dataset:
        VERSION = "test"
        CENTERS = {"atlantis": (1.0, 2.0)}

    summary = apply_map_centers(Dataset, django_apps.get_model)

    assert summary.created == {}
    assert not Province.objects.filter(code="atlantis").exists()


@pytest.mark.django_db
def test_public_provinces_serves_the_centre() -> None:
    body = APIClient().get("/api/v1/public/provinces/").json()

    raqqa = next(item for item in body["items"] if item["code"] == "raqqa")
    assert raqqa["mapCenter"] == {"latitude": RAQQA_LATITUDE, "longitude": RAQQA_LONGITUDE}


@pytest.mark.django_db
def test_a_province_without_a_centre_serves_null() -> None:
    Province.objects.filter(code="raqqa").update(map_center=None)

    body = APIClient().get("/api/v1/public/provinces/").json()

    raqqa = next(item for item in body["items"] if item["code"] == "raqqa")
    assert raqqa["mapCenter"] is None


@pytest.mark.django_db
def test_the_owner_config_serves_the_centre(user: User) -> None:
    client = APIClient()
    client.force_authenticate(user=user)
    raqqa = launch_v1.reference_id("province", "raqqa")

    body = client.get("/api/v1/owner/config/", {"provinceId": str(raqqa)}).json()

    assert body["province"] == {
        "id": str(raqqa),
        "nameAr": "الرقة",
        "mapCenter": {"latitude": RAQQA_LATITUDE, "longitude": RAQQA_LONGITUDE},
    }
