"""What the local seed has to guarantee before Home can be reviewed on a device.

A section with nothing under it tells you nothing about the interface, and a section that leaks
another's facilities tells you something false. These are the checks that a reviewer would
otherwise have to make by hand, once per category, every time the data is rebuilt.
"""

import pytest
from django.core.management import call_command
from rest_framework.test import APIClient

from directory.models import Category, CategoryProvince
from facilities.models import Facility
from locations.models import Province

FACILITIES = "/api/v1/public/facilities/"
CATEGORIES = "/api/v1/public/provinces/{}/categories/"

#: The sections the product opens Raqqa with. Codes, not Arabic names: the name is the
#: product's to change, the code is the contract between the seed and the taxonomy.
SECTIONS = [
    "pharmacy",
    "medical-clinic",
    "medical-laboratory",
    "nursing-center",
    "medical-supplies",
]


@pytest.fixture
def seeded(db):
    call_command("seed_local_directory", "--allow-any-database")
    return Province.objects.get(code="raqqa")


@pytest.mark.django_db
def test_every_seeded_section_is_public_in_raqqa(seeded):
    body = APIClient().get(CATEGORIES.format(seeded.id)).json()

    offered = {item["nameAr"] for item in body["items"]}
    expected = set(
        Category.objects.filter(code__in=SECTIONS).values_list("name_ar", flat=True)
    )
    assert expected <= offered


@pytest.mark.django_db
def test_no_section_is_left_empty(seeded):
    client = APIClient()
    for code in SECTIONS:
        category = Category.objects.get(code=code)
        body = client.get(
            FACILITIES, {"provinceId": str(seeded.id), "categoryId": str(category.id)}
        ).json()

        assert len(body["items"]) >= 3, f"{category.name_ar} has fewer than three facilities"


@pytest.mark.django_db
def test_a_section_never_shows_another_section_facilities(seeded):
    client = APIClient()
    for code in SECTIONS:
        category = Category.objects.get(code=code)
        body = client.get(
            FACILITIES, {"provinceId": str(seeded.id), "categoryId": str(category.id)}
        ).json()

        assert {item["category"]["nameAr"] for item in body["items"]} == {category.name_ar}


@pytest.mark.django_db
def test_running_the_seed_twice_does_not_double_anything(seeded):
    before = Facility.objects.count()

    call_command("seed_local_directory", "--allow-any-database")

    assert Facility.objects.count() == before


@pytest.mark.django_db
def test_only_the_pharmacy_section_carries_a_duty_roster(seeded):
    """A duty filter offered where no roster exists would be a chip that does nothing."""
    body = APIClient().get(CATEGORIES.format(seeded.id)).json()

    duty = {i["nameAr"] for i in body["items"] if i["capabilities"]["duty"]}
    assert duty == {Category.objects.get(code="pharmacy").name_ar}


@pytest.mark.django_db
def test_the_pharmacies_cover_all_three_states_the_product_distinguishes(seeded):
    pharmacy = Category.objects.get(code="pharmacy")
    body = APIClient().get(
        FACILITIES, {"provinceId": str(seeded.id), "categoryId": str(pharmacy.id), "limit": "50"}
    ).json()

    states = {
        (i["availability"]["isOpenNow"], i["availability"]["isOnDutyToday"])
        for i in body["items"]
    }

    assert (True, True) in states, "no pharmacy is open and on today's roster"
    assert (False, True) in states, "no pharmacy is shut and on today's roster"
    assert (True, False) in states, "no pharmacy is open and off the roster"


@pytest.mark.django_db
def test_open_now_and_duty_today_narrow_the_seeded_pharmacies_differently(seeded):
    pharmacy = Category.objects.get(code="pharmacy")
    client = APIClient()

    def names(**params):
        body = client.get(
            FACILITIES,
            {"provinceId": str(seeded.id), "categoryId": str(pharmacy.id), "limit": "50", **params},
        ).json()
        return {i["nameAr"] for i in body["items"]}

    on_duty = names(dutyToday="true")
    both = names(openNow="true", dutyToday="true")

    assert both < on_duty, "being on the roster and being open are not telling anyone apart"


@pytest.mark.django_db
def test_a_section_without_a_roster_still_answers_open_now(seeded):
    lab = Category.objects.get(code="medical-laboratory")
    body = APIClient().get(
        FACILITIES,
        {"provinceId": str(seeded.id), "categoryId": str(lab.id), "openNow": "true"},
    ).json()

    assert 0 < len(body["items"]) < 3, "open-now should narrow the laboratories, not pass them all"


@pytest.mark.django_db
def test_every_seeded_facility_can_be_placed_on_a_map(seeded):
    """Without coordinates there is no distance, no nearest, and nothing to show on a map."""
    seeded_ids = CategoryProvince.objects.filter(
        province=seeded, public_enabled=True
    ).values_list("category_id", flat=True)

    placed = Facility.objects.filter(
        category_id__in=seeded_ids, status=Facility.Status.ACTIVE, location__isnull=False
    ).count()
    total = Facility.objects.filter(
        category_id__in=seeded_ids, status=Facility.Status.ACTIVE
    ).count()

    assert placed == total


@pytest.mark.django_db
def test_search_stays_inside_the_chosen_section(seeded):
    lab = Category.objects.get(code="medical-laboratory")
    body = APIClient().get(
        FACILITIES,
        {"provinceId": str(seeded.id), "categoryId": str(lab.id), "search": "صيدلية"},
    ).json()

    assert body["items"] == []
