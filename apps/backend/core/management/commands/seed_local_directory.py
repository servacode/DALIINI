"""FIXTURE DATA. A directory with something in every section, for local review only.

Nothing in this file is real. It exists so that Home can be opened on a development build and
every category actually has facilities under it — otherwise four of the five sections are empty
and the interface cannot be judged at all.

Two things keep it out of production. The command refuses a database whose name looks like one,
the way the other seed commands do; and it is invoked only from `scripts/local-stack.sh`, never
from a migration, so no environment gets this data by merely being migrated.

Re-running is safe. Every row's primary key is derived from its slug with `uuid5`, so a second
run updates the same rows instead of making a second set, and the hours and duty shifts that
hang off them are rebuilt rather than appended to.

The pharmacies deliberately cover the three states the product distinguishes — open and on
duty, shut and on duty, open and not on duty — plus the ordinary case of a pharmacy that is
neither, because a badge that appears on everything says nothing.
"""

import uuid
from datetime import datetime, time, timedelta
from typing import Any
from zoneinfo import ZoneInfo

from django.contrib.gis.geos import Point
from django.core.management.base import BaseCommand, CommandError
from django.db import transaction
from django.utils import timezone

from business_hours.models import BusinessHour
from business_hours.services import local_day_bounds
from content_services.models import Advertisement
from directory.models import Category, CategoryProvince
from facilities.models import Facility
from locations.models import Province
from pharmacy_duty.models import DutyShift

#: The geography a coordinate resolves to is not invented here any more.
#:
#: This file used to carry a city-sized rectangle and five district-sized ones, laid over Raqqa so
#: that a position would resolve to something on a device. They were made up: the corner of Home
#: read "الرقة — الدرعية" because a box said so. Real boundaries — fourteen governorates,
#: sixty-seven districts and the surveyed quarters of Damascus, Aleppo, Raqqa and Deir ez-Zor
#: — now come from
#: OpenStreetMap through `scripts/osm-boundaries.sh` and `manage.py import_osm_boundaries`, which
#: `scripts/local-stack.sh` runs when the extract is present.
#:
#: The facilities below are still fixtures, and they are placed by coordinate; the import's
#: `--relink` attaches each of them to the real city and quarter its own point falls in.

#: Stable across runs and machines, so a row's identity comes from its slug and nothing else.
NAMESPACE = uuid.UUID("6f1b7a2e-3c44-4b0e-9a1d-5f7c8e2b4a10")
DAMASCUS = ZoneInfo("Asia/Damascus")

#: Raqqa, roughly. Each facility sits a few hundred metres from the next so that ordering by
#: distance is visibly different from ordering by name.
CENTRE_LAT, CENTRE_LON = 35.9528, 39.0085

ALWAYS = "always"
MORNINGS = "mornings"
EVENINGS = "evenings"
SHUT = "shut"

#: (slug, name, address, north offset, east offset, hours)
#:
#: The address is a street, never a quarter: which quarter a facility stands in is answered by
#: the imported boundaries against its own coordinate, and a fixture that also wrote a quarter
#: into its address line would contradict them on the screen.
PHARMACIES = [
    ("ph-hayat", "صيدلية الحياة", "شارع تل أبيض", 0.004, 0.003, ALWAYS),
    ("ph-shifa", "صيدلية الشفاء", "شارع 23 شباط", -0.006, 0.005, SHUT),
    ("ph-amal", "صيدلية الأمل", "شارع القطار", 0.009, -0.004, ALWAYS),
    ("ph-nour", "صيدلية النور", "شارع المنصور", -0.011, -0.007, SHUT),
    ("ph-rawda", "صيدلية الروضة", "شارع الملعب", 0.014, 0.010, MORNINGS),
]

#: The banners Home's slider shows. They exist so that the slider has a place on the screen
#: during development; nothing here is an advertisement for anything. The images are drawn
#: by the command itself rather than shipped, so no artwork enters the repository.
#:
#: (slug, title, subtitle, background, seconds on screen)
ADS = [
    ("ad-one", "مساحة إعلانية", "هنا تظهر إعلانات المنصة", (4, 38, 35), 5),
    ("ad-two", "مساحة إعلانية", "صورة العرض بعرض الشاشة", (17, 94, 79), 5),
    ("ad-three", "مساحة إعلانية", "ثلاث شرائح تتبدّل وحدها", (92, 122, 74), 5),
]

