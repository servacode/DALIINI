"""The operator's authenticator: set it up, pass it each session, switch it off (`accounts.mfa`)."""

from __future__ import annotations

from typing import Any
from uuid import UUID

from django.shortcuts import get_object_or_404
from drf_spectacular.utils import extend_schema
from rest_framework import serializers
from rest_framework.exceptions import PermissionDenied
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from admin_console.permissions import HasAdminPermission
from core.openapi import CONFLICT_409, DOMAIN_400, NOT_FOUND_404, protected
from core.throttles import UserDefaultThrottle, UserOrIpThrottle

from . import mfa
from .authentication import AuthenticatedRequest
from .models import User
from .rbac import is_admin_operator


class MfaThrottle(UserOrIpThrottle):
    """Six digits are guessable without a limit; with ten tries an hour they are not."""

    scope = "mfa"
    only_writes = True


class MfaStatusSerializer(serializers.Serializer[Any]):
    enabled = serializers.BooleanField(help_text="An authenticator is set up and confirmed.")
    required = serializers.BooleanField(  # type: ignore[assignment]
        help_text="This deployment requires every operator to have one."
    )
    verified = serializers.BooleanField(help_text="This session passed the second step.")
    recoveryCodesLeft = serializers.IntegerField(min_value=0)


class MfaSetupSerializer(serializers.Serializer[Any]):
    secret = serializers.CharField(help_text="Base32, for typing into an app by hand.")
    otpauthUri = serializers.CharField()
    qrSvgDataUri = serializers.CharField(help_text="The same URI as a QR code, an SVG data URI.")


class MfaCodeSerializer(serializers.Serializer[Any]):
    code = serializers.CharField(
        max_length=20, help_text="Six digits from the app, or a recovery code where accepted."
    )


class MfaRecoveryCodesSerializer(serializers.Serializer[Any]):
    recoveryCodes = serializers.ListField(
        child=serializers.CharField(),
        help_text="Ten one-time codes. Shown this once; only their digests are kept.",
    )


def _session(request: AuthenticatedRequest) -> Any:
    session = getattr(request, "user_session", None)
    if session is None:
        raise PermissionDenied("A signed-in session is required.")
    return session


def _operator(request: AuthenticatedRequest) -> None:
    if not is_admin_operator(request.user):
        raise PermissionDenied("Only console operators use an authenticator.")


class MfaStatusView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="accountMfaRetrieve",
        tags=["Account"],
        summary="The second sign-in step, for this account and session",
        responses={200: MfaStatusSerializer, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        return Response(mfa.status(request.user, getattr(request, "user_session", None)))


class MfaSetupView(APIView):
    permission_classes = [IsAuthenticated]
    throttle_classes = [MfaThrottle, UserDefaultThrottle]

    @extend_schema(
        operation_id="accountMfaSetup",
        tags=["Account"],
        summary="Start setting up an authenticator app",
        description=(
            "Operators only. Returns a new secret and its QR code; nothing is enabled until "
            "a code from the app confirms it. Starting again replaces an unconfirmed secret."
        ),
        request=None,
        responses={200: MfaSetupSerializer, **protected(), 409: CONFLICT_409},
    )
    def post(self, request: AuthenticatedRequest) -> Response:
        _operator(request)
        return Response(mfa.begin_setup(user=request.user))


class MfaConfirmView(APIView):
    permission_classes = [IsAuthenticated]
    throttle_classes = [MfaThrottle, UserDefaultThrottle]

    @extend_schema(
        operation_id="accountMfaConfirm",
        tags=["Account"],
        summary="Confirm the authenticator with its first code",
        description=(
            "Enables it, marks this session as having passed the second step, and returns ten "
            "recovery codes, shown this once."
        ),
        request=MfaCodeSerializer,
        responses={
            200: MfaRecoveryCodesSerializer,
            400: DOMAIN_400,
            **protected(),
            409: CONFLICT_409,
        },
    )
    def post(self, request: AuthenticatedRequest) -> Response:
        _operator(request)
        payload = MfaCodeSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        codes = mfa.confirm_setup(
            user=request.user,
            session=_session(request),
            code=payload.validated_data["code"],
            request_id=getattr(request, "request_id", ""),
        )
        return Response({"recoveryCodes": codes})


class MfaVerifyView(APIView):
    permission_classes = [IsAuthenticated]
    throttle_classes = [MfaThrottle, UserDefaultThrottle]

    @extend_schema(
        operation_id="accountMfaVerify",
        tags=["Account"],
        summary="Pass the second step for this session",
        description="A code from the app, or one of the recovery codes (each works once).",
        request=MfaCodeSerializer,
        responses={200: MfaStatusSerializer, 400: DOMAIN_400, **protected(), 409: CONFLICT_409},
    )
    def post(self, request: AuthenticatedRequest) -> Response:
        payload = MfaCodeSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        session = _session(request)
        mfa.verify(
            user=request.user,
            session=session,
            code=payload.validated_data["code"],
            request_id=getattr(request, "request_id", ""),
        )
        return Response(mfa.status(request.user, session))


class MfaDisableView(APIView):
    permission_classes = [IsAuthenticated]
    throttle_classes = [MfaThrottle, UserDefaultThrottle]

    @extend_schema(
        operation_id="accountMfaDisable",
        tags=["Account"],
        summary="Switch the authenticator off",
        description=(
            "Needs a current code from the app. Refused with 409 MFA_REQUIRED_BY_POLICY where "
            "every operator must have one."
        ),
        request=MfaCodeSerializer,
        responses={200: MfaStatusSerializer, 400: DOMAIN_400, **protected(), 409: CONFLICT_409},
    )
    def post(self, request: AuthenticatedRequest) -> Response:
        from core.exceptions import ConflictError

        if mfa.required_for_staff() and is_admin_operator(request.user):
            raise ConflictError(
                "MFA_REQUIRED_BY_POLICY",
                message="التحقق بخطوتين إلزامي لفريق التشغيل ولا يمكن إيقافه.",
            )
        payload = MfaCodeSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        mfa.disable(
            user=request.user,
            code=payload.validated_data["code"],
            request_id=getattr(request, "request_id", ""),
        )
        return Response(mfa.status(request.user, getattr(request, "user_session", None)))


class AdminUserMfaResetView(APIView):
    """Lives with the console's user routes; defined here beside the rest of the feature."""

    permission_classes = [IsAuthenticated, HasAdminPermission]
    required_permission = "admin.roles.manage"

    @extend_schema(
        operation_id="adminUserMfaReset",
        tags=["Admin Users"],
        summary="Clear an operator's authenticator after they lost it",
        description=(
            "They set up a new one at their next console sign-in. Their recovery codes are "
            "cleared too. Audited."
        ),
        request=None,
        responses={204: None, **protected(), 404: NOT_FOUND_404},
    )
    def post(self, request: AuthenticatedRequest, user_id: UUID) -> Response:
        target = get_object_or_404(User, pk=user_id)
        mfa.reset_for(
            actor=request.user, user=target, request_id=getattr(request, "request_id", "")
        )
        return Response(status=204)
