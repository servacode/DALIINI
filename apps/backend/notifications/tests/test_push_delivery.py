"""Push delivery reliability: permanent failures never retry, retries never double-send."""

import pytest
from celery.exceptions import Retry  # type: ignore[import-untyped]
from django.core.exceptions import ImproperlyConfigured

from accounts.models import User
from notifications.models import DevicePushToken, Notification, NotificationPushDelivery
from notifications.providers.base import InvalidPushToken, PushMessage, TransientPushError
from notifications.services import register_push_token
from notifications.tasks import deliver_notification_push, retry_countdown


class Recording:
    def __init__(self, fail_with: Exception | None = None) -> None:
        self.sent: list[str] = []
        self.fail_with = fail_with

    def send(self, message: PushMessage) -> None:
        if self.fail_with is not None:
            raise self.fail_with
        self.sent.append(message.token)


@pytest.fixture
def notification(user: User) -> Notification:
    register_push_token(user=user, platform="ANDROID", token="tok-a")
    register_push_token(user=user, platform="ANDROID", token="tok-b")
    return Notification.objects.create(user=user, type="application.changed")


@pytest.mark.django_db
def test_a_second_run_does_not_send_twice(
    notification: Notification, monkeypatch: pytest.MonkeyPatch
) -> None:
    provider = Recording()
    monkeypatch.setattr("notifications.services.get_push_provider", lambda platform: provider)

    deliver_notification_push.apply(args=[str(notification.pk), "t", "b"])
    deliver_notification_push.apply(args=[str(notification.pk), "t", "b"])

    assert sorted(provider.sent) == ["tok-a", "tok-b"]
    assert NotificationPushDelivery.objects.filter(notification=notification).count() == 2


@pytest.mark.django_db
def test_an_invalid_token_is_deactivated_without_retry(
    notification: Notification, monkeypatch: pytest.MonkeyPatch
) -> None:
    provider = Recording(fail_with=InvalidPushToken())
    monkeypatch.setattr("notifications.services.get_push_provider", lambda platform: provider)

    result = deliver_notification_push.apply(args=[str(notification.pk), "t", "b"])

    assert result.successful()
    assert not DevicePushToken.objects.filter(active=True).exists()


@pytest.mark.django_db
def test_a_misconfigured_provider_is_permanent(
    notification: Notification, monkeypatch: pytest.MonkeyPatch
) -> None:
    def broken(platform: str) -> Recording:
        raise ImproperlyConfigured("no transport")

    monkeypatch.setattr("notifications.services.get_push_provider", broken)

    result = deliver_notification_push.apply(args=[str(notification.pk), "t", "b"])

    assert result.successful()
    assert DevicePushToken.objects.filter(active=True).count() == 2


@pytest.mark.django_db
def test_a_transient_failure_retries(
    notification: Notification, monkeypatch: pytest.MonkeyPatch
) -> None:
    provider = Recording(fail_with=TransientPushError("timeout"))
    monkeypatch.setattr("notifications.services.get_push_provider", lambda platform: provider)
    retries: list[int] = []

    def fake_retry(*args: object, **kwargs: object) -> Retry:
        retries.append(int(str(kwargs["countdown"])))
        return Retry()

    monkeypatch.setattr(deliver_notification_push, "retry", fake_retry)

    with pytest.raises(Retry):
        deliver_notification_push.run(str(notification.pk), "t", "b")

    assert len(retries) == 1 and retries[0] >= 1
    assert DevicePushToken.objects.filter(active=True).count() == 2


@pytest.mark.django_db
def test_a_missing_notification_is_not_an_error() -> None:
    result = deliver_notification_push.apply(
        args=["00000000-0000-0000-0000-000000000000", "t", "b"]
    )
    assert result.successful()


def test_backoff_is_capped_and_jittered() -> None:
    for attempt in range(10):
        assert 1 <= retry_countdown(attempt) <= 1800
