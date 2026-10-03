"""LOAD DATA. Thousands of made-up facilities, for measuring the public API at launch scale.

Nothing in this file is real: every name, address, phone number and position is generated. It
exists so that the load test (infrastructure/load/, DECISION-083) runs against a directory the
size the platform expects at launch rather than the handful of rows a development database
holds, because a query that is fast over thirty facilities can be slow over five thousand.

What keeps it out of a real database:

* it refuses any database that already holds a facility it did not make, whatever the
  environment is called, so it cannot mix invented pharmacies into real ones;
* in the production environment it also needs `--disposable`, which only CI's throwaway stack
  passes.

Re-running replaces the previous load data: every id is derived from its index with `uuid5`,
and the previous rows are deleted before the new ones are written, with their hours and duty
shifts.
"""

import random
import uuid
from datetime import datetime, time, timedelta
from typing import Any
from zoneinfo import ZoneInfo

from django.conf import settings
from django.contrib.gis.geos import Point
from django.core.management.base import BaseCommand, CommandError
from django.db import transaction
from django.utils import timezone

from business_hours.models import BusinessHour
from business_hours.services import local_day_bounds
from directory.models import Category, CategoryProvince
from facilities.models import Facility
from locations.models import Province
from pharmacy_duty.models import DutyShift

NAMESPACE = uuid.UUID("0d4c2b8e-7a51-4f8e-b3c6-2e9a1f6d5c70")
DAMASCUS = ZoneInfo("Asia/Damascus")

#: Each governorate's centre and its share of the directory, in percent: the cities the platform
#: launches in carry most of it.
PROVINCES = {
    "raqqa": ((35.9528, 39.0085), 15),
    "damascus": ((33.5138, 36.2765), 15),
    "aleppo": ((36.2021, 37.1343), 15),
    "rif-dimashq": ((33.5711, 36.4019), 10),
    "homs": ((34.7324, 36.7137), 8),
    "hama": ((35.1318, 36.7578), 6),
    "latakia": ((35.5317, 35.7915), 6),
    "deir-ez-zor": ((35.3359, 40.1408), 5),
    "al-hasakah": ((36.5024, 40.7477), 5),
    "tartus": ((34.8890, 35.8866), 4),
    "idlib": ((35.9306, 36.6339), 4),
    "daraa": ((32.6189, 36.1021), 3),
    "as-suwayda": ((32.7090, 36.5695), 2),
    "quneitra": ((33.1256, 35.8240), 2),
}

#: Category code, the word its facilities' names start with, and its share in percent.
CATEGORIES = [
    ("pharmacy", "صيدلية", 40),
    ("medical-clinic", "عيادة", 30),
    ("medical-laboratory", "مخبر", 12),
    ("nursing-center", "مركز", 8),
    ("medical-supplies", "مستودع", 10),
]

NAMES = [
    "الشفاء",
    "الأمل",
    "النور",
    "الحياة",
    "الرحمة",
    "السلام",
    "الفرات",
    "الياسمين",
    "البركة",
    "الهدى",
    "الوفاء",
    "الصحة",
    "المدينة",
    "الزهراء",
    "الرشيد",
    "ابن سينا",
    "الفارابي",
    "دجلة",
    "العاصي",
    "الجزيرة",
]
STREETS = ["شارع تل أبيض", "شارع القطار", "شارع المنصور", "شارع الوادي", "شارع 23 شباط"]

#: Opening hours, by how often they occur.
HOURS = [
    ((time(0, 0), time(23, 59)), 2),
    ((time(8, 0), time(14, 0)), 4),
    ((time(16, 0), time(23, 0)), 3),
    (None, 1),
]


def _weighted(rng: random.Random, options: list[tuple[Any, int]]) -> Any:
    return rng.choices([value for value, _ in options], weights=[w for _, w in options])[0]


class Command(BaseCommand):
    help = "LOAD DATA: thousands of made-up facilities, for the load test only."

    def add_arguments(self, parser: Any) -> None:
        parser.add_argument("--facilities", type=int, default=5000)
        parser.add_argument(
            "--disposable",
            action="store_true",
            help="The database is thrown away after the run (CI's stack). Needed in production.",
        )

    @transaction.atomic
    def handle(self, *args: Any, **options: Any) -> None:
        total = int(options["facilities"])
        if not 1 <= total <= 50_000:
            raise CommandError("--facilities must be between 1 and 50000.")
        ids = [uuid.uuid5(NAMESPACE, f"load-{index}") for index in range(50_000)]
        foreign = Facility.objects.exclude(id__in=ids).count()
        if foreign:
            raise CommandError(
                f"Refusing: this database holds {foreign} facilities this command did not make."
            )
        environment = str(getattr(settings, "ENVIRONMENT_NAME", "")).lower()
        if environment == "production" and not options["disposable"]:
            raise CommandError("Refusing to write load data in production without --disposable.")

        Facility.objects.filter(id__in=ids).delete()
        rng = random.Random(83)
        now = timezone.now()
        provinces = {p.code: p for p in Province.objects.filter(code__in=PROVINCES)}
        categories = {
            c.code: c for c in Category.objects.filter(code__in=[c for c, *_ in CATEGORIES])
        }
        for province in provinces.values():
            for category in categories.values():
                CategoryProvince.objects.update_or_create(
                    province=province,
                    category=category,
                    defaults={"public_enabled": True, "owner_registration_enabled": True},
                )

        facilities: list[Facility] = []
        hours: list[BusinessHour] = []
        pharmacies: list[uuid.UUID] = []
        province_weights = [(code, share) for code, (_, share) in PROVINCES.items()]
        category_weights = [((code, word), share) for code, word, share in CATEGORIES]
        for index in range(total):
            code = _weighted(rng, province_weights)
            category_code, word = _weighted(rng, category_weights)
            (lat, lon), _ = PROVINCES[code]
            facility = Facility(
                id=ids[index],
                category=categories[category_code],
                province=provinces[code],
                name_ar=f"{word} {rng.choice(NAMES)} {index}",
                phone=f"+96393{index:07d}",
                address_ar=rng.choice(STREETS),
                location=Point(
                    lon + rng.uniform(-0.08, 0.08), lat + rng.uniform(-0.06, 0.06), srid=4326
                ),
                status=Facility.Status.ACTIVE,
                activated_at=now,
            )
            facilities.append(facility)
            span = _weighted(rng, HOURS)
            if span is not None:
                hours.extend(
                    BusinessHour(
                        facility=facility, weekday=weekday, opens_at=span[0], closes_at=span[1]
                    )
                    for weekday in range(7)
                )
            if category_code == "pharmacy":
                pharmacies.append(facility.id)
        Facility.objects.bulk_create(facilities, batch_size=1000)
        BusinessHour.objects.bulk_create(hours, batch_size=5000)

        # A tenth of the pharmacies on tonight's roster, as a duty list would have them.
        day_start, _ = local_day_bounds(now)
        evening = datetime.combine(day_start.astimezone(DAMASCUS).date(), time(20, 0), DAMASCUS)
        DutyShift.objects.bulk_create(
            DutyShift(
                facility_id=facility_id, starts_at=evening, ends_at=evening + timedelta(hours=12)
            )
            for facility_id in pharmacies[::10]
        )
        self.stdout.write(
            f"load data: {total} facilities, {len(hours)} opening hours, "
            f"{len(pharmacies[::10])} on duty tonight, in {len(provinces)} provinces"
        )
