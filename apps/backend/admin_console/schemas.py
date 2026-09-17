"""Request and response contract for the Admin operations API.

Two deliberate accuracy notes:

* Several list endpoints answer straight from `QuerySet.values(...)`, so their keys are
  snake_case while the hand-built payloads are camelCase. `08-API-CONTRACT.md` asks for a
  single convention at the boundary. The schema describes what the runtime emits today and
  the inconsistency is recorded as a defect instead of being hidden here.
* System status reports only whether a dependency is configured. No secret, connection
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
    request_id = serializers.CharField(allow_blank=True)
    created_at = serializers.DateTimeField()


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
    target_type = serializers.CharField(allow_blank=True)
    target_id = serializers.CharField(allow_blank=True)
    created_at = serializers.DateTimeField()


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
    """Emitted from `QuerySet.values()`, therefore snake_case."""

    id = serializers.UUIDField()
    code = serializers.CharField()
    name_ar = serializers.CharField()
    name_en = serializers.CharField(allow_blank=True)
    active = serializers.BooleanField()
    sort_order = serializers.IntegerField()


class AdminCategoryGroupListSerializer(serializers.Serializer):
    items = AdminCategoryGroupSerializer(many=True)


class AdminCategorySerializer(serializers.Serializer):
    """Emitted from `QuerySet.values()`, therefore snake_case."""

    id = serializers.UUIDField()
    group_id = serializers.UUIDField()
    code = serializers.CharField()
    slug = serializers.CharField()
    name_ar = serializers.CharField()
    name_en = serializers.CharField(allow_blank=True)
    icon_key = serializers.CharField(allow_blank=True)
    specialization = serializers.CharField()
    active = serializers.BooleanField()
    sort_order = serializers.IntegerField()


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
    """Emitted from `QuerySet.values()`, therefore snake_case."""

    id = serializers.UUIDField()
    code = serializers.CharField()
    name_ar = serializers.CharField()
    name_en = serializers.CharField(allow_blank=True)
    active = serializers.BooleanField()
    sort_order = serializers.IntegerField()


class AdminProvinceListSerializer(serializers.Serializer):
    items = AdminProvinceSerializer(many=True)


class AdminProvinceUpdateRequestSerializer(serializers.Serializer):
    active = serializers.BooleanField(required=False)
    sortOrder = serializers.IntegerField(required=False)


class AdminProvinceUpdatedSerializer(serializers.Serializer):
    active = serializers.BooleanField()
    sortOrder = serializers.IntegerField()


class AdminVerificationRequirementSerializer(serializers.Serializer):
    """Emitted from `QuerySet.values()`, therefore snake_case."""

    id = serializers.UUIDField()
    category_id = serializers.UUIDField()
    label_ar = serializers.CharField()
    label_en = serializers.CharField(allow_blank=True)
    required = serializers.BooleanField()
    active = serializers.BooleanField()
    min_files = serializers.IntegerField()
    max_files = serializers.IntegerField()
    sort_order = serializers.IntegerField()


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
    """Emitted from `QuerySet.values()`, therefore snake_case."""

    id = serializers.UUIDField()
    title_ar = serializers.CharField(allow_blank=True)
    target_scope = serializers.ChoiceField(choices=Advertisement.TargetScope.choices)
    enabled = serializers.BooleanField()
    starts_at = serializers.DateTimeField(allow_null=True)
    ends_at = serializers.DateTimeField(allow_null=True)
    sort_order = serializers.IntegerField()
    slide_duration_ms = serializers.IntegerField()


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
    """Emitted from `QuerySet.values()`, therefore snake_case. Snapshots are redacted."""

    id = serializers.UUIDField()
    actor_id = serializers.UUIDField(allow_null=True)
    action = serializers.CharField()
    target_type = serializers.CharField(allow_blank=True)
    target_id = serializers.CharField(allow_blank=True)
    request_id = serializers.CharField(allow_blank=True)
    metadata = serializers.DictField()
    created_at = serializers.DateTimeField()


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
    """Emitted from `QuerySet.values()`, therefore snake_case."""

    key = serializers.CharField()
    value_type = serializers.CharField()
    value = serializers.JSONField(allow_null=True)
    updated_at = serializers.DateTimeField()


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
