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

from typing import Any

from rest_framework import serializers

from content_services.models import Advertisement
from core.openapi import CoordinatesSerializer
from directory.models import Category
from directory.services import SPECIALTY_SCOPES
from facilities.models import Facility, FacilityApplication, FacilityReport
from storage.public_media import public_media_url

from .quality import QUALITY_ISSUE_CHOICES
from .review import DUPLICATE_REASON_CHOICES


class AdminUserSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    name = serializers.CharField()
    phone = serializers.CharField()
    active = serializers.BooleanField()
    provinceId = serializers.UUIDField(allow_null=True)
    createdAt = serializers.DateTimeField(allow_null=True)
    updatedAt = serializers.DateTimeField(allow_null=True)


class AdminUserDetailSerializer(AdminUserSerializer):
    # `AdminRole` is keyed by an integer, as `AdminRoleSerializer.id` says; this said UUID.
    roleIds = serializers.ListField(child=serializers.IntegerField())


class AdminUserListSerializer(serializers.Serializer[Any]):
    items = AdminUserSerializer(many=True)


class AdminFacilitySerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    nameAr = serializers.CharField()
    nameEn = serializers.CharField(allow_null=True)
    categoryId = serializers.UUIDField()
    provinceId = serializers.UUIDField()
    cityId = serializers.UUIDField(allow_null=True)
    status = serializers.ChoiceField(choices=Facility.Status.choices)
    location = CoordinatesSerializer(allow_null=True)
    updatedAt = serializers.DateTimeField(allow_null=True)
    categoryNameAr = serializers.CharField()
    provinceNameAr = serializers.CharField()
    ownerName = serializers.CharField(allow_null=True, help_text="First owner membership.")
    ownerPhone = serializers.CharField(allow_null=True, help_text="First owner membership.")


class AdminFacilityQualitySerializer(AdminFacilitySerializer):
    """An Admin facility row with its data-quality score (see `admin_console.quality`)."""

    qualityScore = serializers.IntegerField(
        min_value=0, max_value=100, help_text="100 minus a fixed penalty per issue."
    )
    qualityIssues = serializers.ListField(
        child=serializers.ChoiceField(choices=QUALITY_ISSUE_CHOICES),
        help_text=(
            "NO_PHOTOS 10, NO_HOURS 15, NO_LOCATION 20, NO_PHONE 20, STALE 10 (nothing changed "
            "or confirmed for 90 days), OPEN_REPORTS 15, NOT_VERIFIED_RECENTLY 10 (never "
            "approved, or not in 180 days). Photos and hours count only where the category "
            "supports them."
        ),
    )


class AdminFacilityListSerializer(serializers.Serializer[Any]):
    items = AdminFacilityQualitySerializer(many=True)


class AdminApplicationSerializer(serializers.Serializer[Any]):
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
    evidenceComplete = serializers.BooleanField(
        help_text=(
            "Whether every active, required document of the facility's category has its "
            "minimum number of files, as submission requires. False when a requirement was "
            "added after the application was sent."
        )
    )
    categoryNameAr = serializers.CharField()
    provinceNameAr = serializers.CharField()
    ownerName = serializers.CharField(allow_null=True)
    ownerPhone = serializers.CharField(allow_null=True)


class AdminApplicationListSerializer(serializers.Serializer[Any]):
    items = AdminApplicationSerializer(many=True)


class AdminEvidenceRefSerializer(serializers.Serializer[Any]):
    """Reviewer-facing evidence reference. The object key and any URL are withheld."""

    id = serializers.UUIDField()
    requirementId = serializers.IntegerField()
    labelAr = serializers.CharField()


class AdminAuditTrailEntrySerializer(serializers.Serializer[Any]):
    action = serializers.CharField()
    requestId = serializers.CharField(source="request_id", allow_blank=True)
    createdAt = serializers.DateTimeField(source="created_at")


class AdminPublicImageSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    url = serializers.CharField(help_text="Permanent public media address.")


class AdminDuplicateCandidateSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    nameAr = serializers.CharField()
    status = serializers.ChoiceField(choices=Facility.Status.choices)
    reasons = serializers.ListField(
        child=serializers.ChoiceField(choices=DUPLICATE_REASON_CHOICES)
    )


class AdminApplicationDetailSerializer(AdminApplicationSerializer):
    facility = AdminFacilitySerializer()
    snapshot = serializers.DictField(help_text="Redacted submission snapshot.")
    previous = serializers.DictField(
        allow_null=True,
        help_text=(
            "Snapshot of the last approved application of this facility (plus `approvedAt`), "
            "for diffing a REVERIFICATION. Null when the facility was never approved."
        ),
    )
    location = CoordinatesSerializer(allow_null=True)
    duplicates = AdminDuplicateCandidateSerializer(
        many=True,
        help_text="Up to 5 other facilities with the same phone, or the same normalized "
        "Arabic name within 200 m.",
    )
    publicImageIds = serializers.ListField(child=serializers.UUIDField())
    publicImages = AdminPublicImageSerializer(many=True)
    evidence = AdminEvidenceRefSerializer(many=True)
    audit = AdminAuditTrailEntrySerializer(many=True)


class AdminDecisionRequestSerializer(serializers.Serializer[Any]):
    reason = serializers.CharField(
        required=False,
        allow_blank=True,
        help_text="Required in practice for a rejection; recorded in the audit trail.",
    )


class AdminFacilityStatusCountSerializer(serializers.Serializer[Any]):
    status = serializers.ChoiceField(choices=Facility.Status.choices)
    count = serializers.IntegerField()


class AdminRecentActionSerializer(serializers.Serializer[Any]):
    action = serializers.CharField()
    targetType = serializers.CharField(source="target_type", allow_blank=True)
    targetId = serializers.CharField(source="target_id", allow_blank=True)
    createdAt = serializers.DateTimeField(source="created_at")


class AdminDashboardSerializer(serializers.Serializer[Any]):
    dutyActiveNow = serializers.IntegerField(help_text="Facilities on a duty shift right now.")
    newUsers7d = serializers.IntegerField()
    openReports = serializers.IntegerField(help_text="Facility problem reports still OPEN.")
    systemWarnings = serializers.ListField(
        child=serializers.CharField(), help_text="Arabic, configuration-level warnings."
    )
    pendingReviews = serializers.IntegerField()
    reverification = serializers.IntegerField()
    facilitiesByStatus = AdminFacilityStatusCountSerializer(many=True)
    activeUsers = serializers.IntegerField()
    recentActions = AdminRecentActionSerializer(many=True)


class AdminRoleSerializer(serializers.Serializer[Any]):
    id = serializers.IntegerField()
    code = serializers.CharField()
    name = serializers.CharField()
    permissions = serializers.ListField(child=serializers.CharField())


class AdminRoleListSerializer(serializers.Serializer[Any]):
    items = AdminRoleSerializer(many=True)


class AdminUserRolesRequestSerializer(serializers.Serializer[Any]):
    roleIds = serializers.ListField(child=serializers.IntegerField())


class AdminCategoryGroupSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    code = serializers.CharField()
    nameAr = serializers.CharField(source="name_ar")
    nameEn = serializers.CharField(source="name_en", allow_blank=True)
    iconKey = serializers.CharField(source="icon_key", allow_blank=True)
    active = serializers.BooleanField()
    sortOrder = serializers.IntegerField(source="sort_order")


class AdminCategoryGroupListSerializer(serializers.Serializer[Any]):
    items = AdminCategoryGroupSerializer(many=True)


class AdminCategorySerializer(serializers.Serializer[Any]):
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


class AdminCategoryListSerializer(serializers.Serializer[Any]):
    items = AdminCategorySerializer(many=True)


