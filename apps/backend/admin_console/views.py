import logging
import mimetypes
from datetime import datetime, timedelta
from pathlib import PurePosixPath
from typing import Any
from uuid import UUID

from django.core.exceptions import ObjectDoesNotExist
from django.core.exceptions import ValidationError as DjangoValidationError
from django.db import connection, transaction
from django.db.models import Avg, Count, Q
from django.http import FileResponse
from django.shortcuts import get_object_or_404
from django.utils import timezone
from django.utils.dateparse import parse_date, parse_datetime
from drf_spectacular.types import OpenApiTypes
from drf_spectacular.utils import (
    OpenApiParameter,
    OpenApiResponse,
    extend_schema,
    extend_schema_view,
)
from rest_framework.exceptions import NotFound, ValidationError
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from accounts.authentication import AuthenticatedRequest
from accounts.models import AdminRole, User, UserAdminRole
from accounts.rbac import admin_permissions_for
from analytics.models import ProductAnalyticsEvent
from audit.models import AuditEvent
from audit.services import record_audit
from content_services.models import Advertisement
from content_services.services import (
    apply_fields as apply_advertisement_fields,
)
from content_services.services import (
    delete_advertisement,
    save_advertisement,
    update_advertisement,
)
from core.openapi import NOT_FOUND_404, VALIDATION_400, protected
from directory.models import (
    Category,
    CategoryCapabilities,
    CategoryGroup,
    CategoryProvince,
    VerificationRequirement,
)
from directory.services import (
    create_category,
    create_category_group,
    create_verification_requirement,
    update_category,
    update_category_group,
    update_verification_requirement,
)
from facilities.models import Facility, FacilityApplication, FacilityReport, VerificationEvidence
from locations.models import City, Province
from pharmacy_duty.models import DutyShift
from platform_settings.maintenance import get_maintenance_state
from platform_settings.models import PlatformSetting
from platform_settings.operations import TYPED_SETTINGS as TYPED_SETTING_DEFAULTS
from storage.backends import PrivateS3Storage
from storage.public_media import public_media_url

from .permissions import HasAdminPermission, IsAdminOperator
from .quality import QUALITY_ISSUES, filter_issue, quality_payload, with_quality
from .review import find_duplicates, missing_evidence, previous_snapshot, with_evidence_state
from .schemas import (
    AdminAdvertisementListSerializer,
    AdminAdvertisementRequestSerializer,
    AdminAdvertisementSerializer,
    AdminAdvertisementUpdateRequestSerializer,
    AdminAnalyticsSerializer,
    AdminAuditEntrySerializer,
    AdminAuditListSerializer,
    AdminAuditTrailEntrySerializer,
    AdminCapabilitiesRequestSerializer,
    AdminCapabilitiesSerializer,
    AdminCategoryCreateRequestSerializer,
    AdminCategoryGroupListSerializer,
    AdminCategoryGroupRequestSerializer,
    AdminCategoryGroupSerializer,
    AdminCategoryListSerializer,
    AdminCategoryProvinceRequestSerializer,
    AdminCategorySerializer,
    AdminCategoryUpdateRequestSerializer,
    AdminCityAdminListSerializer,
    AdminCityAdminSerializer,
    AdminCityUpdateRequestSerializer,
    AdminDashboardSerializer,
    AdminDecisionRequestSerializer,
    AdminFacilityListSerializer,
    AdminFacilityQualitySerializer,
    AdminFacilityReportListSerializer,
    AdminFacilityReportSerializer,
    AdminFacilitySerializer,
    AdminIdSerializer,
    AdminMeSerializer,
    AdminProvinceListSerializer,
    AdminProvinceSerializer,
    AdminProvinceUpdatedSerializer,
    AdminProvinceUpdateRequestSerializer,
    AdminRecentActionSerializer,
    AdminReportDecisionRequestSerializer,
    AdminRoleListSerializer,
    AdminSettingListSerializer,
    AdminSettingSerializer,
    AdminSettingWriteRequestSerializer,
    AdminSettingWrittenSerializer,
    AdminSystemStatusSerializer,
    AdminUserDetailSerializer,
    AdminUserListSerializer,
    AdminUserRolesRequestSerializer,
    AdminUserSerializer,
    AdminVerificationRequirementListSerializer,
    AdminVerificationRequirementRequestSerializer,
    AdminVerificationRequirementSerializer,
    AdminVerificationRequirementUpdateRequestSerializer,
)
from .schemas import AdminApplicationDetailSerializer as AppDetail
from .schemas import AdminApplicationListSerializer as AppList
from .schemas import AdminApplicationSerializer as App
from .serializers import (
    application_payload,
    facility_payload,
    location_payload,
    user_payload,
    with_application_names,
    with_facility_names,
)
from .services import (
    decide_application,
    decide_report,
    replace_user_roles,
    set_user_blocked,
    transition_facility,
)

logger = logging.getLogger(__name__)


def _request_id(request: Any) -> str:
    return getattr(request, "request_id", "")


def _filter(name: str, description: str) -> OpenApiParameter:
    """Declare an optional query filter. Every one below is already honoured by its view."""
    return OpenApiParameter(
        name, str, OpenApiParameter.QUERY, required=False, description=description
    )


def _validation_error(exc: Any) -> ValidationError:
    if hasattr(exc, "message_dict"):
        return ValidationError(exc.message_dict)
    return ValidationError(getattr(exc, "messages", [str(exc)]))


# Analytics KPI name -> registry event name. Only events the registry records are counted.
KPI_EVENTS = {
    "searches": "search_submitted",
    "zeroResultSearches": "search_zero_results",
    "facilityViews": "facility_view",
    "directionsRequests": "directions_start",
}


# Wire ordering -> columns. Every ordering ends in the primary key so ties are stable.
FACILITY_ORDERINGS: dict[str, tuple[str, ...]] = {
    "qualityScore": ("quality_score", "-updated_at", "id"),
    "-qualityScore": ("-quality_score", "-updated_at", "id"),
    "updatedAt": ("updated_at", "id"),
    "-updatedAt": ("-updated_at", "id"),
}


def filtered_facilities(params: Any) -> Any:
    """The Admin facility list query, with quality annotations, filters and ordering.

    Shared by the list endpoint and the CSV export so the two always select the same rows.
    """
    ordering = params.get("ordering") or "-updatedAt"
    if ordering not in FACILITY_ORDERINGS:
        raise ValidationError({"ordering": f"Use one of {', '.join(FACILITY_ORDERINGS)}."})
    qs = with_quality(with_facility_names(Facility.objects.all()))
    if value := params.get("status"):
        qs = qs.filter(status=value)
    if value := params.get("province"):
        qs = qs.filter(province_id=value)
    if value := params.get("category"):
        qs = qs.filter(category_id=value)
    if value := params.get("q"):
        qs = qs.filter(Q(name_ar__ar_contains=value) | Q(name_en__ar_contains=value))
    if value := params.get("issue"):
        if value not in QUALITY_ISSUES:
            raise ValidationError({"issue": f"Use one of {', '.join(QUALITY_ISSUES)}."})
        qs = filter_issue(qs, value)
    return qs.order_by(*FACILITY_ORDERINGS[ordering])


