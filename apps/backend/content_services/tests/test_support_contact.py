"""How people reach the team: one source the site and the app both read (DECISION-102)."""

import pytest
from django.test import override_settings
from rest_framework.test import APIClient

URL = "/api/v1/public/support/"


@pytest.mark.django_db
@override_settings(SUPPORT_WHATSAPP="0933 123 456", SUPPORT_EMAIL="help@example.org")
def test_the_number_is_canonical_and_comes_with_a_chat_link() -> None:
    body = APIClient().get(URL).json()

    assert body == {
        "whatsapp": "+963933123456",
        "whatsappLink": "https://wa.me/963933123456",
        "email": "help@example.org",
    }


@pytest.mark.django_db
@override_settings(SUPPORT_WHATSAPP="", SUPPORT_EMAIL="")
def test_nothing_configured_says_null_rather_than_an_empty_string() -> None:
    assert APIClient().get(URL).json() == {"whatsapp": None, "whatsappLink": None, "email": None}


@pytest.mark.django_db
@override_settings(SUPPORT_WHATSAPP="not a number")
def test_a_malformed_number_is_left_out_rather_than_shown() -> None:
    body = APIClient().get(URL).json()

    assert body["whatsapp"] is None
    assert body["whatsappLink"] is None
