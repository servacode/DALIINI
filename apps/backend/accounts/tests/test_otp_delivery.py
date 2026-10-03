"""What a person is told when their code cannot be sent, and how often a number can be asked.

A provider failure used to escape as an unhandled exception, so the person saw "unexpected
error" and was left with a challenge row nobody could answer. And the only limits were per
caller address, so many addresses taking turns could keep the sending account busy on one
number — which, for the paired WhatsApp account, is how a number gets banned.
"""

from collections.abc import Callable

import pytest
from rest_framework.test import APIClient

from accounts.models import OTPChallenge, User
from accounts.providers.base import InvalidRecipient, TransientOtpError
from locations.models import Province

START = "/api/v1/auth/register/start/"
RECOVERY = "/api/v1/auth/recovery/start/"
PHONE = "+963900444101"


@pytest.fixture
def province(db: None) -> Province:
    return Province.objects.create(code="otp-province", name_ar="محافظة", active=True)


def _failing(error: Exception) -> Callable[..., None]:
    def send(*, phone: str, code: str) -> None:
        raise error

    return send


@pytest.mark.django_db
@pytest.mark.parametrize(
    ("error", "status", "code"),
    [
        (InvalidRecipient("not on WhatsApp"), 422, "OTP_RECIPIENT_INVALID"),
        (TransientOtpError("timeout"), 503, "OTP_DELIVERY_UNAVAILABLE"),
    ],
)
def test_a_failed_send_is_explained_and_leaves_no_challenge(
    monkeypatch: pytest.MonkeyPatch,
    province: Province,
    error: Exception,
    status: int,
    code: str,
) -> None:
    monkeypatch.setattr("accounts.services.deliver_otp", _failing(error))

    response = APIClient().post(
        START, {"phone": PHONE, "provinceId": str(province.pk)}, format="json"
    )

    assert response.status_code == status
    body = response.json()
    assert body["code"] == code
    # Said in the person's language, and nothing of the provider's own wording.
    assert "واتساب" in body["message"] or "حاول" in body["message"]
    assert "timeout" not in body["message"]
    assert not OTPChallenge.objects.filter(phone=PHONE).exists()


@pytest.mark.django_db
def test_recovery_reports_a_failed_send_the_same_way(monkeypatch: pytest.MonkeyPatch) -> None:
    User.objects.create_user(phone=PHONE, password="StrongPass123!", name="صاحب الرقم")
    monkeypatch.setattr(
        "accounts.services.deliver_otp", _failing(TransientOtpError("bot disconnected"))
    )

    response = APIClient().post(RECOVERY, {"phone": PHONE}, format="json")

    assert response.status_code == 503
    assert response.json()["code"] == "OTP_DELIVERY_UNAVAILABLE"


@pytest.mark.django_db
def test_one_number_has_an_hourly_budget_whatever_address_asks(province: Province) -> None:
    client = APIClient()
    statuses = [
        client.post(
            START,
            {"phone": PHONE, "provinceId": str(province.pk)},
            format="json",
            REMOTE_ADDR=f"10.0.0.{index}",
        ).status_code
        for index in range(1, 7)
    ]

    # Five distinct addresses are each within their own allowance; the sixth request for the
    # same number is refused because of the number, not the address.
    assert statuses[:5] == [202] * 5
    assert statuses[5] == 429


@pytest.mark.django_db
def test_the_budget_is_per_number(province: Province) -> None:
    client = APIClient()
    for index in range(5):
        client.post(
            START,
            {"phone": PHONE, "provinceId": str(province.pk)},
            format="json",
            REMOTE_ADDR=f"10.0.1.{index}",
        )

    other = client.post(
        START,
        {"phone": "+963900444102", "provinceId": str(province.pk)},
        format="json",
        REMOTE_ADDR="10.0.1.99",
    )

    assert other.status_code == 202


@pytest.mark.django_db
def test_an_invalid_number_is_refused_by_validation_not_by_the_budget(
    province: Province,
) -> None:
    client = APIClient()
    statuses = {
        client.post(
            START,
            {"phone": "not-a-number", "provinceId": str(province.pk)},
            format="json",
            REMOTE_ADDR=f"10.0.2.{index}",
        ).status_code
        for index in range(8)
    }

    assert statuses == {400}
