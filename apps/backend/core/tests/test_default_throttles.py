"""Every route has a limit, and nobody escapes theirs by naming a different address."""

from typing import Any

import pytest
from django.test import override_settings
from rest_framework.test import APIClient

from core.throttles import AnonDefaultThrottle, UserDefaultThrottle, WebServerThrottle

# A route that names no throttles of its own, so the defaults are what limit it.
PROVINCES = "/api/v1/public/provinces/"
UNREAD = "/api/v1/account/notifications/unread-count/"


@pytest.fixture
def tight(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setattr(AnonDefaultThrottle, "THROTTLE_RATES", {"anon_default": "2/minute"})
    monkeypatch.setattr(UserDefaultThrottle, "THROTTLE_RATES", {"user_default": "3/minute"})
    monkeypatch.setattr(WebServerThrottle, "THROTTLE_RATES", {"web_server": "5/minute"})


def _codes(client: APIClient, url: str, times: int, **headers: Any) -> list[int]:
    return [client.get(url, **headers).status_code for _ in range(times)]


@pytest.mark.django_db
@pytest.mark.usefixtures("tight")
def test_an_anonymous_caller_is_limited_on_a_route_with_no_limit_of_its_own() -> None:
    codes = _codes(APIClient(), PROVINCES, 3)

    assert codes == [200, 200, 429]


@pytest.mark.django_db
@pytest.mark.usefixtures("tight")
def test_a_forged_forwarded_address_does_not_buy_a_new_allowance() -> None:
    client = APIClient()
    codes = [
        client.get(PROVINCES, HTTP_X_FORWARDED_FOR=f"10.0.0.{n}").status_code for n in range(3)
    ]

    assert codes == [200, 200, 429]


@pytest.mark.django_db
@pytest.mark.usefixtures("tight")
def test_behind_a_stated_proxy_each_caller_has_their_own_allowance() -> None:
    from rest_framework.settings import api_settings

    with override_settings(REST_FRAMEWORK={**api_settings.user_settings, "NUM_PROXIES": 1}):
        client = APIClient()
        first = _codes(client, PROVINCES, 3, HTTP_X_FORWARDED_FOR="203.0.113.1")
        second = _codes(client, PROVINCES, 1, HTTP_X_FORWARDED_FOR="203.0.113.2")

    assert first == [200, 200, 429]
    assert second == [200]


@pytest.mark.django_db
@pytest.mark.usefixtures("tight")
@override_settings(WEB_SERVER_API_KEY="site-key")
def test_the_website_server_has_its_own_larger_allowance() -> None:
    codes = _codes(APIClient(), PROVINCES, 6, HTTP_X_DALIINI_WEB_KEY="site-key")

    assert codes == [200] * 5 + [429]


@pytest.mark.django_db
@pytest.mark.usefixtures("tight")
def test_a_signed_in_account_is_limited_per_account(user: Any) -> None:
    client = APIClient()
    client.force_authenticate(user=user)

    assert _codes(client, UNREAD, 4) == [200, 200, 200, 429]
