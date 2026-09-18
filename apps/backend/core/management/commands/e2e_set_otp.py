"""Give an OTP challenge a known code, for connected tests only.

The development and test OTP providers deliberately deliver nothing and log nothing, so a
suite that drives registration or recovery through the real API has no way to learn the code
the backend generated. This command replaces the stored digest with the digest of a code the
test chooses. It never reveals the generated code, and it refuses to run with a real OTP
provider or against a database that does not look like a test database.
"""

from typing import Any

from django.conf import settings
from django.core.management.base import BaseCommand, CommandError

from accounts.models import OTPChallenge
from accounts.otp import otp_digest


class Command(BaseCommand):
    help = "Set a known code on an open OTP challenge (development and test providers only)."

    def add_arguments(self, parser: Any) -> None:
        parser.add_argument("--challenge", required=True)
        parser.add_argument("--code", required=True)

    def handle(self, *args: Any, **options: Any) -> None:
        provider = str(settings.OTP_PROVIDER).lower()
        if provider not in {"development", "test"}:
            raise CommandError(f"Refusing to set a code with the {provider!r} OTP provider.")
        name = str(settings.DATABASES["default"]["NAME"])
        if "prod" in name.lower():
            raise CommandError(f"Refusing to touch {name!r}.")
        code = str(options["code"])
        if not (code.isdigit() and len(code) == 6):
            raise CommandError("The code must be six digits.")

        challenge = OTPChallenge.objects.filter(
            pk=options["challenge"], consumed_at__isnull=True
        ).first()
        if challenge is None:
            raise CommandError("No open challenge with that id.")
        challenge.otp_digest = otp_digest(challenge_id=challenge.pk, code=code)
        challenge.save(update_fields=["otp_digest"])
        self.stdout.write("code set")