class AdminCapabilitiesRequestSerializer(serializers.Serializer[Any]):
    """Capability flags. Omitting one leaves it as it is.

    INT-039: these were the last `supports_*` names on the wire. The column names are
    unchanged; `source` does the translation, as everywhere else in this app.
    """

    supportsHours = serializers.BooleanField(source="supports_hours", required=False)
    supportsPhotos = serializers.BooleanField(source="supports_photos", required=False)
    supportsRatings = serializers.BooleanField(source="supports_ratings", required=False)
    supportsDuty = serializers.BooleanField(
        source="supports_duty",
        required=False,
        help_text="Rejected unless the category's specialization is PHARMACY.",
    )
    supportsSpecialtyFilter = serializers.BooleanField(
        source="supports_specialty_filter", required=False
    )
    supportsServiceFilter = serializers.BooleanField(
        source="supports_service_filter", required=False
    )
    supportsTemporaryClosure = serializers.BooleanField(
        source="supports_temporary_closure", required=False
    )
    supportsOwnerOnboarding = serializers.BooleanField(
        source="supports_owner_onboarding", required=False
    )


class AdminCapabilitiesSerializer(AdminCapabilitiesRequestSerializer):
    """The response echoes every capability flag after the update."""


class AdminCategoryProvinceRequestSerializer(serializers.Serializer[Any]):
    provinceId = serializers.UUIDField()
    publicEnabled = serializers.BooleanField(required=False)
    ownerRegistrationEnabled = serializers.BooleanField(required=False)
    sortOrder = serializers.IntegerField(required=False)


class AdminIdSerializer(serializers.Serializer[Any]):
    id = serializers.CharField(help_text="Identifier of the affected row.")


class AdminMeSerializer(serializers.Serializer[Any]):
    """Who the caller is and what they may do, for the Admin shell to render against.

    Deliberately minimal. No phone, no session material, no Django groups or
    `user_permissions`, and no `is_superuser`: the UI branches on `permissions`, which come
    from the project's own `AdminRole`/`AdminPermission` tables and nothing else.

    A UI gate is not authorization. The backend re-checks every call, and a permission
    revoked mid-session shows up as a 403 the UI has to handle.
    """

    userId = serializers.UUIDField()
    displayName = serializers.CharField()
    permissions = serializers.ListField(
        child=serializers.CharField(),
        help_text=(
            "Every permission code the caller holds, deduplicated and sorted. An operator "
            "whose roles carry no permissions gets an empty list, which is a valid state."
        ),
    )


class AdminCategoryGroupRequestSerializer(serializers.Serializer[Any]):
    """Create or update a category group. `code` is set once and never changes."""

    code = serializers.CharField(
        required=False,
        help_text="Required on create. Refused on update; the group code is immutable.",
    )
    nameAr = serializers.CharField(required=False)
    nameEn = serializers.CharField(required=False, allow_blank=True)
    iconKey = serializers.CharField(required=False, allow_blank=True, max_length=80)
    active = serializers.BooleanField(required=False)
    sortOrder = serializers.IntegerField(required=False)


class AdminCategoryCreateRequestSerializer(serializers.Serializer[Any]):
    groupId = serializers.UUIDField()
    code = serializers.CharField(help_text="Immutable once created.")
    slug = serializers.SlugField(help_text="Immutable once created.")
    nameAr = serializers.CharField()
    nameEn = serializers.CharField(required=False, allow_blank=True)
    iconKey = serializers.CharField(required=False, allow_blank=True)
    specialization = serializers.ChoiceField(
        choices=Category.Specialization.choices, required=False
    )
    active = serializers.BooleanField(required=False, default=True)
    sortOrder = serializers.IntegerField(required=False, default=0)


class AdminCategoryUpdateRequestSerializer(serializers.Serializer[Any]):
    """Everything a category may become. `code` and `slug` are absent on purpose.

    `06-DATA-MODEL.md` marks both immutable and `09-ADMIN-NEXTJS.md` requires that changing
    them silently be prevented, so sending either is refused rather than ignored.
    """

    groupId = serializers.UUIDField(required=False, help_text="Moves the category.")
    nameAr = serializers.CharField(required=False)
    nameEn = serializers.CharField(required=False, allow_blank=True)
    iconKey = serializers.CharField(required=False, allow_blank=True)
    specialization = serializers.ChoiceField(
        choices=Category.Specialization.choices, required=False
    )
    active = serializers.BooleanField(required=False)
    sortOrder = serializers.IntegerField(required=False)


class AdminProvinceSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    code = serializers.CharField()
    nameAr = serializers.CharField(source="name_ar")
    nameEn = serializers.CharField(source="name_en", allow_blank=True)
    active = serializers.BooleanField()
    sortOrder = serializers.IntegerField(source="sort_order")


class AdminProvinceListSerializer(serializers.Serializer[Any]):
    items = AdminProvinceSerializer(many=True)


class AdminProvinceUpdateRequestSerializer(serializers.Serializer[Any]):
    active = serializers.BooleanField(required=False)
    sortOrder = serializers.IntegerField(required=False)


class AdminProvinceUpdatedSerializer(serializers.Serializer[Any]):
    active = serializers.BooleanField()
    sortOrder = serializers.IntegerField()


class AdminVerificationRequirementSerializer(serializers.Serializer[Any]):
    # INT-043: this was declared as a UUID while the model's primary key is a BigAutoField.
    # DRF's UUIDField stringifies without validating on output, so the contract claimed
    # `format: uuid` for a field that actually returns "2" and no test could see it.
    id = serializers.IntegerField()
    categoryId = serializers.UUIDField(source="category_id")
    labelAr = serializers.CharField(source="label_ar")
    labelEn = serializers.CharField(source="label_en", allow_blank=True)
    # The stubs see Field.required, but the serializer metaclass collects this declaration
    # as the wire field "required" and removes it from the class; the name is the contract.
    required = serializers.BooleanField()  # type: ignore[assignment]
    active = serializers.BooleanField()
    minFiles = serializers.IntegerField(source="min_files")
    maxFiles = serializers.IntegerField(source="max_files")
    sortOrder = serializers.IntegerField(source="sort_order")


class AdminVerificationRequirementListSerializer(serializers.Serializer[Any]):
    items = AdminVerificationRequirementSerializer(many=True)


class AdminVerificationRequirementUpdateRequestSerializer(serializers.Serializer[Any]):
    """Edit a requirement in place. The category it belongs to cannot change.

    Evidence rows point at a (facility, requirement) pair, so moving a requirement to
    another category would leave that evidence attached to a rule its facility never had.
    Retirement is `active = false`; there is no delete, because evidence references it.
    """

    labelAr = serializers.CharField(required=False)
    labelEn = serializers.CharField(required=False, allow_blank=True)
    instructionsAr = serializers.CharField(required=False, allow_blank=True)
    instructionsEn = serializers.CharField(required=False, allow_blank=True)
    # The stubs see Field.required, but the serializer metaclass collects this declaration
    # as the wire field "required" and removes it from the class; the name is the contract.
    required = serializers.BooleanField(required=False)  # type: ignore[assignment]
    active = serializers.BooleanField(required=False)
    minFiles = serializers.IntegerField(required=False)
    maxFiles = serializers.IntegerField(required=False)
    sortOrder = serializers.IntegerField(required=False)


class AdminVerificationRequirementRequestSerializer(serializers.Serializer[Any]):
    categoryId = serializers.UUIDField()
    labelAr = serializers.CharField()
    labelEn = serializers.CharField(required=False, allow_blank=True)
    instructionsAr = serializers.CharField(required=False, allow_blank=True)
    instructionsEn = serializers.CharField(required=False, allow_blank=True)
    # The stubs see Field.required, but the serializer metaclass collects this declaration
    # as the wire field "required" and removes it from the class; the name is the contract.
    required = serializers.BooleanField(required=False, default=True)  # type: ignore[assignment]
    active = serializers.BooleanField(required=False, default=True)
    minFiles = serializers.IntegerField(required=False, default=1)
    maxFiles = serializers.IntegerField(required=False, default=1)
    sortOrder = serializers.IntegerField(required=False, default=0)


class AdminAdvertisementSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    titleAr = serializers.CharField(source="title_ar", allow_blank=True)
    targetScope = serializers.ChoiceField(
        source="target_scope", choices=Advertisement.TargetScope.choices
    )
    # The target itself, so an edit opens with it chosen instead of silently clearing it.
    provinceId = serializers.UUIDField(source="province_id", allow_null=True)
    categoryId = serializers.UUIDField(source="category_id", allow_null=True)
    imageUrl = serializers.SerializerMethodField(
        help_text="Where the slide's image is served from, to preview it while editing."
    )
    enabled = serializers.BooleanField()
    startsAt = serializers.DateTimeField(source="starts_at", allow_null=True)
    endsAt = serializers.DateTimeField(source="ends_at", allow_null=True)
    sortOrder = serializers.IntegerField(source="sort_order")
    slideDurationMs = serializers.IntegerField(source="slide_duration_ms")

    def get_imageUrl(self, obj: dict[str, Any]) -> str | None:
        key = obj.get("image_key")
        return public_media_url(key) if key else None


class AdminAdvertisementListSerializer(serializers.Serializer[Any]):
    items = AdminAdvertisementSerializer(many=True)


class AdminAdvertisementRequestSerializer(serializers.Serializer[Any]):
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
    # INT-044: the schedule was missing from the create contract, so a generated client
    # stripped it and every advertisement was created unscheduled — and the rule that an
    # end must follow its start could never fire on creation.
    startsAt = serializers.DateTimeField(required=False, allow_null=True)
    endsAt = serializers.DateTimeField(required=False, allow_null=True)
    enabled = serializers.BooleanField(required=False, default=False)
    sortOrder = serializers.IntegerField(required=False, default=0)
    slideDurationMs = serializers.IntegerField(required=False, default=5000)


class AdminAdvertisementUpdateRequestSerializer(serializers.Serializer[Any]):
    """Edit an advertisement in place; omitted fields keep their current value.

    The field set is fixed by `06-DATA-MODEL.md`. Schedule, targeting and action payload are
    validated by the model, so an end before its start, an out-of-range slide duration, a
    global advertisement carrying a target, or a payload that does not match its action type
    are all refused there rather than re-checked here.
    """

    imageKey = serializers.CharField(required=False)
    titleAr = serializers.CharField(required=False, allow_blank=True)
    titleEn = serializers.CharField(required=False, allow_blank=True)
    subtitleAr = serializers.CharField(required=False, allow_blank=True)
    subtitleEn = serializers.CharField(required=False, allow_blank=True)
    actionType = serializers.ChoiceField(choices=Advertisement.ActionType.choices, required=False)
    actionPayload = serializers.DictField(required=False)
    targetScope = serializers.ChoiceField(
        choices=Advertisement.TargetScope.choices, required=False
    )
    provinceId = serializers.UUIDField(required=False, allow_null=True)
    categoryId = serializers.UUIDField(required=False, allow_null=True)
    startsAt = serializers.DateTimeField(required=False, allow_null=True)
    endsAt = serializers.DateTimeField(required=False, allow_null=True)
    enabled = serializers.BooleanField(required=False)
    sortOrder = serializers.IntegerField(required=False)
    slideDurationMs = serializers.IntegerField(required=False)


class AdminAuditEntrySerializer(serializers.Serializer[Any]):
    """Snapshots in `metadata` are redacted before they are recorded."""

    id = serializers.UUIDField()
    actorId = serializers.UUIDField(source="actor_id", allow_null=True)
    action = serializers.CharField()
    targetType = serializers.CharField(source="target_type", allow_blank=True)
    targetId = serializers.CharField(source="target_id", allow_blank=True)
    requestId = serializers.CharField(source="request_id", allow_blank=True)
    metadata = serializers.DictField()
    createdAt = serializers.DateTimeField(source="created_at")


class AdminAuditListSerializer(serializers.Serializer[Any]):
    items = AdminAuditEntrySerializer(many=True)


class AdminEventCountSerializer(serializers.Serializer[Any]):
    name = serializers.CharField()
    count = serializers.IntegerField()


