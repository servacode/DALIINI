from django.utils import timezone
from drf_spectacular.utils import extend_schema
from rest_framework import status
from rest_framework.exceptions import ValidationError
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from core.openapi import THROTTLED_429, VALIDATION_400, protected
from locations.models import Province

from .models import OTPChallenge
from .schemas import (
    AccountDeletionRequestedSerializer,
    ChallengeAcceptedSerializer,
    ChallengeVerifiedSerializer,
    LogoutRequestSerializer,
    ProfileSerializer,
    SessionCredentialsSerializer,
    UserSessionListSerializer,
)
from .serializers import (
    ChallengeVerifySerializer,
    DeletionRequestSerializer,
    LoginSerializer,
    ProfilePatchSerializer,
    RecoveryResetSerializer,
    RecoveryStartSerializer,
    RefreshSerializer,
    RegisterCompleteSerializer,
    RegisterStartSerializer,
)
from .services import (
    complete_registration,
    login,
    request_account_deletion,
    reset_password,
    revoke_all_sessions,
    revoke_session,
    rotate_refresh,
    start_challenge,
    verify_challenge,
)
from .throttles import LoginThrottle, OtpStartThrottle, OtpVerifyThrottle, RecoveryThrottle


def _challenge_response(challenge):
    return Response(
        {
            "challengeId": str(challenge.pk),
            "expiresAt": challenge.expires_at.isoformat(),
        },
        status=status.HTTP_202_ACCEPTED,
    )


class RegisterStartView(APIView):
    throttle_classes = [OtpStartThrottle]
    permission_classes = [AllowAny]
    authentication_classes = []

    @extend_schema(
        operation_id="authRegisterStart",
        tags=["Auth"],
        summary="Start registration by requesting an OTP challenge",
        description=(
            "Accepts 09XXXXXXXX, +9639XXXXXXXX or 009639XXXXXXXX and normalises to the "
            "canonical form. The OTP code is delivered by the configured provider and is "
            "never returned in the response."
        ),
        request=RegisterStartSerializer,
        responses={202: ChallengeAcceptedSerializer, 400: VALIDATION_400, 429: THROTTLED_429},
    )
    def post(self, request):
        serializer = RegisterStartSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        data = serializer.validated_data
        if not Province.objects.filter(pk=data["provinceId"], active=True).exists():
            raise ValidationError({"provinceId": ["Province is unavailable."]})
        challenge = start_challenge(
            phone=data["phone"],
            purpose=OTPChallenge.Purpose.REGISTER,
            metadata={
                "displayName": data["displayName"],
                "provinceId": str(data["provinceId"]),
            },
        )
        return _challenge_response(challenge)


class RegisterVerifyView(APIView):
    throttle_classes = [OtpVerifyThrottle]
    permission_classes = [AllowAny]
    authentication_classes = []

    @extend_schema(
        operation_id="authRegisterVerify",
        tags=["Auth"],
        summary="Verify the registration OTP code",
        request=ChallengeVerifySerializer,
        responses={200: ChallengeVerifiedSerializer, 400: VALIDATION_400, 429: THROTTLED_429},
    )
    def post(self, request):
        serializer = ChallengeVerifySerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        challenge = verify_challenge(
            challenge_id=serializer.validated_data["challengeId"],
            code=serializer.validated_data["code"],
            purpose=OTPChallenge.Purpose.REGISTER,
        )
        return Response({"challengeId": str(challenge.pk), "verified": True})


class RegisterCompleteView(APIView):
    permission_classes = [AllowAny]
    authentication_classes = []

    @extend_schema(
        operation_id="authRegisterComplete",
        tags=["Auth"],
        summary="Set the password and open the first session",
        request=RegisterCompleteSerializer,
        responses={201: SessionCredentialsSerializer, 400: VALIDATION_400},
    )
    def post(self, request):
        serializer = RegisterCompleteSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        return Response(complete_registration(**serializer.validated_data), status=201)


class LoginView(APIView):
    throttle_classes = [LoginThrottle]
    permission_classes = [AllowAny]
    authentication_classes = []

    @extend_schema(
        operation_id="authLogin",
        tags=["Auth"],
        summary="Exchange phone and password for session credentials",
        request=LoginSerializer,
        responses={200: SessionCredentialsSerializer, 400: VALIDATION_400, 429: THROTTLED_429},
    )
    def post(self, request):
        serializer = LoginSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        return Response(login(**serializer.validated_data))


class RefreshView(APIView):
    permission_classes = [AllowAny]
    authentication_classes = []

    @extend_schema(
        operation_id="authRefresh",
        tags=["Auth"],
        summary="Rotate the refresh secret and issue a new access token",
        description=(
            "The supplied secret is rotated on every successful call. Replaying a secret "
            "outside the short concurrency grace window is treated as compromise and "
            "revokes every session belonging to the user."
        ),
        request=RefreshSerializer,
        responses={200: SessionCredentialsSerializer, 400: VALIDATION_400},
    )
    def post(self, request):
        serializer = RefreshSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        return Response(rotate_refresh(raw_refresh=serializer.validated_data["refreshToken"]))


class LogoutView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="authLogout",
        tags=["Auth"],
        summary="Revoke one session",
        request=LogoutRequestSerializer,
        responses={204: None, 400: VALIDATION_400, **protected()},
    )
    def post(self, request):
        session_id = request.data.get("sessionId")
        if not session_id:
            raise ValidationError({"sessionId": ["This field is required."]})
        revoke_session(user=request.user, session_id=session_id)
        return Response(status=204)


