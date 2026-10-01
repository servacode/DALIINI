"""Specialties and services on the public surface: the detail, the filters and the search.

The ids are integers everywhere, as the rows are keyed. A filter matches only where the
category offers it, and only an item still in use; a value that is not a positive whole
number is a 400, where it used to reach the database and fail as a 500.
"""

from typing import Any

import pytest
from django.contrib.gis.geos import Point
from rest_framework.test import APIClient

from directory.models import CategoryCapabilities, CategoryProvince, ServiceTag, Specialty
from facilities.models import Facility, FacilityServiceTag, FacilitySpecialty

LIST = "/api/v1/public/facilities/"
SEARCH = "/api/v1/public/search/"
MAP = "/api/v1/public/map/facilities/"


@pytest.fixture
def tagged(facility: Facility) -> dict[str, Any]:
    """A public facility with a specialty and a service, and a neighbour with neither."""
    CategoryProvince.objects.create(
        province=facility.province, category=facility.category, public_enabled=True
    )
    CategoryCapabilities.objects.filter(category=facility.category).update(
        supports_specialty_filter=True, supports_service_filter=True
    )
    facility.location = Point(39.0, 35.95, srid=4326)
    facility.save()
    neighbour = Facility.objects.create(
        category=facility.category,
        province=facility.province,
        name_ar="صيدلية الجوار",
        status=Facility.Status.ACTIVE,
        location=Point(39.001, 35.951, srid=4326),
    )
    second = Specialty.objects.create(category=facility.category, name_ar="تركيب", sort_order=2)
    first = Specialty.objects.create(specialization="PHARMACY", name_ar="سريرية", sort_order=1)
    retired = Specialty.objects.create(category=facility.category, name_ar="منسية", active=False)
    service = ServiceTag.objects.create(category=facility.category, name_ar="قياس ضغط")
    gone = ServiceTag.objects.create(category=facility.category, name_ar="حقن", active=False)
    for specialty in (second, first, retired):
        FacilitySpecialty.objects.create(facility=facility, specialty=specialty)
    for tag in (service, gone):
        FacilityServiceTag.objects.create(facility=facility, service_tag=tag)
    return {
        "facility": facility,
        "neighbour": neighbour,
        "first": first,
        "second": second,
        "retired": retired,
        "service": service,
        "gone": gone,
    }


def ids(response: Any) -> set[str]:
    assert response.status_code == 200, response.content
    return {row["id"] for row in response.json()["items"]}


def scope(facility: Facility, **extra: Any) -> dict[str, Any]:
    return {
        "provinceId": str(facility.province_id),
        "categoryId": str(facility.category_id),
        **extra,
    }


# --------------------------------------------------------------------------------------
# Detail
# --------------------------------------------------------------------------------------


@pytest.mark.django_db
def test_the_detail_names_active_items_with_integer_ids_in_order(tagged: dict[str, Any]) -> None:
    facility = tagged["facility"]

    body = APIClient().get(f"/api/v1/public/facilities/{facility.pk}/").json()

    assert body["specialties"] == [
        {"id": tagged["first"].pk, "nameAr": "سريرية"},
        {"id": tagged["second"].pk, "nameAr": "تركيب"},
    ]
    assert body["services"] == [{"id": tagged["service"].pk, "nameAr": "قياس ضغط"}]


# --------------------------------------------------------------------------------------
# Filters
# --------------------------------------------------------------------------------------


@pytest.mark.django_db
def test_a_specialty_narrows_the_list(tagged: dict[str, Any]) -> None:
    facility = tagged["facility"]
    client = APIClient()

    everyone = ids(client.get(LIST, scope(facility)))
    narrowed = ids(client.get(LIST, scope(facility, specialtyId=tagged["second"].pk)))
    shared = ids(client.get(LIST, scope(facility, specialtyId=str(tagged["first"].pk))))

    assert everyone == {str(facility.pk), str(tagged["neighbour"].pk)}
    assert narrowed == shared == {str(facility.pk)}


@pytest.mark.django_db
@pytest.mark.parametrize("name", ["serviceTagId", "serviceId"])
def test_a_service_narrows_the_list(tagged: dict[str, Any], name: str) -> None:
    facility = tagged["facility"]

    narrowed = ids(APIClient().get(LIST, scope(facility, **{name: tagged["service"].pk})))

    assert narrowed == {str(facility.pk)}


