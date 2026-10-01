"""Wire contract of the smart admin console: tasks, alerts, search, timeline, bulk actions,
broadcasts, rejection templates, content, emergency numbers, contact messages, readiness,
staff analytics, ad images and the duty roster. camelCase throughout, like `schemas.py`.
"""

from typing import Any

from rest_framework import serializers

from content_services.models import AppRelease, ContactMessage, EmergencyNumber, LegalDocument
from facilities.models import FacilityReport
from facilities.serializers import UploadedFileField
from notifications.models import Broadcast
from pharmacy_duty.models import DutyShift

from .insights import ALERT_KINDS, ALERT_SEVERITIES, LINK_ENTITY_TYPES, READINESS_CODES

# ---------------------------------------------------------------------------------- tasks


class AdminTaskApplicationSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    facilityId = serializers.UUIDField()
    facilityNameAr = serializers.CharField()
    provinceNameAr = serializers.CharField()
    submittedAt = serializers.DateTimeField(allow_null=True)
    ageHours = serializers.FloatField()
    overdue = serializers.BooleanField(help_text="Older than the review SLA.")


class AdminTaskApplicationBucketSerializer(serializers.Serializer[Any]):
    count = serializers.IntegerField()
    overdueCount = serializers.IntegerField()
    oldest = AdminTaskApplicationSerializer(many=True, help_text="Up to 10, oldest first.")


class AdminTaskApplicationsSerializer(serializers.Serializer[Any]):
    initial = AdminTaskApplicationBucketSerializer()
    reverification = AdminTaskApplicationBucketSerializer()


class AdminTaskReportGroupSerializer(serializers.Serializer[Any]):
    facilityId = serializers.UUIDField()
    facilityNameAr = serializers.CharField()
    openCount = serializers.IntegerField()
    oldestAt = serializers.DateTimeField()
    ageHours = serializers.FloatField()
    overdue = serializers.BooleanField()
    reasons = serializers.ListField(
        child=serializers.ChoiceField(choices=FacilityReport.Reason.choices)
    )


class AdminTaskReportsSerializer(serializers.Serializer[Any]):
    count = serializers.IntegerField(help_text="OPEN reports.")
    facilityCount = serializers.IntegerField(help_text="Facilities with an OPEN report.")
    overdueCount = serializers.IntegerField(help_text="OPEN reports older than the SLA.")
    oldest = AdminTaskReportGroupSerializer(
        many=True,
        help_text="Up to 10 facilities: those with 2 or more open reports first, then oldest.",
    )


class AdminTaskReverificationSerializer(serializers.Serializer[Any]):
    facilityId = serializers.UUIDField()
    facilityNameAr = serializers.CharField()
    provinceNameAr = serializers.CharField()
    since = serializers.DateTimeField(help_text="When the facility last changed state.")
    ageHours = serializers.FloatField()
    overdue = serializers.BooleanField()


class AdminTaskReverificationBucketSerializer(serializers.Serializer[Any]):
    count = serializers.IntegerField()
    overdueCount = serializers.IntegerField()
    oldest = AdminTaskReverificationSerializer(many=True)


class AdminTasksSerializer(serializers.Serializer[Any]):
    slaHours = serializers.IntegerField(help_text="Platform setting `review.slaHours`.")
    generatedAt = serializers.DateTimeField()
    applications = AdminTaskApplicationsSerializer()
    reports = AdminTaskReportsSerializer()
    reverificationRequired = AdminTaskReverificationBucketSerializer()


# --------------------------------------------------------------------------------- alerts


class AdminAlertLinkSerializer(serializers.Serializer[Any]):
    entityType = serializers.ChoiceField(choices=LINK_ENTITY_TYPES)
    entityId = serializers.CharField(allow_null=True)
    query = serializers.CharField(
        allow_null=True, help_text="Query string for the linked list screen, if any."
    )


class AdminAlertSerializer(serializers.Serializer[Any]):
    kind = serializers.ChoiceField(choices=ALERT_KINDS)
    severity = serializers.ChoiceField(choices=ALERT_SEVERITIES)
    titleAr = serializers.CharField()
    detailAr = serializers.CharField()
    count = serializers.IntegerField()
    link = AdminAlertLinkSerializer(allow_null=True)


