"""Reconcile the database with the launch reference dataset, without undoing an operator.

The same dataset is applied by `directory/migrations/0004_launch_baseline.py`, so a fresh
database never needs this command. It exists for the cases a migration cannot serve:
qualification runs, verifying a restored database, and recreating a canonical row that was
removed.

Re-running is safe and is meant to be safe. A row that already exists is left exactly as it
is, including `active`, `public_enabled` and `owner_registration_enabled` — those belong to
whoever changed them in the Admin. What the command does check is identity: a canonical row
carrying the wrong primary key is reported and nothing is written.

There is no `--force` and no `--reset`. A command that can flatten production configuration
should not be one keystroke away; reconciling a diverged database is a separate, audited
operation.
"""

from typing import Any

from django.apps import apps
from django.core.management.base import BaseCommand, CommandError
from django.db import transaction

from directory.reference_data import launch_v1
from directory.reference_data.apply import ReferenceIdMismatch, apply_dataset


class Command(BaseCommand):
    help = "Create any missing launch reference data. Never overwrites operator changes."

    def add_arguments(self, parser: Any) -> None:
        parser.add_argument(
            "--check",
            action="store_true",
            help=(
                "Report what is missing and exit non-zero without writing anything. "
                "Intended for a qualification gate."
            ),
        )

    def handle(self, *args: Any, **options: Any) -> None:
        check_only = options["check"]
        try:
            with transaction.atomic():
                summary = apply_dataset(launch_v1, apps.get_model)
                missing = sum(summary.created.values())
                if check_only:
                    transaction.set_rollback(True)
        except ReferenceIdMismatch as exc:
            raise CommandError(str(exc)) from exc

        self.stdout.write(f"dataset {summary.version}")
        for line in summary.lines():
            self.stdout.write(f"  {line}")

        if check_only:
            if missing:
                raise CommandError(
                    f"{missing} reference rows are missing. Nothing was written; run the "
                    f"command without --check to create them."
                )
            self.stdout.write(self.style.SUCCESS("Reference data is complete."))
            return

        if missing:
            self.stdout.write(self.style.SUCCESS(f"Created {missing} missing rows."))
        else:
            self.stdout.write(self.style.SUCCESS("Nothing to do; everything is present."))
