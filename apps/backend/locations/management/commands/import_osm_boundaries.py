"""Load real administrative boundaries and city quarters into the platform's geography.

    python manage.py import_osm_boundaries .local-stack/osm/boundaries.geojsonl

The file is made by `scripts/osm-boundaries.sh` from an OpenStreetMap extract of Syria; that script
does the filtering with GDAL, and this does the matching, the containment and the writing. What each
OpenStreetMap level becomes here is decided in `locations/osm.py`, where it is also explained.

This replaces a fixture. Until now the only geography finer than a province was five rectangles laid
over Raqqa so that a position would resolve to something on a device, and those rectangles were
invented — the corner of Home read "الرقة — الدرعية" because a box said so, not because anyone had
drawn that quarter. What goes in now is what contributors surveyed: fourteen governorates, sixty-
seven districts, and the quarters of Damascus, Aleppo, Raqqa and Deir ez-Zor.

Re-running is safe and is the point: every row's key is derived from the OpenStreetMap id of the
boundary it came from, so a second import corrects the same rows rather than making a second set.

Two options are for a fixture database and say so:

* `--prune` removes cities and quarters this import did not write — the invented ones. A facility
  that pointed at one is left pointing at nothing, which `--relink` then fixes.
* `--relink` attaches every facility to the place its own coordinate falls in. That is right for
  fixture data and wrong for real data, where a facility's address is what its owner told us, so it
  refuses a database whose name looks like production.
"""

from __future__ import annotations

import json
import uuid
from collections.abc import Iterator
from pathlib import Path
from typing import Any

from django.contrib.gis.geos import GeometryCollection, GEOSGeometry, MultiPolygon, Polygon
from django.core.management.base import BaseCommand, CommandError
from django.db import transaction

from facilities.models import Facility
from locations.models import City, Neighborhood, Province
from locations.osm import (
    city_code,
    english_name,
    is_city,
    is_neighbourhood,
    is_province,
    match_key,
    osm_identity,
    row_id,
    strip_kind,
)


