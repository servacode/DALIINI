"""Request and response contract for the Admin operations API.

Every response is camelCase, as `08-API-CONTRACT.md` requires. The list endpoints read
through `QuerySet.values(...)` so a grid page does not instantiate models, and the
serializers below translate those snake_case column names to the wire names with `source`.
No database column is renamed for this; the boundary is the only place the two conventions
meet. Before this batch the `values()` endpoints returned raw column names, which was
INT-036.

System status reports only whether a dependency is configured. No secret, connection
string or credential is described or returned.
"""

from rest_framework import serializers

from content_services.models import Advertisement
from core.openapi import CoordinatesSerializer
from facilities.models import Facility, FacilityApplication


class AdminUserSerializer(serializers.Serializer):
    id = serializers.UUIDField()
    name = serializers.CharField()
    phone = serializers.CharField()
    active = serializers.BooleanField()
    provinceId = serializers.UUIDField(allow_null=True)
    createdAt = serializers.DateTimeField(allow_null=True)
    updatedAt = serializers.DateTimeField(allow_null=True)


class AdminUserDetailSerializer(AdminUserSerializer):
    roleIds = serializers.ListField(child=serializers.UUIDField())


class AdminUserListSerializer(serializers.Serializer):
    items = AdminUserSerializer(many=True)


class AdminFacilitySerializer(serializers.Serializer):
    id = serializers.UUIDField()
    nameAr = serializers.CharField()
    nameEn = serializers.CharField(allow_null=True)
    categoryId = serializers.UUIDField()
    provinceId = serializers.UUIDField()
    cityId = serializers.UUIDField(allow_null=True)
    status = serializers.ChoiceField(choices=Facility.Status.choices)
    location = CoordinatesSerializer(allow_null=True)
    updatedAt = serializers.DateTimeField(allow_null=True)


class AdminFacilityListSerializer(serializers.Serializer):
    items = AdminFacilitySerializer(many=True)


class AdminApplicationSerializer(serializers.Serializer):
    id = serializers.UUIDField()
    facilityId = serializers.UUIDField()
    facilityNameAr = serializers.CharField()
    kind = serializers.ChoiceField(choices=FacilityApplication.Kind.choices)
    status = serializers.ChoiceField(choices=FacilityApplication.Status.choices)
    provinceId = serializers.UUIDField()
    categoryId = serializers.UUIDField()
    submittedAt = serializers.DateTimeField(allow_null=True)
    reviewedAt = serializers.DateTimeField(allow_null=True)
    rejectionReason = serializers.CharField(allow_null=True)


class AdminApplicationListSerializer(serializers.Serializer):
    items = AdminApplicationSerializer(many=True)


class AdminEvidenceRefSerializer(serializers.Serializer):
    """Reviewer-facing evidence reference. The object key and any URL are withheld."""

    id = serializers.UUIDField()
    requirementId = serializers.UUIDField()
    labelAr = serializers.CharField()


class AdminAuditTrailEntrySerializer(serializers.Serializer):
    action = serializers.CharField()
    requestId = serializers.CharField(source="request_id", allow_blank=True)
    createdAt = serializers.DateTimeField(source="created_at")


class AdminApplicationDetailSerializer(AdminApplicationSerializer):
    facility = AdminFacilitySerializer()
    snapshot = serializers.DictField(help_text="Redacted submission snapshot.")
    publicImageIds = serializers.ListField(child=serializers.UUIDField())
    evidence = AdminEvidenceRefSerializer(many=True)
    audit = AdminAuditTrailEntrySerializer(many=True)


class AdminDecisionRequestSerializer(serializers.Serializer):
    reason = serializers.CharField(
        required=False,
        allow_blank=True,
        help_text="Required in practice for a rejection; recorded in the audit trail.",
    )


class AdminFacilityStatusCountSerializer(serializers.Serializer):
    status = serializers.ChoiceField(choices=Facility.Status.choices)
    count = serializers.IntegerField()


class AdminRecentActionSerializer(serializers.Serializer):
    action = serializers.CharField()
    targetType = serializers.CharField(source="target_type", allow_blank=True)
    targetId = serializers.CharField(source="target_id", allow_blank=True)
    createdAt = serializers.DateTimeField(source="created_at")


class AdminDashboardSerializer(serializers.Serializer):
    pendingReviews = serializers.IntegerField()
    reverification = serializers.IntegerField()
    facilitiesByStatus = AdminFacilityStatusCountSerializer(many=True)
    activeUsers = serializers.IntegerField()
    recentActions = AdminRecentActionSerializer(many=True)


