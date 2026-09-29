"""The public duty roster by date, and the website server's separate throttle."""

from __future__ import annotations

from datetime import timedelta

import pytest
from rest_framework.test import APIClient

from core.throttles import SearchThrottle, WebServerThrottle
from directory.models import CategoryProvince
from facilities.models import Facility
from pharmacy_duty.coverage import day_bounds, local_today
from pharmacy_duty.models import DutyShift

URL = "/api/v1/public/duty/"


@pytest.fixture
def pharmacy(facility: Facility) -> Facility:
    CategoryProvince.objects.create(
        category=facility.category, province=facility.province, public_enabled=True
    )
    return facility


@pytest.mark.django_db
def test_duty_by_date_lists_each_day_a_shift_touches(pharmacy: Facility) -> None:
    tomorrow = local_today() + timedelta(days=1)
    start, _ = day_bounds(tomorrow)
    DutyShift.objects.create(
        facility=pharmacy,
        starts_at=start + timedelta(hours=20),
        ends_at=start + timedelta(hours=30),
    )
    client = APIClient()

    response = client.get(
        URL, {"provinceId": str(pharmacy.province_id), "date": tomorrow.isoformat(), "days": "3"}
    )

    assert response.status_code == 200
    assert response["Cache-Control"] == "public, max-age=60"
    days = response.json()["days"]
    assert [day["date"] for day in days] == [
        (tomorrow + timedelta(days=offset)).isoformat() for offset in range(3)
    ]
    assert [[item["id"] for item in day["items"]] for day in days] == [
        [str(pharmacy.pk)],
        [str(pharmacy.pk)],
        [],
    ]
    assert days[0]["items"][0]["nameAr"] == pharmacy.name_ar  # the public compact row
    assert days[0]["shifts"][0]["facilityId"] == str(pharmacy.pk)

    today = client.get(URL, {"provinceId": str(pharmacy.province_id)}).json()["days"]
    assert len(today) == 1 and today[0]["items"] == []

    Facility.objects.filter(pk=pharmacy.pk).update(status="SUSPENDED")
    hidden = client.get(
        URL, {"provinceId": str(pharmacy.province_id), "date": tomorrow.isoformat()}
    ).json()["days"]
    assert hidden[0]["items"] == []


@pytest.mark.django_db
def test_duty_by_date_validates_its_parameters(pharmacy: Facility) -> None:
    client = APIClient()
    province = str(pharmacy.province_id)
    assert client.get(URL).status_code == 400
    assert client.get(URL, {"provinceId": "nope"}).status_code == 400
    assert client.get(URL, {"provinceId": province, "date": "31-12-2026"}).status_code == 400
    assert client.get(URL, {"provinceId": province, "days": "8"}).status_code == 400
    assert client.get(URL, {"provinceId": province, "days": "0"}).status_code == 400


@pytest.mark.django_db
def test_web_server_key_moves_public_reads_to_their_own_throttle(
    pharmacy: Facility, settings: object, monkeypatch: pytest.MonkeyPatch
) -> None:
    settings.WEB_SERVER_API_KEY = "shared-web-secret"  # type: ignore[attr-defined]
    monkeypatch.setattr(SearchThrottle, "THROTTLE_RATES", {"search": "2/minute"})
    monkeypatch.setattr(WebServerThrottle, "THROTTLE_RATES", {"web_server": "4/minute"})
    params = {"provinceId": str(pharmacy.province_id)}
    client = APIClient()

    assert [client.get(URL, params).status_code for _ in range(3)] == [200, 200, 429]

    # The site's server shares one address with every visitor; with the key it is counted
    # under its own, higher limit, and still gets exactly the same public answer.
    trusted = [
        client.get(URL, params, HTTP_X_DALIINI_WEB_KEY="shared-web-secret").status_code
        for _ in range(5)
    ]
    assert trusted == [200, 200, 200, 200, 429]
    # A wrong key is just an anonymous caller: still limited per address.
    assert client.get(URL, params, HTTP_X_DALIINI_WEB_KEY="guess").status_code == 429


@pytest.mark.django_db
def test_web_server_key_is_ignored_when_unset(
    pharmacy: Facility, settings: object, monkeypatch: pytest.MonkeyPatch
) -> None:
    settings.WEB_SERVER_API_KEY = ""  # type: ignore[attr-defined]
    monkeypatch.setattr(SearchThrottle, "THROTTLE_RATES", {"search": "1/minute"})
    params = {"provinceId": str(pharmacy.province_id)}
    client = APIClient()
    assert client.get(URL, params, HTTP_X_DALIINI_WEB_KEY="").status_code == 200
    assert client.get(URL, params, HTTP_X_DALIINI_WEB_KEY="").status_code == 429
