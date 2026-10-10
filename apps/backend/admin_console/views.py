"""The console's base view and shared helpers, and its overview screens.

`AdminView` (permission, audit logging) and the list, paging and period helpers every console
module uses live here, with the screens that belong to no single domain: the operator's own
identity, the dashboard, the audit log, analytics, settings and system status. Each domain has
its own module beside this one (`views_reviews`, `views_facilities`, `views_users`, …).
"""

import logging
from datetime import datetime, timedelta
from typing import Any
from uuid import UUID

from django.core.exceptions import ValidationError as DjangoValidationError
from django.db import connection
from django.db.models import Avg, Count, Q
from django.utils import timezone
from django.utils.dateparse import parse_date, parse_datetime
from drf_spectacular.utils import (
    OpenApiParameter,
    extend_schema,
)
from rest_framework.exceptions import ValidationError
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from accounts.authentication import AuthenticatedRequest
from accounts.mfa import status as mfa_status
from accounts.models import User
from accounts.phone import normalize_syrian_phone
from accounts.rbac import admin_permissions_for
from analytics.models import ProductAnalyticsEvent
from audit.models import AuditEvent
from audit.services import record_audit
from core.openapi import VALIDATION_400, protected
from core.pagination import QueryOrderedCursorPage, page_parameters
from facilities.models import Facility, FacilityApplication, FacilityReport
from pharmacy_duty.models import DutyShift
from platform_settings.maintenance import get_maintenance_state
from platform_settings.models import PlatformSetting
from platform_settings.operations import SUPPORT_WHATSAPP_KEY
from platform_settings.operations import TYPED_SETTINGS as TYPED_SETTING_DEFAULTS

from .permissions import HasAdminPermission, IsAdminOperator
from .quality import QUALITY_ISSUES, filter_issue, with_quality
from .schemas import (
    AdminAnalyticsSerializer,
    AdminAuditEntrySerializer,
    AdminAuditListSerializer,
    AdminDashboardSerializer,
    AdminMeSerializer,
    AdminRecentActionSerializer,
    AdminSettingListSerializer,
    AdminSettingSerializer,
    AdminSettingWriteRequestSerializer,
    AdminSettingWrittenSerializer,
    AdminSystemStatusSerializer,
)
from .serializers import (
    with_facility_names,
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
    if value := params.get("id"):
        # One facility by id: what a link to a facility, written before the console had cards,
        # resolves to (DECISION-109).
        try:
            qs = qs.filter(pk=UUID(str(value)))
        except ValueError as exc:
            raise ValidationError({"id": "Not a facility id."}) from exc
    if value := params.get("status"):
        qs = qs.filter(status=value)
    if value := params.get("province"):
        qs = qs.filter(province_id=value)
    if value := params.get("city"):
        qs = qs.filter(city_id=value)
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
    qs = AuditEvent.objects.order_by("-created_at", "-id")
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
    qs = FacilityReport.objects.select_related("facility").order_by("-created_at", "-id")
    if value := params.get("status"):
        qs = qs.filter(status=value.upper())
    if value := params.get("facility"):
        qs = qs.filter(facility_id=value)
    return qs


def _page(request: Any, queryset: Any, payload: Any) -> Response:
    """One cursor page of an ordered console list, each row shaped by `payload`."""
    paginator = QueryOrderedCursorPage()
    page = paginator.paginate_queryset(queryset, request)
    return paginator.get_paginated_response([payload(item) for item in page or []])


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
    """Cheap warnings for the dashboard: configuration, and what the services last recorded.

    No network calls; the system page asks the services themselves (DECISION-073).
    """
    from django.conf import settings

    from health.beacons import BACKUP, OTP, SCHEDULER
    from health.models import ServiceSignal

    from .system_health import check_backup, check_scheduler

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
        warnings.append("تتبع الأخطاء غير مفعّل.")
    signals = {signal.name: signal for signal in ServiceSignal.objects.all()}
    now = timezone.now()
    for label, check in (
        ("المهام المجدولة", check_scheduler(signals.get(SCHEDULER), now)),
        ("النسخ الاحتياطي", check_backup(signals.get(BACKUP), now)),
    ):
        if check.status in {"warning", "failed"}:
            warnings.append(f"{label}: {check.summary}")
    otp = signals.get(OTP)
    if otp and otp.failures and otp.failed_at and (otp.ok_at is None or otp.failed_at > otp.ok_at):
        warnings.append(f"رموز التحقق: آخر {otp.failures} محاولة إرسال فشلت.")
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
                # Where the console sends the operator before anything else: the second step,
                # or setting it up (DECISION-065).
                "mfa": mfa_status(request.user, getattr(request, "user_session", None)),
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


class AuditListView(AdminView):
    required_permission = "admin.audit.read"

    @extend_schema(
        operation_id="adminAuditList",
        tags=["Admin Audit"],
        summary="Search the audit trail",
        description=(
            "Newest first, in cursor pages. Snapshots and metadata are stored redacted. Every "
            "filter is optional and combines with the rest."
        ),
        parameters=[
            *page_parameters(QueryOrderedCursorPage),
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
        rows = filtered_audit(request.query_params).values(
            "id",
            "actor_id",
            "action",
            "target_type",
            "target_id",
            "request_id",
            "metadata",
            "created_at",
        )
        paginator = QueryOrderedCursorPage()
        page = paginator.paginate_queryset(rows, request, view=self)
        return paginator.get_paginated_response(AdminAuditEntrySerializer(page, many=True).data)


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
        value = request.data.get("value")
        if key == SUPPORT_WHATSAPP_KEY and isinstance(value, str) and value.strip():
            # Shown to everyone as a chat link, so a number that is not one is refused here
            # rather than silently hidden from the app and the site.
            try:
                value = normalize_syrian_phone(value)
            except ValueError as exc:
                raise ValidationError(
                    {"value": "اكتب رقم واتساب سورياً صحيحاً، مثل 0933123456."}
                ) from exc
        setting, _ = PlatformSetting.objects.get_or_create(
            key=key,
            defaults={"value_type": request.data.get("type", "JSON"), "value": None},
        )
        before = {"type": setting.value_type, "value": setting.value}
        setting.value_type = request.data.get("type", setting.value_type)
        setting.value = value
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
        summary="Every dependency, asked directly",
        description=(
            "The database, Redis and the workers, the scheduler's heartbeat, storage, the "
            "verification-code channel, push, backups, error reporting and maintenance mode, "
            "each with a status and a sentence (DECISION-073). Probes time out after two "
            "seconds. No host, URL, credential or exception text is returned."
        ),
        responses={200: AdminSystemStatusSerializer, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        from django.conf import settings

        from .system_health import run_checks

        return Response(
            {
                "apiVersion": settings.SPECTACULAR_SETTINGS.get("VERSION", ""),
                "environment": getattr(settings, "ENVIRONMENT_NAME", "unknown"),
                **run_checks(),
                "schemaHash": getattr(settings, "OPENAPI_SCHEMA_HASH", "unavailable"),
                "checkedAt": timezone.now().isoformat(),
            }
        )
