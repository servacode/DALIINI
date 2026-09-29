from typing import Any

from drf_spectacular.types import OpenApiTypes
from drf_spectacular.utils import extend_schema_field
from rest_framework import serializers

from accounts.phone import normalize_syrian_phone

from .models import FacilityMembership


@extend_schema_field(OpenApiTypes.BINARY)
class UploadedFileField(serializers.FileField):
    """A file part of a multipart request, described as bytes rather than a URL.

    drf-spectacular describes `FileField` as `format: uri` whenever request and response
    share components, which is right for a response and wrong for an upload: the Kotlin
    and Swift generators then type the part as a URI and send its text instead of the
    file (INT-048). These serializers only ever read uploads, so the part is binary.
    """


class FacilityCreateSerializer(serializers.Serializer[Any]):
    provinceId = serializers.UUIDField()
    categoryId = serializers.UUIDField()
    nameAr = serializers.CharField(max_length=160)
    nameEn = serializers.CharField(max_length=160, required=False, allow_blank=True)


class FacilityPatchSerializer(serializers.Serializer[Any]):
    nameAr = serializers.CharField(max_length=160, required=False)
    nameEn = serializers.CharField(max_length=160, required=False, allow_blank=True)
    descriptionAr = serializers.CharField(required=False, allow_blank=True)
    descriptionEn = serializers.CharField(required=False, allow_blank=True)
    phone = serializers.CharField(max_length=16, required=False, allow_blank=True)
    whatsapp = serializers.CharField(
        max_length=20,
        required=False,
        allow_blank=True,
        help_text="Optional Syrian mobile (09XXXXXXXX or +9639XXXXXXXX); blank clears it.",
    )
    addressAr = serializers.CharField(max_length=255, required=False, allow_blank=True)
    addressEn = serializers.CharField(max_length=255, required=False, allow_blank=True)
    cityId = serializers.UUIDField(required=False, allow_null=True)
    neighborhoodId = serializers.UUIDField(required=False, allow_null=True)
    # Integer keys, as the rows are. Declared as UUIDs before, which no real id could pass.
    specialtyIds = serializers.ListField(
        child=serializers.IntegerField(min_value=1),
        required=False,
        help_text=(
            "Replaces the facility's specialties. Ids come from the category's `specialties` "
            "in ownerConfigRetrieve; an empty list clears them."
        ),
    )
    serviceTagIds = serializers.ListField(
        child=serializers.IntegerField(min_value=1),
        required=False,
        help_text=(
            "Replaces the facility's services. Ids come from the category's `services` in "
            "ownerConfigRetrieve; an empty list clears them."
        ),
    )


    def validate_whatsapp(self, value: str) -> str:
        if not value.strip():
            return ""
        try:
            return normalize_syrian_phone(value)
        except ValueError as exc:
            raise serializers.ValidationError("Enter a valid Syrian mobile number.") from exc


class FacilityLocationSerializer(serializers.Serializer[Any]):
    latitude = serializers.FloatField(min_value=-90, max_value=90)
    longitude = serializers.FloatField(min_value=-180, max_value=180)


class EvidenceUploadSerializer(serializers.Serializer[Any]):
    requirementId = serializers.IntegerField(min_value=1)
    file = UploadedFileField()


class PublicImageUploadSerializer(serializers.Serializer[Any]):
    file = UploadedFileField()


class FacilityMemberSerializer(serializers.Serializer[Any]):
    userId = serializers.UUIDField()
    role = serializers.ChoiceField(choices=FacilityMembership.Role.choices)
