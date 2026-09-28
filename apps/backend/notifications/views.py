"""Device push registration for the signed-in user.

The token is a credential-like identifier: it is stored encrypted with a keyed digest for
lookup, never returned, never logged and never shown to operators. A token is tied to the
session that registered it, so it stops receiving pushes when that session ends.
"""

from typing import Any

from drf_spectacular.utils import extend_schema
from rest_framework.exceptions import NotFound
from rest_framework.permissions import IsAuthenticated
from rest_framework.request import Request
from rest_framework.response import Response
from rest_framework.views import APIView

from core.openapi import NOT_FOUND_404, VALIDATION_400, protected
from core.pagination import CursorPage
from core.throttles import PushTokenThrottle
from search.views import PAGE_PARAMS
from sessions.models import UserSession

from .models import Notification
from .presenters import notification_payload
from .schemas import (
    NotificationPageSerializer,
    PushTokenRegisterSerializer,
    PushTokenSerializer,
    UnreadCountSerializer,
)
from .services import (
    deactivate_push_token,
    mark_all_notifications_read,
    mark_notification_read,
    register_push_token,
    unread_notification_count,
)


def _session(request: Request) -> UserSession | None:
    claims: Any = request.auth
    session_id = claims.get("sid") if isinstance(claims, dict) else None
    user_id = request.user.pk
    if not session_id or user_id is None:
        return None
    return UserSession.objects.filter(pk=session_id, user_id=user_id).first()


class PushTokenView(APIView):
    throttle_classes = [PushTokenThrottle]
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="accountPushTokenRegister",
        tags=["Account"],
        summary="Register or refresh this device's push token",
        description=(
            "Idempotent. A new token from the same session replaces the previous one. The "
            "token is tied to the calling session and deactivated when that session ends."
        ),
        request=PushTokenRegisterSerializer,
        responses={204: None, 400: VALIDATION_400, **protected()},
    )
    def put(self, request: Request) -> Response:
        serializer = PushTokenRegisterSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        register_push_token(
            user=request.user,
            platform=serializer.validated_data["platform"],
            token=serializer.validated_data["token"],
            session=_session(request),
        )
        return Response(status=204)


class PushTokenUnregisterView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="accountPushTokenUnregister",
        tags=["Account"],
        summary="Stop sending pushes to a device token",
        description="Idempotent: an unknown or already inactive token also answers 204.",
        request=PushTokenSerializer,
        responses={204: None, 400: VALIDATION_400, **protected()},
    )
    def post(self, request: Request) -> Response:
        serializer = PushTokenSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        deactivate_push_token(user=request.user, token=serializer.validated_data["token"])
        return Response(status=204)


class NotificationCursorPagination(CursorPage):
    """The inbox, newest first, with the id keeping the ordering total."""

    ordering = ("-created_at", "id")


class NotificationsView(APIView):
    """The account's own inbox.

    Every message the platform has sent this account is here whether or not a push ever
    reached the device, which is what makes the inbox the record and the push only an
    announcement.
    """

    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="accountNotificationsList",
        tags=["Account"],
        summary="List the caller's notifications, newest first",
        parameters=PAGE_PARAMS,
        responses={200: NotificationPageSerializer, **protected()},
    )
    def get(self, request: Request) -> Response:
        inbox = Notification.objects.filter(user=request.user).order_by("-created_at", "id")
        paginator = NotificationCursorPagination()
        page = paginator.paginate_queryset(inbox, request, view=self) or []
        body = paginator.get_paginated_payload([notification_payload(row) for row in page])
        body["unreadCount"] = unread_notification_count(user=request.user)
        return Response(body)


class NotificationsUnreadCountView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="accountNotificationsUnreadCount",
        tags=["Account"],
        summary="How many of the caller's notifications are unread",
        responses={200: UnreadCountSerializer, **protected()},
    )
    def get(self, request: Request) -> Response:
        return Response({"unreadCount": unread_notification_count(user=request.user)})


class NotificationReadView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="accountNotificationMarkRead",
        tags=["Account"],
        summary="Mark one notification as read",
        description="Idempotent: a message that was already read keeps the time it was read.",
        request=None,
        responses={200: UnreadCountSerializer, **protected(), 404: NOT_FOUND_404},
    )
    def post(self, request: Request, notification_id: str) -> Response:
        try:
            mark_notification_read(user=request.user, notification_id=notification_id)
        except Notification.DoesNotExist:
            raise NotFound() from None
        return Response({"unreadCount": unread_notification_count(user=request.user)})


class NotificationsReadAllView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="accountNotificationsMarkAllRead",
        tags=["Account"],
        summary="Mark every unread notification as read",
        request=None,
        responses={200: UnreadCountSerializer, **protected()},
    )
    def post(self, request: Request) -> Response:
        mark_all_notifications_read(user=request.user)
        return Response({"unreadCount": 0})