class AdminAlertListSerializer(serializers.Serializer[Any]):
    generatedAt = serializers.DateTimeField()
    items = AdminAlertSerializer(many=True, help_text="Critical first, then warning, then info.")


# --------------------------------------------------------------------------------- search

SEARCH_HIT_TYPES = [("FACILITY", "Facility"), ("USER", "User"), ("APPLICATION", "Application")]


class AdminSearchHitSerializer(serializers.Serializer[Any]):
    type = serializers.ChoiceField(choices=SEARCH_HIT_TYPES)
    id = serializers.UUIDField()
    titleAr = serializers.CharField()
    subtitle = serializers.CharField(allow_blank=True)


class AdminSearchGroupSerializer(serializers.Serializer[Any]):
    type = serializers.ChoiceField(choices=SEARCH_HIT_TYPES)
    items = AdminSearchHitSerializer(many=True, help_text="Up to 5.")


class AdminSearchResultSerializer(serializers.Serializer[Any]):
    query = serializers.CharField()
    groups = AdminSearchGroupSerializer(
        many=True, help_text="Only the groups the caller may read are present."
    )


# ------------------------------------------------------------------------------- timeline

TIMELINE_KINDS = [
    ("APPLICATION_SUBMITTED", "Application submitted"),
    ("APPLICATION_APPROVED", "Application approved"),
    ("APPLICATION_REJECTED", "Application rejected"),
    ("REPORT_CREATED", "Report created"),
    ("REPORT_RESOLVED", "Report resolved"),
    ("REPORT_DISMISSED", "Report dismissed"),
    ("DUTY_SUMMARY", "Upcoming duty shifts"),
    ("AUDIT", "Audited change"),
]


class AdminTimelineEventSerializer(serializers.Serializer[Any]):
    at = serializers.DateTimeField()
    kind = serializers.ChoiceField(choices=TIMELINE_KINDS)
    titleAr = serializers.CharField()
    actorName = serializers.CharField(allow_null=True)
    requestId = serializers.CharField(allow_null=True)
    action = serializers.CharField(
        allow_null=True, help_text="The audit action code, for AUDIT events."
    )
    refId = serializers.CharField(
        allow_null=True, help_text="Id of the application, report or audited row involved."
    )


class AdminTimelineSerializer(serializers.Serializer[Any]):
    facilityId = serializers.UUIDField()
    items = AdminTimelineEventSerializer(many=True, help_text="Newest first, up to 200.")


# ------------------------------------------------------------------------- bulk reports

BULK_REPORT_ACTIONS = [("resolve", "Resolve"), ("dismiss", "Dismiss")]
BULK_OUTCOMES = [("DECIDED", "Decided"), ("NOT_FOUND", "Not found"), ("NOT_OPEN", "Not open")]


class AdminReportBulkRequestSerializer(serializers.Serializer[Any]):
    ids = serializers.ListField(
        child=serializers.UUIDField(), min_length=1, max_length=100, allow_empty=False
    )
    action = serializers.ChoiceField(choices=BULK_REPORT_ACTIONS)
    note = serializers.CharField(required=False, allow_blank=True, max_length=500)


class AdminReportBulkResultSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    outcome = serializers.ChoiceField(choices=BULK_OUTCOMES)
    status = serializers.ChoiceField(
        choices=FacilityReport.Status.choices,
        allow_null=True,
        help_text="The report's status after the call; null when it was not found.",
    )


class AdminReportBulkResponseSerializer(serializers.Serializer[Any]):
    action = serializers.ChoiceField(choices=BULK_REPORT_ACTIONS)
    decided = serializers.IntegerField()
    results = AdminReportBulkResultSerializer(many=True)


# --------------------------------------------------------------------------- broadcasts


class AdminBroadcastRequestSerializer(serializers.Serializer[Any]):
    titleAr = serializers.CharField(max_length=180)
    bodyAr = serializers.CharField(max_length=400)
    provinceId = serializers.UUIDField(required=False, allow_null=True)
    audience = serializers.ChoiceField(choices=Broadcast.Audience.choices)


class AdminBroadcastSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    titleAr = serializers.CharField()
    bodyAr = serializers.CharField()
    audience = serializers.ChoiceField(choices=Broadcast.Audience.choices)
    provinceId = serializers.UUIDField(allow_null=True)
    recipientCount = serializers.IntegerField()
    actorId = serializers.UUIDField(allow_null=True)
    actorName = serializers.CharField(allow_null=True)
    createdAt = serializers.DateTimeField()


class AdminBroadcastPageSerializer(serializers.Serializer[Any]):
    items = AdminBroadcastSerializer(many=True)
    nextCursor = serializers.CharField(allow_null=True)
    hasMore = serializers.BooleanField()


# ------------------------------------------------------------------ rejection templates


class AdminRejectionTemplateSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    titleAr = serializers.CharField()
    bodyAr = serializers.CharField()
    active = serializers.BooleanField()
    sortOrder = serializers.IntegerField()


class AdminRejectionTemplateListSerializer(serializers.Serializer[Any]):
    items = AdminRejectionTemplateSerializer(many=True)


class AdminRejectionTemplateRequestSerializer(serializers.Serializer[Any]):
    """Required on create; on update every field is optional."""

    titleAr = serializers.CharField(max_length=120)
    bodyAr = serializers.CharField(max_length=1000)
    active = serializers.BooleanField(required=False, default=True)
    sortOrder = serializers.IntegerField(required=False, default=0, min_value=0)


# ---------------------------------------------------------------------------- content


class AdminContentPageSerializer(serializers.Serializer[Any]):
    slug = serializers.CharField()
    kind = serializers.ChoiceField(choices=LegalDocument.Kind.choices)
    titleAr = serializers.CharField(help_text="The newest version's words.")
    bodyAr = serializers.CharField()
    published = serializers.BooleanField()
    version = serializers.IntegerField(help_text="The newest version.")
    publishedVersion = serializers.IntegerField(allow_null=True)
    hasUnpublishedChanges = serializers.BooleanField()
    builtIn = serializers.BooleanField(
        help_text="A page the apps link to: can be unpublished, not deleted."
    )
    publishedAt = serializers.DateTimeField(allow_null=True)
    updatedAt = serializers.DateTimeField()


class AdminContentPageListSerializer(serializers.Serializer[Any]):
    items = AdminContentPageSerializer(many=True)


class AdminContentPageCreateRequestSerializer(serializers.Serializer[Any]):
    slug = serializers.RegexField(
        r"^[A-Za-z0-9](?:[A-Za-z0-9-]{0,62}[A-Za-z0-9])?$",
        help_text="URL slug: letters, digits and hyphens. Case-insensitive, unique.",
    )
    kind = serializers.ChoiceField(choices=LegalDocument.Kind.choices, default="PAGE")
    titleAr = serializers.CharField(max_length=180)
    bodyAr = serializers.CharField(max_length=50000)
    published = serializers.BooleanField(required=False, default=False)


class AdminContentPageUpdateRequestSerializer(serializers.Serializer[Any]):
    kind = serializers.ChoiceField(choices=LegalDocument.Kind.choices, required=False)
    titleAr = serializers.CharField(max_length=180, required=False)
    bodyAr = serializers.CharField(max_length=50000, required=False)
    published = serializers.BooleanField(
        required=False,
        help_text="true publishes the newest version; false unpublishes the page.",
    )


class AdminFaqEntrySerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    questionAr = serializers.CharField()
    answerAr = serializers.CharField()
    sortOrder = serializers.IntegerField()
    published = serializers.BooleanField()
    updatedAt = serializers.DateTimeField()


class AdminFaqEntryListSerializer(serializers.Serializer[Any]):
    items = AdminFaqEntrySerializer(many=True)


