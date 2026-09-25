"""Reading OpenStreetMap's geography into this platform's own.

The geography under the app used to be five invented rectangles. What replaces them is other
people's survey work, and the two things that can go wrong with it are both about recognition: a
governorate written one way here and another way there, and the same quarter arriving twice under
two tags. Both are asserted here, on the spellings the Syrian extract actually contains.
"""

import json

import pytest
from django.contrib.gis.geos import Point
from django.core.management import call_command

from locations.models import City, Neighborhood, Province
from locations.osm import (
    english_name,
    is_city,
    is_neighbourhood,
    is_province,
    match_key,
    osm_identity,
    row_id,
    strip_kind,
)


class TestNames:
    """A place is recognised by how its name sounds, not by how it was typed."""

    def test_a_governorate_is_matched_without_its_kind_word(self):
        assert match_key("محافظة الرقة") == match_key("الرقة")
        assert match_key("منطقة تل أبيض") == match_key("تل ابيض")

    def test_the_spellings_of_one_letter_are_one_letter(self):
        # Every one of these pairs appears between the extract and this platform's own rows.
        assert match_key("إدلب") == match_key("ادلب")
        assert match_key("حماة") == match_key("حماه")
        assert match_key("اللاذقية") == match_key("اللاذقيه")

    def test_a_hamza_is_a_letter_and_is_not_normalised_away(self):
        # "السويداء" is how both the extract and this platform write it, so nothing needs to be
        # dropped — and dropping it would make two different names into one.
        assert match_key("محافظة السويداء") == match_key("السويداء")
        assert match_key("السويداء") != match_key("السويدا")

    def test_a_mark_nobody_can_see_does_not_lose_a_match(self):
        # One district in the extract carries a left-to-right mark inside its name.
        assert match_key("منطقة جرابلس‎‎") == match_key("جرابلس")

    def test_two_different_places_do_not_match(self):
        assert match_key("محافظة حمص") != match_key("محافظة حماة")
        assert match_key("الرقة") != match_key("الرقه الجديدة")

    def test_the_kind_word_is_only_stripped_when_it_is_the_kind_word(self):
        assert strip_kind("محافظة الرقة") == "الرقة"
        assert strip_kind("ناحية مركز الرقة") == "مركز الرقة"
        assert strip_kind("الرقة") == "الرقة"
        # Not a prefix: a name that merely begins with those letters keeps them.
        assert strip_kind("محافظةالرقة") == "محافظةالرقة"

    def test_an_english_name_is_read_out_of_the_tags_gdal_could_not_column(self):
        tags = '"ISO3166-2"=>"SY-RA","name:en"=>"Ar-Raqqah","name:fr"=>"Raqqa"'
        assert english_name(tags) == "Ar-Raqqah"
        assert english_name('"name:ar"=>"الرقة"') is None
        assert english_name(None) is None


class TestWhatAFeatureIs:
    """Which of this platform's places a boundary becomes, and which are left out."""

    def test_the_three_levels_this_platform_keeps(self):
        assert is_province({"boundary": "administrative", "admin_level": "4"})
        assert is_city({"boundary": "administrative", "admin_level": "5"})
        assert is_neighbourhood({"boundary": "administrative", "admin_level": "10"})

    def test_a_subdistrict_is_not_a_city_because_a_reader_does_not_say_it(self):
        nahiya = {"boundary": "administrative", "admin_level": "6", "name": "ناحية مركز الرقة"}
        assert not is_city(nahiya)
        assert not is_province(nahiya)
        assert not is_neighbourhood(nahiya)

    def test_a_quarter_counts_however_the_contributor_tagged_it(self):
        assert is_neighbourhood({"place": "neighbourhood", "name": "المشلب"})
        assert is_neighbourhood({"place": "suburb", "name": "رميلة"})
        assert is_neighbourhood({"place": "quarter", "name": "الطيار"})
        assert not is_neighbourhood({"place": "village", "name": "شمرة"})

    def test_identity_is_the_osm_id_and_never_the_name(self):
        assert osm_identity({"osm_id": "1234"}) == "relation:1234"
        assert osm_identity({"osm_way_id": "99"}) == "way:99"
        # Two quarters of Raqqa are both called البعث, so a name cannot be an identity.
        assert osm_identity({"name": "البعث"}) is None

    def test_the_same_place_keeps_its_row_across_imports(self):
        assert row_id("city", "relation:1") == row_id("city", "relation:1")
        assert row_id("city", "relation:1") != row_id("neighbourhood", "relation:1")


def _feature(properties: dict, ring: list[tuple[float, float]]) -> str:
    closed = [*ring, ring[0]]
    return json.dumps(
        {
            "type": "Feature",
            "properties": properties,
            "geometry": {"type": "Polygon", "coordinates": [[list(point) for point in closed]]},
        },
        ensure_ascii=False,
    )


