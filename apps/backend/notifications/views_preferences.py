"""An account's choice of which notices are announced on its devices (`preferences`)."""

from __future__ import annotations

from typing import Any

from drf_spectacular.utils import extend_schema
from rest_framework import serializers
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from accounts.authentication import AuthenticatedRequest
from core.openapi import VALIDATION_400, protected

from .preferences import preferences_of, save_preferences


class NotificationPreferencesSerializer(serializers.Serializer[Any]):
    dutyReminders = serializers.BooleanField(
        help_text="Days nobody covers on the province's duty roster."
    )
    provinceNews = serializers.BooleanField(
        help_text="What the platform announces to the account's province."
    )
    applicationStatus = serializers.BooleanField(
        help_text="An owner's applications and facilities: decisions, hours to confirm."
    )


class NotificationPreferencesView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="accountNotificationPreferencesRetrieve",
        tags=["Account"],
        summary="Which kinds of notice are pushed to this account's devices",
        description=(
            "All are on until the account turns one off. Only the push is governed: every "
            "message still reaches the inbox. A staff change to an owner's own duty shift, and "
            "any kind outside these three, is always pushed."
        ),
        responses={200: NotificationPreferencesSerializer, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        return Response(preferences_of(request.user))

    @extend_schema(
        operation_id="accountNotificationPreferencesUpdate",
        tags=["Account"],
        summary="Change which kinds of notice are pushed",
        description="Only the fields sent change.",
        request=NotificationPreferencesSerializer(partial=True),
        responses={200: NotificationPreferencesSerializer, 400: VALIDATION_400, **protected()},
    )
    def patch(self, request: AuthenticatedRequest) -> Response:
        payload = NotificationPreferencesSerializer(data=request.data, partial=True)
        payload.is_valid(raise_exception=True)
        return Response(save_preferences(request.user, dict(payload.validated_data)))