class AdminFaqEntryRequestSerializer(serializers.Serializer[Any]):
    """Required on create; on update every field is optional."""

    questionAr = serializers.CharField(max_length=300)
    answerAr = serializers.CharField(max_length=4000)
    sortOrder = serializers.IntegerField(required=False, default=0, min_value=0)
    published = serializers.BooleanField(required=False, default=False)


class AdminEmergencyNumberSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    provinceId = serializers.UUIDField(allow_null=True, help_text="Null means national.")
    provinceNameAr = serializers.CharField(allow_null=True)
    labelAr = serializers.CharField()
    phone = serializers.CharField()
    kind = serializers.ChoiceField(choices=EmergencyNumber.Kind.choices)
    sortOrder = serializers.IntegerField()
    active = serializers.BooleanField()
    adminNote = serializers.CharField(
        allow_blank=True, help_text="Operators only; never served publicly."
    )
    updatedAt = serializers.DateTimeField()


class AdminEmergencyNumberListSerializer(serializers.Serializer[Any]):
    items = AdminEmergencyNumberSerializer(many=True)


class AdminEmergencyNumberRequestSerializer(serializers.Serializer[Any]):
    """Required on create; on update every field is optional."""

    provinceId = serializers.UUIDField(required=False, allow_null=True)
    labelAr = serializers.CharField(max_length=120)
    phone = serializers.RegexField(r"^\+?[0-9]{2,15}$", max_length=20)
    kind = serializers.ChoiceField(choices=EmergencyNumber.Kind.choices)
    sortOrder = serializers.IntegerField(required=False, default=0, min_value=0)
    active = serializers.BooleanField(required=False, default=True)
    adminNote = serializers.CharField(required=False, allow_blank=True, max_length=240)


class AdminContactMessageSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    name = serializers.CharField()
    phone = serializers.CharField(allow_null=True)
    message = serializers.CharField()
    kind = serializers.ChoiceField(choices=ContactMessage.Kind.choices)
    userId = serializers.UUIDField(allow_null=True, help_text="Set when sent signed in.")
    handled = serializers.BooleanField()
    handledAt = serializers.DateTimeField(allow_null=True)
    handledById = serializers.UUIDField(allow_null=True)
    createdAt = serializers.DateTimeField()


class AdminContactMessagePageSerializer(serializers.Serializer[Any]):
    items = AdminContactMessageSerializer(many=True)
    nextCursor = serializers.CharField(allow_null=True)
    hasMore = serializers.BooleanField()


class AdminContactHandleRequestSerializer(serializers.Serializer[Any]):
    note = serializers.CharField(
        required=False, allow_blank=True, max_length=500, help_text="Recorded in the audit."
    )


# -------------------------------------------------------------------------- readiness


class AdminReadinessItemSerializer(serializers.Serializer[Any]):
    code = serializers.ChoiceField(choices=READINESS_CODES)
    ok = serializers.BooleanField()
    detailAr = serializers.CharField()


class AdminProvinceReadinessSerializer(serializers.Serializer[Any]):
    provinceId = serializers.UUIDField()
    provinceNameAr = serializers.CharField()
    ready = serializers.BooleanField(help_text="Every item is ok.")
    minActiveFacilities = serializers.IntegerField(
        help_text="Platform setting `readiness.minActiveFacilities`."
    )
    items = AdminReadinessItemSerializer(many=True)


# -------------------------------------------------------------------------- analytics


class AdminStaffMemberSerializer(serializers.Serializer[Any]):
    userId = serializers.UUIDField()
    name = serializers.CharField()
    decisions = serializers.IntegerField(help_text="Applications approved or rejected.")
    approvals = serializers.IntegerField()
    rejections = serializers.IntegerField()
    medianDecisionHours = serializers.FloatField(
        allow_null=True, help_text="Median submit-to-decision hours of those decisions."
    )
    reportDecisions = serializers.IntegerField(help_text="Problem reports resolved or dismissed.")


class AdminStaffPerformanceSerializer(serializers.Serializer[Any]):
    to = serializers.DateTimeField(help_text="Period end, exclusive.")
    items = AdminStaffMemberSerializer(many=True)

    def get_fields(self) -> Any:
        # `from` is a Python keyword, so it cannot be declared as a class attribute.
        return {"from": serializers.DateTimeField(), **super().get_fields()}


