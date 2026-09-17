from django.contrib.auth.password_validation import validate_password
from rest_framework import serializers

from .phone import normalize_syrian_phone


class PhoneField(serializers.CharField):
    def to_internal_value(self, data):
        raw = super().to_internal_value(data)
        try:
            return normalize_syrian_phone(raw)
        except ValueError as exc:
            raise serializers.ValidationError(str(exc)) from exc


class RegisterStartSerializer(serializers.Serializer):
    displayName = serializers.CharField(max_length=120)
    phone = PhoneField()
    provinceId = serializers.UUIDField()


class ChallengeVerifySerializer(serializers.Serializer):
    challengeId = serializers.UUIDField()
    code = serializers.RegexField(r"^\d{6}$")


class RegisterCompleteSerializer(serializers.Serializer):
    # The wire names stay camelCase; `source` maps them to the snake_case keyword
    # arguments of complete_registration, which receives **validated_data.
    challengeId = serializers.UUIDField(source="challenge_id")
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


class DeletionRequestSerializer(serializers.Serializer):
    confirm = serializers.BooleanField()

    def validate_confirm(self, value):
        if value is not True:
            raise serializers.ValidationError("Explicit confirmation is required.")
        return value
