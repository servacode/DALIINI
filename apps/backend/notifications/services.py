from __future__ import annotations

import logging

from django.core.exceptions import ImproperlyConfigured
from django.db import transaction
from django.utils import timezone

from realtime.events import EventName, RealtimeEvent, ScopeType
from realtime.publisher import publish_after_commit

from .crypto import decrypt_push_token, encrypt_push_token, push_token_digest
from .models import DevicePushToken, Notification, NotificationPushDelivery
from .providers.base import InvalidPushToken, PushMessage, TransientPushError
from .providers.factory import get_push_provider
from .sanitization import safe_notification_payload

logger = logging.getLogger(__name__)


def register_push_token(*, user, platform: str, token: str, session=None) -> DevicePushToken:
    if not token or len(token) > 4096:
        raise ValueError("Invalid push token")
    digest = push_token_digest(token)
    with transaction.atomic():
        if session is not None:
            # One device per session: a new token from the same session replaces the one
            # the provider rotated away, so the old token stops receiving anything.
            DevicePushToken.objects.filter(session=session, platform=platform, active=True).exclude(
                token_digest=digest
            ).update(active=False)
        obj, _ = DevicePushToken.objects.update_or_create(
            token_digest=digest,
            defaults={
                "user": user,
                "session": session,
                "platform": platform,
                "token_ciphertext": encrypt_push_token(token),
                "active": True,
            },
        )
    return obj


def deactivate_push_tokens_for_sessions(session_ids: list[object]) -> int:
    """Stop pushing to the devices of sessions that have ended."""
    return DevicePushToken.objects.filter(session_id__in=session_ids, active=True).update(
        active=False
    )


def deactivate_push_tokens_for_user(user: object) -> int:
    """Stop pushing to every device of a user whose sessions have all ended."""
    return DevicePushToken.objects.filter(user=user, active=True).update(active=False)


def deactivate_push_token(*, user, token: str) -> int:
    return DevicePushToken.objects.filter(
        user=user,
        token_digest=push_token_digest(token),
    ).update(active=False)


@transaction.atomic
def create_notification(
    *,
    user,
    type: str,
    payload: dict,
    title_ar: str = "",
    body_ar: str = "",
    destination: str = Notification.Destination.NONE,
) -> Notification:
    """Put a message in an account's inbox, and tell the account's open sessions about it.

    The words are stored with the message rather than assembled by whoever reads it, so the
    inbox says the same thing the push said, and a client that has never seen this type
    still has something to show.
    """
    if destination not in Notification.Destination.values:
        raise ValueError(f"Unsupported notification destination: {destination}")
    notification = Notification.objects.create(
        user=user,
        type=type,
        title_ar=title_ar,
        body_ar=body_ar,
        destination=destination,
        payload=safe_notification_payload(payload),
    )
    publish_after_commit(
        RealtimeEvent(
            name=EventName.USER_FACILITY_CHANGED,
            scope_type=ScopeType.USER,
            scope_id=str(user.pk),
            resource_id=str(notification.pk),
        )
    )
    return notification


def mark_all_notifications_read(*, user) -> int:
    """Everything unread becomes read at one moment. Returns how many changed."""
    return Notification.objects.filter(user=user, read_at__isnull=True).update(
        read_at=timezone.now()
    )


def unread_notification_count(*, user) -> int:
    return Notification.objects.filter(user=user, read_at__isnull=True).count()


def mark_notification_read(*, user, notification_id) -> Notification:
    notification = Notification.objects.get(pk=notification_id, user=user)
    if notification.read_at is None:
        notification.read_at = timezone.now()
        notification.save(update_fields=["read_at"])
    return notification


def push_notification(notification: Notification, *, title: str, body: str) -> None:
    """Send to every active device that has not received this notification yet.

    InvalidPushToken deactivates the device (permanent, never retried). A misconfigured
    provider is logged and skipped (permanent). TransientPushError propagates after the
    remaining devices were attempted, so the caller can retry only what is left.
    """
    data = {"notificationId": str(notification.id), "type": notification.type}
    delivered = NotificationPushDelivery.objects.filter(notification=notification).values(
        "device_id"
    )
    devices = notification.user.push_tokens.filter(active=True).exclude(id__in=delivered)
    transient: TransientPushError | None = None
    for device in devices:
        with transaction.atomic():
            # Lock the device row so two concurrent attempts cannot both send.
            locked = DevicePushToken.objects.select_for_update().filter(pk=device.pk).first()
            if locked is None or not locked.active:
                continue
            if NotificationPushDelivery.objects.filter(
                notification=notification, device=locked
            ).exists():
                continue
            try:
                provider = get_push_provider(locked.platform)
                provider.send(
                    PushMessage(
                        token=decrypt_push_token(locked.token_ciphertext),
                        title=title,
                        body=body,
                        data=data,
                    )
                )
            except InvalidPushToken:
                # The provider no longer knows this token; keeping it would only fail again.
                locked.active = False
                locked.save(update_fields=["active"])
                logger.info("push.token_deactivated", extra={"device_id": str(locked.pk)})
                continue
            except ImproperlyConfigured:
                logger.error(
                    "push.provider_misconfigured",
                    extra={"platform": locked.platform, "notification_id": str(notification.pk)},
                )
                continue
            except TransientPushError as exc:
                logger.warning(
                    "push.send_failed_transient",
                    extra={"device_id": str(locked.pk), "notification_id": str(notification.pk)},
                )
                transient = exc
                continue
            NotificationPushDelivery.objects.create(notification=notification, device=locked)
    if transient is not None:
        raise transient