class AdminRoleSerializer(serializers.Serializer):
    id = serializers.IntegerField()
    code = serializers.CharField()
    name = serializers.CharField()
    permissions = serializers.ListField(child=serializers.CharField())


class AdminRoleListSerializer(serializers.Serializer):
    items = AdminRoleSerializer(many=True)


class AdminUserRolesRequestSerializer(serializers.Serializer):
    roleIds = serializers.ListField(child=serializers.UUIDField())


class AdminCategoryGroupSerializer(serializers.Serializer):
    id = serializers.UUIDField()
    code = serializers.CharField()
    nameAr = serializers.CharField(source="name_ar")
    nameEn = serializers.CharField(source="name_en", allow_blank=True)
    active = serializers.BooleanField()
    sortOrder = serializers.IntegerField(source="sort_order")


class AdminCategoryGroupListSerializer(serializers.Serializer):
    items = AdminCategoryGroupSerializer(many=True)


class AdminCategorySerializer(serializers.Serializer):
    id = serializers.UUIDField()
    groupId = serializers.UUIDField(source="group_id")
    code = serializers.CharField()
    slug = serializers.CharField()
    nameAr = serializers.CharField(source="name_ar")
    nameEn = serializers.CharField(source="name_en", allow_blank=True)
    iconKey = serializers.CharField(source="icon_key", allow_blank=True)
    specialization = serializers.CharField()
    active = serializers.BooleanField()
    sortOrder = serializers.IntegerField(source="sort_order")


class AdminCategoryListSerializer(serializers.Serializer):
    items = AdminCategorySerializer(many=True)


class AdminCapabilitiesRequestSerializer(serializers.Serializer):
    supports_hours = serializers.BooleanField(required=False)
    supports_photos = serializers.BooleanField(required=False)
    supports_ratings = serializers.BooleanField(required=False)
    supports_duty = serializers.BooleanField(required=False)
    supports_specialty_filter = serializers.BooleanField(required=False)
    supports_service_filter = serializers.BooleanField(required=False)
    supports_temporary_closure = serializers.BooleanField(required=False)
    supports_owner_onboarding = serializers.BooleanField(required=False)


class AdminCapabilitiesSerializer(AdminCapabilitiesRequestSerializer):
    """The response echoes every capability flag after the update."""


class AdminCategoryProvinceRequestSerializer(serializers.Serializer):
    provinceId = serializers.UUIDField()
    publicEnabled = serializers.BooleanField(required=False)
    ownerRegistrationEnabled = serializers.BooleanField(required=False)
    sortOrder = serializers.IntegerField(required=False)


class AdminIdSerializer(serializers.Serializer):
    id = serializers.CharField(help_text="Identifier of the affected row.")


class AdminProvinceSerializer(serializers.Serializer):
    id = serializers.UUIDField()
    code = serializers.CharField()
    nameAr = serializers.CharField(source="name_ar")
    nameEn = serializers.CharField(source="name_en", allow_blank=True)
    active = serializers.BooleanField()
    sortOrder = serializers.IntegerField(source="sort_order")


class AdminProvinceListSerializer(serializers.Serializer):
    items = AdminProvinceSerializer(many=True)


class AdminProvinceUpdateRequestSerializer(serializers.Serializer):
    active = serializers.BooleanField(required=False)
    sortOrder = serializers.IntegerField(required=False)


class AdminProvinceUpdatedSerializer(serializers.Serializer):
    active = serializers.BooleanField()
    sortOrder = serializers.IntegerField()


class AdminVerificationRequirementSerializer(serializers.Serializer):
    id = serializers.UUIDField()
    categoryId = serializers.UUIDField(source="category_id")
    labelAr = serializers.CharField(source="label_ar")
    labelEn = serializers.CharField(source="label_en", allow_blank=True)
    required = serializers.BooleanField()
    active = serializers.BooleanField()
    minFiles = serializers.IntegerField(source="min_files")
    maxFiles = serializers.IntegerField(source="max_files")
    sortOrder = serializers.IntegerField(source="sort_order")


class AdminVerificationRequirementListSerializer(serializers.Serializer):
    items = AdminVerificationRequirementSerializer(many=True)


