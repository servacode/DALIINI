"""Give one account the owner role: how the first operator of a new deployment is appointed.

    manage.py grant_operator 0912345678
    manage.py grant_operator 0912345678 --create --name "اسم المدير"

Every operator after the first is appointed from the console (المستخدمون ← الأدوار), by
someone who holds `admin.roles.manage`; this command is for the moment nobody does yet, and
for getting back in when nobody can (DECISION-072).

The account normally exists already: the person registered in the app with their number.
`--create` makes it here instead, with a password typed at a prompt and never on the command
line, where shell history and the process list would keep it. The owner role is brought up to
every permission first, so it is whole even on a database `migrate` has not touched since.
"""

from getpass import getpass
from typing import Any

from django.contrib.auth.password_validation import validate_password
from django.core.exceptions import ValidationError
from django.core.management.base import BaseCommand, CommandError
from django.db import transaction

from accounts.models import User, UserAdminRole
from accounts.phone import normalize_syrian_phone
from accounts.roles import sync_owner_role
from audit.services import record_audit


class Command(BaseCommand):
    help = "Grant the owner role (every console permission) to one account, by phone number."

    def add_arguments(self, parser: Any) -> None:
        parser.add_argument("phone", help="The account's Syrian mobile number, in any usual form.")
        parser.add_argument(
            "--create",
            action="store_true",
            help="Create the account when it does not exist (asks for the password).",
        )
        parser.add_argument("--name", default="", help="The new account's name, with --create.")

    def handle(self, *args: Any, **options: Any) -> None:
        try:
            phone = normalize_syrian_phone(options["phone"])
        except ValueError as exc:
            raise CommandError("Not a Syrian mobile number (09XXXXXXXX or +9639XXXXXXXX).") from exc

        user = User.objects.filter(phone=phone).first()
        if user is None:
            if not options["create"]:
                raise CommandError(
                    f"No account has {phone}. Register it in the app first, or add --create."
                )
            user = self._create(phone, options["name"].strip())
        elif not user.is_active:
            raise CommandError(f"The account {phone} is blocked; unblock it before granting.")

        with transaction.atomic():
            role = sync_owner_role()
            link, created = UserAdminRole.objects.get_or_create(user=user, role=role)
            already = link.active and not created
            if not link.active:
                link.active = True
                link.save(update_fields=["active"])
            if not already:
                record_audit(
                    actor=None,
                    action="admin_role.granted_from_shell",
                    target=user,
                    after_snapshot={"role": role.code},
                    metadata={"command": "grant_operator"},
                )

        if already:
            self.stdout.write(f"{phone} already holds the owner role; its permissions are current.")
        else:
            self.stdout.write(self.style.SUCCESS(f"{phone} now holds the owner role."))

    def _create(self, phone: str, name: str) -> User:
        if not name:
            raise CommandError("--create needs --name.")
        password = getpass("Password: ")
        if password != getpass("Password again: "):
            raise CommandError("The two passwords differ.")
        candidate = User(phone=phone, name=name)
        try:
            validate_password(password, user=candidate)
        except ValidationError as exc:
            raise CommandError(" ".join(exc.messages)) from exc
        return User.objects.create_user(phone=phone, password=password, name=name)
