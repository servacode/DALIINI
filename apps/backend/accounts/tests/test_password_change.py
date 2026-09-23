"""Changing a password.

The caller proves the old one, the new one has to be different and has to pass the project's
validators, and every session ends — the reason to change a password is usually that someone
else may have had it.
"""

from datetime import timedelta

import pytest
from django.utils import timezone
from rest_framework.test import APIClient

from sessions.models import UserSession

CHANGE = "/api/v1/account/password/"


@pytest.fixture
def signed_in(db, user):
    client = APIClient()
    client.force_authenticate(user=user)
    return client


@pytest.mark.django_db
def test_the_password_is_replaced_and_every_session_ends(signed_in, user):
    session = UserSession.objects.create(
        user=user,
        refresh_digest="digest-for-the-test",
        platform="ANDROID",
        device_name="qa",
        expires_at=timezone.now() + timedelta(days=30),
    )

    response = signed_in.post(
        CHANGE,
        {"currentPassword": "StrongPass123!", "newPassword": "EvenStronger456!"},
        format="json",
    )

    assert response.status_code == 204
    user.refresh_from_db()
    assert user.check_password("EvenStronger456!")
    session.refresh_from_db()
    assert session.revoked_at is not None


@pytest.mark.django_db
def test_the_wrong_current_password_changes_nothing(signed_in, user):
    response = signed_in.post(
        CHANGE,
        {"currentPassword": "NotThePassword1!", "newPassword": "EvenStronger456!"},
        format="json",
    )

    assert response.status_code == 400
    user.refresh_from_db()
    assert user.check_password("StrongPass123!")


@pytest.mark.django_db
def test_the_new_password_must_differ_and_must_be_strong(signed_in, user):
    same = signed_in.post(
        CHANGE,
        {"currentPassword": "StrongPass123!", "newPassword": "StrongPass123!"},
        format="json",
    )
    weak = signed_in.post(
        CHANGE,
        {"currentPassword": "StrongPass123!", "newPassword": "12345678"},
        format="json",
    )

    assert same.status_code == 400
    assert weak.status_code == 400
    user.refresh_from_db()
    assert user.check_password("StrongPass123!")


@pytest.mark.django_db
def test_changing_a_password_needs_an_account():
    assert APIClient().post(
        CHANGE,
        {"currentPassword": "a", "newPassword": "b"},
        format="json",
    ).status_code in (401, 403)