#: The shape Home draws a banner in, so a placeholder is the size of the real thing.
AD_WIDTH, AD_HEIGHT = 1080, 540


CLINICS = [
    ("cl-ibnsina", "عيادة ابن سينا", "شارع تل أبيض", 0.003, -0.002, MORNINGS),
    ("cl-rasheed", "عيادة الرشيد التخصصية", "شارع الكورنيش", -0.005, 0.004, EVENINGS),
    ("cl-farabi", "عيادة الفارابي", "شارع القطار", 0.008, 0.006, ALWAYS),
    ("cl-yarmouk", "عيادة اليرموك", "شارع السباهي", -0.012, -0.003, MORNINGS),
]

LABORATORIES = [
    ("lab-tahlil", "مخبر التحليل الحديث", "شارع تل أبيض", 0.002, 0.007, MORNINGS),
    ("lab-diqqa", "مخبر الدقة للتحاليل", "شارع 23 شباط", -0.007, -0.006, ALWAYS),
    ("lab-safa", "مخبر الصفا الطبي", "شارع الملعب", 0.010, 0.002, EVENINGS),
]

NURSING = [
    ("nr-rahma", "مركز الرحمة للتمريض", "شارع المنصور", 0.005, 0.009, ALWAYS),
    ("nr-anaya", "مركز العناية المنزلية", "شارع القطار", -0.009, 0.001, MORNINGS),
    ("nr-hayah", "مركز الحياة للخدمات التمريضية", "شارع الكورنيش", 0.012, -0.008, EVENINGS),
]

SUPPLIES = [
    ("ms-tibbi", "مستودع المستلزمات الطبية", "شارع تل أبيض", 0.006, -0.010, MORNINGS),
    ("ms-ajhiza", "مركز الأجهزة الطبية", "شارع 23 شباط", -0.003, 0.008, MORNINGS),
    ("ms-siha", "بيت الصحة للمستلزمات", "شارع الملعب", 0.015, 0.004, EVENINGS),
]

SECTIONS = [
    ("pharmacy", PHARMACIES),
    ("medical-clinic", CLINICS),
    ("medical-laboratory", LABORATORIES),
    ("nursing-center", NURSING),
    ("medical-supplies", SUPPLIES),
]

HOURS = {
    ALWAYS: (time(0, 0), time(23, 59)),
    MORNINGS: (time(8, 0), time(14, 0)),
    EVENINGS: (time(16, 0), time(23, 0)),
}