class AdminVerificationRequirementRequestSerializer(serializers.Serializer):
    categoryId = serializers.UUIDField()
    labelAr = serializers.CharField()
    labelEn = serializers.CharField(required=False, allow_blank=True)
    instructionsAr = serializers.CharField(required=False, allow_blank=True)
    instructionsEn = serializers.CharField(required=False, allow_blank=True)
    required = serializers.BooleanField(required=False, default=True)
    active = serializers.BooleanField(required=False, default=True)
    minFiles = serializers.IntegerField(required=False, default=1)
    maxFiles = serializers.IntegerField(required=False, default=1)
    sortOrder = serializers.IntegerField(required=False, default=0)


class AdminAdvertisementSerializer(serializers.Serializer):
    id = serializers.UUIDField()
    titleAr = serializers.CharField(source="title_ar", allow_blank=True)
    targetScope = serializers.ChoiceField(
        source="target_scope", choices=Advertisement.TargetScope.choices
    )
    enabled = serializers.BooleanField()
    startsAt = serializers.DateTimeField(source="starts_at", allow_null=True)
    endsAt = serializers.DateTimeField(source="ends_at", allow_null=True)
    sortOrder = serializers.IntegerField(source="sort_order")
    slideDurationMs = serializers.IntegerField(source="slide_duration_ms")


class AdminAdvertisementListSerializer(serializers.Serializer):
    items = AdminAdvertisementSerializer(many=True)


class AdminAdvertisementRequestSerializer(serializers.Serializer):
    imageKey = serializers.CharField()
    titleAr = serializers.CharField(required=False, allow_blank=True)
    titleEn = serializers.CharField(required=False, allow_blank=True)
    subtitleAr = serializers.CharField(required=False, allow_blank=True)
    subtitleEn = serializers.CharField(required=False, allow_blank=True)
    actionType = serializers.ChoiceField(choices=Advertisement.ActionType.choices, required=False)
    actionPayload = serializers.DictField(required=False)
    targetScope = serializers.ChoiceField(choices=Advertisement.TargetScope.choices, required=False)
    provinceId = serializers.UUIDField(required=False, allow_null=True)
    categoryId = serializers.UUIDField(required=False, allow_null=True)
    enabled = serializers.BooleanField(required=False, default=False)
    sortOrder = serializers.IntegerField(required=False, default=0)
    slideDurationMs = serializers.IntegerField(required=False, default=5000)


class AdminAuditEntrySerializer(serializers.Serializer):
    """Snapshots in `metadata` are redacted before they are recorded."""

    id = serializers.UUIDField()
    actorId = serializers.UUIDField(source="actor_id", allow_null=True)
    action = serializers.CharField()
    targetType = serializers.CharField(source="target_type", allow_blank=True)
    targetId = serializers.CharField(source="target_id", allow_blank=True)
    requestId = serializers.CharField(source="request_id", allow_blank=True)
    metadata = serializers.DictField()
    createdAt = serializers.DateTimeField(source="created_at")


class AdminAuditListSerializer(serializers.Serializer):
    items = AdminAuditEntrySerializer(many=True)


class AdminEventCountSerializer(serializers.Serializer):
    name = serializers.CharField()
    count = serializers.IntegerField()


class AdminAnalyticsSerializer(serializers.Serializer):
    activeFacilities = serializers.IntegerField()
    pendingReviews = serializers.IntegerField()
    ratingAverage = serializers.FloatField(allow_null=True)
    events = AdminEventCountSerializer(many=True)


class AdminSettingSerializer(serializers.Serializer):
    key = serializers.CharField()
    valueType = serializers.CharField(source="value_type")
    value = serializers.JSONField(allow_null=True)
    updatedAt = serializers.DateTimeField(source="updated_at")


class AdminSettingListSerializer(serializers.Serializer):
    items = AdminSettingSerializer(many=True)


class AdminSettingWriteRequestSerializer(serializers.Serializer):
    key = serializers.CharField()
    type = serializers.CharField(required=False)
    value = serializers.JSONField(required=False, allow_null=True)


class AdminSettingWrittenSerializer(serializers.Serializer):
    key = serializers.CharField()
    type = serializers.CharField()
    value = serializers.JSONField(allow_null=True)


class AdminSystemStatusSerializer(serializers.Serializer):
    """Configuration presence only. No credential or connection string is exposed."""

    apiVersion = serializers.CharField()
    environment = serializers.CharField()
    database = serializers.ChoiceField(choices=["ok", "unavailable"])
    redis = serializers.ChoiceField(choices=["configured", "unconfigured"])
    celery = serializers.ChoiceField(choices=["configured", "unconfigured"])
    storage = serializers.ChoiceField(choices=["configured", "unconfigured"])
    schemaHash = serializers.CharField()
    checkedAt = serializers.DateTimeField()
