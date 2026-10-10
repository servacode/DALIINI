from typing import Any
from uuid import UUID

from django.core.exceptions import ValidationError as DjangoValidationError
from django.utils import timezone
from drf_spectacular.utils import extend_schema
from rest_framework import status
from rest_framework.exceptions import ValidationError
from rest_framework.parsers import FormParser, MultiPartParser
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.request import Request
from rest_framework.response import Response
from rest_framework.views import APIView

from core.openapi import (
    CONFLICT_409,
    FORBIDDEN_403,
    OTP_UNAVAILABLE_503,
    OTP_UNDELIVERABLE_422,
    THROTTLED_429,
    UNAUTHENTICATED_401,
    VALIDATION_400,
    protected,
)
from locations.models import Province

from .authentication import AuthenticatedRequest
from .media import delete_profile_image, profile_image_url, save_profile_image
from .models import OTPChallenge, User
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
    PasswordChangeSerializer,
    PhoneChangeStartSerializer,
    ProfileImageUploadSerializer,
    ProfilePatchSerializer,
    RecoveryResetSerializer,
    RecoveryStartSerializer,
    RefreshSerializer,
    RegisterCompleteSerializer,
    RegisterStartSerializer,
)
from .services import (
    change_password,
    complete_phone_change,
    complete_registration,
    hand_over_console_code,
    login,
    request_account_deletion,
    reset_password,
    revoke_all_sessions,
    revoke_session,
    rotate_refresh,
    start_challenge,
    start_phone_change,
    verify_challenge,
)
from .throttles import (
    OTP_SEND_THROTTLES,
    LoginThrottle,
    OtpStartThrottle,
    OtpVerifyThrottle,
    RecoveryThrottle,
)


def _profile_payload(user: User) -> dict[str, Any]:
    """Everything the app is told about its own account, in one place.

    One function rather than one per view, so a field added here cannot appear in the answer
    to a read and go missing from the answer to a write.
    """
    return {
        "id": str(user.pk),
        "displayName": user.name,
        "phone": user.phone,
        "provinceId": str(user.province_id) if user.province_id else None,
        "phoneVerifiedAt": (
            user.phone_verified_at.isoformat() if user.phone_verified_at else None
        ),
        "address": user.address,
        "profileImageUrl": profile_image_url(user.profile_image_key),
    }


def _challenge_response(challenge: OTPChallenge) -> Response:
    return Response(
        {
            "challengeId": str(challenge.pk),
            "expiresAt": challenge.expires_at.isoformat(),
        },
        status=status.HTTP_202_ACCEPTED,
    )


class RegisterStartView(APIView):
    throttle_classes = [OtpStartThrottle, *OTP_SEND_THROTTLES]
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
        responses={
            202: ChallengeAcceptedSerializer,
            400: VALIDATION_400,
            422: OTP_UNDELIVERABLE_422,
            429: THROTTLED_429,
            503: OTP_UNAVAILABLE_503,
        },
    )
    def post(self, request: Request) -> Response:
        serializer = RegisterStartSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        data = serializer.validated_data
        if not Province.objects.filter(pk=data["provinceId"], active=True).exists():
            raise ValidationError({"provinceId": ["Province is unavailable."]})
        challenge = start_challenge(
            phone=data["phone"],
            purpose=OTPChallenge.Purpose.REGISTER,
            metadata={"provinceId": str(data["provinceId"])},
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
    def post(self, request: Request) -> Response:
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
        responses={201: SessionCredentialsSerializer, 400: VALIDATION_400, 409: CONFLICT_409},
    )
    def post(self, request: Request) -> Response:
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
        responses={
            200: SessionCredentialsSerializer,
            400: VALIDATION_400,
            401: UNAUTHENTICATED_401,
            # ACCOUNT_BLOCKED, and only to the account's own password.
            403: FORBIDDEN_403,
            429: THROTTLED_429,
        },
    )
    def post(self, request: Request) -> Response:
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
    def post(self, request: Request) -> Response:
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
    def post(self, request: AuthenticatedRequest) -> Response:
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
    def post(self, request: AuthenticatedRequest) -> Response:
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
    def get(self, request: AuthenticatedRequest) -> Response:
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
    def delete(self, request: AuthenticatedRequest, session_id: UUID) -> Response:
        revoke_session(user=request.user, session_id=session_id)
        return Response(status=204)


class RecoveryStartView(APIView):
    throttle_classes = [RecoveryThrottle, *OTP_SEND_THROTTLES]
    permission_classes = [AllowAny]
    authentication_classes = []

    @extend_schema(
        operation_id="authRecoveryStart",
        tags=["Auth"],
        summary="Start password recovery by requesting an OTP challenge",
        request=RecoveryStartSerializer,
        responses={
            202: ChallengeAcceptedSerializer,
            400: VALIDATION_400,
            422: OTP_UNDELIVERABLE_422,
            429: THROTTLED_429,
            503: OTP_UNAVAILABLE_503,
        },
    )
    def post(self, request: Request) -> Response:
        serializer = RecoveryStartSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        phone = serializer.validated_data["phone"]
        handed = hand_over_console_code(phone)
        if handed is not None:
            return _challenge_response(handed)
        challenge = start_challenge(phone=phone, purpose=OTPChallenge.Purpose.RECOVERY)
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
    def post(self, request: Request) -> Response:
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
    def post(self, request: Request) -> Response:
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
    def get(self, request: AuthenticatedRequest) -> Response:
        return Response(_profile_payload(request.user))

    @extend_schema(
        operation_id="accountProfileUpdate",
        tags=["Account"],
        summary="Update the display name or profile province of the caller",
        request=ProfilePatchSerializer,
        responses={200: ProfileSerializer, 400: VALIDATION_400, **protected()},
    )
    def patch(self, request: AuthenticatedRequest) -> Response:
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
        if "address" in data:
            user.address = data["address"]
        user.updated_at = timezone.now()
        user.save()
        return self.get(request)