class LogoutAllView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="authLogoutAll",
        tags=["Auth"],
        summary="Revoke every session belonging to the caller",
        request=None,
        responses={204: None, **protected()},
    )
    def post(self, request):
        revoke_all_sessions(user=request.user)
        return Response(status=204)


class SessionsView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="authSessionsList",
        tags=["Auth"],
        summary="List the sessions and devices of the caller",
        description="Session secrets are never returned, only metadata and revocation state.",
        responses={200: UserSessionListSerializer, **protected()},
    )
    def get(self, request):
        rows = request.user.sessions.order_by("-created_at")
        return Response(
            {
                "items": [
                    {
                        "id": str(row.pk),
                        "platform": row.platform,
                        "deviceName": row.device_name,
                        "createdAt": row.created_at.isoformat(),
                        "lastSeenAt": row.last_seen_at.isoformat() if row.last_seen_at else None,
                        "revoked": row.revoked_at is not None,
                    }
                    for row in rows
                ]
            }
        )


class SessionDetailView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="authSessionRevoke",
        tags=["Auth"],
        summary="Revoke a specific session of the caller",
        responses={204: None, 400: VALIDATION_400, **protected()},
    )
    def delete(self, request, session_id):
        revoke_session(user=request.user, session_id=session_id)
        return Response(status=204)


class RecoveryStartView(APIView):
    throttle_classes = [RecoveryThrottle]
    permission_classes = [AllowAny]
    authentication_classes = []

    @extend_schema(
        operation_id="authRecoveryStart",
        tags=["Auth"],
        summary="Start password recovery by requesting an OTP challenge",
        request=RecoveryStartSerializer,
        responses={202: ChallengeAcceptedSerializer, 400: VALIDATION_400, 429: THROTTLED_429},
    )
    def post(self, request):
        serializer = RecoveryStartSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        challenge = start_challenge(
            phone=serializer.validated_data["phone"],
            purpose=OTPChallenge.Purpose.RECOVERY,
        )
        return _challenge_response(challenge)


class RecoveryVerifyView(APIView):
    throttle_classes = [OtpVerifyThrottle]
    permission_classes = [AllowAny]
    authentication_classes = []

    @extend_schema(
        operation_id="authRecoveryVerify",
        tags=["Auth"],
        summary="Verify the recovery OTP code",
        request=ChallengeVerifySerializer,
        responses={200: ChallengeVerifiedSerializer, 400: VALIDATION_400, 429: THROTTLED_429},
    )
    def post(self, request):
        serializer = ChallengeVerifySerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        challenge = verify_challenge(
            challenge_id=serializer.validated_data["challengeId"],
            code=serializer.validated_data["code"],
            purpose=OTPChallenge.Purpose.RECOVERY,
        )
        return Response({"challengeId": str(challenge.pk), "verified": True})


class RecoveryResetView(APIView):
    permission_classes = [AllowAny]
    authentication_classes = []

    @extend_schema(
        operation_id="authRecoveryReset",
        tags=["Auth"],
        summary="Set a new password using a verified recovery challenge",
        description="A successful reset revokes every existing session for that user.",
        request=RecoveryResetSerializer,
        responses={204: None, 400: VALIDATION_400},
    )
    def post(self, request):
        serializer = RecoveryResetSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        reset_password(**serializer.validated_data)
        return Response(status=204)


class ProfileView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="accountProfileRetrieve",
        tags=["Account"],
        summary="Retrieve the profile of the caller",
        responses={200: ProfileSerializer, **protected()},
    )
    def get(self, request):
        user = request.user
        return Response(
            {
                "id": str(user.pk),
                "displayName": user.name,
                "phone": user.phone,
                "provinceId": str(user.province_id) if user.province_id else None,
                "phoneVerifiedAt": (
                    user.phone_verified_at.isoformat() if user.phone_verified_at else None
                ),
            }
        )

    @extend_schema(
        operation_id="accountProfileUpdate",
        tags=["Account"],
        summary="Update the display name or profile province of the caller",
        request=ProfilePatchSerializer,
        responses={200: ProfileSerializer, 400: VALIDATION_400, **protected()},
    )
    def patch(self, request):
        serializer = ProfilePatchSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user = request.user
        data = serializer.validated_data
        if "displayName" in data:
            user.name = data["displayName"]
        if "provinceId" in data:
            province_id = data["provinceId"]
            if province_id is None:
                user.province = None
            else:
                province = Province.objects.filter(pk=province_id, active=True).first()
                if province is None:
                    raise ValidationError(
                        {"provinceId": ["Province is unavailable."]}
                    )
                user.province = province
        user.updated_at = timezone.now()
        user.save()
        return self.get(request)


class AccountDeletionRequestView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="accountDeletionRequestCreate",
        tags=["Account"],
        summary="Request deletion of the account of the caller",
        description=(
            "Required by Play policy for any app that creates accounts. Ownership "
            "obligations and legally retained records are handled by the deletion policy."
        ),
        request=DeletionRequestSerializer,
        responses={202: AccountDeletionRequestedSerializer, 400: VALIDATION_400, **protected()},
    )
    def post(self, request):
        serializer = DeletionRequestSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        deletion = request_account_deletion(
            user=request.user,
            request_id=getattr(request, "request_id", ""),
        )
        return Response(
            {"id": str(deletion.pk), "status": deletion.status},
            status=status.HTTP_202_ACCEPTED,
        )
