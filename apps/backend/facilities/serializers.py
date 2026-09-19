from drf_spectacular.types import OpenApiTypes
from drf_spectacular.utils import extend_schema_field
from rest_framework import serializers

from .models import FacilityMembership


@extend_schema_field(OpenApiTypes.BINARY)
class UploadedFileField(serializers.FileField):
    """A file part of a multipart request, described as bytes rather than a URL.

    drf-spectacular describes `FileField` as `format: uri` whenever request and response
    share components, which is right for a response and wrong for an upload: the Kotlin
    and Swift generators then type the part as a URI and send its text instead of the
    file (INT-048). These serializers only ever read uploads, so the part is binary.
    """


class FacilityCreateSerializer(serializers.Serializer):
    provinceId = serializers.UUIDField()
    categoryId = serializers.UUIDField()
    nameAr = serializers.CharField(max_length=160)
    nameEn = serializers.CharField(max_length=160, required=False, allow_blank=True)


class FacilityPatchSerializer(serializers.Serializer):
    nameAr = serializers.CharField(max_length=160, required=False)
    nameEn = serializers.CharField(max_length=160, required=False, allow_blank=True)
    descriptionAr = serializers.CharField(required=False, allow_blank=True)
    descriptionEn = serializers.CharField(required=False, allow_blank=True)
    phone = serializers.CharField(max_length=16, required=False, allow_blank=True)
    addressAr = serializers.CharField(max_length=255, required=False, allow_blank=True)
    addressEn = serializers.CharField(max_length=255, required=False, allow_blank=True)
    cityId = serializers.UUIDField(required=False, allow_null=True)
    neighborhoodId = serializers.UUIDField(required=False, allow_null=True)
    specialtyIds = serializers.ListField(
        child=serializers.UUIDField(), required=False
    )
    serviceTagIds = serializers.ListField(
        child=serializers.UUIDField(), required=False
    )


class FacilityLocationSerializer(serializers.Serializer):
    latitude = serializers.FloatField(min_value=-90, max_value=90)
    longitude = serializers.FloatField(min_value=-180, max_value=180)


class EvidenceUploadSerializer(serializers.Serializer):
    requirementId = serializers.IntegerField(min_value=1)
    file = UploadedFileField()


class PublicImageUploadSerializer(serializers.Serializer):
    file = UploadedFileField()


class FacilityMemberSerializer(serializers.Serializer):
    userId = serializers.UUIDField()
    role = serializers.ChoiceField(choices=FacilityMembership.Role.choices)