def filtered_audit(params: Any) -> Any:
    """The audit search query, shared by the list endpoint and the CSV export."""
    qs = AuditEvent.objects.order_by("-created_at")
    if value := params.get("actor"):
        qs = qs.filter(actor_id=value)
    if value := params.get("action"):
        qs = qs.filter(action__icontains=value)
    if value := params.get("resource"):
        qs = qs.filter(Q(target_type__icontains=value) | Q(target_id=value))
    if value := params.get("requestId"):
        qs = qs.filter(request_id=value)
    if value := params.get("from"):
        qs = qs.filter(created_at__gte=_parse_bound(value, "from", end=False))
    if value := params.get("to"):
        bound = _parse_bound(value, "to", end=True)
        qs = (
            qs.filter(created_at__lt=bound)
            if _is_date(value)
            else qs.filter(created_at__lte=bound)
        )
    return qs


def filtered_reports(params: Any) -> Any:
    """The report list query, shared by the list endpoint and the CSV export."""
    qs = FacilityReport.objects.select_related("facility").order_by("-created_at")
    if value := params.get("status"):
        qs = qs.filter(status=value.upper())
    if value := params.get("facility"):
        qs = qs.filter(facility_id=value)
    return qs


def _is_date(value: str) -> bool:
    return len(value) == 10 and parse_date(value) is not None


def _parse_bound(value: str, name: str, *, end: bool) -> datetime:
    """Parse an ISO date or datetime query bound; a bare `to` date means the next midnight."""
    day = parse_date(value) if _is_date(value) else None
    if day is not None:
        if end:
            day = day + timedelta(days=1)
        return timezone.make_aware(datetime.combine(day, datetime.min.time()))
    try:
        moment = parse_datetime(value)
    except ValueError:
        moment = None
    if moment is None:
        raise ValidationError({name: "Expected an ISO date or datetime."})
    return moment if timezone.is_aware(moment) else timezone.make_aware(moment)


def _approval_median_hours(since: datetime, until: datetime | None = None) -> float | None:
    """Median hours from submission to approval, over applications approved in the period."""
    until = until or timezone.now()
    with connection.cursor() as cursor:
        cursor.execute(
            "SELECT percentile_cont(0.5) WITHIN GROUP ("
            "ORDER BY EXTRACT(EPOCH FROM (reviewed_at - submitted_at)) / 3600.0) "
            "FROM facilities_facilityapplication "
            "WHERE status = %s AND reviewed_at >= %s AND reviewed_at < %s "
            "AND submitted_at IS NOT NULL",
            [FacilityApplication.Status.APPROVED, since, until],
        )
        row = cursor.fetchone()
    return round(float(row[0]), 2) if row and row[0] is not None else None


ANALYTICS_DEFAULT_DAYS = 30
ANALYTICS_MAX_DAYS = 366


def analytics_period(request: Any) -> tuple[datetime, datetime]:
    """`from`/`to` query bounds as a half-open period; the default is the last 30 days."""
    now = timezone.now()
    raw_to = request.query_params.get("to")
    end = _parse_bound(raw_to, "to", end=True) if raw_to else now
    raw_from = request.query_params.get("from")
    start = (
        _parse_bound(raw_from, "from", end=False)
        if raw_from
        else end - timedelta(days=ANALYTICS_DEFAULT_DAYS)
    )
    if start >= end:
        raise ValidationError({"from": "Must be before `to`."})
    if end - start > timedelta(days=ANALYTICS_MAX_DAYS):
        raise ValidationError({"from": f"The period may span at most {ANALYTICS_MAX_DAYS} days."})
    return start, end


def _period_kpis(start: datetime, end: datetime) -> dict[str, Any]:
    named = dict(
        ProductAnalyticsEvent.objects.filter(
            occurred_at__gte=start, occurred_at__lt=end, name__in=KPI_EVENTS.values()
        )
        .values_list("name")
        .annotate(count=Count("id"))
        .values_list("name", "count")
    )
    return {
        "approvalMedianHours": _approval_median_hours(start, end),
        **{key: named.get(name, 0) for key, name in KPI_EVENTS.items()},
    }


def _open_reports_count() -> int:
    from facilities.models import FacilityReport

    return FacilityReport.objects.filter(status=FacilityReport.Status.OPEN).count()


def system_warnings() -> list[str]:
    """Cheap, configuration-level warnings for the dashboard. No network calls."""
    from django.conf import settings

    warnings: list[str] = []
    if get_maintenance_state().enabled:
        warnings.append("وضع الصيانة مفعّل: الواجهات العامة ترد بـ 503.")
    if not getattr(settings, "REDIS_URL", ""):
        warnings.append("Redis غير مهيأ: المهام الخلفية والإشعارات اللحظية معطلة.")
    if not getattr(settings, "CELERY_BROKER_URL", ""):
        warnings.append("Celery غير مهيأ: لن تُنفّذ المهام المجدولة.")
    push_provider = str(getattr(settings, "PUSH_PROVIDER", "")).lower()
    if push_provider == "development":
        warnings.append("مزود الإشعارات في وضع التطوير: لن تصل الإشعارات إلى الأجهزة.")
    elif push_provider == "fcm" and not (
        getattr(settings, "FCM_PROJECT_ID", "")
        and str(getattr(settings, "FCM_SERVICE_ACCOUNT_JSON", "")).strip()
    ):
        warnings.append("إشعارات أندرويد غير مربوطة بحساب Firebase: لن تصل إلى الأجهزة.")
    if str(getattr(settings, "OTP_PROVIDER", "")).lower() in {"development", "test"}:
        warnings.append("رموز الدخول في وضع التطوير: لن تصل رسائل التحقق إلى الهواتف.")
    if not getattr(settings, "SENTRY_DSN", ""):
        warnings.append("تتبع الأخطاء (Sentry) غير مفعّل.")
    return warnings


# What a log line may say a request was. Anything else is recorded as OTHER.
LOGGED_METHODS = frozenset({"POST", "PUT", "PATCH", "DELETE"})


class AdminView(APIView):
    permission_classes = [IsAuthenticated, HasAdminPermission]

    def handle_exception(self, exc: Exception) -> Response:
        # Failed admin mutations are operationally interesting even when they are 4xx.
        if self.request.method not in ("GET", "HEAD", "OPTIONS"):
            # One of a fixed set, never the request's own text: a log line is read by tools
            # that trust its shape.
            method = self.request.method if self.request.method in LOGGED_METHODS else "OTHER"
            logger.warning(
                "admin.mutation_failed",
                extra={
                    "view": type(self).__name__,
                    "method": method,
                    "error": type(exc).__name__,
                    "actor_id": str(getattr(self.request.user, "pk", "") or ""),
                },
            )
        return super().handle_exception(exc)


