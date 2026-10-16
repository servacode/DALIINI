"""The account store reviewers sign in with (DECISION-123)."""

from io import StringIO
from unittest import mock

import pytest
from django.core.management import call_command
from django.core.management.base import CommandError
from rest_framework.test import APIClient

from accounts.models import User
from accounts.roles import sync_owner_role
from audit.models import AuditEvent

PROMPT = "accounts.management.commands.create_review_account.getpass"


def _run(phone: str, password: str = "Review-Pass-2026!") -> None:
    with mock.patch(PROMPT, side_effect=[password, password]):
        call_command("create_review_account", phone, stdout=StringIO())


@pytest.mark.django_db
def test_it_opens_a_verified_account_with_no_role_that_signs_in() -> None:
    _run("0933333333")

    user = User.objects.get(phone="+963933333333")
    assert user.check_password("Review-Pass-2026!")
    assert user.phone_verified_at is not None
    assert user.province is not None and user.province.active
    assert not user.admin_role_links.exists()
    assert AuditEvent.objects.filter(action="review_account.created_from_shell").exists()

    signed_in = APIClient().post(
        "/api/v1/auth/login/",
        {"phone": "0933333333", "password": "Review-Pass-2026!"},
        format="json",
    )
    assert signed_in.status_code == 200, signed_in.content


@pytest.mark.django_db
def test_running_it_again_sets_a_new_password() -> None:
    _run("0933333333")
    _run("0933333333", "Another-Pass-2026!")

    user = User.objects.get(phone="+963933333333")
    assert user.check_password("Another-Pass-2026!")
    assert User.objects.filter(phone="+963933333333").count() == 1
    assert AuditEvent.objects.filter(action="review_account.password_reset_from_shell").exists()


@pytest.mark.django_db
def test_it_refuses_an_account_that_holds_a_console_role() -> None:
    user = User.objects.create_user(phone="+963944444444", password="x-Pass-2026!", name="مدير")
    user.admin_role_links.create(role=sync_owner_role())

    with pytest.raises(CommandError, match="console role"):
        _run("0944444444")
    user.refresh_from_db()
    assert user.check_password("x-Pass-2026!")


@pytest.mark.django_db
def test_it_refuses_a_weak_password_and_a_foreign_number() -> None:
    with pytest.raises(CommandError):
        _run("0933333333", "123")
    with pytest.raises(CommandError, match="Syrian"):
        _run("+15555550100")
    assert not User.objects.filter(phone="+963933333333").exists()
