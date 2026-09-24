from django.contrib.auth.password_validation import validate_password
from rest_framework import serializers

from facilities.serializers import UploadedFileField

from .phone import normalize_syrian_phone


class PhoneField(serializers.CharField):
    def to_internal_value(self, data):
        raw = super().to_internal_value(data)
        try:
            return normalize_syrian_phone(raw)
        except ValueError as exc:
            raise serializers.ValidationError(str(exc)) from exc


class RegisterStartSerializer(serializers.Serializer):
    """What is needed to send a code: a number, and where the account will live.

    The name is not asked for here. Nothing should be collected about a person before
    they have shown the number is theirs, and a name given to a challenge that is never
    completed is a name stored for nothing.
    """

    phone = PhoneField()
    provinceId = serializers.UUIDField()


class ChallengeVerifySerializer(serializers.Serializer):
    challengeId = serializers.UUIDField()
    code = serializers.RegexField(r"^\d{6}$")


class RegisterCompleteSerializer(serializers.Serializer):
    # The wire names stay camelCase; `source` maps them to the snake_case keyword
    # arguments of complete_registration, which receives **validated_data.
    challengeId = serializers.UUIDField(source="challenge_id")
    displayName = serializers.CharField(source="display_name", max_length=120)
    password = serializers.CharField(write_only=True, trim_whitespace=False)
    platform = serializers.CharField(max_length=32, required=False, default="UNKNOWN")
    deviceName = serializers.CharField(
        source="device_name",
        max_length=120,
        required=False,
        allow_blank=True,
        default="",
    )

    def validate_password(self, value):
        validate_password(value)
        return value


class LoginSerializer(serializers.Serializer):
    phone = PhoneField()
    password = serializers.CharField(write_only=True, trim_whitespace=False)
    platform = serializers.CharField(max_length=32, required=False, default="UNKNOWN")
    deviceName = serializers.CharField(
        source="device_name",
        max_length=120,
        required=False,
        allow_blank=True,
        default="",
    )


class RefreshSerializer(serializers.Serializer):
    refreshToken = serializers.CharField(write_only=True, trim_whitespace=False)


class RecoveryStartSerializer(serializers.Serializer):
    phone = PhoneField()


class RecoveryResetSerializer(serializers.Serializer):
    challengeId = serializers.UUIDField(source="challenge_id")
    password = serializers.CharField(write_only=True, trim_whitespace=False)

    def validate_password(self, value):
        validate_password(value)
        return value


class ProfilePatchSerializer(serializers.Serializer):
    displayName = serializers.CharField(max_length=120, required=False)
    provinceId = serializers.UUIDField(required=False, allow_null=True)
    address = serializers.CharField(
        max_length=240,
        required=False,
        allow_blank=True,
        trim_whitespace=True,
    )


class ProfileImageUploadSerializer(serializers.Serializer):
    """One picture, sent as multipart. The declared type is not trusted."""

    file = UploadedFileField()


class PhoneChangeStartSerializer(serializers.Serializer):
    """The number the account is to move to, in any Syrian form."""

    phone = PhoneField()


class DeletionRequestSerializer(serializers.Serializer):
    confirm = serializers.BooleanField()

    def validate_confirm(self, value):
        if value is not True:
            raise serializers.ValidationError("Explicit confirmation is required.")
        return value


class PasswordChangeSerializer(serializers.Serializer):  # type: ignore[type-arg]
    """What the caller must supply to replace their password.

    Neither value is ever echoed back, logged, or put in an audit record.
    """

    currentPassword = serializers.CharField(write_only=True, trim_whitespace=False)
    newPassword = serializers.CharField(write_only=True, trim_whitespace=False)

    def validate_newPassword(self, value):  # noqa: N802 - the wire name is camelCase
        validate_password(value)
        return value
