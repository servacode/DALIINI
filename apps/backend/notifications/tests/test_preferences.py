"""Which notices an account wants pushed; the inbox keeps everything (DECISION-067)."""

from typing import Any

import pytest
from rest_framework.test import APIClient

from accounts.models import User
from accounts.services import create_session
from notifications.models import Notification
from notifications.preferences import category_of
from notifications.providers.base import PushMessage
from notifications.services import push_notification, send_broadcast
from notifications.tasks import fan_out_broadcast_push

PREFERENCES = "/api/v1/account/notification-preferences/"
TOKEN = "fcm-token-" + "b" * 140


def _signed_in(user: User) -> APIClient:
    session = create_session(user=user, platform="ANDROID", device_name="test")
    client = APIClient()
    client.credentials(HTTP_AUTHORIZATION=f"Bearer {session['accessToken']}")
    return client


class Recording:
    sent: list[PushMessage] = []

    def send(self, message: PushMessage) -> None:
        Recording.sent.append(message)


def test_the_categories_are_the_apps_own() -> None:
    assert category_of("duty.gap_nudge") == "dutyReminders"
    assert category_of("duty.shift.admin_changed") is None
    assert category_of("facility.application.approved") == "applicationStatus"
    assert category_of("facility.hours.confirm_request") == "applicationStatus"
    assert category_of("platform.broadcast") == "provinceNews"
    assert category_of("something.new") is None


@pytest.mark.django_db
def test_everything_is_on_until_turned_off_and_only_sent_fields_change(user: User) -> None:
    client = _signed_in(user)

    assert client.get(PREFERENCES).json() == {
        "dutyReminders": True,
        "provinceNews": True,
        "applicationStatus": True,
    }
    changed = client.patch(PREFERENCES, {"provinceNews": False}, format="json").json()

    assert changed == {"dutyReminders": True, "provinceNews": False, "applicationStatus": True}
    assert APIClient().get(PREFERENCES).status_code == 401


@pytest.mark.django_db
def test_a_muted_kind_stays_in_the_inbox_and_is_not_pushed(
    user: User, monkeypatch: pytest.MonkeyPatch
) -> None:
    client = _signed_in(user)
    client.put(
        "/api/v1/account/push-token/", {"platform": "ANDROID", "token": TOKEN}, format="json"
    )
    client.patch(PREFERENCES, {"dutyReminders": False}, format="json")
    monkeypatch.setattr("notifications.services.get_push_provider", lambda platform: Recording())
    Recording.sent = []
    muted = Notification.objects.create(user=user, type="duty.gap_nudge")
    roster = Notification.objects.create(user=user, type="duty.shift.admin_changed")

    push_notification(muted, title="t", body="b")
    push_notification(roster, title="t", body="b")

    # The staff change to the owner's own roster is always pushed; the reminder is not.
    assert len(Recording.sent) == 1
    assert Notification.objects.filter(user=user).count() == 2


@pytest.mark.django_db
def test_a_broadcast_skips_the_accounts_that_turned_province_news_off(
    user: User, monkeypatch: pytest.MonkeyPatch
) -> None:
    other = User.objects.create_user(phone="+963933999000", password="x" * 12, name="B")
    for account in (user, other):
        _signed_in(account).put(
            "/api/v1/account/push-token/",
            {"platform": "ANDROID", "token": TOKEN + account.phone[-3:]},
            format="json",
        )
    _signed_in(other).patch(PREFERENCES, {"provinceNews": False}, format="json")
    queued: list[Any] = []
    monkeypatch.setattr(
        "notifications.tasks.deliver_notification_push.delay", lambda *args: queued.append(args)
    )

    broadcast = send_broadcast(actor=user, title_ar="خبر", body_ar="نص", audience="ALL")
    count = fan_out_broadcast_push(str(broadcast.pk))

    assert broadcast.recipient_count == 2
    assert count == 1
