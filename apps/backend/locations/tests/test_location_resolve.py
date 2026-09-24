"""What the platform calls a coordinate.

The app asks this so a user is not made to choose their province from a list. The answers
have to come from the platform's own geography, and a point nowhere near it must come back
empty rather than as the nearest guess from across the country.
"""

import pytest
from django.contrib.gis.geos import MultiPolygon, Point, Polygon
from rest_framework.test import APIClient

from locations.models import City, Neighborhood, Province

RESOLVE = "/api/v1/public/locations/resolve/"


def _square(longitude: float, latitude: float, side: float = 0.1) -> MultiPolygon:
    half = side / 2
    ring = (
        (longitude - half, latitude - half),
        (longitude + half, latitude - half),
        (longitude + half, latitude + half),
        (longitude - half, latitude + half),
        (longitude - half, latitude - half),
    )
    return MultiPolygon(Polygon(ring), srid=4326)


@pytest.fixture
def geography(db):
    province = Province.objects.create(
        code="raqqa-resolve",
        name_ar="الرقة",
        active=True,
        map_center=Point(39.0085, 35.9528, srid=4326),
    )
    city = City.objects.create(
        province=province,
        code="raqqa-city",
        name_ar="الرقة",
        boundary=_square(39.0085, 35.9528, side=0.2),
    )
    neighborhood = Neighborhood.objects.create(
        city=city,
        name_ar="المشلب",
        boundary=_square(39.0300, 35.9600, side=0.02),
    )
    return province, city, neighborhood


@pytest.mark.django_db
def test_a_point_inside_a_neighbourhood_is_named_by_it(geography):
    province, city, neighborhood = geography
    body = APIClient().get(RESOLVE, {"latitude": 35.9600, "longitude": 39.0300}).json()

    assert body["province"]["id"] == str(province.id)
    assert body["city"]["id"] == str(city.id)
    assert body["neighborhood"]["id"] == str(neighborhood.id)
    assert body["label"] == "الرقة — المشلب"
    assert body["resolvedBy"] == "BOUNDARY"


@pytest.mark.django_db
def test_a_point_in_the_city_but_no_neighbourhood_is_named_by_the_city(geography):
    province, city, _ = geography
    body = APIClient().get(RESOLVE, {"latitude": 35.9000, "longitude": 38.9500}).json()

    assert body["province"]["id"] == str(province.id)
    assert body["city"]["id"] == str(city.id)
    assert body["neighborhood"] is None
    # The city carries the province's own name here, so the label does not say it twice.
    assert body["label"] == "الرقة"
    assert body["resolvedBy"] == "BOUNDARY"


@pytest.mark.django_db
def test_a_city_of_its_own_name_is_the_finer_half_of_the_label(geography):
    """The whole point of the second half: a town that is not the province's own capital."""
    province, _, _ = geography
    tell_abyad = City.objects.create(
        province=province,
        code="tell-abyad",
        name_ar="تل أبيض",
        boundary=_square(38.9500, 36.6900, side=0.1),
    )

    body = APIClient().get(RESOLVE, {"latitude": 36.6900, "longitude": 38.9500}).json()

    assert body["city"]["id"] == str(tell_abyad.id)
    assert body["neighborhood"] is None
    assert body["label"] == "الرقة — تل أبيض"


@pytest.mark.django_db
def test_a_neighbourhood_names_the_place_ahead_of_the_city_it_is_in(geography):
    """Two halves, never three: the finest place the platform knows, and the province."""
    province, _, _ = geography
    outskirts = City.objects.create(
        province=province,
        code="outskirts",
        name_ar="تل أبيض",
        boundary=_square(38.9500, 36.6900, side=0.1),
    )
    Neighborhood.objects.create(
        city=outskirts,
        name_ar="الرميلة",
        boundary=_square(38.9520, 36.6920, side=0.01),
    )

    body = APIClient().get(RESOLVE, {"latitude": 36.6920, "longitude": 38.9520}).json()

    assert body["label"] == "الرقة — الرميلة"


@pytest.mark.django_db
def test_a_point_outside_every_boundary_falls_back_to_the_nearest_province(geography):
    # Sixty kilometres north-east of the boundaries, inside no city the platform has seeded.
    body = APIClient().get(RESOLVE, {"latitude": 36.5000, "longitude": 39.5000}).json()

    assert body["resolvedBy"] == "NEAREST_PROVINCE"
    assert body["province"] is not None
    assert body["city"] is None
    assert body["neighborhood"] is None
    assert body["label"] == body["province"]["nameAr"]


@pytest.mark.django_db
def test_a_point_in_another_country_resolves_to_nothing(geography):
    body = APIClient().get(RESOLVE, {"latitude": 48.8566, "longitude": 2.3522}).json()

    assert body["province"] is None
    assert body["label"] is None
    assert body["resolvedBy"] == "NONE"


@pytest.mark.django_db
def test_an_inactive_province_takes_its_boundaries_out_of_the_answer(geography):
    province, _, _ = geography
    province.active = False
    province.save(update_fields=["active"])

    body = APIClient().get(RESOLVE, {"latitude": 35.9600, "longitude": 39.0300}).json()

    # The neighbourhood is inside a province the platform no longer serves, so it is not the
    # answer; whatever else is nearby answers instead, and never by boundary.
    assert body["neighborhood"] is None
    assert body["city"] is None
    assert body["resolvedBy"] != "BOUNDARY"


@pytest.mark.django_db
def test_a_missing_or_impossible_coordinate_is_refused(geography):
    client = APIClient()

    assert client.get(RESOLVE).status_code == 400
    assert client.get(RESOLVE, {"latitude": 35.9, "longitude": "east"}).status_code == 400
    assert client.get(RESOLVE, {"latitude": 99.0, "longitude": 39.0}).status_code == 400
