"""Delete one facility outright, by its exact Arabic name: for test data, never for a real place.

    manage.py delete_facility "صيدلية الامل" --confirm "صيدلية الامل"

A real facility is closed from the console, and its history kept. This is for the rows a launch
rehearsal left behind (DECISION-127): the facility, its hours, ratings, favourites, memberships
and applications go with it, and its photographs and verification files are removed from storage.
The name must be given twice, and must match exactly one facility, so a typo deletes nothing.
"""

from __future__ import annotations

from typing import Any

from django.core.management.base import BaseCommand, CommandError
from django.db import transaction

from audit.services import record_audit
from facilities.models import Facility, FacilityImage, VerificationEvidence
from storage.backends import PrivateS3Storage, PublicS3Storage


class Command(BaseCommand):
    help = "Delete one test facility, with its files, by its exact Arabic name."

    def add_arguments(self, parser: Any) -> None:
        parser.add_argument("name", help="The facility's exact Arabic name.")
        parser.add_argument("--confirm", required=True, help="The same name again.")

    def handle(self, *args: Any, **options: Any) -> None:
        name = options["name"].strip()
        if options["confirm"].strip() != name:
            raise CommandError("--confirm does not repeat the name; nothing was deleted.")
        matches = list(Facility.objects.filter(name_ar=name))
        if len(matches) != 1:
            raise CommandError(
                f"{len(matches)} facilities are named {name!r}; nothing was deleted."
            )
        facility = matches[0]
        public = list(
            FacilityImage.objects.filter(facility=facility).values_list("storage_key", flat=True)
        )
        private = list(
            VerificationEvidence.objects.filter(facility=facility).values_list(
                "storage_key", flat=True
            )
        )
        with transaction.atomic():
            record_audit(
                actor=None,
                action="facility.deleted_from_shell",
                target=facility,
                before_snapshot={"nameAr": facility.name_ar, "status": facility.status},
                metadata={"command": "delete_facility"},
            )
            facility.delete()
        for storage, keys in ((PublicS3Storage(), public), (PrivateS3Storage(), private)):
            for key in keys:
                try:
                    storage.delete(key)
                except Exception:  # noqa: BLE001 - a file already gone is not a failure
                    pass
        self.stdout.write(
            self.style.SUCCESS(
                f"Deleted {name}: {len(public)} photographs, {len(private)} verification files."
            )
        )
