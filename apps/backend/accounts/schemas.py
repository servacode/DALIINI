"""Request and response contract for authentication and account endpoints.

Nothing here exposes security material that the runtime keeps server-side. OTP digests,
refresh digests, previous-refresh digests and password hashes are never serialised. The
`refreshToken` field carries the opaque rotating secret itself, which is the value the
client is meant to store; it is not the stored digest.
"""

from rest_framework import serializers

from .models import AccountDeletionRequest


class ChallengeAcceptedSerializer(serializers.Serializer):
    """Returned when an OTP challenge is created. The code itself is never returned."""

    challengeId = serializers.UUIDField()
    expiresAt = serializers.DateTimeField()


class ChallengeVerifiedSerializer(serializers.Serializer):
    challengeId = serializers.UUIDField()
    verified = serializers.BooleanField()


class SessionCredentialsSerializer(serializers.Serializer):
    """Issued on registration, login and refresh."""

    accessToken = serializers.CharField(
        help_text="Short-lived JOSE/JWT access token for the Authorization header."
    )
    refreshToken = serializers.CharField(
        help_text=(
            "Opaque rotating refresh secret. Store it in platform secure storage and "
            "replace it on every refresh; the server keeps only a digest."
        )
    )
    sessionId = serializers.UUIDField()
    expiresAt = serializers.DateTimeField(help_text="Expiry of the refresh session.")


class LogoutRequestSerializer(serializers.Serializer):
    sessionId = serializers.UUIDField(help_text="Session to revoke.")


class UserSessionSerializer(serializers.Serializer):
    id = serializers.UUIDField()
    platform = serializers.CharField(allow_blank=True)
    deviceName = serializers.CharField(allow_blank=True)
    createdAt = serializers.DateTimeField()
    lastSeenAt = serializers.DateTimeField(allow_null=True)
    revoked = serializers.BooleanField()


class UserSessionListSerializer(serializers.Serializer):
    items = UserSessionSerializer(many=True)


class ProfileSerializer(serializers.Serializer):
    id = serializers.UUIDField()
    displayName = serializers.CharField()
    phone = serializers.CharField(help_text="Canonical +9639XXXXXXXX form.")
    provinceId = serializers.UUIDField(allow_null=True)
    phoneVerifiedAt = serializers.DateTimeField(allow_null=True)
    address = serializers.CharField(
        allow_blank=True,
        help_text="Free text, as the person writes it. Empty when they have not given one.",
    )
    profileImageUrl = serializers.URLField(
        allow_null=True,
        help_text="Public URL of the profile picture, or null when there is none.",
    )


class AccountDeletionRequestedSerializer(serializers.Serializer):
    id = serializers.UUIDField()
    status = serializers.ChoiceField(choices=AccountDeletionRequest.Status.choices)
