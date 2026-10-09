"""A new account finds one message waiting: the platform's own.

It is written to the inbox rather than sent over WhatsApp. The registration code has to go
out over that channel; nothing else does, and every extra message from an unofficial account
raises the chance of the number being blocked, which stops registration for everyone at once.
"""

from collections.abc import Iterator
from typing import Any

import pytest
from django.core.cache import cache
from rest_framework.response import Response
from rest_framework.test import APIClient

from accounts.models import OTPChallenge, User
from accounts.otp import otp_digest
from accounts.services import WELCOME_TYPE
from locations.models import Province
from notifications.models import Notification
from notifications.preferences import category_of, push_wanted, save_preferences

START = "/api/v1/auth/register/start/"
VERIFY = "/api/v1/auth/register/verify/"
COMPLETE = "/api/v1/auth/register/complete/"
PHONE = "+963900555001"


@pytest.fixture
def province(db: None) -> Province:
    return Province.objects.create(code="test-province", name_ar="محافظة", active=True)


@pytest.fixture
def client() -> APIClient:
    return APIClient()


@pytest.fixture(autouse=True)
def spent_allowance() -> Iterator[None]:
    cache.clear()
    yield
    cache.clear()


def register(client: APIClient, province: Province, phone: str = PHONE) -> Response:
    started = client.post(START, {"phone": phone, "provinceId": str(province.pk)}, format="json")
    challenge_id = started.data["challengeId"]
    challenge = OTPChallenge.objects.get(pk=challenge_id)
    challenge.otp_digest = otp_digest(challenge_id=challenge.pk, code="123456")
    challenge.save(update_fields=["otp_digest"])
    client.post(VERIFY, {"challengeId": challenge_id, "code": "123456"}, format="json")
    return client.post(
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


@pytest.mark.django_db
def test_a_new_account_is_welcomed_in_its_inbox(client: APIClient, province: Province) -> None:
    assert register(client, province).status_code == 201

    user = User.objects.get(phone=PHONE)
    welcome = Notification.objects.get(user=user, type=WELCOME_TYPE)
    assert "دليني" in welcome.title_ar
    assert welcome.body_ar
    # Nowhere to send the reader: a welcome is read, not acted on.
    assert welcome.destination == Notification.Destination.NONE
    assert welcome.read_at is None


@pytest.mark.django_db
def test_the_welcome_is_sent_once_and_only_to_its_own_account(
    client: APIClient, province: Province
) -> None:
    register(client, province)
    register(client, province, phone="+963900555002")

    first = User.objects.get(phone=PHONE)
    second = User.objects.get(phone="+963900555002")
    assert Notification.objects.filter(user=first, type=WELCOME_TYPE).count() == 1
    assert Notification.objects.filter(user=second, type=WELCOME_TYPE).count() == 1


@pytest.mark.django_db
def test_an_account_that_muted_everything_is_still_welcomed(
    client: APIClient, province: Province
) -> None:
    """The welcome belongs to no category a reader can switch off.

    Preferences are set after the fact here because there is no account to set them on until
    registration has happened; what is being checked is the rule, not the order.
    """
    register(client, province)
    user = User.objects.get(phone=PHONE)
    save_preferences(
        user, {"dutyReminders": False, "provinceNews": False, "applicationStatus": False}
    )

    assert category_of(WELCOME_TYPE) is None
    assert push_wanted(user, WELCOME_TYPE) is True


@pytest.mark.django_db
def test_an_invitation_reaches_a_reader_who_muted_application_status(
    client: APIClient, province: Province
) -> None:
    """The backend's unmutable set must match the app's, or the reader simply never hears.

    `NotificationCategory.of` on Android treats an invitation as never muted: it is addressed
    to this person rather than news about a facility. The backend decides whether a push
    leaves, so a disagreement here is silent and one-sided.
    """
    register(client, province)
    user = User.objects.get(phone=PHONE)
    save_preferences(user, {"applicationStatus": False})

    assert push_wanted(user, "facility.invitation.received") is True
    # The rest of that family still obeys the switch.
    assert push_wanted(user, "facility.application.approved") is False


# ----------------------------------------------------------------- the WhatsApp welcome


@pytest.mark.django_db
def test_registration_queues_the_whatsapp_welcome_after_the_codes_gap(
    client: APIClient, province: Province, django_capture_on_commit_callbacks: Any
) -> None:
    """Queued once the account is committed, after the bot's per-number gap has passed."""
    from unittest import mock

    from accounts.tasks import WELCOME_DELAY_SECONDS

    with mock.patch("accounts.tasks.send_whatsapp_welcome.apply_async") as queued:
        with django_capture_on_commit_callbacks(execute=True):
            assert register(client, province).status_code == 201

    user = User.objects.get(phone=PHONE)
    queued.assert_called_once_with(args=[str(user.pk)], countdown=WELCOME_DELAY_SECONDS)
    assert WELCOME_DELAY_SECONDS > 60


@pytest.mark.django_db
def test_the_bot_is_asked_for_the_welcome_by_kind_never_by_text(province: Province) -> None:
    """The words live in the bot; the backend names the kind, so the bot sends nothing else."""
    import json
    from unittest import mock

    from django.test import override_settings

    from accounts.providers.whatsapp_bot import WhatsAppBotOtpSender
    from accounts.tasks import send_whatsapp_welcome

    user = User.objects.create_user(phone="+963900555777", name="جديد", province=province)
    sent: list[dict[str, str]] = []

    def opener(url: str, body: bytes, headers: dict[str, str]) -> tuple[int, bytes]:
        sent.append(json.loads(body))
        return 200, b'{"sent": true}'

    with override_settings(WHATSAPP_BOT_URL="http://bot:8085", WHATSAPP_BOT_TOKEN="t"):
        with mock.patch(
            "accounts.otp.get_otp_sender", return_value=WhatsAppBotOtpSender(opener=opener)
        ):
            outcome = send_whatsapp_welcome.apply(args=[str(user.pk)]).get()

    assert outcome == "sent"
    assert sent == [{"phone": "+963900555777", "kind": "welcome"}]


@pytest.mark.django_db
def test_a_number_without_whatsapp_is_let_go_quietly(province: Province) -> None:
    from unittest import mock

    from accounts.providers.base import InvalidRecipient
    from accounts.tasks import send_whatsapp_welcome

    user = User.objects.create_user(phone="+963900555778", name="جديد", province=province)
    sender = mock.Mock()
    sender.send_welcome.side_effect = InvalidRecipient("no whatsapp")

    with mock.patch("accounts.otp.get_otp_sender", return_value=sender):
        outcome = send_whatsapp_welcome.apply(args=[str(user.pk)]).get()

    assert outcome == "not_on_whatsapp"


@pytest.mark.django_db
def test_a_channel_with_no_welcome_sends_nothing(province: Province) -> None:
    """The development sender, and the Cloud API until it has an approved template."""
    from accounts.tasks import send_whatsapp_welcome

    user = User.objects.create_user(phone="+963900555779", name="جديد", province=province)

    assert send_whatsapp_welcome.apply(args=[str(user.pk)]).get() == "not_this_channel"
