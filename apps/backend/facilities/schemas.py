"""Response contract for the owner facility surface.

Private verification evidence is represented by identifiers and timestamps only. Raw
object storage keys, private bucket paths and reviewer notes never appear here, per
`15-SECURITY-PRIVACY.md`.
"""

from typing import Any

from rest_framework import serializers

from core.openapi import CoordinatesSerializer, NamedIntRefSerializer, NamedRefSerializer
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


class OwnerVerificationRequirementSerializer(serializers.Serializer[Any]):
    """Safe descriptor of a requirement. The evidence itself is never described here."""

    # The model's integer key, as the Admin contract already declares it (INT-068).
    id = serializers.IntegerField()
    labelAr = serializers.CharField()
    labelEn = serializers.CharField(allow_null=True)
    instructionsAr = serializers.CharField(allow_null=True)
    # The stubs see Field.required, but the serializer metaclass collects this declaration
    # as the wire field "required" and removes it from the class; the name is the contract.
    required = serializers.BooleanField()  # type: ignore[assignment]
    minFiles = serializers.IntegerField()
    maxFiles = serializers.IntegerField()


class OwnerCategorySerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    nameAr = serializers.CharField()
    nameEn = serializers.CharField(allow_null=True)
    iconKey = serializers.CharField(allow_null=True)
    specialization = serializers.ChoiceField(choices=Category.Specialization.choices)


class OwnerConfigCategorySerializer(serializers.Serializer[Any]):
    category = OwnerCategorySerializer()
    capabilities = CategoryCapabilitiesSerializer()
    verificationRequirements = OwnerVerificationRequirementSerializer(many=True)
    specialties = NamedIntRefSerializer(
        many=True,
        help_text=(
            "The specialties an owner may pick, as publicCategoryTagsRetrieve lists them. "
            "Their ids are what `specialtyIds` takes."
        ),
    )
    services = NamedIntRefSerializer(
        many=True,
        help_text=(
            "The services an owner may pick, as publicCategoryTagsRetrieve lists them. "
            "Their ids are what `serviceTagIds` takes."
        ),
    )


class OwnerConfigProvinceSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    nameAr = serializers.CharField()
    mapCenter = CoordinatesSerializer(
        allow_null=True,
        help_text=(
            "Where the location picker opens when the owner's own position is unknown. "
            "Null when no centre has been set."
        ),
    )


class OwnerConfigSerializer(serializers.Serializer[Any]):
    province = OwnerConfigProvinceSerializer()
    categories = OwnerConfigCategorySerializer(many=True)


class OwnerFacilitySummarySerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    nameAr = serializers.CharField()
    category = NamedRefSerializer()
    province = NamedRefSerializer()
    status = serializers.ChoiceField(choices=Facility.Status.choices)
    lastUpdate = serializers.DateTimeField()
    requiredAction = serializers.ChoiceField(choices=REQUIRED_ACTIONS, allow_null=True)
    capabilities = CategoryCapabilitiesSerializer()


class OwnerFacilitySummaryListSerializer(serializers.Serializer[Any]):
    items = OwnerFacilitySummarySerializer(many=True)


class OwnerEvidenceRefSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    requirementId = serializers.IntegerField()
    createdAt = serializers.DateTimeField()


class OwnerHoursEntrySerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    weekday = serializers.IntegerField(min_value=0, max_value=6)
    opensAt = serializers.TimeField()
    closesAt = serializers.TimeField()
    sequence = serializers.IntegerField()


class OwnerApplicationSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    kind = serializers.ChoiceField(choices=FacilityApplication.Kind.choices)
    status = serializers.ChoiceField(choices=FacilityApplication.Status.choices)
    rejectionReason = serializers.CharField(allow_null=True)
    submittedAt = serializers.DateTimeField(allow_null=True)


class OwnerPendingChangeSerializer(serializers.Serializer[Any]):
    """A live facility's edit waiting for review; the facility stays published meanwhile."""

    id = serializers.UUIDField(help_text="The CHANGE application.")
    proposedFields = serializers.ListField(
        child=serializers.ChoiceField(
            choices=[
                (name, name)
                for name in (
                    "nameAr",
                    "nameEn",
                    "addressAr",
                    "addressEn",
                    "cityId",
                    "neighborhoodId",
                    "location",
                )
            ]
        ),
        help_text="Which of the values in this response are proposed rather than published.",
    )
    submittedAt = serializers.DateTimeField(allow_null=True)


class OwnerFacilityDetailSerializer(OwnerFacilitySummarySerializer):
    nameEn = serializers.CharField(allow_null=True)
    descriptionAr = serializers.CharField(allow_null=True)
    descriptionEn = serializers.CharField(allow_null=True)
    phone = serializers.CharField(allow_null=True)
    whatsapp = serializers.CharField(allow_null=True, help_text="E.164 Syrian mobile.")
    addressAr = serializers.CharField(allow_null=True)
    addressEn = serializers.CharField(allow_null=True)
    cityId = serializers.UUIDField(allow_null=True)
    neighborhoodId = serializers.UUIDField(allow_null=True)
    location = CoordinatesSerializer(allow_null=True)
    specialtyIds = serializers.ListField(
        child=serializers.IntegerField(),
        help_text="The facility's active specialties, in order; retired ones are left out.",
    )
    serviceTagIds = serializers.ListField(
        child=serializers.IntegerField(),
        help_text="The facility's active services, in order; retired ones are left out.",
    )
    evidence = OwnerEvidenceRefSerializer(many=True)
    hours = OwnerHoursEntrySerializer(many=True)
    hoursConfirmedAt = serializers.DateTimeField(
        allow_null=True,
        help_text=(
            "When a member last confirmed the opening hours (or replaced them). The app asks "
            "again once this is a week old."
        ),
    )
    application = OwnerApplicationSerializer(allow_null=True)
    pendingChange = OwnerPendingChangeSerializer(
        allow_null=True,
        help_text=(
            "Set while an edit to the live facility waits for review. The listed fields show "
            "the owner's proposed values; the public still sees the published ones."
        ),
    )


class OwnerSubmitResultSerializer(serializers.Serializer[Any]):
    applicationId = serializers.UUIDField()
    status = serializers.ChoiceField(choices=FacilityApplication.Status.choices)
    submittedAt = serializers.DateTimeField()


class OwnerFacilityImageSerializer(serializers.Serializer[Any]):
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


class OwnerFacilityImageListSerializer(serializers.Serializer[Any]):
    items = OwnerFacilityImageSerializer(many=True)


class OwnerEvidenceCreatedSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    requirementId = serializers.IntegerField()


class OwnerMemberSerializer(serializers.Serializer[Any]):
    userId = serializers.UUIDField()
    name = serializers.CharField()
    phone = serializers.CharField()
    role = serializers.ChoiceField(choices=FacilityMembership.Role.choices)


class OwnerMemberListSerializer(serializers.Serializer[Any]):
    items = OwnerMemberSerializer(many=True)


class OwnerMemberUpsertedSerializer(serializers.Serializer[Any]):
    userId = serializers.UUIDField()
    name = serializers.CharField()
    role = serializers.ChoiceField(choices=FacilityMembership.Role.choices)


class OwnerHoursConfirmedSerializer(serializers.Serializer[Any]):
    facilityId = serializers.UUIDField()
    hoursConfirmedAt = serializers.DateTimeField()
    infoConfirmedAt = serializers.DateTimeField(
        help_text="The later of `hoursConfirmedAt` and the last operator approval."
    )
