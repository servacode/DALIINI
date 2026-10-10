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


@pytest.mark.django_db
@override_settings(SUPPORT_WHATSAPP="0933 111 111")
def test_the_consoles_number_wins_over_the_environment() -> None:
    from platform_settings.models import PlatformSetting

    PlatformSetting.objects.update_or_create(
        key="support.whatsapp", defaults={"value_type": "STRING", "value": "+963944222222"}
    )

    assert APIClient().get(URL).json()["whatsapp"] == "+963944222222"


@pytest.mark.django_db
def test_the_console_refuses_a_number_that_is_not_one(admin_api) -> None:  # type: ignore[no-untyped-def]
    client = admin_api("admin.settings.read", "admin.settings.manage")

    refused = client.put(
        "/api/v1/admin/settings/",
        {"key": "support.whatsapp", "type": "STRING", "value": "call me"},
        format="json",
    )
    saved = client.put(
        "/api/v1/admin/settings/",
        {"key": "support.whatsapp", "type": "STRING", "value": "0944 222 222"},
        format="json",
    )

    assert refused.status_code == 400
    assert saved.status_code == 200
    assert saved.json()["value"] == "+963944222222"
