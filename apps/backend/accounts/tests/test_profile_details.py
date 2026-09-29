"""The three things a person can now say about themselves, and the one they must prove.

An address and a picture are theirs to write and to remove. A phone number is the account's
identity, so changing it is not a field: it is a code sent to the number being claimed, and
proving it ends every session, the caller's included.
"""

from datetime import timedelta
from io import BytesIO
from uuid import uuid4

import pytest
from django.utils import timezone
from PIL import Image
from rest_framework.test import APIClient

from accounts.models import OTPChallenge, User
from accounts.otp import otp_digest
from facilities.media import safe_reencode_image
from sessions.models import UserSession

PROFILE = "/api/v1/account/profile/"
IMAGE = "/api/v1/account/profile/image/"
PHONE_START = "/api/v1/account/phone/start/"
PHONE_CONFIRM = "/api/v1/account/phone/confirm/"


class _Storage:
    """Stands in for the object store; these tests are about the endpoint, not about MinIO."""

    def url(self, key: str) -> str:
        return f"https://media.invalid/{key}"

    def delete(self, key: str) -> None:
        return None


@pytest.fixture
def signed_in(db, user, monkeypatch):
    # The bytes are still decoded and re-encoded, so an upload that is not an image is still
    # refused for the real reason; only the write to the store is stood in for.
    def save(*, user_id, upload):
        safe_reencode_image(upload)
        return _Storage(), f"accounts/{user_id}/avatar/{uuid4().hex}.jpg"

    monkeypatch.setattr("accounts.views.save_profile_image", save)
    monkeypatch.setattr("accounts.views.delete_profile_image", lambda key: None)
    client = APIClient()
    client.force_authenticate(user=user)
    return client


@pytest.fixture
def new_phone(request):
    """A number of this test's own.

    The start throttle is keyed by caller and number, and the cache outlives a test, so tests
    that shared one number spent each other's allowance and read as failures.
    """
    digits = f"{abs(hash(request.node.name)) % 10_000_000:07d}"
    return f"+9639{digits[:8].ljust(8, '0')}"


def a_png(size=(40, 30)) -> BytesIO:
    buffer = BytesIO()
    Image.new("RGB", size, (10, 120, 90)).save(buffer, format="PNG")
    buffer.seek(0)
    buffer.name = "photo.png"
    return buffer


def a_session(user) -> UserSession:
    return UserSession.objects.create(
        user=user,
        refresh_digest="digest-for-the-test",
        platform="ANDROID",
        device_name="qa",
        expires_at=timezone.now() + timedelta(days=30),
    )


# --- the address -----------------------------------------------------------


@pytest.mark.django_db
def test_an_address_is_written_and_read_back(signed_in, user):
    response = signed_in.patch(PROFILE, {"address": "حي الأمين، خلف الجامع"}, format="json")

    assert response.status_code == 200
    assert response.data["address"] == "حي الأمين، خلف الجامع"
    user.refresh_from_db()
    assert user.address == "حي الأمين، خلف الجامع"


@pytest.mark.django_db
def test_an_account_that_has_given_no_address_reads_as_empty_not_missing(signed_in):
    response = signed_in.get(PROFILE)

    assert response.status_code == 200
    assert response.data["address"] == ""
    assert response.data["profileImageUrl"] is None


@pytest.mark.django_db
def test_an_address_can_be_cleared(signed_in, user):
    signed_in.patch(PROFILE, {"address": "حي الأمين"}, format="json")

    response = signed_in.patch(PROFILE, {"address": ""}, format="json")

    assert response.status_code == 200
    assert response.data["address"] == ""


@pytest.mark.django_db
def test_a_patch_that_says_nothing_about_the_address_leaves_it_alone(signed_in, user):
    signed_in.patch(PROFILE, {"address": "حي الأمين"}, format="json")

    response = signed_in.patch(PROFILE, {"displayName": "اسم آخر"}, format="json")

    assert response.data["displayName"] == "اسم آخر"
    assert response.data["address"] == "حي الأمين"


# --- the picture -----------------------------------------------------------


@pytest.mark.django_db
def test_a_picture_is_stored_and_its_url_is_returned(signed_in, user):
    response = signed_in.put(IMAGE, {"file": a_png()}, format="multipart")

    assert response.status_code == 200
    assert response.data["profileImageUrl"]
    user.refresh_from_db()
    assert user.profile_image_key.startswith(f"accounts/{user.pk}/avatar/")
    # Re-encoded to JPEG whatever was sent, which is what strips the metadata a camera writes.
    assert user.profile_image_key.endswith(".jpg")


@pytest.mark.django_db
def test_the_key_does_not_carry_the_name_of_the_person(signed_in, user):
    signed_in.put(IMAGE, {"file": a_png()}, format="multipart")

    user.refresh_from_db()
    assert user.name not in user.profile_image_key
    assert user.phone not in user.profile_image_key


@pytest.mark.django_db
def test_a_second_picture_replaces_the_first(signed_in, user):
    signed_in.put(IMAGE, {"file": a_png()}, format="multipart")
    user.refresh_from_db()
    first = user.profile_image_key

    signed_in.put(IMAGE, {"file": a_png(size=(50, 50))}, format="multipart")

    user.refresh_from_db()
    assert user.profile_image_key != first


@pytest.mark.django_db
def test_a_picture_is_removed(signed_in, user):
    signed_in.put(IMAGE, {"file": a_png()}, format="multipart")

    response = signed_in.delete(IMAGE)

    assert response.status_code == 200
    assert response.data["profileImageUrl"] is None
    user.refresh_from_db()
    assert user.profile_image_key == ""