class AdminMeView(APIView):
    """Who the caller is and what they may do.

    Guarded by `IsAdminOperator` rather than a permission code, because requiring one here
    would be circular: the caller is asking which codes they hold. Holding an active Admin
    role is the gate; an ordinary account is refused, and an operator whose roles carry no
    permissions gets an empty list, which is a real state and not an error.

    The permission set comes from `accounts.rbac`, the same resolver `HasAdminPermission`
    uses, so the shell cannot render against a different answer from the one that will be
    enforced. Django `groups`, `user_permissions` and `is_superuser` grant nothing here.
    """

    permission_classes = [IsAuthenticated, IsAdminOperator]

    @extend_schema(
        operation_id="adminMeRetrieve",
        tags=["Admin System"],
        summary="The current operator and the permissions they hold",
        description=(
            "Drives navigation visibility and action gating in the Admin. A UI gate is not "
            "authorization: every endpoint re-checks, and a permission revoked mid-session "
            "surfaces as a 403 on the next call."
        ),
        responses={200: AdminMeSerializer, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        return Response(
            {
                "userId": str(request.user.pk),
                "displayName": request.user.name,
                "permissions": admin_permissions_for(request.user),
            }
        )


class DashboardView(AdminView):
    required_permission = "admin.dashboard.read"

    @extend_schema(
        operation_id="adminDashboardRetrieve",
        tags=["Admin System"],
        summary="Operational counters for the review desk",
        responses={200: AdminDashboardSerializer, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        now = timezone.now()
        return Response(
            {
                "dutyActiveNow": DutyShift.objects.filter(starts_at__lte=now, ends_at__gt=now)
                .values("facility_id")
                .distinct()
                .count(),
                "newUsers7d": User.objects.filter(created_at__gte=now - timedelta(days=7)).count(),
                "openReports": _open_reports_count(),
                "systemWarnings": system_warnings(),
                "pendingReviews": FacilityApplication.objects.filter(
                    status=FacilityApplication.Status.SUBMITTED
                ).count(),
                "reverification": Facility.objects.filter(
                    status=Facility.Status.REVERIFICATION_REQUIRED
                ).count(),
                "facilitiesByStatus": list(
                    Facility.objects.values("status").annotate(count=Count("id"))
                ),
                "activeUsers": User.objects.filter(is_active=True).count(),
                "recentActions": AdminRecentActionSerializer(
                    AuditEvent.objects.order_by("-created_at").values(
                        "action", "target_type", "target_id", "created_at"
                    )[:10],
                    many=True,
                ).data,
            }
        )


class ApplicationListView(AdminView):
    required_permission = "admin.reviews.read"

    @extend_schema(
        operation_id="adminReviewsList",
        tags=["Admin Reviews"],
        summary="List facility applications awaiting or past review",
        description="Capped at 200 rows. Every filter is optional and combines with the rest.",
        parameters=[
            _filter("kind", "Application kind, for example REGISTRATION or REVERIFICATION."),
            _filter("status", "Application status, for example SUBMITTED or APPROVED."),
            _filter("province", "Province id of the facility the application belongs to."),
            _filter("category", "Category id of the facility the application belongs to."),
            _filter(
                "from",
                "Submitted on or after this day (YYYY-MM-DD, Damascus) or this ISO datetime.",
            ),
            _filter(
                "to",
                "Submitted on or before this day (YYYY-MM-DD, Damascus) or before this datetime.",
            ),
            _filter(
                "evidence",
                "`complete` or `incomplete`: whether every required document is uploaded.",
            ),
        ],
        responses={200: AppList, 400: VALIDATION_400, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        qs = with_evidence_state(
            with_application_names(FacilityApplication.objects.all())
        ).order_by("-submitted_at")
        for field, param in (("kind", "kind"), ("status", "status")):
            if value := request.query_params.get(param):
                qs = qs.filter(**{field: value})
        if value := request.query_params.get("province"):
            qs = qs.filter(facility__province_id=value)
        if value := request.query_params.get("category"):
            qs = qs.filter(facility__category_id=value)
        if value := request.query_params.get("from"):
            qs = qs.filter(submitted_at__gte=_parse_bound(value, "from", end=False))
        if value := request.query_params.get("to"):
            qs = qs.filter(submitted_at__lt=_parse_bound(value, "to", end=True))
        evidence = request.query_params.get("evidence")
        if evidence == "complete":
            qs = qs.filter(~missing_evidence())
        elif evidence == "incomplete":
            qs = qs.filter(missing_evidence())
        elif evidence:
            raise ValidationError({"evidence": "Expected `complete` or `incomplete`."})
        return Response({"items": [application_payload(item) for item in qs[:200]]})


class ApplicationDetailView(AdminView):
    required_permission = "admin.reviews.read"

    @extend_schema(
        operation_id="adminReviewRetrieve",
        tags=["Admin Reviews"],
        summary="Retrieve one application with its review context",
        description=(
            "Evidence is referenced by identifier only; content is fetched separately "
            "through the audited evidence endpoint."
        ),
        responses={200: AppDetail, **protected(), 404: NOT_FOUND_404},
    )
    def get(self, request: AuthenticatedRequest, application_id: UUID) -> Response:
        item = get_object_or_404(
            with_application_names(FacilityApplication.objects.all()), pk=application_id
        )
        facility = item.facility
        evidence = facility.evidence.select_related("requirement").all()
        images = list(facility.images.order_by("sort_order", "created_at"))
        return Response(
            {
                **application_payload(item),
                "facility": facility_payload(facility),
                "snapshot": item.snapshot,
                "previous": previous_snapshot(item),
                "location": location_payload(facility),
                "duplicates": find_duplicates(facility),
                "publicImageIds": [str(image.id) for image in images],
                "publicImages": [
                    {"id": str(image.id), "url": public_media_url(image.storage_key)}
                    for image in images
                ],
                "evidence": [
                    {
                        "id": str(row.id),
                        "requirementId": row.requirement_id,
                        "labelAr": row.requirement.label_ar,
                    }
                    for row in evidence
                ],
                "audit": AdminAuditTrailEntrySerializer(
                    AuditEvent.objects.filter(target_id=str(item.id))
                    .order_by("-created_at")
                    .values("action", "request_id", "created_at")[:50],
                    many=True,
                ).data,
            }
        )


class ApplicationDecisionView(AdminView):
    required_permission = "admin.reviews.decide"
    approve = False

    @extend_schema(
        operation_id="adminReviewDecide",
        tags=["Admin Reviews"],
        summary="Decide an application",
        request=AdminDecisionRequestSerializer,
        responses={200: App, 400: VALIDATION_400, **protected(), 404: NOT_FOUND_404},
    )
    def post(self, request: AuthenticatedRequest, application_id: UUID) -> Response:
        try:
            item = decide_application(
                request=request,
                application_id=application_id,
                approve=self.approve,
                reason=str(request.data.get("reason", "")),
            )
        except ObjectDoesNotExist as exc:
            # The service locks the row itself; an id that does not exist is the
            # caller's 404, not a server fault.
            raise NotFound() from exc
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        return Response(application_payload(item))


@extend_schema_view(
    post=extend_schema(
        operation_id="adminReviewApprove",
        tags=["Admin Reviews"],
        summary="Approve an application",
        description=(
            "Runs in one transaction: the application and the facility lifecycle are "
            "locked, the current requirements are re-checked, the change is audited and "
            "the realtime event is emitted only after commit."
        ),
        request=AdminDecisionRequestSerializer,
        responses={200: App, 400: VALIDATION_400, **protected(), 404: NOT_FOUND_404},
    )
)
class ApplicationApproveView(ApplicationDecisionView):
    approve = True


@extend_schema_view(
    post=extend_schema(
        operation_id="adminReviewReject",
        tags=["Admin Reviews"],
        summary="Reject an application",
        description="A reason is recorded in the audit trail; nothing is silently deleted.",
        request=AdminDecisionRequestSerializer,
        responses={200: App, 400: VALIDATION_400, **protected(), 404: NOT_FOUND_404},
    )
)
class ApplicationRejectView(ApplicationDecisionView):
    approve = False


class EvidenceContentView(AdminView):
    required_permission = "admin.evidence.read"

    @extend_schema(
        operation_id="adminEvidenceContentRetrieve",
        tags=["Admin Reviews"],
        summary="Stream one piece of private verification evidence",
        responses={
            200: OpenApiResponse(
                response=OpenApiTypes.BINARY,
                description=(
                    "Evidence bytes. Served with Cache-Control private, no-store and "
                    "X-Content-Type-Options nosniff, and every access is audited."
                ),
            ),
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def get(self, request: AuthenticatedRequest, evidence_id: UUID) -> FileResponse:
        evidence = get_object_or_404(VerificationEvidence, pk=evidence_id)
        record_audit(
            actor=request.user,
            action="verification_evidence.viewed",
            target=evidence,
            metadata={"facilityId": str(evidence.facility_id)},
            request_id=_request_id(request),
        )
        logger.info(
            "evidence.accessed",
            extra={
                "evidence_id": str(evidence.pk),
                "facility_id": str(evidence.facility_id),
                "actor_id": str(request.user.pk),
            },
        )
        try:
            stream = PrivateS3Storage().open(evidence.storage_key, "rb")
        except Exception:
            logger.exception("evidence.open_failed", extra={"evidence_id": str(evidence.pk)})
            raise
        # Evidence is re-encoded to JPEG on upload, and the stored name says so. Serving it as
        # octet-stream told the operator's browser nothing about what it received (INT-066).
        content_type = mimetypes.guess_type(evidence.storage_key)[0] or "application/octet-stream"
        # FileResponse would otherwise name the download after the stream, and an S3 file is
        # named by its object key: the file name must say nothing about where it is stored.
        suffix = PurePosixPath(evidence.storage_key).suffix
        response = FileResponse(
            stream, content_type=content_type, filename=f"evidence-{evidence.pk}{suffix}"
        )
        response["Cache-Control"] = "private, no-store"
        response["X-Content-Type-Options"] = "nosniff"
        return response


class FacilityListView(AdminView):
    required_permission = "admin.facilities.read"

    @extend_schema(
        operation_id="adminFacilitiesList",
        tags=["Admin Facilities"],
        summary="List facilities for operations",
        description=(
            "Capped at 250 rows. Every filter is optional and combines with the rest. Each "
            "row carries `qualityScore` (0-100) and `qualityIssues`, computed in the same "
            "query."
        ),
        parameters=[
            _filter("status", "Facility status, for example ACTIVE or SUSPENDED."),
            _filter("province", "Province id."),
            _filter("category", "Category id."),
            _filter("q", "Free text matched against the Arabic and English facility names."),
            OpenApiParameter(
                "issue",
                str,
                OpenApiParameter.QUERY,
                required=False,
                enum=QUALITY_ISSUES,
                description="Keep facilities that have this quality issue.",
            ),
            OpenApiParameter(
                "ordering",
                str,
                OpenApiParameter.QUERY,
                required=False,
                enum=list(FACILITY_ORDERINGS),
                description="Sort order; the default is `-updatedAt` (most recently changed).",
            ),
        ],
        responses={200: AdminFacilityListSerializer, 400: VALIDATION_400, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        qs = filtered_facilities(request.query_params)
        return Response(
            {"items": [{**facility_payload(item), **quality_payload(item)} for item in qs[:250]]}
        )


class FacilityDetailView(AdminView):
    required_permission = "admin.facilities.read"

    @extend_schema(
        operation_id="adminFacilityRetrieve",
        tags=["Admin Facilities"],
        summary="Retrieve one facility",
        responses={200: AdminFacilityQualitySerializer, **protected(), 404: NOT_FOUND_404},
    )
    def get(self, request: AuthenticatedRequest, facility_id: UUID) -> Response:
        facility = get_object_or_404(
            with_quality(with_facility_names(Facility.objects.all())), pk=facility_id
        )
        return Response({**facility_payload(facility), **quality_payload(facility)})


class FacilityTransitionView(AdminView):
    required_permission = "admin.facilities.manage"
    target_status = ""

    @extend_schema(
        operation_id="adminFacilityTransition",
        tags=["Admin Facilities"],
        summary="Move a facility to a lifecycle state",
        request=AdminDecisionRequestSerializer,
        responses={
            200: AdminFacilitySerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def post(self, request: AuthenticatedRequest, facility_id: UUID) -> Response:
        try:
            facility = transition_facility(
                request=request,
                facility_id=facility_id,
                target_status=self.target_status,
                reason=str(request.data.get("reason", "")),
            )
        except ObjectDoesNotExist as exc:
            # The service locks the row itself; an id that does not exist is the
            # caller's 404, not a server fault.
            raise NotFound() from exc
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        return Response(facility_payload(facility))


@extend_schema_view(
    post=extend_schema(
        operation_id="adminFacilitySuspend",
        tags=["Admin Facilities"],
        summary="Suspend a facility",
        description="A suspended facility leaves public discovery and cannot self-reactivate.",
        request=AdminDecisionRequestSerializer,
        responses={
            200: AdminFacilitySerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
)
class FacilitySuspendView(FacilityTransitionView):
    target_status = Facility.Status.SUSPENDED


@extend_schema_view(
    post=extend_schema(
        operation_id="adminFacilityReactivate",
        tags=["Admin Facilities"],
        summary="Reactivate a suspended facility",
        request=AdminDecisionRequestSerializer,
        responses={
            200: AdminFacilitySerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
)
class FacilityReactivateView(FacilityTransitionView):
    target_status = Facility.Status.ACTIVE


@extend_schema_view(
    post=extend_schema(
        operation_id="adminFacilityClose",
        tags=["Admin Facilities"],
        summary="Close a facility",
        request=AdminDecisionRequestSerializer,
        responses={
            200: AdminFacilitySerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
)
class FacilityCloseView(FacilityTransitionView):
    target_status = Facility.Status.CLOSED


class UserListView(AdminView):
    required_permission = "admin.users.read"

    @extend_schema(
        operation_id="adminUsersList",
        tags=["Admin Users"],
        summary="Search user accounts",
        description=(
            "Password hashes and session secret material are never returned. Capped at 250 "
            "rows. Both filters are optional."
        ),
        parameters=[
            _filter("q", "Free text matched against the account name and phone number."),
            _filter(
                "status",
                "`active` keeps active accounts; any other value keeps blocked accounts.",
            ),
            _filter(
                "role",
                "Admin role id or code; keeps accounts holding that role actively. The value "
                "`any` keeps every operator, `none` every non-operator.",
            ),
        ],
        responses={200: AdminUserListSerializer, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        qs = User.objects.order_by("-created_at")
        if value := request.query_params.get("q"):
            qs = qs.filter(Q(name__icontains=value) | Q(phone__icontains=value))
        if value := request.query_params.get("status"):
            qs = qs.filter(is_active=value.lower() == "active")
        if value := request.query_params.get("role"):
            operators = UserAdminRole.objects.filter(active=True)
            if value == "none":
                qs = qs.exclude(pk__in=operators.values("user_id"))
            else:
                if value != "any":
                    by = Q(role__code=value)
                    if value.isdigit():
                        by |= Q(role_id=int(value))
                    operators = operators.filter(by)
                qs = qs.filter(pk__in=operators.values("user_id"))
        return Response({"items": [user_payload(item) for item in qs[:250]]})


class UserDetailView(AdminView):
    required_permission = "admin.users.read"

    @extend_schema(
        operation_id="adminUserRetrieve",
        tags=["Admin Users"],
        summary="Retrieve one user with the roles assigned",
        responses={200: AdminUserDetailSerializer, **protected(), 404: NOT_FOUND_404},
    )
    def get(self, request: AuthenticatedRequest, user_id: UUID) -> Response:
        user = get_object_or_404(User, pk=user_id)
        payload = user_payload(user)
        # Integers, as `AdminRole` is keyed and as the role list declares its ids.
        payload["roleIds"] = list(
            user.admin_role_links.filter(active=True).values_list("role_id", flat=True)
        )
        return Response(payload)


class UserBlockView(AdminView):
    required_permission = "admin.users.manage"
    blocked = True

    @extend_schema(
        operation_id="adminUserBlock",
        tags=["Admin Users"],
        summary="Block a user account",
        description="Blocking also revokes every active refresh session of that user.",
        request=None,
        responses={200: AdminUserSerializer, **protected(), 404: NOT_FOUND_404},
    )
    def post(self, request: AuthenticatedRequest, user_id: UUID) -> Response:
        user = get_object_or_404(User, pk=user_id)
        updated = set_user_blocked(
            request=request,
            user=user,
            blocked=self.blocked,
        )
        return Response(user_payload(updated))


@extend_schema_view(
    post=extend_schema(
        operation_id="adminUserUnblock",
        tags=["Admin Users"],
        summary="Unblock a user account",
        request=None,
        responses={200: AdminUserSerializer, **protected(), 404: NOT_FOUND_404},
    )
)
class UserUnblockView(UserBlockView):
    blocked = False


class RoleListView(AdminView):
    required_permission = "admin.roles.read"

    @extend_schema(
        operation_id="adminRolesList",
        tags=["Admin Users"],
        summary="List admin roles and their permission codes",
        responses={200: AdminRoleListSerializer, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        items = AdminRole.objects.prefetch_related("permissions").order_by("name")
        return Response(
            {
                "items": [
                    {
                        # An integer, as `AdminRoleSerializer` has always declared it.
                        "id": role.id,
                        "code": role.code,
                        "name": role.name,
                        "permissions": list(role.permissions.values_list("code", flat=True)),
                    }
                    for role in items
                ]
            }
        )


class UserRolesView(AdminView):
    required_permission = "admin.roles.manage"

    @extend_schema(
        operation_id="adminUserRolesReplace",
        tags=["Admin Users"],
        summary="Replace the admin roles of a user",
        description=(
            "Authorization is always re-checked server-side; the Admin UI only hides "
            "actions as a convenience."
        ),
        request=AdminUserRolesRequestSerializer,
        responses={204: None, 400: VALIDATION_400, **protected(), 404: NOT_FOUND_404},
    )
    def put(self, request: AuthenticatedRequest, user_id: UUID) -> Response:
        role_ids = request.data.get("roleIds", [])
        if not isinstance(role_ids, list):
            raise ValidationError({"roleIds": "Must be a list."})
        if AdminRole.objects.filter(id__in=role_ids).count() != len(set(role_ids)):
            raise ValidationError({"roleIds": "Unknown role."})
        user = get_object_or_404(User, pk=user_id)
        replace_user_roles(request=request, user=user, role_ids=role_ids)
        return Response(status=204)


class TaxonomyView(AdminView):
    required_permission = "admin.taxonomy.read"
    # CategoryGroup or Category, set by each subclass.
    model: Any = None

    @extend_schema(
        operation_id="adminTaxonomyList",
        tags=["Admin Taxonomy"],
        summary="List taxonomy rows",
        responses={200: AdminCategoryListSerializer, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        serializer: type[AdminCategoryGroupSerializer] | type[AdminCategorySerializer]
        if self.model is CategoryGroup:
            rows = self.model.objects.order_by("sort_order", "name_ar").values(
                "id", "code", "name_ar", "name_en", "icon_key", "active", "sort_order"
            )
            serializer = AdminCategoryGroupSerializer
        else:
            rows = self.model.objects.order_by("sort_order", "name_ar").values(
                "id",
                "group_id",
                "code",
                "slug",
                "name_ar",
                "name_en",
                "icon_key",
                "specialization",
                "active",
                "sort_order",
            )
            serializer = AdminCategorySerializer
        return Response({"items": serializer(rows, many=True).data})


@extend_schema_view(
    get=extend_schema(
        operation_id="adminCategoryGroupsList",
        tags=["Admin Taxonomy"],
        summary="List category groups",
        responses={200: AdminCategoryGroupListSerializer, **protected()},
    )
)
class CategoryGroupListView(TaxonomyView):
    model = CategoryGroup


@extend_schema_view(
    get=extend_schema(
        operation_id="adminCategoriesList",
        tags=["Admin Taxonomy"],
        summary="List categories",
        responses={200: AdminCategoryListSerializer, **protected()},
    )
)
class CategoryListView(TaxonomyView):
    model = Category


class CategoryGroupCreateView(AdminView):
    """Cycle J begins here: a group has to exist before a category can join it."""

    required_permission = "admin.taxonomy.manage"

    @extend_schema(
        operation_id="adminCategoryGroupCreate",
        tags=["Admin Taxonomy"],
        summary="Create a category group",
        request=AdminCategoryGroupRequestSerializer,
        responses={201: AdminIdSerializer, 400: VALIDATION_400, **protected()},
    )
    def post(self, request: AuthenticatedRequest) -> Response:
        payload = AdminCategoryGroupRequestSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        if not payload.validated_data.get("code") or not payload.validated_data.get("nameAr"):
            raise ValidationError({"code": ["Required."], "nameAr": ["Required."]})
        try:
            group = create_category_group(request=request, data=payload.validated_data)
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        return Response({"id": str(group.pk)}, status=201)


class CategoryGroupDetailView(AdminView):
    required_permission = "admin.taxonomy.manage"

    @extend_schema(
        operation_id="adminCategoryGroupUpdate",
        tags=["Admin Taxonomy"],
        summary="Rename, reorder or deactivate a category group",
        description="The group code is immutable; sending a different one is refused.",
        request=AdminCategoryGroupRequestSerializer,
        responses={
            200: AdminCategoryGroupSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def put(self, request: AuthenticatedRequest, group_id: UUID) -> Response:
        payload = AdminCategoryGroupRequestSerializer(data=request.data, partial=True)
        payload.is_valid(raise_exception=True)
        try:
            group = update_category_group(
                request=request, group_id=group_id, data=payload.validated_data
            )
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        return Response(AdminCategoryGroupSerializer(group).data)


class CategoryCreateView(AdminView):
    required_permission = "admin.taxonomy.manage"

    @extend_schema(
        operation_id="adminCategoryCreate",
        tags=["Admin Taxonomy"],
        summary="Create a category",
        description=(
            "`code` and `slug` are fixed at creation and cannot be changed afterwards. A "
            "new category is invisible everywhere until its per-province switches are "
            "turned on, whatever `active` says."
        ),
        request=AdminCategoryCreateRequestSerializer,
        responses={201: AdminIdSerializer, 400: VALIDATION_400, **protected()},
    )
    def post(self, request: AuthenticatedRequest) -> Response:
        payload = AdminCategoryCreateRequestSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        try:
            category = create_category(request=request, data=payload.validated_data)
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        return Response({"id": str(category.pk)}, status=201)


class CategoryDetailView(AdminView):
    required_permission = "admin.taxonomy.manage"

    @extend_schema(
        operation_id="adminCategoryUpdate",
        tags=["Admin Taxonomy"],
        summary="Rename, move, reorder or deactivate a category",
        description=(
            "`code` and `slug` are immutable and are not accepted. Changing the "
            "specialization re-validates the capability set, so a category that carries "
            "duty cannot be moved off PHARMACY while it does."
        ),
        request=AdminCategoryUpdateRequestSerializer,
        responses={
            200: AdminCategorySerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def put(self, request: AuthenticatedRequest, category_id: UUID) -> Response:
        payload = AdminCategoryUpdateRequestSerializer(data=request.data, partial=True)
        payload.is_valid(raise_exception=True)
        data = dict(payload.validated_data)
        # The serializer drops `code` and `slug`, so an attempt to change them would be
        # silently ignored. 09-ADMIN-NEXTJS.md asks for the opposite: refuse, visibly.
        for field in ("code", "slug"):
            if field in request.data:
                data[field] = request.data[field]
        try:
            category = update_category(request=request, category_id=category_id, data=data)
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        return Response(AdminCategorySerializer(category).data)


class CategoryCapabilitiesView(AdminView):
    required_permission = "admin.taxonomy.manage"

    @extend_schema(
        operation_id="adminCategoryCapabilitiesReplace",
        tags=["Admin Taxonomy"],
        summary="Set the capability flags of a category",
        description="Duty can only be enabled for an approved specialization.",
        request=AdminCapabilitiesRequestSerializer,
        responses={
            200: AdminCapabilitiesSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def put(self, request: AuthenticatedRequest, category_id: UUID) -> Response:
        category = get_object_or_404(Category, pk=category_id)
        capabilities, _ = CategoryCapabilities.objects.get_or_create(category=category)
        payload = AdminCapabilitiesRequestSerializer(data=request.data, partial=True)
        payload.is_valid(raise_exception=True)

        # INT-039: the wire is camelCase and the columns are not. The serializer's `source`
        # mapping is the translation, so `validated_data` already carries column names.
        columns = [
            field.name for field in capabilities._meta.fields if field.name.startswith("supports_")
        ]
        before = {name: getattr(capabilities, name) for name in columns}
        for name, value in payload.validated_data.items():
            setattr(capabilities, name, bool(value))
        try:
            capabilities.full_clean()
            capabilities.save()
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        record_audit(
            actor=request.user,
            action="category.capabilities.updated",
            target=category,
            before_snapshot=before,
            after_snapshot={name: getattr(capabilities, name) for name in columns},
            request_id=_request_id(request),
        )
        return Response(AdminCapabilitiesSerializer(capabilities).data)


class ProvinceListView(AdminView):
    required_permission = "admin.provinces.read"

    @extend_schema(
        operation_id="adminProvincesList",
        tags=["Admin Provinces"],
        summary="List every province",
        responses={200: AdminProvinceListSerializer, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        return Response(
            {
                "items": AdminProvinceSerializer(
                    Province.objects.order_by("sort_order", "name_ar").values(
                        "id", "code", "name_ar", "name_en", "active", "sort_order"
                    ),
                    many=True,
                ).data
            }
        )


class ProvinceDetailView(AdminView):
    required_permission = "admin.provinces.manage"

    @extend_schema(
        operation_id="adminProvinceUpdate",
        tags=["Admin Provinces"],
        summary="Activate a province or change its order",
        request=AdminProvinceUpdateRequestSerializer,
        responses={200: AdminProvinceUpdatedSerializer, **protected(), 404: NOT_FOUND_404},
    )
    def put(self, request: AuthenticatedRequest, province_id: UUID) -> Response:
        province = get_object_or_404(Province, pk=province_id)
        before = {"active": province.active, "sortOrder": province.sort_order}
        if "active" in request.data:
            province.active = bool(request.data["active"])
        if "sortOrder" in request.data:
            province.sort_order = int(request.data["sortOrder"])
        province.save(update_fields=["active", "sort_order"])
        record_audit(
            actor=request.user,
            action="province.updated",
            target=province,
            before_snapshot=before,
            after_snapshot={"active": province.active, "sortOrder": province.sort_order},
            request_id=_request_id(request),
        )
        return Response({"active": province.active, "sortOrder": province.sort_order})


class CategoryProvinceView(AdminView):
    required_permission = "admin.taxonomy.manage"

    @extend_schema(
        operation_id="adminCategoryProvinceReplace",
        tags=["Admin Taxonomy"],
        summary="Set the per-province switches of a category",
        description="Public visibility and owner onboarding are independent switches.",
        request=AdminCategoryProvinceRequestSerializer,
        responses={200: AdminIdSerializer, 400: VALIDATION_400, **protected(), 404: NOT_FOUND_404},
    )
    def put(self, request: AuthenticatedRequest, category_id: UUID) -> Response:
        province_id = request.data.get("provinceId")
        if not province_id:
            raise ValidationError({"provinceId": "Required."})
        row, _ = CategoryProvince.objects.get_or_create(
            category_id=category_id,
            province_id=province_id,
        )
        before = {
            "publicEnabled": row.public_enabled,
            "ownerRegistrationEnabled": row.owner_registration_enabled,
        }
        row.public_enabled = bool(request.data.get("publicEnabled", row.public_enabled))
        row.owner_registration_enabled = bool(
            request.data.get("ownerRegistrationEnabled", row.owner_registration_enabled)
        )
        row.sort_order = int(request.data.get("sortOrder", row.sort_order))
        row.save()
        record_audit(
            actor=request.user,
            action="category.province.updated",
            target=row,
            before_snapshot=before,
            after_snapshot={
                "publicEnabled": row.public_enabled,
                "ownerRegistrationEnabled": row.owner_registration_enabled,
            },
            request_id=_request_id(request),
        )
        return Response({"id": row.pk})


class VerificationRequirementListView(AdminView):
    required_permission = "admin.verification.read"

    @extend_schema(
        operation_id="adminVerificationRequirementsList",
        tags=["Admin Verification"],
        summary="List verification requirements",
        responses={200: AdminVerificationRequirementListSerializer, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        qs = VerificationRequirement.objects.order_by("category_id", "sort_order")
        return Response(
            {
                "items": AdminVerificationRequirementSerializer(
                    qs.values(
                        "id",
                        "category_id",
                        "label_ar",
                        "label_en",
                        "required",
                        "active",
                        "min_files",
                        "max_files",
                        "sort_order",
                    ),
                    many=True,
                ).data
            }
        )

    @extend_schema(
        operation_id="adminVerificationRequirementCreate",
        tags=["Admin Verification"],
        summary="Create a verification requirement",
        description="Requires the manage permission, which is re-checked inside the handler.",
        request=AdminVerificationRequirementRequestSerializer,
        responses={201: AdminIdSerializer, 400: VALIDATION_400, **protected()},
    )
    def post(self, request: AuthenticatedRequest) -> Response:
        self.required_permission = "admin.verification.manage"
        if not HasAdminPermission().has_permission(request, self):
            self.permission_denied(request)
        payload = AdminVerificationRequirementRequestSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        try:
            requirement = create_verification_requirement(
                request=request, data=payload.validated_data
            )
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        return Response({"id": str(requirement.pk)}, status=201)


class VerificationRequirementDetailView(AdminView):
    required_permission = "admin.verification.manage"

    @extend_schema(
        operation_id="adminVerificationRequirementUpdate",
        tags=["Admin Verification"],
        summary="Edit a verification requirement, or retire it",
        description=(
            "The owning category cannot change: evidence already submitted points at a "
            "(facility, requirement) pair. Retirement is `active = false`; there is no "
            "delete, because evidence references the row."
        ),
        request=AdminVerificationRequirementUpdateRequestSerializer,
        responses={
            200: AdminVerificationRequirementSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def put(self, request: AuthenticatedRequest, requirement_id: int) -> Response:
        payload = AdminVerificationRequirementUpdateRequestSerializer(
            data=request.data, partial=True
        )
        payload.is_valid(raise_exception=True)
        data = dict(payload.validated_data)
        if "categoryId" in request.data:
            data["categoryId"] = request.data["categoryId"]
        try:
            requirement = update_verification_requirement(
                request=request, requirement_id=requirement_id, data=data
            )
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        return Response(AdminVerificationRequirementSerializer(requirement).data)


class AdvertisementListView(AdminView):
    required_permission = "admin.ads.read"

    @extend_schema(
        operation_id="adminAdsList",
        tags=["Admin Ads"],
        summary="List advertisements",
        responses={200: AdminAdvertisementListSerializer, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        return Response(
            {
                "items": AdminAdvertisementSerializer(
                    Advertisement.objects.order_by("sort_order", "-updated_at").values(
                        "id",
                        "title_ar",
                        "target_scope",
                        "province_id",
                        "category_id",
                        "image_key",
                        "enabled",
                        "starts_at",
                        "ends_at",
                        "sort_order",
                        "slide_duration_ms",
                    ),
                    many=True,
                ).data
            }
        )

    @extend_schema(
        operation_id="adminAdCreate",
        tags=["Admin Ads"],
        summary="Create an advertisement",
        description=(
            "Requires the manage permission, which is re-checked inside the handler. "
            "Action payloads are validated per action type."
        ),
        request=AdminAdvertisementRequestSerializer,
        responses={201: AdminIdSerializer, 400: VALIDATION_400, **protected()},
    )
    def post(self, request: AuthenticatedRequest) -> Response:
        self.required_permission = "admin.ads.manage"
        if not HasAdminPermission().has_permission(request, self):
            self.permission_denied(request)
        payload = AdminAdvertisementRequestSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        # One field mapping, shared with the update path, so the two cannot drift.
        ad = apply_advertisement_fields(Advertisement(), request.data)
        try:
            save_advertisement(actor=request.user, advertisement=ad)
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        return Response({"id": str(ad.pk)}, status=201)


class AdvertisementDetailView(AdminView):
    required_permission = "admin.ads.manage"

    @extend_schema(
        operation_id="adminAdUpdate",
        tags=["Admin Ads"],
        summary="Edit an advertisement, its schedule or its activation",
        description=(
            "Omitted fields keep their current value. Schedule, targeting and action "
            "payload are validated together, so an end before its start or a global "
            "advertisement carrying a target is refused."
        ),
        request=AdminAdvertisementUpdateRequestSerializer,
        responses={
            200: AdminIdSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def put(self, request: AuthenticatedRequest, advertisement_id: UUID) -> Response:
        payload = AdminAdvertisementUpdateRequestSerializer(data=request.data, partial=True)
        payload.is_valid(raise_exception=True)
        ad = get_object_or_404(Advertisement, pk=advertisement_id)
        try:
            update_advertisement(actor=request.user, advertisement=ad, data=request.data)
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        return Response({"id": str(ad.pk)})

    @extend_schema(
        operation_id="adminAdDelete",
        tags=["Admin Ads"],
        summary="Delete an advertisement",
        responses={204: None, **protected(), 404: NOT_FOUND_404},
    )
    def delete(self, request: AuthenticatedRequest, advertisement_id: UUID) -> Response:
        ad = get_object_or_404(Advertisement, pk=advertisement_id)
        delete_advertisement(actor=request.user, advertisement=ad)
        return Response(status=204)


class AuditListView(AdminView):
    required_permission = "admin.audit.read"

    @extend_schema(
        operation_id="adminAuditList",
        tags=["Admin Audit"],
        summary="Search the audit trail",
        description=(
            "Capped at 250 rows. Snapshots and metadata are stored redacted. Every filter "
            "is optional and combines with the rest."
        ),
        parameters=[
            _filter("actor", "Actor user id."),
            _filter("action", "Substring matched against the action code, case-insensitive."),
            _filter(
                "resource",
                "Substring matched against the target type, or an exact target id.",
            ),
            _filter("requestId", "Exact request correlation id, as returned in an error body."),
            _filter("from", "ISO date or datetime; keeps entries created at or after it."),
            _filter("to", "ISO date or datetime; a bare date includes that whole day."),
        ],
        responses={200: AdminAuditListSerializer, 400: VALIDATION_400, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        qs = filtered_audit(request.query_params)
        return Response(
            {
                "items": AdminAuditEntrySerializer(
                    qs.values(
                        "id",
                        "actor_id",
                        "action",
                        "target_type",
                        "target_id",
                        "request_id",
                        "metadata",
                        "created_at",
                    )[:250],
                    many=True,
                ).data
            }
        )


class AnalyticsView(AdminView):
    required_permission = "admin.analytics.read"

    @extend_schema(
        operation_id="adminAnalyticsRetrieve",
        tags=["Admin Analytics"],
        summary="Operational KPIs",
        description=(
            "Period-bound KPIs (approval median and the four event counts) cover `from` to "
            "`to`, by default the last 30 days, and `previous` holds the same KPIs for the "
            "equally long period just before, for comparison. The remaining fields are "
            "current totals."
        ),
        parameters=[
            _filter("from", "ISO date or datetime; default 30 days before `to`."),
            _filter("to", "ISO date or datetime; a bare date includes that whole day."),
        ],
        responses={200: AdminAnalyticsSerializer, 400: VALIDATION_400, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        start, end = analytics_period(request)
        span = end - start
        event_counts = list(
            ProductAnalyticsEvent.objects.values("name")
            .annotate(count=Count("id"))
            .order_by("name")
        )
        return Response(
            {
                "from": start.isoformat(),
                "to": end.isoformat(),
                **_period_kpis(start, end),
                "previous": {
                    "from": (start - span).isoformat(),
                    "to": start.isoformat(),
                    **_period_kpis(start - span, start),
                },
                "activeFacilities": Facility.objects.filter(status=Facility.Status.ACTIVE).count(),
                "pendingReviews": FacilityApplication.objects.filter(
                    status=FacilityApplication.Status.SUBMITTED
                ).count(),
                "ratingAverage": Facility.objects.aggregate(value=Avg("ratings__stars"))["value"],
                "events": event_counts,
            }
        )


class SettingsView(AdminView):
    required_permission = "admin.settings.read"

    @extend_schema(
        operation_id="adminSettingsList",
        tags=["Admin Settings"],
        summary="List typed platform settings",
        responses={200: AdminSettingListSerializer, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        return Response(
            {
                "items": AdminSettingSerializer(
                    PlatformSetting.objects.order_by("key").values(
                        "key", "value_type", "value", "updated_at"
                    ),
                    many=True,
                ).data
            }
        )

    @extend_schema(
        operation_id="adminSettingWrite",
        tags=["Admin Settings"],
        summary="Create or update a typed platform setting",
        description="Requires the manage permission, which is re-checked inside the handler.",
        request=AdminSettingWriteRequestSerializer,
        responses={200: AdminSettingWrittenSerializer, 400: VALIDATION_400, **protected()},
    )
    def put(self, request: AuthenticatedRequest) -> Response:
        self.required_permission = "admin.settings.manage"
        if not HasAdminPermission().has_permission(request, self):
            self.permission_denied(request)
        key = str(request.data.get("key", "")).strip()
        if not key:
            raise ValidationError({"key": "Required."})
        typed = TYPED_SETTING_DEFAULTS.get(key)
        if typed is not None and request.data.get("type", typed[0]) != typed[0]:
            raise ValidationError({"type": f"{key} must be {typed[0]}."})
        setting, _ = PlatformSetting.objects.get_or_create(
            key=key,
            defaults={"value_type": request.data.get("type", "JSON"), "value": None},
        )
        before = {"type": setting.value_type, "value": setting.value}
        setting.value_type = request.data.get("type", setting.value_type)
        setting.value = request.data.get("value")
        setting.updated_by = request.user
        try:
            setting.full_clean()
            setting.save()
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        record_audit(
            actor=request.user,
            action="platform_setting.updated",
            target=setting,
            before_snapshot=before,
            after_snapshot={"type": setting.value_type, "value": setting.value},
            request_id=_request_id(request),
        )
        return Response({"key": setting.key, "type": setting.value_type, "value": setting.value})


class SystemStatusView(AdminView):
    required_permission = "admin.system.read"

    @extend_schema(
        operation_id="adminSystemStatusRetrieve",
        tags=["Admin System"],
        summary="Runtime and configuration status",
        description=(
            "Reports only whether each dependency is configured. No secret, connection "
            "string or credential is returned."
        ),
        responses={200: AdminSystemStatusSerializer, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        database = "unavailable"
        try:
            with connection.cursor() as cursor:
                cursor.execute("SELECT 1")
                cursor.fetchone()
            database = "ok"
        except Exception:
            database = "unavailable"
        from django.conf import settings

        return Response(
            {
                "apiVersion": "1.0.0",
                "environment": getattr(settings, "ENVIRONMENT_NAME", "unknown"),
                "database": database,
                "redis": "configured" if getattr(settings, "REDIS_URL", "") else "unconfigured",
                "celery": (
                    "configured" if getattr(settings, "CELERY_BROKER_URL", "") else "unconfigured"
                ),
                "storage": (
                    "configured" if getattr(settings, "S3_ENDPOINT_URL", "") else "unconfigured"
                ),
                "schemaHash": getattr(settings, "OPENAPI_SCHEMA_HASH", "unavailable"),
                "checkedAt": timezone.now().isoformat(),
            }
        )


def _report_payload(report: Any) -> dict[str, Any]:
    return {
        "id": str(report.pk),
        "facilityId": str(report.facility_id),
        "facilityNameAr": report.facility.name_ar,
        "reporterId": str(report.reporter_id) if report.reporter_id else None,
        "reason": report.reason,
        "note": report.note,
        "status": report.status,
        "createdAt": report.created_at.isoformat(),
        "resolvedById": str(report.resolved_by_id) if report.resolved_by_id else None,
        "resolvedAt": report.resolved_at.isoformat() if report.resolved_at else None,
    }


class ReportListView(AdminView):
    required_permission = "admin.reports.read"

    @extend_schema(
        operation_id="adminReportsList",
        tags=["Admin Reports"],
        summary="List facility problem reports",
        description="Newest first, capped at 250 rows.",
        parameters=[
            _filter("status", "OPEN, RESOLVED or DISMISSED."),
            _filter("facility", "Facility id."),
        ],
        responses={200: AdminFacilityReportListSerializer, **protected()},
    )
    def get(self, request: Any) -> Response:
        qs = filtered_reports(request.query_params)
        return Response({"items": [_report_payload(item) for item in qs[:250]]})


class ReportDecisionView(AdminView):
    required_permission = "admin.reports.manage"
    target_status = FacilityReport.Status.RESOLVED

    @extend_schema(
        operation_id="adminReportResolve",
        tags=["Admin Reports"],
        summary="Mark a report resolved",
        description="Only OPEN reports can be decided; the decision is audited.",
        request=AdminReportDecisionRequestSerializer,
        responses={
            200: AdminFacilityReportSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def post(self, request: Any, report_id: Any) -> Response:
        payload = AdminReportDecisionRequestSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        with transaction.atomic():
            report = get_object_or_404(
                FacilityReport.objects.select_for_update().select_related("facility"),
                pk=report_id,
            )
            try:
                decide_report(
                    report=report,
                    target_status=self.target_status,
                    actor=request.user,
                    note=payload.validated_data.get("note", ""),
                    request_id=_request_id(request),
                )
            except DjangoValidationError as exc:
                raise _validation_error(exc) from exc
        return Response(_report_payload(report))


@extend_schema_view(
    post=extend_schema(
        operation_id="adminReportDismiss",
        tags=["Admin Reports"],
        summary="Dismiss a report",
        description="Only OPEN reports can be decided; the decision is audited.",
        request=AdminReportDecisionRequestSerializer,
        responses={
            200: AdminFacilityReportSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
)
class ReportDismissView(ReportDecisionView):
    target_status = FacilityReport.Status.DISMISSED


def _city_payload(city: Any) -> dict[str, Any]:
    return {
        "id": str(city.pk),
        "code": city.code,
        "nameAr": city.name_ar,
        "nameEn": city.name_en or None,
        "active": city.active,
    }


class ProvinceCityListView(AdminView):
    required_permission = "admin.provinces.read"

    @extend_schema(
        operation_id="adminProvinceCitiesList",
        tags=["Admin Provinces"],
        summary="List every city of a province, active or not",
        responses={200: AdminCityAdminListSerializer, **protected(), 404: NOT_FOUND_404},
    )
    def get(self, request: Any, province_id: Any) -> Response:
        province = get_object_or_404(Province, pk=province_id)
        cities = City.objects.filter(province=province).order_by("name_ar")
        return Response({"items": [_city_payload(city) for city in cities]})


class ProvinceCityDetailView(AdminView):
    required_permission = "admin.provinces.manage"

    @extend_schema(
        operation_id="adminProvinceCityUpdate",
        tags=["Admin Provinces"],
        summary="Activate or deactivate a city",
        request=AdminCityUpdateRequestSerializer,
        responses={
            200: AdminCityAdminSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def put(self, request: Any, province_id: Any, city_id: Any) -> Response:
        payload = AdminCityUpdateRequestSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        city = get_object_or_404(City, pk=city_id, province_id=province_id)
        before = {"active": city.active}
        city.active = payload.validated_data["active"]
        city.save(update_fields=["active"])
        record_audit(
            actor=request.user,
            action="city.updated",
            target=city,
            before_snapshot=before,
            after_snapshot={"active": city.active},
            request_id=_request_id(request),
        )
        return Response(_city_payload(city))