class AdminAnalyticsPeriodKpisSerializer(serializers.Serializer[Any]):
    """The period-bound KPIs of the comparison period, `from` inclusive, `to` exclusive."""

    approvalMedianHours = serializers.FloatField(allow_null=True)
    searches = serializers.IntegerField()
    zeroResultSearches = serializers.IntegerField()
    facilityViews = serializers.IntegerField()
    directionsRequests = serializers.IntegerField()

    def get_fields(self) -> Any:
        fields = super().get_fields()
        return {
            "from": serializers.DateTimeField(),
            "to": serializers.DateTimeField(),
            **fields,
        }


class AdminAnalyticsSerializer(serializers.Serializer[Any]):
    approvalMedianHours = serializers.FloatField(
        allow_null=True, help_text="Median submit-to-approval time in the period."
    )
    searches = serializers.IntegerField(help_text="search_submitted events in the period.")
    zeroResultSearches = serializers.IntegerField(help_text="search_zero_results in the period.")
    facilityViews = serializers.IntegerField(help_text="facility_view events in the period.")
    directionsRequests = serializers.IntegerField(help_text="directions_start in the period.")
    previous = AdminAnalyticsPeriodKpisSerializer(
        help_text="The same KPIs for the equally long period just before `from`."
    )
    activeFacilities = serializers.IntegerField()
    pendingReviews = serializers.IntegerField()
    ratingAverage = serializers.FloatField(allow_null=True)
    events = AdminEventCountSerializer(many=True, help_text="All-time counts per event name.")

    def get_fields(self) -> Any:
        fields = super().get_fields()
        return {
            "from": serializers.DateTimeField(help_text="Period start, inclusive."),
            "to": serializers.DateTimeField(help_text="Period end, exclusive."),
            **fields,
        }


class AdminSettingSerializer(serializers.Serializer[Any]):
    key = serializers.CharField()
    valueType = serializers.CharField(source="value_type")
    value = serializers.JSONField(allow_null=True)
    updatedAt = serializers.DateTimeField(source="updated_at")


class AdminSettingListSerializer(serializers.Serializer[Any]):
    items = AdminSettingSerializer(many=True)


class AdminSettingWriteRequestSerializer(serializers.Serializer[Any]):
    key = serializers.CharField()
    type = serializers.CharField(required=False)
    value = serializers.JSONField(required=False, allow_null=True)


class AdminSettingWrittenSerializer(serializers.Serializer[Any]):
    key = serializers.CharField()
    type = serializers.CharField()
    value = serializers.JSONField(allow_null=True)


class AdminSystemStatusSerializer(serializers.Serializer[Any]):
    """Configuration presence only. No credential or connection string is exposed."""

    apiVersion = serializers.CharField()
    environment = serializers.CharField()
    database = serializers.ChoiceField(choices=["ok", "unavailable"])
    redis = serializers.ChoiceField(choices=["configured", "unconfigured"])
    celery = serializers.ChoiceField(choices=["configured", "unconfigured"])
    storage = serializers.ChoiceField(choices=["configured", "unconfigured"])
    schemaHash = serializers.CharField()
    checkedAt = serializers.DateTimeField()


class AdminFacilityReportSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    facilityId = serializers.UUIDField()
    facilityNameAr = serializers.CharField()
    reporterId = serializers.UUIDField(allow_null=True)
    reason = serializers.ChoiceField(choices=FacilityReport.Reason.choices)
    note = serializers.CharField(allow_blank=True)
    status = serializers.ChoiceField(choices=FacilityReport.Status.choices)
    createdAt = serializers.DateTimeField()
    resolvedById = serializers.UUIDField(allow_null=True)
    resolvedAt = serializers.DateTimeField(allow_null=True)


class AdminFacilityReportListSerializer(serializers.Serializer[Any]):
    items = AdminFacilityReportSerializer(many=True)


class AdminReportDecisionRequestSerializer(serializers.Serializer[Any]):
    note = serializers.CharField(
        required=False, allow_blank=True, max_length=500, help_text="Recorded in the audit trail."
    )


class AdminCityAdminSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    nameAr = serializers.CharField()
    nameEn = serializers.CharField(allow_null=True)
    code = serializers.CharField()
    active = serializers.BooleanField()


