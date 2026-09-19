"""Device push registration for the signed-in user.

The token is a credential-like identifier: it is stored encrypted with a keyed digest for
lookup, never returned, never logged and never shown to operators. A token is tied to the
session that registered it, so it stops receiving pushes when that session ends.
"""

from typing import Any

from drf_spectacular.utils import extend_schema
from rest_framework.permissions import IsAuthenticated
from rest_framework.request import Request
from rest_framework.response import Response
from rest_framework.views import APIView

from core.openapi import VALIDATION_400, protected
from sessions.models import UserSession

from .schemas import PushTokenRegisterSerializer, PushTokenSerializer
from .services import deactivate_push_token, register_push_token


def _session(request: Request) -> UserSession | None:
    claims: Any = request.auth
    session_id = claims.get("sid") if isinstance(claims, dict) else None
    user_id = request.user.pk
    if not session_id or user_id is None:
        return None
    return UserSession.objects.filter(pk=session_id, user_id=user_id).first()


class PushTokenView(APIView):
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
