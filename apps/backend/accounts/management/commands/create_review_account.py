"""Open, or reset, the account a store's reviewers sign in with (DECISION-123).

    manage.py create_review_account 0912345678 --name "حساب المراجعة"

Google Play's reviewers must reach every screen behind sign-in, and they cannot receive the
WhatsApp code a registration needs. Sign-in itself is a number and a password, so they need only
an account that already exists: this opens one, verified, in the first active province, with a
password typed at a prompt and never on the command line. Run again, it sets a new password, so
the credentials given to the store can be changed at any time.

It is an ordinary account and nothing more: no console role, and the command refuses one that
has a role, so it can never be used to hand an operator's account to a stranger. Block it from
the console when the review is over.
"""

from __future__ import annotations

from getpass import getpass
from typing import Any

from django.contrib.auth.password_validation import validate_password
from django.core.exceptions import ValidationError
from django.core.management.base import BaseCommand, CommandError
from django.db import transaction
from django.utils import timezone

from accounts.models import User
from accounts.phone import normalize_syrian_phone
from audit.services import record_audit
from locations.models import Province


class Command(BaseCommand):
    help = "Open or reset the account store reviewers sign in with, by phone number."

    def add_arguments(self, parser: Any) -> None:
        parser.add_argument("phone", help="The account's Syrian mobile number, in any usual form.")
        parser.add_argument("--name", default="حساب المراجعة", help="The account's name.")

    def handle(self, *args: Any, **options: Any) -> None:
        try:
            phone = normalize_syrian_phone(options["phone"])
        except ValueError as exc:
            raise CommandError("Not a Syrian mobile number (09XXXXXXXX or +9639XXXXXXXX).") from exc

        user = User.objects.filter(phone=phone).first()
        if user is not None and user.admin_role_links.filter(active=True).exists():
            raise CommandError(f"{phone} holds a console role; a review account must not.")

        name = options["name"].strip() or "حساب المراجعة"
        password = getpass("Password: ")
        if password != getpass("Password again: "):
            raise CommandError("The two passwords differ.")
        try:
            validate_password(password, user=user or User(phone=phone, name=name))
        except ValidationError as exc:
            raise CommandError(" ".join(exc.messages)) from exc

        with transaction.atomic():
            if user is None:
                province = Province.objects.filter(active=True).order_by("sort_order", "pk").first()
                if province is None:
                    raise CommandError("No province is active; activate one first.")
                user = User.objects.create_user(
                    phone=phone,
                    password=password,
                    name=name[:120],
                    province=province,
                    phone_verified_at=timezone.now(),
                )
                action = "review_account.created_from_shell"
            else:
                user.set_password(password)
                user.is_active = True
                user.save(update_fields=["password", "is_active"])
                action = "review_account.password_reset_from_shell"
            record_audit(
                actor=None,
                action=action,
                target=user,
                metadata={"command": "create_review_account"},
            )

        verb = "opened" if action.endswith("created_from_shell") else "reset"
        self.stdout.write(self.style.SUCCESS(f"Review account {phone} {verb}."))
