"""A blocked account is told it is blocked — by its own password, and to no one else.

It answered «wrong number or password», so its owner chased a password that was right.
"""

from typing import Any

import pytest
from django.core.cache import cache
from rest_framework.test import APIClient

from accounts.models import User

LOGIN = "/api/v1/auth/login/"
PHONE = "+963900444093"
PASSWORD = "Daliini2026x"


@pytest.fixture
def blocked(db: None) -> User:
    cache.clear()
    user = User.objects.create_user(phone=PHONE, password=PASSWORD, name="محظور")
    user.is_active = False
    user.save(update_fields=["is_active"])
    return user


def sign_in(password: str) -> tuple[int, dict[str, Any]]:
    response = APIClient().post(
        LOGIN,
        {"phone": PHONE, "password": password, "platform": "ANDROID", "deviceName": "test"},
        format="json",
    )
    return response.status_code, response.json()


@pytest.mark.django_db
def test_the_right_password_learns_the_account_is_blocked(blocked: User) -> None:
    status, body = sign_in(PASSWORD)

    assert status == 403
    assert body["code"] == "ACCOUNT_BLOCKED"


@pytest.mark.django_db
def test_a_wrong_password_learns_nothing_more(blocked: User) -> None:
    status, body = sign_in("Wrong2026x")

    assert status == 401
    assert body["code"] != "ACCOUNT_BLOCKED"


@pytest.mark.django_db
def test_many_addresses_taking_turns_at_one_number_are_stopped(blocked: User) -> None:
    """The per-address limit alone let a crowd of addresses guess one account's password."""
    codes = []
    for attempt in range(31):
        response = APIClient(REMOTE_ADDR=f"10.0.{attempt // 250}.{attempt % 250 + 1}").post(
            LOGIN,
            {"phone": PHONE, "password": "Wrong2026x", "platform": "ANDROID", "deviceName": "t"},
            format="json",
        )
        codes.append(response.status_code)

    assert 429 not in codes[:30]
    assert codes[30] == 429