class Command(BaseCommand):
    help = "Import provinces' districts and city quarters from an OpenStreetMap extract."

    def add_arguments(self, parser: Any) -> None:
        parser.add_argument("path", help="A GeoJSON or GeoJSON-sequence file of boundaries.")
        parser.add_argument(
            "--prune",
            action="store_true",
            help="Remove cities and quarters this import did not write.",
        )
        parser.add_argument(
            "--relink",
            action="store_true",
            help="Attach every facility to the place its coordinate falls in (fixture data only).",
        )
        parser.add_argument(
            "--dry-run",
            action="store_true",
            help="Report what would change and write nothing.",
        )
        parser.add_argument("--allow-any-database", action="store_true")

    @transaction.atomic
    def handle(self, *args: Any, **options: Any) -> None:
        path = Path(options["path"])
        if not path.exists():
            raise CommandError(f"No such file: {path}")

        features = list(self._read(path))
        if not features:
            raise CommandError(f"{path} carries no features with geometry.")

        provinces = self._provinces([f for f in features if is_province(f[0])])
        cities = self._cities([f for f in features if is_city(f[0])], provinces, options["dry_run"])
        quarters = self._quarters(
            [f for f in features if is_neighbourhood(f[0])], cities, options["dry_run"]
        )

        if options["prune"]:
            self._prune(
                {city.id for _, city, _ in cities},
                {quarter.id for quarter in quarters},
                options["dry_run"],
            )
        if options["relink"]:
            self._relink(options)

        if options["dry_run"]:
            self.stdout.write(self.style.WARNING("dry run: nothing was written"))
            transaction.set_rollback(True)

    # --- reading -------------------------------------------------------------------------

    def _read(self, path: Path) -> Iterator[tuple[dict[str, Any], GEOSGeometry]]:
        """Every feature that carries a geometry, from either GeoJSON shape.

        A whole-country extract is tens of megabytes as one object; the sequence form is a feature
        per line, which is why the script writes that. Both are accepted because a reviewer will
        have whichever one their tools produce.
        """
        text = path.read_text(encoding="utf-8")
        stripped = text.lstrip()
        if stripped.startswith("{") and '"FeatureCollection"' in stripped[:200]:
            documents = json.loads(text).get("features", [])
        else:
            documents = [json.loads(line) for line in text.splitlines() if line.strip()]
        for document in documents:
            geometry = document.get("geometry")
            if not geometry:
                continue
            yield document.get("properties") or {}, GEOSGeometry(json.dumps(geometry), srid=4326)

    # --- the three levels ----------------------------------------------------------------

    def _provinces(
        self, features: list[tuple[dict[str, Any], GEOSGeometry]]
    ) -> list[tuple[Province, GEOSGeometry]]:
        """The governorates in the file, matched to the provinces this platform already has.

        The platform's provinces are reference data with their own codes and their own map centres;
        this does not create or rename one. It only learns which polygon is which province, so that
        a district can be put in the right one.
        """
        by_key: dict[str, Province] = {}
        ambiguous: set[str] = set()
        for province in Province.objects.all():
            key = match_key(province.name_ar)
            if key in by_key:
                # Two of this platform's provinces reduce to the same name. Matching either one
                # would be a guess, so neither is matched and both are named in the report.
                ambiguous.add(key)
            by_key[key] = province
        matched: list[tuple[Province, GEOSGeometry]] = []
        missing: list[str] = []
        for properties, geometry in features:
            key = match_key(properties.get("name"))
            found = None if key in ambiguous else by_key.get(key)
            if found is None:
                missing.append(str(properties.get("name")))
                continue
            matched.append((found, geometry))
        self.stdout.write(f"governorates: {len(matched)} matched of {len(features)} in the file")
        if missing:
            self.stdout.write(self.style.WARNING(f"  no province for: {', '.join(missing)}"))
        unmatched = set(by_key.values()) - {p for p, _ in matched}
        if unmatched:
            names = ", ".join(sorted(p.name_ar for p in unmatched))
            self.stdout.write(self.style.WARNING(f"  no boundary for: {names}"))
        return matched

    def _cities(
        self,
        features: list[tuple[dict[str, Any], GEOSGeometry]],
        provinces: list[tuple[Province, GEOSGeometry]],
        dry_run: bool,
    ) -> list[tuple[dict[str, Any], City, GEOSGeometry]]:
        """Districts, as this platform's cities, each in the governorate that contains it."""
        written: list[tuple[dict[str, Any], City, GEOSGeometry]] = []
        orphans = 0
        for properties, geometry in features:
            identity = osm_identity(properties)
            name = strip_kind(properties.get("name"))
            if not identity or not name:
                continue
            province = self._containing_province(geometry, provinces)
            if province is None:
                orphans += 1
                continue
            boundary = _as_multipolygon(geometry)
            if boundary is None:
                orphans += 1
                continue
            city = City(
                id=row_id("city", identity),
                province=province,
                code=city_code(identity),
                name_ar=name,
                name_en=english_name(properties.get("other_tags")) or "",
                boundary=boundary,
                active=True,
            )
            if not dry_run:
                City.objects.update_or_create(
                    id=city.id,
                    defaults={
                        "province": province,
                        "code": city.code,
                        "name_ar": city.name_ar,
                        "name_en": city.name_en,
                        "boundary": boundary,
                        "active": True,
                    },
                )
            written.append((properties, city, geometry))
        self.stdout.write(f"districts: {len(written)} written, {orphans} outside every governorate")
        return written

    def _quarters(
        self,
        features: list[tuple[dict[str, Any], GEOSGeometry]],
        cities: list[tuple[dict[str, Any], City, GEOSGeometry]],
        dry_run: bool,
    ) -> list[Neighborhood]:
        """Quarters, each in the district that contains it.

        A quarter that falls in no district is skipped rather than attached to the nearest one: the
        resolver answers with a city when it has no quarter, and that is a true answer, while a
        quarter attached to the wrong city is a false one on the reader's screen.
        """
        written: list[Neighborhood] = []
        seen: set[uuid.UUID] = set()
        orphans = 0
        # One way in Syria's extract carries both tags for the same quarter —
        # `admin_level=10` and `place=neighbourhood` — so it arrives twice and is one place.
        # The key is the id of the way, which is what recognises the second arrival.
        same_place = 0
        for properties, geometry in features:
            identity = osm_identity(properties)
            name = (properties.get("name") or "").strip()
            if not identity or not name:
                continue
            key = row_id("neighbourhood", identity)
            if key in seen:
                same_place += 1
                continue
            city = self._containing_city(geometry, cities)
            if city is None:
                orphans += 1
                continue
            boundary = _as_multipolygon(geometry)
            if boundary is None:
                orphans += 1
                continue
            quarter = Neighborhood(
                id=key,
                city=city,
                name_ar=name,
                name_en=english_name(properties.get("other_tags")) or "",
                boundary=boundary,
                active=True,
            )
            if not dry_run:
                Neighborhood.objects.update_or_create(
                    id=quarter.id,
                    defaults={
                        "city": city,
                        "name_ar": quarter.name_ar,
                        "name_en": quarter.name_en,
                        "boundary": boundary,
                        "active": True,
                    },
                )
            seen.add(key)
            written.append(quarter)
        twice = f", {same_place} tagged twice and imported once" if same_place else ""
        self.stdout.write(
            f"quarters: {len(written)} written, {orphans} outside every district{twice}"
        )
        return written

    # --- the fixture's two chores --------------------------------------------------------

    def _prune(self, city_ids: set[uuid.UUID], quarter_ids: set[uuid.UUID], dry_run: bool) -> None:
        stale_quarters = Neighborhood.objects.exclude(id__in=quarter_ids)
        stale_cities = City.objects.exclude(id__in=city_ids)
        self.stdout.write(
            f"pruning: {stale_quarters.count()} quarters and {stale_cities.count()} cities "
            "this import did not write"
        )
        if not dry_run:
            stale_quarters.delete()
            stale_cities.delete()

    def _relink(self, options: dict[str, Any]) -> None:
        from django.conf import settings

        name = str(settings.DATABASES["default"]["NAME"])
        if not options["allow_any_database"] and "prod" in name.lower():
            raise CommandError(f"Refusing to rewrite facility places in {name!r}.")

        moved = 0
        for facility in Facility.objects.exclude(location=None).select_related("province"):
            quarter = (
                Neighborhood.objects.filter(boundary__contains=facility.location)
                .select_related("city")
                .first()
            )
            city = (
                quarter.city
                if quarter
                else City.objects.filter(boundary__contains=facility.location).first()
            )
            if facility.city_id == (city.id if city else None) and facility.neighborhood_id == (
                quarter.id if quarter else None
            ):
                continue
            facility.city = city
            facility.neighborhood = quarter
            if not options["dry_run"]:
                facility.save(update_fields=["city", "neighborhood"])
            moved += 1
        self.stdout.write(f"facilities relinked: {moved}")

    # --- containment ---------------------------------------------------------------------

    def _containing_province(
        self, geometry: GEOSGeometry, provinces: list[tuple[Province, GEOSGeometry]]
    ) -> Province | None:
        probe = _probe(geometry)
        if probe is None:
            return None
        for province, boundary in provinces:
            if boundary.contains(probe):
                return province
        return None

    def _containing_city(
        self, geometry: GEOSGeometry, cities: list[tuple[dict[str, Any], City, GEOSGeometry]]
    ) -> City | None:
        probe = _probe(geometry)
        if probe is None:
            return None
        for _, city, boundary in cities:
            if boundary.contains(probe):
                return city
        return None