@pytest.mark.django_db
def test_a_file_that_is_not_an_image_is_refused(signed_in, user):
    text = BytesIO(b"this is not a picture")
    text.name = "photo.jpg"

    response = signed_in.put(IMAGE, {"file": text}, format="multipart")

    assert response.status_code == 400
    user.refresh_from_db()
    assert user.profile_image_key == ""


# --- the phone number ------------------------------------------------------


@pytest.mark.django_db
def test_the_code_goes_to_the_number_being_claimed(signed_in, user, new_phone):
    response = signed_in.post(PHONE_START, {"phone": new_phone}, format="json")

    assert response.status_code == 202
    challenge = OTPChallenge.objects.get(pk=response.data["challengeId"])
    assert challenge.phone == new_phone
    assert challenge.purpose == OTPChallenge.Purpose.PHONE_CHANGE
    assert challenge.metadata["userId"] == str(user.pk)
    # Nothing has moved yet: a code asked for is not a code proved.
    user.refresh_from_db()
    assert user.phone == "+963900000001"


@pytest.mark.django_db
def test_a_proved_code_moves_the_account_and_ends_every_session(signed_in, user, new_phone):
    session = a_session(user)
    started = signed_in.post(PHONE_START, {"phone": new_phone}, format="json")
    challenge = OTPChallenge.objects.get(pk=started.data["challengeId"])
    challenge.otp_digest = otp_digest(challenge_id=challenge.pk, code="123456")
    challenge.save(update_fields=["otp_digest"])

    response = signed_in.post(
        PHONE_CONFIRM,
        {"challengeId": str(challenge.pk), "code": "123456"},
        format="json",
    )

    assert response.status_code == 200
    assert response.data["phone"] == new_phone
    user.refresh_from_db()
    assert user.phone == new_phone
    assert user.phone_verified_at is not None
    session.refresh_from_db()
    assert session.revoked_at is not None


@pytest.mark.django_db
def test_a_number_another_account_already_has_is_refused(signed_in, user, new_phone):
    User.objects.create_user(phone=new_phone, password="StrongPass123!", name="Someone Else")

    response = signed_in.post(PHONE_START, {"phone": new_phone}, format="json")

    assert response.status_code == 400
    assert not OTPChallenge.objects.filter(phone=new_phone).exists()


@pytest.mark.django_db
def test_the_number_the_account_already_has_is_refused(signed_in, user):
    response = signed_in.post(PHONE_START, {"phone": user.phone}, format="json")

    assert response.status_code == 400


@pytest.mark.django_db
def test_a_wrong_code_does_not_move_the_account(signed_in, user, new_phone):
    started = signed_in.post(PHONE_START, {"phone": new_phone}, format="json")

    response = signed_in.post(
        PHONE_CONFIRM,
        {"challengeId": started.data["challengeId"], "code": "000000"},
        format="json",
    )

    assert response.status_code == 400
    user.refresh_from_db()
    assert user.phone == "+963900000001"


@pytest.mark.django_db
def test_a_code_started_for_one_account_cannot_be_spent_by_another(db, user, new_phone):
    other = User.objects.create_user(
        phone="+963900000002",
        password="StrongPass123!",
        name="Someone Else",
    )
    mine = APIClient()
    mine.force_authenticate(user=user)
    started = mine.post(PHONE_START, {"phone": new_phone}, format="json")
    challenge = OTPChallenge.objects.get(pk=started.data["challengeId"])
    challenge.otp_digest = otp_digest(challenge_id=challenge.pk, code="123456")
    challenge.save(update_fields=["otp_digest"])

    theirs = APIClient()
    theirs.force_authenticate(user=other)
    response = theirs.post(
        PHONE_CONFIRM,
        {"challengeId": str(challenge.pk), "code": "123456"},
        format="json",
    )

    assert response.status_code == 400
    other.refresh_from_db()
    assert other.phone == "+963900000002"


@pytest.mark.django_db
def test_a_code_is_good_once(signed_in, user, new_phone):
    started = signed_in.post(PHONE_START, {"phone": new_phone}, format="json")
    challenge = OTPChallenge.objects.get(pk=started.data["challengeId"])
    challenge.otp_digest = otp_digest(challenge_id=challenge.pk, code="123456")
    challenge.save(update_fields=["otp_digest"])
    signed_in.post(
        PHONE_CONFIRM,
        {"challengeId": str(challenge.pk), "code": "123456"},
        format="json",
    )

    again = signed_in.post(
        PHONE_CONFIRM,
        {"challengeId": str(challenge.pk), "code": "123456"},
        format="json",
    )

    assert again.status_code == 400


@pytest.mark.django_db
def test_a_number_that_is_not_syrian_is_refused_before_any_code_is_sent(signed_in):
    response = signed_in.post(PHONE_START, {"phone": "+15551234567"}, format="json")

    assert response.status_code == 400
    assert not OTPChallenge.objects.exists()


@pytest.mark.django_db
def test_the_account_is_reached_by_any_form_of_the_number(signed_in, user):
    # 09XXXXXXXX is how it is written on a shopfront; it means the same number.
    response = signed_in.post(PHONE_START, {"phone": "0912345678"}, format="json")

    assert response.status_code == 202
    assert OTPChallenge.objects.get(pk=response.data["challengeId"]).phone == "+963912345678"


@pytest.mark.django_db
def test_signing_out_is_not_required_to_read_a_profile(signed_in):
    response = signed_in.get(PROFILE)

    assert response.status_code == 200
    assert set(response.data) == {
        "id",
        "displayName",
        "phone",
        "provinceId",
        "phoneVerifiedAt",
        "address",
        "profileImageUrl",
    }
