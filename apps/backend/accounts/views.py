from django.utils import timezone
from rest_framework import status
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from locations.models import Province

from .models import OTPChallenge
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
from .throttles import LoginThrottle, OtpStartThrottle, OtpVerifyThrottle, RecoveryThrottle
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

    def post(self, request):
        serializer = RegisterStartSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        data = serializer.validated_data
        if not Province.objects.filter(pk=data["provinceId"], is_active=True).exists():
            return Response({"provinceId": "Province is unavailable."}, status=400)
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

    def post(self, request):
        serializer = RegisterCompleteSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        return Response(complete_registration(**serializer.validated_data), status=201)


class LoginView(APIView):
    throttle_classes = [LoginThrottle]
    permission_classes = [AllowAny]
    authentication_classes = []

    def post(self, request):
        serializer = LoginSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        return Response(login(**serializer.validated_data))


class RefreshView(APIView):
    permission_classes = [AllowAny]
    authentication_classes = []

    def post(self, request):
        serializer = RefreshSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        return Response(rotate_refresh(raw_refresh=serializer.validated_data["refreshToken"]))


class LogoutView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        session_id = request.data.get("sessionId")
        if not session_id:
            return Response({"sessionId": "Required."}, status=400)
        revoke_session(user=request.user, session_id=session_id)
        return Response(status=204)


class LogoutAllView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        revoke_all_sessions(user=request.user)
        return Response(status=204)


class SessionsView(APIView):
    permission_classes = [IsAuthenticated]

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

    def delete(self, request, session_id):
        revoke_session(user=request.user, session_id=session_id)
        return Response(status=204)


class RecoveryStartView(APIView):
    throttle_classes = [RecoveryThrottle]
    permission_classes = [AllowAny]
    authentication_classes = []

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

    def post(self, request):
        serializer = RecoveryResetSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        reset_password(**serializer.validated_data)
        return Response(status=204)


class ProfileView(APIView):
    permission_classes = [IsAuthenticated]

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
                province = Province.objects.filter(pk=province_id, is_active=True).first()
                if province is None:
                    return Response({"provinceId": "Province is unavailable."}, status=400)
                user.province = province
        user.updated_at = timezone.now()
        user.save()
        return self.get(request)


class AccountDeletionRequestView(APIView):
    permission_classes = [IsAuthenticated]

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