@pytest.mark.django_db
def test_the_filters_combine(tagged: dict[str, Any]) -> None:
    facility = tagged["facility"]
    other = ServiceTag.objects.create(category=facility.category, name_ar="توصيل")
    client = APIClient()

    both = ids(
        client.get(
            LIST,
            scope(facility, specialtyId=tagged["second"].pk, serviceTagId=tagged["service"].pk),
        )
    )
    neither = ids(
        client.get(LIST, scope(facility, specialtyId=tagged["second"].pk, serviceTagId=other.pk))
    )

    assert both == {str(facility.pk)}
    assert neither == set()


@pytest.mark.django_db
def test_a_retired_item_matches_nothing(tagged: dict[str, Any]) -> None:
    facility = tagged["facility"]
    client = APIClient()

    assert ids(client.get(LIST, scope(facility, specialtyId=tagged["retired"].pk))) == set()
    assert ids(client.get(LIST, scope(facility, serviceTagId=tagged["gone"].pk))) == set()


@pytest.mark.django_db
def test_a_filter_matches_only_where_the_category_offers_it(tagged: dict[str, Any]) -> None:
    facility = tagged["facility"]
    CategoryCapabilities.objects.filter(category=facility.category).update(
        supports_specialty_filter=False, supports_service_filter=False
    )
    client = APIClient()

    assert ids(client.get(LIST, scope(facility, specialtyId=tagged["second"].pk))) == set()
    assert ids(client.get(LIST, scope(facility, serviceTagId=tagged["service"].pk))) == set()
    # Without a filter the category lists as it always did.
    assert len(ids(client.get(LIST, scope(facility)))) == 2


@pytest.mark.django_db
def test_the_province_wide_list_keeps_the_same_rule(tagged: dict[str, Any]) -> None:
    facility = tagged["facility"]
    params = {"provinceId": str(facility.province_id), "specialtyId": tagged["second"].pk}

    assert ids(APIClient().get(LIST, params)) == {str(facility.pk)}


@pytest.mark.django_db
@pytest.mark.parametrize("name", ["specialtyId", "serviceTagId", "serviceId"])
@pytest.mark.parametrize("value", ["abc", "1.5", "0", "-3", "12345678901234567890", "٣"])
def test_a_value_that_is_not_a_positive_whole_number_is_refused(
    tagged: dict[str, Any], name: str, value: str
) -> None:
    response = APIClient().get(LIST, scope(tagged["facility"], **{name: value}))

    assert response.status_code == 400, response.content
    body = response.json()
    assert body["code"] == "VALIDATION_ERROR"
    assert name in body["details"]


@pytest.mark.django_db
def test_an_empty_filter_is_no_filter(tagged: dict[str, Any]) -> None:
    facility = tagged["facility"]

    assert len(ids(APIClient().get(LIST, scope(facility, specialtyId="", serviceTagId="")))) == 2


@pytest.mark.django_db
def test_the_map_and_the_search_apply_and_check_the_filters_alike(tagged: dict[str, Any]) -> None:
    facility = tagged["facility"]
    client = APIClient()
    bbox = "38.9,35.9,39.1,36.0"

    on_map = ids(client.get(MAP, scope(facility, bbox=bbox, specialtyId=tagged["second"].pk)))
    found = ids(client.get(SEARCH, scope(facility, q="صيدلية", serviceTagId=tagged["service"].pk)))

    assert on_map == found == {str(facility.pk)}
    assert client.get(MAP, scope(facility, bbox=bbox, specialtyId="x")).status_code == 400
    assert client.get(SEARCH, scope(facility, q="صيدلية", serviceTagId="x")).status_code == 400


# --------------------------------------------------------------------------------------
# Text search over names
# --------------------------------------------------------------------------------------


@pytest.mark.django_db
def test_a_specialty_or_service_name_finds_the_facility(tagged: dict[str, Any]) -> None:
    facility = tagged["facility"]
    client = APIClient()

    assert ids(client.get(SEARCH, scope(facility, q="تركيب"))) == {str(facility.pk)}
    assert ids(client.get(SEARCH, scope(facility, q="ضغط"))) == {str(facility.pk)}
    assert ids(client.get(LIST, scope(facility, search="سريرية"))) == {str(facility.pk)}


@pytest.mark.django_db
def test_a_retired_name_finds_nothing(tagged: dict[str, Any]) -> None:
    facility = tagged["facility"]
    client = APIClient()

    assert ids(client.get(SEARCH, scope(facility, q="منسية"))) == set()
    assert ids(client.get(SEARCH, scope(facility, q="حقن"))) == set()