class ProfileImageView(APIView):
    """The picture on the account: one at a time, replaced or removed."""

    parser_classes = [MultiPartParser, FormParser]
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="accountProfileImageUpdate",
        tags=["Account"],
        summary="Upload or replace the profile picture of the caller",
        description=(
            "Sent as multipart/form-data. The server decodes the file, enforces byte and "
            "pixel limits, re-encodes to JPEG and strips metadata — a photograph carries "
            "where it was taken. The declared extension and MIME type are not trusted."
        ),
        request={"multipart/form-data": ProfileImageUploadSerializer},
        responses={200: ProfileSerializer, 400: VALIDATION_400, **protected()},
    )
    def put(self, request: AuthenticatedRequest) -> Response:
        serializer = ProfileImageUploadSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user = request.user
        previous = user.profile_image_key
        try:
            _storage, key = save_profile_image(
                user_id=user.pk,
                upload=serializer.validated_data["file"],
            )
        except DjangoValidationError as exc:
            raise ValidationError({"file": list(exc.messages)}) from exc
        user.profile_image_key = key
        user.updated_at = timezone.now()
        user.save(update_fields=["profile_image_key", "updated_at"])
        # Only once the new one is the account's: a failure above leaves the old picture
        # in place rather than leaving the account with none.
        delete_profile_image(previous)
        return Response(_profile_payload(user))

    @extend_schema(
        operation_id="accountProfileImageDelete",
        tags=["Account"],
        summary="Remove the profile picture of the caller",
        responses={200: ProfileSerializer, **protected()},
    )
    def delete(self, request: AuthenticatedRequest) -> Response:
        user = request.user
        previous = user.profile_image_key
        user.profile_image_key = ""
        user.updated_at = timezone.now()
        user.save(update_fields=["profile_image_key", "updated_at"])
        delete_profile_image(previous)
        return Response(_profile_payload(user))


class PhoneChangeStartView(APIView):
    """Ask for a code on the number the account is to move to."""

    throttle_classes = [RecoveryThrottle, *OTP_SEND_THROTTLES]
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="accountPhoneChangeStart",
        tags=["Account"],
        summary="Start moving the account to another phone number",
        description=(
            "The code is sent to the new number, which is what proves the caller can "
            "receive on it. The account is not changed until the code is confirmed."
        ),
        request=PhoneChangeStartSerializer,
        responses={
            202: ChallengeAcceptedSerializer,
            400: VALIDATION_400,
            422: OTP_UNDELIVERABLE_422,
            429: THROTTLED_429,
            503: OTP_UNAVAILABLE_503,
            **protected(),
        },
    )
    def post(self, request: AuthenticatedRequest) -> Response:
        serializer = PhoneChangeStartSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        challenge = start_phone_change(
            user=request.user,
            phone=serializer.validated_data["phone"],
        )
        return _challenge_response(challenge)


class PhoneChangeConfirmView(APIView):
    """Prove the code, and the account answers to the new number from now on."""

    throttle_classes = [OtpVerifyThrottle]
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="accountPhoneChangeConfirm",
        tags=["Account"],
        summary="Confirm the code and move the account to the new number",
        description=(
            "Every session ends, this one included: the phone is how this account signs "
            "in, so a session issued to the old identity does not outlive it."
        ),
        request=ChallengeVerifySerializer,
        responses={200: ProfileSerializer, 400: VALIDATION_400, 429: THROTTLED_429, **protected()},
    )
    def post(self, request: AuthenticatedRequest) -> Response:
        serializer = ChallengeVerifySerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user = complete_phone_change(
            user=request.user,
            challenge_id=serializer.validated_data["challengeId"],
            code=serializer.validated_data["code"],
        )
        return Response(_profile_payload(user))


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
    def post(self, request: AuthenticatedRequest) -> Response:
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


class PasswordChangeView(APIView):
    """Replace the caller's password, and end every session while doing so."""

    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="accountPasswordChange",
        tags=["Account"],
        summary="Change the caller's password",
        description=(
            "The caller proves the current password first. A successful change revokes every "
            "session, including this one, so the caller signs in again with the new password."
        ),
        request=PasswordChangeSerializer,
        responses={204: None, 400: VALIDATION_400, **protected()},
    )
    def post(self, request: AuthenticatedRequest) -> Response:
        serializer = PasswordChangeSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        change_password(
            user=request.user,
            current_password=serializer.validated_data["currentPassword"],
            new_password=serializer.validated_data["newPassword"],
        )
        return Response(status=204)
