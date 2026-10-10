"""A wrong code counts against its challenge, and enough of them close it.

The count used to be written inside the transaction that the refusal then rolled back, so it
never moved: a challenge took guesses without end. These prove the count holds and the door
shuts.
"""

from collections.abc import Iterator

import pytest
from django.core.cache import cache
from rest_framework.test import APIClient

from accounts.models import OTPChallenge
from accounts.otp import otp_digest
from locations.models import Province

START = "/api/v1/auth/register/start/"
VERIFY = "/api/v1/auth/register/verify/"
PHONE = "+963900444091"


@pytest.fixture(autouse=True)
def fresh_allowance() -> Iterator[None]:
    cache.clear()
    yield
    cache.clear()


@pytest.fixture
def challenge(db: None) -> OTPChallenge:
    province = Province.objects.create(code="test-province", name_ar="محافظة", active=True)
    response = APIClient().post(
        START, {"phone": PHONE, "provinceId": str(province.pk)}, format="json"
    )
    assert response.status_code == 202
    found = OTPChallenge.objects.get(pk=response.data["challengeId"])
    found.otp_digest = otp_digest(challenge_id=found.pk, code="246810")
    found.save(update_fields=["otp_digest"])
    return found


def verify(challenge: OTPChallenge, code: str) -> int:
    # Each try from a fresh allowance: the throttle is a separate guard, not the one tested.
    cache.clear()
    response = APIClient().post(
        VERIFY, {"challengeId": str(challenge.pk), "code": code}, format="json"
    )
    return response.status_code


@pytest.mark.django_db
def test_a_wrong_code_is_counted(challenge: OTPChallenge) -> None:
    assert verify(challenge, "111111") == 400

    challenge.refresh_from_db()
    assert challenge.attempt_count == 1
    assert challenge.verified_at is None


@pytest.mark.django_db
def test_enough_wrong_codes_close_the_challenge_even_to_the_right_one(
    challenge: OTPChallenge,
) -> None:
    for _ in range(challenge.max_attempts):
        assert verify(challenge, "111111") == 400

    assert verify(challenge, "246810") == 400
    challenge.refresh_from_db()
    assert challenge.attempt_count == challenge.max_attempts
    assert challenge.verified_at is None


@pytest.mark.django_db
def test_the_right_code_after_a_wrong_one_still_proves_the_number(
    challenge: OTPChallenge,
) -> None:
    assert verify(challenge, "111111") == 400
    assert verify(challenge, "246810") == 200

    challenge.refresh_from_db()
    assert challenge.verified_at is not None
