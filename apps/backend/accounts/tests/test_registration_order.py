"""The number is proved first, and only then is anything written down about the person.

Registration used to take a name before it took a code. A name given to a challenge that is
never completed is a name stored for nothing, and asking for it first makes the reader fill a
form before they know whether they can even receive the code.
"""

import pytest
from rest_framework.test import APIClient

from accounts.models import OTPChallenge, User
from accounts.otp import otp_digest
from locations.models import Province

START = "/api/v1/auth/register/start/"
VERIFY = "/api/v1/auth/register/verify/"
COMPLETE = "/api/v1/auth/register/complete/"
PHONE = "+963900444001"


@pytest.fixture
def province(db) -> Province:
    return Province.objects.create(code="test-province", name_ar="محافظة", active=True)


@pytest.fixture
def client() -> APIClient:
    return APIClient()


def start(client, province, phone=PHONE):
    return client.post(START, {"phone": phone, "provinceId": str(province.pk)}, format="json")


def prove(challenge_id, code="123456"):
    challenge = OTPChallenge.objects.get(pk=challenge_id)
    challenge.otp_digest = otp_digest(challenge_id=challenge.pk, code=code)
    challenge.save(update_fields=["otp_digest"])
    return code


@pytest.mark.django_db
def test_a_code_is_sent_without_asking_for_a_name(client, province):
    response = start(client, province)

    assert response.status_code == 202
    challenge = OTPChallenge.objects.get(pk=response.data["challengeId"])
    # Nothing about the person is in the challenge but where the account will live.
    assert "displayName" not in challenge.metadata
    assert challenge.metadata["provinceId"] == str(province.pk)


@pytest.mark.django_db
def test_a_name_sent_with_the_code_request_is_simply_not_stored(client, province):
    response = client.post(
        START,
        {"phone": PHONE, "provinceId": str(province.pk), "displayName": "لا يُحفظ"},
        format="json",
    )

    assert response.status_code == 202
    assert "displayName" not in OTPChallenge.objects.get(pk=response.data["challengeId"]).metadata


@pytest.mark.django_db
def test_the_name_arrives_with_the_password_and_opens_the_account(client, province):
    started = start(client, province)
    challenge_id = started.data["challengeId"]
    code = prove(challenge_id)
    client.post(VERIFY, {"challengeId": challenge_id, "code": code}, format="json")

    response = client.post(
        COMPLETE,
        {
            "challengeId": challenge_id,
            "displayName": "اسم كامل",
            "password": "StrongPass123!",
            "platform": "ANDROID",
            "deviceName": "qa",
        },
        format="json",
    )

    assert response.status_code == 201
    user = User.objects.get(phone=PHONE)
    assert user.name == "اسم كامل"
    assert user.province_id == province.pk
    assert user.phone_verified_at is not None


@pytest.mark.django_db
def test_completing_without_a_name_is_refused(client, province):
    started = start(client, province)
    challenge_id = started.data["challengeId"]
    code = prove(challenge_id)
    client.post(VERIFY, {"challengeId": challenge_id, "code": code}, format="json")

    response = client.post(
        COMPLETE,
        {"challengeId": challenge_id, "password": "StrongPass123!"},
        format="json",
    )

    assert response.status_code == 400
    assert not User.objects.filter(phone=PHONE).exists()


@pytest.mark.django_db
def test_a_code_that_was_never_proved_cannot_open_an_account(client, province):
    started = start(client, province)

    response = client.post(
        COMPLETE,
        {
            "challengeId": started.data["challengeId"],
            "displayName": "اسم كامل",
            "password": "StrongPass123!",
        },
        format="json",
    )

    assert response.status_code == 400
    assert not User.objects.filter(phone=PHONE).exists()
