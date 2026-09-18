"""Deterministic fixtures for the Android connected suite.

Like `seed_e2e_fixtures` for the Admin, and separate from it: the launch dataset is frozen
reference data, and nothing here may ever ship. Every row is recognisable by its `e2e-m-`
prefix or by one of the phone numbers below, and is removed and rebuilt on every run.

The five public pharmacies cover what a discovery test has to tell apart: one open, one on
duty, one temporarily closed, one with two spans on the same day, and one simply closed. All
sit a few hundred metres apart in Raqqa, so nearest-first ordering is observable.

This command refuses to run against a database that does not look like a test database.
"""

from datetime import time, timedelta
from typing import Any

from django.contrib.gis.geos import Point
from django.core.management.base import BaseCommand, CommandError
from django.db import transaction
from django.utils import timezone

from accounts.models import OTPChallenge, User
from business_hours.models import BusinessHour, TemporaryClosure
from directory.models import Category
from facilities.models import Facility, FacilityMembership
from locations.models import Province
from pharmacy_duty.models import DutyShift

CITIZEN = {"phone": "+963900777001", "password": "CitizenPass123!", "name": "مواطن الاختبار"}
MOBILE_OWNER = {"phone": "+963900777002", "password": "OwnerMobile123!", "name": "مالك الجوال"}
# The registration test creates this account; it must not exist when a run starts.
REGISTRANT_PHONE = "+963900777100"
PREFIX = "e2e-m-"
# Raqqa city centre, longitude first as PostGIS expects.
CENTRE = (39.0085, 35.9528)


class Command(BaseCommand):
    help = "Reset and create the deterministic fixtures the Android connected suite expects."

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

        phones = [CITIZEN["phone"], MOBILE_OWNER["phone"], REGISTRANT_PHONE]
        Facility.objects.filter(name_ar__startswith=PREFIX).delete()
        User.objects.filter(phone__in=phones).delete()
        OTPChallenge.objects.filter(phone__in=phones).delete()

        self._account(CITIZEN)
        self._account(MOBILE_OWNER)

        pharmacy = Category.objects.get(code="pharmacy")
        raqqa = Province.objects.get(code="raqqa")
        now = timezone.now()

        def public(index: int, label: str) -> Facility:
            longitude, latitude = CENTRE
            return Facility.objects.create(
                category=pharmacy,
                province=raqqa,
                name_ar=f"{PREFIX}صيدلية {index} {label}",
                phone="+963933000000",
                address_ar="شارع الاختبار",
                # Each one a little further north, so distance orders them 1..5.
                location=Point(longitude, latitude + 0.002 * index, srid=4326),
                status=Facility.Status.ACTIVE,
                activated_at=now,
            )

        open_all_day = public(1, "مفتوحة")
        for weekday in range(7):
            BusinessHour.objects.create(
                facility=open_all_day, weekday=weekday, opens_at=time(0, 0), closes_at=time(23, 59)
            )

        on_duty = public(2, "مناوبة")
        DutyShift.objects.create(
            facility=on_duty, starts_at=now - timedelta(hours=1), ends_at=now + timedelta(hours=6)
        )

        closed_for_now = public(3, "مغلقة مؤقتا")
        for weekday in range(7):
            BusinessHour.objects.create(
                facility=closed_for_now,
                weekday=weekday,
                opens_at=time(0, 0),
                closes_at=time(23, 59),
            )
        TemporaryClosure.objects.create(
            facility=closed_for_now,
            starts_at=now - timedelta(hours=1),
            ends_at=now + timedelta(hours=6),
            reason="صيانة",
        )

        split_day = public(4, "فترتان")
        BusinessHour.objects.create(
            facility=split_day, weekday=0, opens_at=time(16, 0), closes_at=time(22, 0), sort_order=1
        )
        BusinessHour.objects.create(
            facility=split_day, weekday=0, opens_at=time(8, 0), closes_at=time(12, 0), sort_order=0
        )

        public(5, "مغلقة")

        owner = User.objects.get(phone=MOBILE_OWNER["phone"])
        # Public and owned by the mobile owner, so the realtime test can change its
        # availability through the owner API and watch the event arrive. The other five are
        # never touched, so the discovery tests can rely on their state.
        broadcast = public(6, "للبث")
        FacilityMembership.objects.create(
            facility=broadcast, user=owner, role=FacilityMembership.Role.OWNER
        )

        owned = Facility.objects.create(
            category=pharmacy,
            province=raqqa,
            name_ar=f"{PREFIX}صيدلية المالك",
            status=Facility.Status.DRAFT,
        )
        FacilityMembership.objects.create(
            facility=owned, user=owner, role=FacilityMembership.Role.OWNER
        )

        self.stdout.write(self.style.SUCCESS("mobile fixtures ready: 2 accounts, 7 facilities"))

    @staticmethod
    def _account(spec: dict[str, str]) -> User:
        user = User.objects.create_user(phone=spec["phone"], name=spec["name"])
        user.set_password(spec["password"])
        user.is_active = True
        user.save()
        return user