def _probe(geometry: GEOSGeometry) -> GEOSGeometry | None:
    """A point certain to be inside the shape.

    `point_on_surface` rather than the centroid: the centroid of a crescent-shaped district falls
    outside it, and this decides which governorate a district belongs to.
    """
    try:
        return geometry.point_on_surface
    except Exception:  # noqa: BLE001 - a broken ring is data, not a programming error
        return None


def _as_multipolygon(geometry: GEOSGeometry) -> MultiPolygon | None:
    """The shape as the column holds it: a multipolygon, valid, in WGS 84.

    GDAL hands over multipolygons already — every one of the 276 features in Syria's extract is one,
    and it closes the unclosed rings itself as it reads them. The other branches are for the shapes
    a hand-written GeoJSON has, and for what a repair returns: contributors do leave
    self-intersections behind, and a boundary PostGIS cannot answer `contains` for is worse than no
    boundary at all.
    """
    shape: GEOSGeometry = geometry
    if not shape.valid:
        repaired = _repaired(shape)
        if repaired is None:
            return None
        shape = repaired
    if isinstance(shape, MultiPolygon):
        shape.srid = 4326
        return shape
    if isinstance(shape, Polygon):
        return MultiPolygon(shape, srid=4326)
    if isinstance(shape, GeometryCollection):
        polygons = [part for part in shape if isinstance(part, Polygon)]
        return MultiPolygon(*polygons, srid=4326) if polygons else None
    return None


def _repaired(shape: GEOSGeometry) -> GEOSGeometry | None:
    """The shape with its rings made valid, or nothing when GEOS cannot do it."""
    try:
        # django-stubs does not carry `make_valid`, which GEOS has had since 3.8 and Django since
        # 4.2. The runtime attribute is checked by the import's own tests.
        repaired = shape.make_valid()  # type: ignore[attr-defined]
    except Exception:  # noqa: BLE001 - a ring nothing can close is data, not a programming error
        return None
    return repaired if repaired.valid else None