# ------------------------------------------------------------------------------ media


class AdminAdImageUploadSerializer(serializers.Serializer[Any]):
    file = UploadedFileField(help_text="JPEG, PNG or WebP, at most 2 MB, 100-4096 px a side.")


class AdminAdImageSerializer(serializers.Serializer[Any]):
    imageKey = serializers.CharField(help_text="Pass as `imageKey` when saving the ad.")
    url = serializers.CharField(help_text="Permanent public media address.")
    width = serializers.IntegerField()
    height = serializers.IntegerField()


# ------------------------------------------------------------------------------- duty

SHIFT_STATUSES = [("UPCOMING", "Upcoming"), ("ONGOING", "Ongoing"), ("ENDED", "Ended")]


class AdminDutyShiftSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    facilityId = serializers.UUIDField()
    facilityNameAr = serializers.CharField()
    cityId = serializers.UUIDField(allow_null=True)
    startsAt = serializers.DateTimeField()
    endsAt = serializers.DateTimeField()
    createdBy = serializers.ChoiceField(
        choices=DutyShift.Source.choices, help_text="Who put the shift on the roster."
    )
    status = serializers.ChoiceField(choices=SHIFT_STATUSES)


class AdminDutyDaySerializer(serializers.Serializer[Any]):
    date = serializers.DateField(help_text="A Damascus calendar day.")
    gap = serializers.BooleanField(
        help_text="No ACTIVE pharmacy covers any part of this day (the DUTY_GAP rule)."
    )
    shifts = AdminDutyShiftSerializer(many=True, help_text="Shifts overlapping this day.")


class AdminDutyRosterSerializer(serializers.Serializer[Any]):
    provinceId = serializers.UUIDField()
    cityId = serializers.UUIDField(allow_null=True)
    to = serializers.DateField(help_text="Last day, inclusive.")
    days = AdminDutyDaySerializer(many=True)

    def get_fields(self) -> Any:
        # `from` is a Python keyword, so it cannot be declared as a class attribute.
        return {"from": serializers.DateField(help_text="First day."), **super().get_fields()}


class AdminDutyShiftCreateRequestSerializer(serializers.Serializer[Any]):
    facilityId = serializers.UUIDField()
    startsAt = serializers.DateTimeField()
    endsAt = serializers.DateTimeField()


class AdminDutyShiftUpdateRequestSerializer(serializers.Serializer[Any]):
    startsAt = serializers.DateTimeField(required=False)
    endsAt = serializers.DateTimeField(required=False)


class AdminAppReleaseSerializer(serializers.Serializer[Any]):
    platform = serializers.ChoiceField(choices=AppRelease.Platform.choices)
    minimumVersionCode = serializers.IntegerField()
    latestVersionCode = serializers.IntegerField()
    storeUrl = serializers.CharField(allow_blank=True)
    noticeAr = serializers.CharField(allow_blank=True)
    updatedAt = serializers.DateTimeField(allow_null=True)


class AdminAppReleaseRequestSerializer(serializers.Serializer[Any]):
    minimumVersionCode = serializers.IntegerField(
        min_value=0,
        help_text="A build below this is refused. Zero refuses nobody.",
    )
    latestVersionCode = serializers.IntegerField(
        min_value=0,
        help_text="The newest build there is. Must not be below the minimum.",
    )
    storeUrl = serializers.CharField(
        allow_blank=True,
        max_length=200,
        help_text="Where a blocked person is sent. Empty shows the notice without a button.",
    )
    noticeAr = serializers.CharField(
        allow_blank=True,
        help_text="What the blocking screen says. Empty uses the app's own wording.",
    )

    def validate(self, attrs: dict[str, Any]) -> dict[str, Any]:
        if attrs["minimumVersionCode"] > attrs["latestVersionCode"]:
            raise serializers.ValidationError(
                {
                    "minimumVersionCode": [
                        "Cannot be above latestVersionCode: nobody can install a build that "
                        "does not exist."
                    ]
                }
            )
        return attrs
