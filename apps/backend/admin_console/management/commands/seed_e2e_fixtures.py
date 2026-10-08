"""Deterministic fixtures for the Admin end-to-end suite.

Separate from `launch_v1` on purpose. The launch dataset is frozen reference data that ships
to production; bending it to suit a browser test would mean production carrying rows that
exist only for testing. Everything here is created fresh, is recognisable by its `e2e-`
prefix, and is removed and rebuilt on every run so a suite never inherits the previous one's
state.

Two operators, because the most important assertion in the suite is the negative one: what a
limited operator cannot see, cannot reach by typing the URL, and cannot perform.

This command refuses to run against a database that does not look like a test database.
"""

from typing import Any

from django.core.management.base import BaseCommand, CommandError
from django.db import transaction
from django.utils import timezone

from accounts.models import AdminPermission, AdminRole, User, UserAdminRole
from directory.models import Category
from facilities.models import Facility, FacilityApplication, FacilityMembership
from locations.models import Province

FULL_OPERATOR = {"phone": "+963900111222", "password": "OperatorPass123!", "name": "مشغّل كامل"}
LIMITED_OPERATOR = {
    "phone": "+963900333444",
    "password": "LimitedPass123!",
    "name": "مشغّل محدود",
}
# Only the review queue. Everything else must be invisible and unreachable.
LIMITED_PERMISSIONS = ("admin.dashboard.read", "admin.reviews.read")

OWNER = {"phone": "+963900555666", "password": "OwnerPass123!", "name": "مالك الاختبار"}


class Command(BaseCommand):
    help = "Reset and create the deterministic fixtures the Playwright suite expects."

    def add_arguments(self, parser: Any) -> None:
        parser.add_argument(
            "--allow-any-database",
            action="store_true",
            help="Skip the safety check. Intended for CI, where the database is disposable.",
        )

    @transaction.atomic
    def handle(self, *args: Any, **options: Any) -> None:
        from django.conf import settings

        name = settings.DATABASES["default"]["NAME"]
        if not options["allow_any_database"] and "prod" in str(name).lower():
            raise CommandError(f"Refusing to seed fixtures into {name!r}.")

        # Remove anything a previous run left, so the suite always starts from the same place.
        Facility.objects.filter(name_ar__startswith="e2e-").delete()
        User.objects.filter(
            phone__in=[FULL_OPERATOR["phone"], LIMITED_OPERATOR["phone"], OWNER["phone"]]
        ).delete()
        AdminRole.objects.filter(code__startswith="e2e-").delete()

        pharmacy = Category.objects.get(code="pharmacy")
        raqqa = Province.objects.get(code="raqqa")

        full = self._operator(FULL_OPERATOR, "e2e-full", None, raqqa)
        self._operator(LIMITED_OPERATOR, "e2e-limited", LIMITED_PERMISSIONS, raqqa)
        owner = self._account(OWNER, raqqa)

        # One facility waiting in the review queue, and one already active to suspend.
        pending = Facility.objects.create(
            category=pharmacy,
            province=raqqa,
            name_ar="e2e-صيدلية قيد المراجعة",
            status=Facility.Status.SUBMITTED,
        )
        FacilityMembership.objects.create(
            facility=pending, user=owner, role=FacilityMembership.Role.OWNER
        )
        FacilityApplication.objects.create(
            facility=pending,
            kind=FacilityApplication.Kind.INITIAL,
            status=FacilityApplication.Status.SUBMITTED,
            submitted_at=timezone.now(),
            snapshot={"nameAr": "e2e-صيدلية قيد المراجعة"},
        )
        # A second pending application, so approval and rejection each have their own.
        approvable = Facility.objects.create(
            category=pharmacy,
            province=raqqa,
            name_ar="e2e-صيدلية للقبول",
            status=Facility.Status.SUBMITTED,
        )
        FacilityMembership.objects.create(
            facility=approvable, user=owner, role=FacilityMembership.Role.OWNER
        )
        FacilityApplication.objects.create(
            facility=approvable,
            kind=FacilityApplication.Kind.INITIAL,
            status=FacilityApplication.Status.SUBMITTED,
            submitted_at=timezone.now(),
            snapshot={"nameAr": "e2e-صيدلية للقبول"},
        )
        Facility.objects.create(
            category=pharmacy,
            province=raqqa,
            name_ar="e2e-صيدلية فعّالة",
            status=Facility.Status.ACTIVE,
            activated_at=timezone.now(),
        )

        self.stdout.write(
            self.style.SUCCESS(
                f"fixtures ready: full operator {full.phone}, "
                f"limited operator {LIMITED_OPERATOR['phone']}, 3 facilities"
            )
        )

    def _account(self, spec: dict[str, str], province: Province) -> User:
        # Registration will not open an account without a province and will not open one on
        # an unproved number, so neither does the seed. A fixture in a state the product
        # cannot produce sends whoever reads the console hunting a defect that is not there.
        user = User.objects.create_user(
            phone=spec["phone"], name=spec["name"], province=province
        )
        user.set_password(spec["password"])
        user.is_active = True
        user.phone_verified_at = timezone.now()
        user.save()
        return user

    def _operator(
        self,
        spec: dict[str, str],
        role_code: str,
        permissions: tuple[str, ...] | None,
        province: Province,
    ) -> User:
        user = self._account(spec, province)
        role = AdminRole.objects.create(code=role_code, name=role_code)
        codes = (
            AdminPermission.objects.values_list("code", flat=True)
            if permissions is None
            else permissions
        )
        for code in codes:
            role.permissions.add(AdminPermission.objects.get_or_create(code=code)[0])
        UserAdminRole.objects.create(user=user, role=role, active=True)
        return user
