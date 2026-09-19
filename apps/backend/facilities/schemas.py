"""Response contract for the owner facility surface.

Private verification evidence is represented by identifiers and timestamps only. Raw
object storage keys, private bucket paths and reviewer notes never appear here, per
`15-SECURITY-PRIVACY.md`.
"""

from rest_framework import serializers

from core.openapi import CoordinatesSerializer, NamedRefSerializer
from directory.models import Category
from directory.schemas import CategoryCapabilitiesSerializer

from .models import Facility, FacilityApplication, FacilityMembership

REQUIRED_ACTIONS = [
    "REVIEW_REJECTION",
    "COMPLETE_AND_SUBMIT",
    "REVERIFY_AND_SUBMIT",
    "WAIT_FOR_REVIEW",
    "CONTACT_SUPPORT",
]


class OwnerVerificationRequirementSerializer(serializers.Serializer):
    """Safe descriptor of a requirement. The evidence itself is never described here."""

    id = serializers.UUIDField()
    labelAr = serializers.CharField()
    labelEn = serializers.CharField(allow_null=True)
    instructionsAr = serializers.CharField(allow_null=True)
    required = serializers.BooleanField()
    minFiles = serializers.IntegerField()
    maxFiles = serializers.IntegerField()


class OwnerCategorySerializer(serializers.Serializer):
    id = serializers.UUIDField()
    nameAr = serializers.CharField()
    nameEn = serializers.CharField(allow_null=True)
    iconKey = serializers.CharField(allow_null=True)
    specialization = serializers.ChoiceField(choices=Category.Specialization.choices)


class OwnerConfigCategorySerializer(serializers.Serializer):
    category = OwnerCategorySerializer()
    capabilities = CategoryCapabilitiesSerializer()
    verificationRequirements = OwnerVerificationRequirementSerializer(many=True)


class OwnerConfigSerializer(serializers.Serializer):
    province = NamedRefSerializer()
    categories = OwnerConfigCategorySerializer(many=True)


class OwnerFacilitySummarySerializer(serializers.Serializer):
    id = serializers.UUIDField()
    nameAr = serializers.CharField()
    category = NamedRefSerializer()
    province = NamedRefSerializer()
    status = serializers.ChoiceField(choices=Facility.Status.choices)
    lastUpdate = serializers.DateTimeField()
    requiredAction = serializers.ChoiceField(choices=REQUIRED_ACTIONS, allow_null=True)
    capabilities = CategoryCapabilitiesSerializer()


class OwnerFacilitySummaryListSerializer(serializers.Serializer):
    items = OwnerFacilitySummarySerializer(many=True)


class OwnerEvidenceRefSerializer(serializers.Serializer):
    id = serializers.UUIDField()
    requirementId = serializers.UUIDField()
    createdAt = serializers.DateTimeField()


class OwnerHoursEntrySerializer(serializers.Serializer):
    id = serializers.UUIDField()
    weekday = serializers.IntegerField(min_value=0, max_value=6)
    opensAt = serializers.TimeField()
    closesAt = serializers.TimeField()
    sequence = serializers.IntegerField()


class OwnerApplicationSerializer(serializers.Serializer):
    id = serializers.UUIDField()
    kind = serializers.ChoiceField(choices=FacilityApplication.Kind.choices)
    status = serializers.ChoiceField(choices=FacilityApplication.Status.choices)
    rejectionReason = serializers.CharField(allow_null=True)
    submittedAt = serializers.DateTimeField(allow_null=True)


class OwnerFacilityDetailSerializer(OwnerFacilitySummarySerializer):
    nameEn = serializers.CharField(allow_null=True)
    descriptionAr = serializers.CharField(allow_null=True)
    descriptionEn = serializers.CharField(allow_null=True)
    phone = serializers.CharField(allow_null=True)
    addressAr = serializers.CharField(allow_null=True)
    addressEn = serializers.CharField(allow_null=True)
    cityId = serializers.UUIDField(allow_null=True)
    neighborhoodId = serializers.UUIDField(allow_null=True)
    location = CoordinatesSerializer(allow_null=True)
    specialtyIds = serializers.ListField(child=serializers.UUIDField())
    serviceTagIds = serializers.ListField(child=serializers.UUIDField())
    evidence = OwnerEvidenceRefSerializer(many=True)
    hours = OwnerHoursEntrySerializer(many=True)
    application = OwnerApplicationSerializer(allow_null=True)


class OwnerSubmitResultSerializer(serializers.Serializer):
    applicationId = serializers.UUIDField()
    status = serializers.ChoiceField(choices=FacilityApplication.Status.choices)
    submittedAt = serializers.DateTimeField()


class OwnerFacilityImageSerializer(serializers.Serializer):
    id = serializers.UUIDField()
    url = serializers.CharField(
        help_text=(
            "Time-limited signed URL for the public image object. The raw storage key is "
            "never returned."
        )
    )
    sortOrder = serializers.IntegerField()
    width = serializers.IntegerField()
    height = serializers.IntegerField()


class OwnerFacilityImageListSerializer(serializers.Serializer):
    items = OwnerFacilityImageSerializer(many=True)


class OwnerEvidenceCreatedSerializer(serializers.Serializer):
    id = serializers.UUIDField()
    requirementId = serializers.UUIDField()


class OwnerMemberSerializer(serializers.Serializer):
    userId = serializers.UUIDField()
    name = serializers.CharField()
    phone = serializers.CharField()
    role = serializers.ChoiceField(choices=FacilityMembership.Role.choices)


class OwnerMemberListSerializer(serializers.Serializer):
    items = OwnerMemberSerializer(many=True)


class OwnerMemberUpsertedSerializer(serializers.Serializer):
    userId = serializers.UUIDField()
    name = serializers.CharField()
    role = serializers.ChoiceField(choices=FacilityMembership.Role.choices)
