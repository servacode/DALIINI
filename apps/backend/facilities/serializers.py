from rest_framework import serializers

from .models import FacilityMembership


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
    requirementId = serializers.UUIDField()
    file = serializers.FileField()


class PublicImageUploadSerializer(serializers.Serializer):
    file = serializers.FileField()


class FacilityMemberSerializer(serializers.Serializer):
    userId = serializers.UUIDField()
    role = serializers.ChoiceField(choices=FacilityMembership.Role.choices)
