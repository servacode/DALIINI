from __future__ import annotations

from django.db import transaction
from django.utils import timezone

from realtime.events import EventName, RealtimeEvent, ScopeType
from realtime.publisher import publish_after_commit

from .crypto import decrypt_push_token, encrypt_push_token, push_token_digest
from .models import DevicePushToken, Notification
from .providers.base import PushMessage
from .providers.factory import get_push_provider
from .sanitization import safe_notification_payload


def register_push_token(*, user, platform: str, token: str, session=None) -> DevicePushToken:
    if not token or len(token) > 4096:
        raise ValueError("Invalid push token")
    digest = push_token_digest(token)
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


def deactivate_push_token(*, user, token: str) -> int:
    return DevicePushToken.objects.filter(
        user=user,
        token_digest=push_token_digest(token),
    ).update(active=False)


@transaction.atomic
def create_notification(*, user, type: str, payload: dict) -> Notification:
    notification = Notification.objects.create(
        user=user,
        type=type,
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


def mark_notification_read(*, user, notification_id) -> Notification:
    notification = Notification.objects.get(pk=notification_id, user=user)
    if notification.read_at is None:
        notification.read_at = timezone.now()
        notification.save(update_fields=["read_at"])
    return notification


def push_notification(notification: Notification, *, title: str, body: str) -> None:
    data = {"notificationId": str(notification.id), "type": notification.type}
    for device in notification.user.push_tokens.filter(active=True):
        provider = get_push_provider(device.platform)
        provider.send(
            PushMessage(
                token=decrypt_push_token(device.token_ciphertext),
                title=title,
                body=body,
                data=data,
            )
        )