class Command(BaseCommand):
    help = "FIXTURE DATA: fill every enabled category in Raqqa so Home can be reviewed locally."

    def add_arguments(self, parser: Any) -> None:
        parser.add_argument(
            "--allow-any-database",
            action="store_true",
            help="Skip the safety check. Intended for CI, where the database is disposable.",
        )

    @transaction.atomic
    def handle(self, *args: Any, **options: Any) -> None:
        from django.conf import settings

        name = str(settings.DATABASES["default"]["NAME"])
        if not options["allow_any_database"] and "prod" in name.lower():
            raise CommandError(f"Refusing to seed fixtures into {name!r}.")

        raqqa = Province.objects.get(code="raqqa")
        now = timezone.now()
        counts = {}

        for code, rows in SECTIONS:
            category = Category.objects.select_related("capabilities").get(code=code)
            # Development only: the launch baseline leaves these off because which categories a
            # province opens with is a product decision, not a technical one.
            CategoryProvince.objects.update_or_create(
                province=raqqa,
                category=category,
                defaults={"public_enabled": True, "owner_registration_enabled": True},
            )
            for slug, facility_name, address, north, east, hours in rows:
                facility = self._facility(
                    slug, category, raqqa, facility_name, address, north, east, now
                )
                self._hours(facility, hours)
                counts[category.name_ar] = counts.get(category.name_ar, 0) + 1

        self._duty(now)
        self._ads(raqqa)

        for category_name, total in counts.items():
            self.stdout.write(f"{category_name}: {total}")

    def _ads(self, province):
        """Three banners, with an image each, so the slider on Home has something to show.

        The picture is generated and written to the public bucket here rather than kept in
        the repository: it is a coloured rectangle with a line of Arabic on it, and a
        checked-in binary would outlive the reason for it.
        """
        for index, (slug, title, subtitle, colour, seconds) in enumerate(ADS):
            key = self._ad_image(slug, title, subtitle, colour)
            Advertisement.objects.update_or_create(
                id=uuid.uuid5(NAMESPACE, slug),
                defaults={
                    "image_key": key,
                    "title_ar": title,
                    "subtitle_ar": subtitle,
                    "action_type": Advertisement.ActionType.NONE,
                    # Global rather than tied to Raqqa: the banner exists so the slider has a
                    # place on the screen, and a reviewer whose device is set elsewhere should
                    # still see it.
                    "target_scope": Advertisement.TargetScope.GLOBAL,
                    "province": None,
                    "enabled": True,
                    "sort_order": index,
                    "slide_duration_ms": seconds * 1000,
                },
            )
        self.stdout.write(f"إعلانات: {len(ADS)}")

    def _ad_image(self, slug, title, subtitle, colour):
        """Draw the banner and store it, returning the key the serializer turns into a URL."""
        from io import BytesIO

        from django.core.files.base import ContentFile
        from PIL import Image, ImageDraw

        from storage.backends import PublicS3Storage

        image = Image.new("RGB", (AD_WIDTH, AD_HEIGHT), colour)
        draw = ImageDraw.Draw(image)
        # No font is bundled, and Arabic would need shaping to be drawn correctly here. Two
        # bars stand in for the words instead: the point is the banner's size and place.
        draw.rounded_rectangle(
            (80, AD_HEIGHT // 2 - 70, AD_WIDTH - 80, AD_HEIGHT // 2 - 20),
            radius=25,
            fill=(255, 255, 255),
        )
        draw.rounded_rectangle(
            (80, AD_HEIGHT // 2 + 10, AD_WIDTH // 2, AD_HEIGHT // 2 + 50),
            radius=20,
            fill=(255, 255, 255, 160),
        )
        buffer = BytesIO()
        image.save(buffer, format="JPEG", quality=85)

        storage = PublicS3Storage()
        key = f"ads/{slug}.jpg"
        # A fixed key, so a second run replaces the picture instead of leaving a new one
        # beside it; the storage would otherwise rename rather than overwrite.
        if storage.exists(key):
            storage.delete(key)
        return storage.save(key, ContentFile(buffer.getvalue(), name="ad.jpg"))

    def _facility(self, slug, category, province, name, address, north, east, now):
        facility, _ = Facility.objects.update_or_create(
            id=uuid.uuid5(NAMESPACE, slug),
            defaults={
                "category": category,
                "province": province,
                "name_ar": name,
                "phone": "+963933000000",
                "address_ar": address,
                "location": Point(CENTRE_LON + east, CENTRE_LAT + north, srid=4326),
                "status": Facility.Status.ACTIVE,
                "activated_at": now,
            },
        )
        return facility

    def _hours(self, facility, kind):
        BusinessHour.objects.filter(facility=facility).delete()
        if kind == SHUT:
            return
        opens_at, closes_at = HOURS[kind]
        BusinessHour.objects.bulk_create(
            BusinessHour(
                facility=facility, weekday=weekday, opens_at=opens_at, closes_at=closes_at
            )
            for weekday in range(7)
        )

    def _duty(self, now):
        """Today's roster: two pharmacies on it, one open and one shut.

        The shut one is the case the whole feature exists for — somebody looking for a pharmacy
        tonight needs to see it listed hours before its doors open. The other pharmacies are
        left off the roster entirely, and the interface says nothing about them, which is the
        point: there is no badge for not being on duty.
        """
        pharmacies = [uuid.uuid5(NAMESPACE, slug) for slug, *_ in PHARMACIES]
        DutyShift.objects.filter(facility__id__in=pharmacies).delete()
        day_start, _ = local_day_bounds(now)
        local_start = day_start.astimezone(DAMASCUS)
        evening = datetime.combine(local_start.date(), time(20, 0), tzinfo=DAMASCUS)

        # Open now and on duty today: the shift covers the whole local day.
        DutyShift.objects.create(
            facility_id=uuid.uuid5(NAMESPACE, "ph-hayat"),
            starts_at=day_start,
            ends_at=day_start + timedelta(hours=23, minutes=59),
        )
        # Shut now and on duty today: tonight's shift, which has not started yet.
        DutyShift.objects.create(
            facility_id=uuid.uuid5(NAMESPACE, "ph-shifa"),
            starts_at=evening,
            ends_at=evening + timedelta(hours=8),
        )
