from __future__ import annotations

import logging
import uuid
from datetime import date
from typing import TYPE_CHECKING, Any
from uuid import UUID

from django.core.exceptions import ImproperlyConfigured
from django.db import transaction
from django.db.models import QuerySet
from django.utils import timezone

from realtime.events import EventName, RealtimeEvent, ScopeType
from realtime.publisher import publish_after_commit

from .crypto import decrypt_push_token, encrypt_push_token, push_token_digest
from .models import Broadcast, DevicePushToken, Notification, NotificationPushDelivery
from .providers.base import InvalidPushToken, PushMessage, TransientPushError
from .providers.factory import get_push_provider
from .sanitization import safe_notification_payload

if TYPE_CHECKING:
    from accounts.models import User
    from sessions.models import UserSession

logger = logging.getLogger(__name__)


def register_push_token(
    *, user: User, platform: str, token: str, session: UserSession | None = None
) -> DevicePushToken:
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


def deactivate_push_tokens_for_user(user: User) -> int:
    """Stop pushing to every device of a user whose sessions have all ended."""
    return DevicePushToken.objects.filter(user=user, active=True).update(active=False)


def deactivate_push_token(*, user: User, token: str) -> int:
    return DevicePushToken.objects.filter(
        user=user,
        token_digest=push_token_digest(token),
    ).update(active=False)


@transaction.atomic
def create_notification(
    *,
    user: User,
    type: str,
    payload: dict[str, Any],
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


def mark_all_notifications_read(*, user: User) -> int:
    """Everything unread becomes read at one moment. Returns how many changed."""
    return Notification.objects.filter(user=user, read_at__isnull=True).update(
        read_at=timezone.now()
    )


def unread_notification_count(*, user: User) -> int:
    return Notification.objects.filter(user=user, read_at__isnull=True).count()


def mark_notification_read(*, user: User, notification_id: UUID | str) -> Notification:
    notification = Notification.objects.get(pk=notification_id, user=user)
    if notification.read_at is None:
        notification.read_at = timezone.now()
        notification.save(update_fields=["read_at"])
    return notification


def push_routing(payload: dict[str, Any]) -> dict[str, str]:
    """The identifiers a push may carry besides its own, so a tap lands on the right screen.

    Only these three, and only in the shape of an identifier or a day: the facility a notice
    is about, and the day and province of a duty gap. Anything else in the payload (another
    id, a name) stays on the server; the app refuses a push that carries any other key.
    """
    routing: dict[str, str] = {}
    for key in ("facilityId", "provinceId"):
        try:
            routing[key] = str(uuid.UUID(str(payload.get(key))))
        except (TypeError, ValueError):
            continue
    try:
        routing["gapDate"] = date.fromisoformat(str(payload.get("gapDate"))).isoformat()
    except (TypeError, ValueError):
        pass
    return routing


def push_notification(notification: Notification, *, title: str, body: str) -> None:
    """Send to every active device that has not received this notification yet.

    InvalidPushToken deactivates the device (permanent, never retried). A misconfigured
    provider is logged and skipped (permanent). TransientPushError propagates after the
    remaining devices were attempted, so the caller can retry only what is left.
    """
    data = {"notificationId": str(notification.id), "type": notification.type}
    data.update(push_routing(notification.payload))
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


def enqueue_push(notification: Notification) -> None:
    """Announce a stored notification on the account's devices, after the transaction commits.

    The inbox row is the record, so a broker that cannot be reached must not undo it or fail
    the request that created it: the failure is logged and the message stays in the inbox.
    """
    notification_id = str(notification.pk)
    title, body = notification.title_ar, notification.body_ar

    def _send() -> None:
        from .tasks import deliver_notification_push

        try:
            deliver_notification_push.delay(notification_id, title, body)
        except Exception:  # noqa: BLE001 - the broker is optional; the inbox is the record
            logger.warning("push.enqueue_failed", extra={"notification_id": notification_id})

    transaction.on_commit(_send)


def notify(
    *,
    user: Any,
    type: str,
    title_ar: str,
    body_ar: str,
    destination: str = Notification.Destination.NONE,
    payload: dict[str, Any] | None = None,
) -> Notification:
    """Store a notification in the inbox and queue its push."""
    notification = create_notification(
        user=user,
        type=type,
        payload=payload or {},
        title_ar=title_ar,
        body_ar=body_ar,
        destination=destination,
    )
    enqueue_push(notification)
    return notification


BROADCAST_TYPE = "platform.broadcast"
BROADCAST_CHUNK = 1000


def broadcast_recipients(*, audience: str, province_id: Any = None) -> QuerySet[Any]:
    """Active accounts a broadcast reaches.

    ALL is every active account, narrowed to those who chose `province_id` when one is given.
    OWNERS is every active owner or manager of a facility, narrowed to facilities in the
    province when one is given.
    """
    from accounts.models import User

    users = User.objects.filter(is_active=True)
    if audience == Broadcast.Audience.OWNERS:
        memberships = {"facility_memberships__isnull": False}
        if province_id:
            memberships = {"facility_memberships__facility__province_id": province_id}
        return users.filter(**memberships).distinct()
    if province_id:
        users = users.filter(province_id=province_id)
    return users


@transaction.atomic
def send_broadcast(
    *, actor: Any, title_ar: str, body_ar: str, audience: str, province: Any = None
) -> Broadcast:
    """Put one message in the inbox of every recipient, then push it after commit.

    Rows are written in bulk, so a broadcast to many accounts is a handful of INSERTs. The
    per-account realtime ping `create_notification` sends is skipped here; the push, fanned
    out by a task, is the announcement.
    """
    broadcast = Broadcast.objects.create(
        actor=actor, title_ar=title_ar, body_ar=body_ar, audience=audience, province=province
    )
    destination = (
        Notification.Destination.OWNER_FACILITIES
        if audience == Broadcast.Audience.OWNERS
        else Notification.Destination.NONE
    )
    payload = safe_notification_payload({"broadcastId": str(broadcast.pk)})
    user_ids = broadcast_recipients(
        audience=audience, province_id=province.pk if province else None
    ).values_list("pk", flat=True)
    total = 0
    batch: list[Notification] = []
    for user_id in user_ids.iterator(chunk_size=BROADCAST_CHUNK):
        batch.append(
            Notification(
                user_id=user_id,
                type=BROADCAST_TYPE,
                title_ar=title_ar,
                body_ar=body_ar,
                destination=destination,
                payload=payload,
            )
        )
        if len(batch) >= BROADCAST_CHUNK:
            Notification.objects.bulk_create(batch)
            total += len(batch)
            batch = []
    if batch:
        Notification.objects.bulk_create(batch)
        total += len(batch)
    broadcast.recipient_count = total
    broadcast.save(update_fields=["recipient_count"])
    broadcast_id = str(broadcast.pk)

    def _fan_out() -> None:
        from .tasks import fan_out_broadcast_push

        try:
            fan_out_broadcast_push.delay(broadcast_id)
        except Exception:  # noqa: BLE001 - the broker is optional; the inbox is the record
            logger.warning("push.broadcast_enqueue_failed", extra={"broadcast_id": broadcast_id})

    transaction.on_commit(_fan_out)
    return broadcast