class AdminCityAdminListSerializer(serializers.Serializer[Any]):
    items = AdminCityAdminSerializer(many=True)


class AdminCityUpdateRequestSerializer(serializers.Serializer[Any]):
    active = serializers.BooleanField()


# --------------------------------------------------------------------------------------
# Specialties and services
#
# Integer keys, like the rows. A specialty belongs to one category or to a specialization;
# a service to one category. Neither can move once created.
# --------------------------------------------------------------------------------------

TAG_NAME_MAX = 120
SORT_ORDER_MAX = 2_147_483_647


class AdminSpecialtySerializer(serializers.Serializer[Any]):
    """A specialty a category offers, with how many facilities list it."""

    id = serializers.IntegerField()
    scope = serializers.ChoiceField(
        choices=SPECIALTY_SCOPES,
        help_text=(
            "CATEGORY: offered by one category. SPECIALIZATION: offered by every category "
            "of the specialization."
        ),
    )
    categoryId = serializers.UUIDField(allow_null=True, help_text="Set for the CATEGORY scope.")
    specialization = serializers.ChoiceField(
        choices=Category.Specialization.choices,
        allow_null=True,
        help_text="Set for the SPECIALIZATION scope.",
    )
    nameAr = serializers.CharField()
    nameEn = serializers.CharField(allow_blank=True)
    active = serializers.BooleanField(
        help_text="False retires it: off the public pages and filters, and out of owners' choices."
    )
    sortOrder = serializers.IntegerField()
    facilityCount = serializers.IntegerField(
        help_text=(
            "Facilities that list it. Only an item no facility lists can be deleted; one in "
            "use is retired with `active = false`."
        )
    )


class AdminSpecialtyListSerializer(serializers.Serializer[Any]):
    items = AdminSpecialtySerializer(many=True)


class AdminServiceTagSerializer(serializers.Serializer[Any]):
    """A service of one category, with how many facilities list it."""

    id = serializers.IntegerField()
    categoryId = serializers.UUIDField()
    nameAr = serializers.CharField()
    nameEn = serializers.CharField(allow_blank=True)
    active = serializers.BooleanField(
        help_text="False retires it: off the public pages and filters, and out of owners' choices."
    )
    sortOrder = serializers.IntegerField()
    facilityCount = serializers.IntegerField(
        help_text=(
            "Facilities that list it. Only an item no facility lists can be deleted; one in "
            "use is retired with `active = false`."
        )
    )


class AdminServiceTagListSerializer(serializers.Serializer[Any]):
    items = AdminServiceTagSerializer(many=True)


class AdminServiceTagCreateRequestSerializer(serializers.Serializer[Any]):
    nameAr = serializers.CharField(
        max_length=TAG_NAME_MAX, help_text="Unique within its scope, retired items included."
    )
    nameEn = serializers.CharField(max_length=TAG_NAME_MAX, required=False, allow_blank=True)
    active = serializers.BooleanField(required=False, default=True)
    sortOrder = serializers.IntegerField(
        required=False, default=0, min_value=0, max_value=SORT_ORDER_MAX
    )


class AdminSpecialtyCreateRequestSerializer(AdminServiceTagCreateRequestSerializer):
    scope = serializers.ChoiceField(
        choices=SPECIALTY_SCOPES,
        help_text=(
            "CATEGORY scopes it to the category in the path. SPECIALIZATION shares it with "
            "every category of that category's specialization, and is refused for a GENERIC one."
        ),
    )


class AdminTagUpdateRequestSerializer(serializers.Serializer[Any]):
    """Rename, reorder, retire or bring back a specialty or a service.

    Omitted fields keep their value. The scope is not here: sending `scope`, `categoryId` or
    `specialization` is refused rather than ignored.
    """

    nameAr = serializers.CharField(max_length=TAG_NAME_MAX, required=False)
    nameEn = serializers.CharField(max_length=TAG_NAME_MAX, required=False, allow_blank=True)
    active = serializers.BooleanField(required=False)
    sortOrder = serializers.IntegerField(
        required=False, min_value=0, max_value=SORT_ORDER_MAX
    )