def _box(west: float, south: float, east: float, north: float) -> list[tuple[float, float]]:
    return [(west, south), (east, south), (east, north), (west, north)]


@pytest.fixture
def boundaries(tmp_path):
    """A governorate, a district inside it, and three quarters — one of them tagged twice."""
    path = tmp_path / "boundaries.geojsonl"
    path.write_text(
        "\n".join(
            [
                _feature(
                    {
                        "osm_id": "10",
                        "boundary": "administrative",
                        "admin_level": "4",
                        "name": "محافظة الرقة",
                        "other_tags": '"name:en"=>"Ar-Raqqah"',
                    },
                    _box(38.5, 35.5, 39.5, 36.5),
                ),
                _feature(
                    {
                        "osm_id": "20",
                        "boundary": "administrative",
                        "admin_level": "5",
                        "name": "منطقة الرقة",
                    },
                    _box(38.9, 35.9, 39.1, 36.1),
                ),
                _feature(
                    {"osm_way_id": "30", "place": "neighbourhood", "name": "المشلب"},
                    _box(38.95, 35.95, 39.0, 36.0),
                ),
                # The same way, tagged both ways, as one Raqqa quarter is in the real extract.
                _feature(
                    {
                        "osm_way_id": "30",
                        "boundary": "administrative",
                        "admin_level": "10",
                        "name": "المشلب",
                    },
                    _box(38.95, 35.95, 39.0, 36.0),
                ),
                # Inside the governorate but outside every district.
                _feature(
                    {"osm_way_id": "40", "place": "neighbourhood", "name": "حي بلا منطقة"},
                    _box(38.6, 35.6, 38.65, 35.65),
                ),
            ]
        ),
        encoding="utf-8",
    )
    return path


@pytest.fixture
def province(db):
    """The platform's own Raqqa, as the reference-data migration created it.

    The import matches what is already there rather than creating provinces, so the test has to
    run against the real fourteen — including their spellings, which is the whole difficulty.
    """
    province = Province.objects.get(code="raqqa")
    province.active = True
    province.map_center = Point(39.0085, 35.9528, srid=4326)
    province.save(update_fields=["active", "map_center"])
    return province


@pytest.mark.django_db
class TestImport:
    def test_a_district_becomes_a_city_of_its_governorate(self, province, boundaries):
        call_command("import_osm_boundaries", str(boundaries))

        city = City.objects.get(province=province)
        assert city.name_ar == "الرقة"
        assert city.code == "osm-relation-20"
        assert city.boundary is not None
        assert city.active is True

    def test_a_quarter_becomes_a_neighbourhood_of_its_district(self, province, boundaries):
        call_command("import_osm_boundaries", str(boundaries))

        quarters = Neighborhood.objects.all()
        assert [q.name_ar for q in quarters] == ["المشلب"]
        assert quarters[0].city.name_ar == "الرقة"

    def test_a_quarter_in_no_district_is_left_out(self, province, boundaries):
        call_command("import_osm_boundaries", str(boundaries))

        assert not Neighborhood.objects.filter(name_ar="حي بلا منطقة").exists()

    def test_the_governorate_is_matched_and_not_rewritten(self, province, boundaries):
        before = Province.objects.count()

        call_command("import_osm_boundaries", str(boundaries))

        province.refresh_from_db()
        assert Province.objects.count() == before
        assert province.name_ar == "الرقة"
        assert province.code == "raqqa"

    def test_importing_twice_corrects_the_same_rows(self, province, boundaries):
        call_command("import_osm_boundaries", str(boundaries))
        call_command("import_osm_boundaries", str(boundaries))

        assert City.objects.count() == 1
        assert Neighborhood.objects.count() == 1

    def test_a_dry_run_writes_nothing(self, province, boundaries):
        call_command("import_osm_boundaries", str(boundaries), "--dry-run")

        assert City.objects.count() == 0
        assert Neighborhood.objects.count() == 0

    def test_pruning_removes_what_the_import_did_not_write(self, province, boundaries):
        invented = City.objects.create(
            province=province,
            code="invented",
            name_ar="صندوق",
            boundary=None,
        )
        Neighborhood.objects.create(city=invented, name_ar="حي مختلق")

        call_command("import_osm_boundaries", str(boundaries), "--prune")

        assert [c.code for c in City.objects.all()] == ["osm-relation-20"]
        assert [n.name_ar for n in Neighborhood.objects.all()] == ["المشلب"]

    def test_the_resolver_then_answers_with_the_real_quarter(self, province, boundaries):
        from locations.resolver import ResolvedBy, resolve

        call_command("import_osm_boundaries", str(boundaries))
        answer = resolve(latitude=35.97, longitude=38.97)

        assert answer.resolved_by == ResolvedBy.BOUNDARY
        assert answer.neighborhood is not None
        assert answer.neighborhood.name_ar == "المشلب"
        # The city is not repeated when it carries the province's own name.
        assert answer.label_ar == "الرقة — المشلب"
