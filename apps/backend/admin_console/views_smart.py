"""The smart admin console: what needs attention, finding anything, and acting in bulk.

Every endpoint re-checks its own permission code; the Admin UI hiding an action is a
convenience, never the gate. Every mutation is audited with `record_audit`.
"""

from __future__ import annotations

import re
from datetime import timedelta
from typing import Any

from django.core.exceptions import ValidationError as DjangoValidationError
from django.db import transaction
from django.db.models import Q
from django.shortcuts import get_object_or_404
from django.utils import timezone
from drf_spectacular.utils import OpenApiParameter, extend_schema
from rest_framework.exceptions import Throttled, ValidationError
from rest_framework.parsers import FormParser, MultiPartParser
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from accounts.models import User
from accounts.rbac import admin_permissions_for
from audit.models import AuditEvent
from audit.services import record_audit
from content_services.media import save_ad_image
from core.openapi import NOT_FOUND_404, THROTTLED_429, VALIDATION_400, protected
from core.pagination import CursorPage
from facilities.models import Facility, FacilityApplication, FacilityReport, RejectionTemplate
from locations.models import Province
from notifications.models import Broadcast
from notifications.services import send_broadcast

from .insights import province_readiness, smart_alerts, staff_performance, tasks_center
from .permissions import HasAdminPermission, IsAdminOperator
from .schemas_smart import (
    AdminAdImageSerializer,
    AdminAdImageUploadSerializer,
    AdminAlertListSerializer,
    AdminBroadcastPageSerializer,
    AdminBroadcastRequestSerializer,
    AdminBroadcastSerializer,
    AdminProvinceReadinessSerializer,
    AdminRejectionTemplateListSerializer,
    AdminRejectionTemplateRequestSerializer,
    AdminRejectionTemplateSerializer,
    AdminReportBulkRequestSerializer,
    AdminReportBulkResponseSerializer,
    AdminSearchResultSerializer,
    AdminStaffPerformanceSerializer,
    AdminTasksSerializer,
    AdminTimelineSerializer,
)
from .services import decide_report
from .views import AdminView, analytics_period

SEARCH_LIMIT = 5
SEARCH_MIN_LENGTH = 2
TIMELINE_LIMIT = 200
BROADCASTS_PER_HOUR = 5

FACILITY_STATUS_AR = {
    "DRAFT": "مسودة",
    "SUBMITTED": "قيد المراجعة",
    "ACTIVE": "فعّالة",
    "REVERIFICATION_REQUIRED": "تحتاج إعادة تحقق",
    "SUSPENDED": "موقوفة",
    "CLOSED": "مغلقة",
}
APPLICATION_KIND_AR = {"INITIAL": "طلب أول", "REVERIFICATION": "إعادة تحقق"}
APPLICATION_STATUS_AR = {
    "DRAFT": "مسودة",
    "SUBMITTED": "بانتظار المراجعة",
    "APPROVED": "مقبول",
    "REJECTED": "مرفوض",
}


def _request_id(request: Any) -> str:
    return getattr(request, "request_id", "")


def require_permission(view: APIView, request: Any, code: str) -> None:
    """Re-check a method-specific permission inside a handler."""
    view.required_permission = code  # type: ignore[attr-defined]
    if not HasAdminPermission().has_permission(request, view):
        view.permission_denied(request)


# --------------------------------------------------------------------------- dashboard


class TasksView(AdminView):
    required_permission = "admin.dashboard.read"

    @extend_schema(
        operation_id="adminTasksRetrieve",
        tags=["Admin System"],
        summary="The operator's queue: what is waiting, oldest first",
        description=(
            "Submitted applications split into INITIAL and REVERIFICATION, open problem "
            "reports grouped by facility (facilities with 2 or more open reports first) and "
            "facilities waiting in REVERIFICATION_REQUIRED. Each bucket has its count, how "
            "many are past the SLA (platform setting `review.slaHours`, default 48) and up "
            "to 10 oldest items with their age in hours and an `overdue` flag."
        ),
        responses={200: AdminTasksSerializer, **protected()},
    )
    def get(self, request: Any) -> Response:
        return Response(tasks_center())


class AlertsView(AdminView):
    required_permission = "admin.dashboard.read"

    @extend_schema(
        operation_id="adminAlertsList",
        tags=["Admin System"],
        summary="Smart alerts: problems worth acting on now",
        description=(
            "DUTY_GAP: per province offering a duty category, the Damascus days of the next "
            "14 with no duty shift of any ACTIVE pharmacy (critical when the first gap is "
            "today or tomorrow). STALE_FACILITY: ACTIVE facilities with no change, owner "
            "confirmation or approval for 90 days. REPORTED_FACILITY: 3 or more open reports "
            "(critical from 5). ZERO_RESULT_SEARCH: searches without results in the last 7 "
            "days, grouped by province and category because search text is never recorded. "
            "REVIEW_OVERDUE: submitted applications past the SLA (critical past twice it). "
            "MAINTENANCE_ON."
        ),
        responses={200: AdminAlertListSerializer, **protected()},
    )
    def get(self, request: Any) -> Response:
        now = timezone.now()
        return Response({"generatedAt": now.isoformat(), "items": smart_alerts(now)})


# ------------------------------------------------------------------------------ search


def _phone_digits(query: str) -> str | None:
    digits = re.sub(r"\D", "", query)
    if len(digits) < 4:
        return None
    if digits.startswith("00"):
        digits = digits[2:]
    elif digits.startswith("0"):
        digits = "963" + digits[1:]
    return digits


def mask_phone(phone: str) -> str:
    return f"{'•' * max(0, len(phone) - 4)}{phone[-4:]}" if phone else ""


class AdminSearchView(APIView):
    """Any operator may search; each group is present only when they may read it."""

    permission_classes = [IsAuthenticated, IsAdminOperator]

    @extend_schema(
        operation_id="adminSearchRetrieve",
        tags=["Admin System"],
        summary="Search facilities, users and applications at once",
        description=(
            "Up to 5 hits per group. FACILITY (admin.facilities.read): Arabic or English "
            "name, or phone digits. USER (admin.users.read, or admin.facilities.read with the "
            "phone masked to its last 4 digits): name or phone digits. APPLICATION "
            "(admin.reviews.read): facility name. A group the caller may not read is left "
            "out, not returned empty."
        ),
        parameters=[
            OpenApiParameter(
                "q", str, OpenApiParameter.QUERY, required=True, description="At least 2 chars."
            )
        ],
        responses={200: AdminSearchResultSerializer, 400: VALIDATION_400, **protected()},
    )
    def get(self, request: Any) -> Response:
        query = (request.query_params.get("q") or "").strip()
        if len(query) < SEARCH_MIN_LENGTH:
            raise ValidationError({"q": [f"At least {SEARCH_MIN_LENGTH} characters."]})
        granted = set(admin_permissions_for(request.user))
        digits = _phone_digits(query)
        groups: list[dict[str, Any]] = []
        if "admin.facilities.read" in granted:
            match = Q(name_ar__icontains=query) | Q(name_en__icontains=query)
            if digits:
                match |= Q(phone__contains=digits)
            facilities = (
                Facility.objects.filter(match)
                .select_related("category", "province")
                .order_by("-updated_at", "id")[:SEARCH_LIMIT]
            )
            groups.append(
                {
                    "type": "FACILITY",
                    "items": [
                        {
                            "type": "FACILITY",
                            "id": str(item.pk),
                            "titleAr": item.name_ar,
                            "subtitle": " · ".join(
                                [
                                    item.category.name_ar,
                                    item.province.name_ar,
                                    FACILITY_STATUS_AR.get(item.status, item.status),
                                ]
                            ),
                        }
                        for item in facilities
                    ],
                }
            )
        if granted & {"admin.users.read", "admin.facilities.read"}:
            full_phone = "admin.users.read" in granted
            match = Q(name__icontains=query)
            if digits:
                match |= Q(phone__contains=digits)
            users = User.objects.filter(match).order_by("-created_at", "id")[:SEARCH_LIMIT]
            groups.append(
                {
                    "type": "USER",
                    "items": [
                        {
                            "type": "USER",
                            "id": str(item.pk),
                            "titleAr": item.name,
                            "subtitle": item.phone if full_phone else mask_phone(item.phone),
                        }
                        for item in users
                    ],
                }
            )
        if "admin.reviews.read" in granted:
            applications = (
                FacilityApplication.objects.filter(
                    Q(facility__name_ar__icontains=query) | Q(facility__name_en__icontains=query)
                )
                .select_related("facility")
                .order_by("-created_at", "id")[:SEARCH_LIMIT]
            )
            groups.append(
                {
                    "type": "APPLICATION",
                    "items": [
                        {
                            "type": "APPLICATION",
                            "id": str(item.pk),
                            "titleAr": item.facility.name_ar,
                            "subtitle": " · ".join(
                                [
                                    APPLICATION_KIND_AR.get(item.kind, item.kind),
                                    APPLICATION_STATUS_AR.get(item.status, item.status),
                                ]
                            ),
                        }
                        for item in applications
                    ],
                }
            )
        return Response({"query": query, "groups": groups})


# ---------------------------------------------------------------------------- timeline

AUDIT_TITLES_AR = {
    "facility.owner_draft.created": "أنشأ المالك مسودة المنشأة",
    "facility.owner_core.updated": "عدّل المالك بيانات المنشأة",
    "facility.owner_location.updated": "عدّل المالك موقع المنشأة",
    "facility.owner_submitted": "أرسل المالك المنشأة للمراجعة",
    "facility.hours.replaced": "عُدّلت أوقات الدوام",
    "facility.hours.confirmed": "أكّد المالك أوقات الدوام",
    "facility.suspended": "أُوقفت المنشأة",
    "facility.active": "أُعيد تفعيل المنشأة",
    "facility.closed": "أُغلقت المنشأة",
    "facility.public_image.created": "أُضيفت صورة",
    "facility.public_image.deleted": "حُذفت صورة",
    "facility.evidence.created": "رُفع إثبات تحقق",
    "facility.evidence.deleted": "حُذف إثبات تحقق",
    "verification_evidence.viewed": "اطّلع مشرف على إثبات تحقق",
    "duty_shift.created": "أضافت الإدارة وردية مناوبة",
    "duty_shift.updated": "عدّلت الإدارة وردية مناوبة",
    "duty_shift.deleted": "ألغت الإدارة وردية مناوبة",
}
# Decisions the timeline already shows from the application and report rows themselves;
# their audit rows only lend the request id.
REPRESENTED_ACTIONS = {
    "facility_application.approved",
    "facility_application.rejected",
    "facility_report.resolved",
    "facility_report.dismissed",
}


def _event(
    at: Any,
    kind: str,
    title_ar: str,
    actor: Any = None,
    request_id: str | None = None,
    action: str | None = None,
    ref_id: str | None = None,
) -> dict[str, Any]:
    return {
        "at": at,
        "kind": kind,
        "titleAr": title_ar,
        "actorName": actor.name if actor is not None else None,
        "requestId": request_id or None,
        "action": action,
        "refId": ref_id,
    }


def facility_timeline(facility: Facility) -> list[dict[str, Any]]:
    applications = list(facility.applications.select_related("reviewed_by"))
    reports = list(facility.reports.select_related("resolved_by", "reporter"))
    related_ids = [str(facility.pk)]
    related_ids += [str(row.pk) for row in applications]
    related_ids += [str(row.pk) for row in reports]
    related_ids += [str(pk) for pk in facility.images.values_list("pk", flat=True)]
    related_ids += [str(pk) for pk in facility.evidence.values_list("pk", flat=True)]
    related_ids += [str(pk) for pk in facility.duty_shifts.values_list("pk", flat=True)]
    audits = list(
        AuditEvent.objects.filter(target_id__in=related_ids)
        .select_related("actor")
        .order_by("-created_at")[:TIMELINE_LIMIT]
    )
    decision_request = {
        (row.target_id, row.action): row.request_id
        for row in audits
        if row.action in REPRESENTED_ACTIONS
    }
    events: list[dict[str, Any]] = []
    for application in applications:
        kind_ar = APPLICATION_KIND_AR.get(application.kind, application.kind)
        if application.submitted_at:
            events.append(
                _event(
                    application.submitted_at,
                    "APPLICATION_SUBMITTED",
                    f"أُرسل {kind_ar} للمراجعة",
                    ref_id=str(application.pk),
                )
            )
        if application.reviewed_at and application.status in ("APPROVED", "REJECTED"):
            approved = application.status == "APPROVED"
            action = (
                "facility_application.approved" if approved else "facility_application.rejected"
            )
            events.append(
                _event(
                    application.reviewed_at,
                    "APPLICATION_APPROVED" if approved else "APPLICATION_REJECTED",
                    f"قُبل {kind_ar}"
                    if approved
                    else f"رُفض {kind_ar}: {application.rejection_reason}"[:300],
                    application.reviewed_by,
                    decision_request.get((str(application.pk), action)),
                    ref_id=str(application.pk),
                )
            )
    for report in reports:
        events.append(
            _event(
                report.created_at,
                "REPORT_CREATED",
                f"بلاغ جديد ({report.get_reason_display()})",
                ref_id=str(report.pk),
            )
        )
        if report.resolved_at and report.status != FacilityReport.Status.OPEN:
            resolved = report.status == FacilityReport.Status.RESOLVED
            events.append(
                _event(
                    report.resolved_at,
                    "REPORT_RESOLVED" if resolved else "REPORT_DISMISSED",
                    "عولج البلاغ" if resolved else "رُفض البلاغ",
                    report.resolved_by,
                    decision_request.get(
                        (str(report.pk), f"facility_report.{report.status.lower()}")
                    ),
                    ref_id=str(report.pk),
                )
            )
    for audit in audits:
        if audit.action in REPRESENTED_ACTIONS:
            continue
        events.append(
            _event(
                audit.created_at,
                "AUDIT",
                AUDIT_TITLES_AR.get(audit.action, audit.action),
                audit.actor,
                audit.request_id,
                audit.action,
                audit.target_id,
            )
        )
    now = timezone.now()
    upcoming = facility.duty_shifts.filter(
        ends_at__gt=now, starts_at__lt=now + timedelta(days=14)
    ).count()
    if upcoming:
        events.append(
            _event(now, "DUTY_SUMMARY", f"{upcoming} وردية مناوبة خلال الأيام الـ14 القادمة")
        )
    events.sort(key=lambda item: item["at"], reverse=True)
    return [{**item, "at": item["at"].isoformat()} for item in events[:TIMELINE_LIMIT]]


class FacilityTimelineView(AdminView):
    required_permission = "admin.facilities.read"

    @extend_schema(
        operation_id="adminFacilityTimelineRetrieve",
        tags=["Admin Facilities"],
        summary="Everything that happened to a facility, newest first",
        description=(
            "Merges applications (submitted, decided), problem reports (created, resolved or "
            "dismissed), audited changes to the facility and its applications, reports, "
            "images, evidence and duty shifts, and a summary of the next 14 days of duty. "
            "Up to 200 events."
        ),
        responses={200: AdminTimelineSerializer, **protected(), 404: NOT_FOUND_404},
    )
    def get(self, request: Any, facility_id: Any) -> Response:
        facility = get_object_or_404(Facility, pk=facility_id)
        return Response({"facilityId": str(facility.pk), "items": facility_timeline(facility)})


# ------------------------------------------------------------------------ bulk reports


class ReportBulkDecisionView(AdminView):
    required_permission = "admin.reports.manage"

    @extend_schema(
        operation_id="adminReportsBulkDecide",
        tags=["Admin Reports"],
        summary="Resolve or dismiss many reports at once",
        description=(
            "Up to 100 ids, in one transaction: every OPEN report is decided and audited "
            "individually, exactly as the single-report endpoints do. An id that does not "
            "exist or is no longer OPEN is reported per id (NOT_FOUND, NOT_OPEN) and left "
            "alone; it does not fail the others."
        ),
        request=AdminReportBulkRequestSerializer,
        responses={200: AdminReportBulkResponseSerializer, 400: VALIDATION_400, **protected()},
    )
    def post(self, request: Any) -> Response:
        payload = AdminReportBulkRequestSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        data = payload.validated_data
        target = (
            FacilityReport.Status.RESOLVED
            if data["action"] == "resolve"
            else FacilityReport.Status.DISMISSED
        )
        ids = list(dict.fromkeys(data["ids"]))
        results = []
        decided = 0
        with transaction.atomic():
            locked = {
                report.pk: report
                for report in FacilityReport.objects.select_for_update()
                .filter(pk__in=ids)
                .order_by("pk")
            }
            for report_id in ids:
                report = locked.get(report_id)
                if report is None:
                    results.append({"id": str(report_id), "outcome": "NOT_FOUND", "status": None})
                    continue
                if report.status != FacilityReport.Status.OPEN:
                    results.append(
                        {"id": str(report_id), "outcome": "NOT_OPEN", "status": report.status}
                    )
                    continue
                decide_report(
                    report=report,
                    target_status=target,
                    actor=request.user,
                    note=data.get("note", ""),
                    request_id=_request_id(request),
                )
                decided += 1
                results.append({"id": str(report_id), "outcome": "DECIDED", "status": target})
        return Response({"action": data["action"], "decided": decided, "results": results})


# ------------------------------------------------------------------------- readiness


class ProvinceReadinessView(AdminView):
    required_permission = "admin.provinces.read"

    @extend_schema(
        operation_id="adminProvinceReadinessRetrieve",
        tags=["Admin Provinces"],
        summary="Launch checklist for a province",
        description=(
            "PROVINCE_ACTIVE; CATEGORY_PUBLIC (at least one active category publicly "
            "enabled); MIN_ACTIVE_FACILITIES (platform setting "
            "`readiness.minActiveFacilities`, default 5); DUTY_COVERAGE (no DUTY_GAP in the "
            "next 14 days, or not applicable when the province offers no duty category); "
            "EMERGENCY_NUMBERS (an active national or provincial number)."
        ),
        responses={200: AdminProvinceReadinessSerializer, **protected(), 404: NOT_FOUND_404},
    )
    def get(self, request: Any, province_id: Any) -> Response:
        return Response(province_readiness(get_object_or_404(Province, pk=province_id)))


# ------------------------------------------------------------------------- analytics


class StaffAnalyticsView(AdminView):
    required_permission = "admin.analytics.read"

    @extend_schema(
        operation_id="adminAnalyticsStaffRetrieve",
        tags=["Admin Analytics"],
        summary="Reviewer performance in a period",
        description=(
            "Per reviewer, over applications decided in [`from`, `to`) (default last 30 "
            "days): decisions, approvals, rejections and the median submit-to-decision "
            "hours; plus problem reports they resolved or dismissed in the period."
        ),
        parameters=[
            OpenApiParameter("from", str, OpenApiParameter.QUERY, required=False),
            OpenApiParameter("to", str, OpenApiParameter.QUERY, required=False),
        ],
        responses={200: AdminStaffPerformanceSerializer, 400: VALIDATION_400, **protected()},
    )
    def get(self, request: Any) -> Response:
        start, end = analytics_period(request)
        return Response(
            {
                "from": start.isoformat(),
                "to": end.isoformat(),
                "items": staff_performance(start, end),
            }
        )


# ------------------------------------------------------------------------ broadcasts


def _broadcast_payload(broadcast: Broadcast) -> dict[str, Any]:
    return {
        "id": str(broadcast.pk),
        "titleAr": broadcast.title_ar,
        "bodyAr": broadcast.body_ar,
        "audience": broadcast.audience,
        "provinceId": str(broadcast.province_id) if broadcast.province_id else None,
        "recipientCount": broadcast.recipient_count,
        "actorId": str(broadcast.actor_id) if broadcast.actor_id else None,
        "actorName": broadcast.actor.name if broadcast.actor else None,
        "createdAt": broadcast.created_at.isoformat(),
    }


class BroadcastCursorPage(CursorPage):
    ordering = ("-created_at", "id")


class BroadcastSendView(AdminView):
    required_permission = "admin.notifications.send"

    @extend_schema(
        operation_id="adminNotificationBroadcast",
        tags=["Admin Notifications"],
        summary="Send a notification to many users",
        description=(
            "ALL reaches every active account (narrowed to accounts that chose `provinceId` "
            "when given); OWNERS reaches every active owner or manager (narrowed to "
            "facilities in `provinceId`). Each recipient gets an inbox notification of type "
            "`platform.broadcast`, and a push is queued for accounts with a device. Audited. "
            f"At most {BROADCASTS_PER_HOUR} broadcasts per operator per hour (429)."
        ),
        request=AdminBroadcastRequestSerializer,
        responses={
            201: AdminBroadcastSerializer,
            400: VALIDATION_400,
            **protected(),
            429: THROTTLED_429,
        },
    )
    def post(self, request: Any) -> Response:
        payload = AdminBroadcastRequestSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        data = payload.validated_data
        if not data["titleAr"].strip() or not data["bodyAr"].strip():
            raise ValidationError({"titleAr": ["Required."], "bodyAr": ["Required."]})
        province = None
        if data.get("provinceId"):
            province = Province.objects.filter(pk=data["provinceId"]).first()
            if province is None:
                raise ValidationError({"provinceId": ["Unknown province."]})
        now = timezone.now()
        recent = list(
            Broadcast.objects.filter(
                actor=request.user, created_at__gte=now - timedelta(hours=1)
            ).values_list("created_at", flat=True)
        )
        if len(recent) >= BROADCASTS_PER_HOUR:
            wait = (min(recent) + timedelta(hours=1) - now).total_seconds()
            raise Throttled(wait=max(1, int(wait)))
        broadcast = send_broadcast(
            actor=request.user,
            title_ar=data["titleAr"].strip(),
            body_ar=data["bodyAr"].strip(),
            audience=data["audience"],
            province=province,
        )
        record_audit(
            actor=request.user,
            action="notification.broadcast.sent",
            target=broadcast,
            after_snapshot={"titleAr": broadcast.title_ar, "bodyAr": broadcast.body_ar},
            metadata={
                "audience": broadcast.audience,
                "provinceId": str(province.pk) if province else None,
                "recipientCount": broadcast.recipient_count,
            },
            request_id=_request_id(request),
        )
        return Response(_broadcast_payload(broadcast), status=201)


class BroadcastHistoryView(AdminView):
    required_permission = "admin.notifications.send"

    @extend_schema(
        operation_id="adminNotificationBroadcastsList",
        tags=["Admin Notifications"],
        summary="Broadcast history, newest first",
        parameters=[
            OpenApiParameter("cursor", str, OpenApiParameter.QUERY, required=False),
            OpenApiParameter("limit", int, OpenApiParameter.QUERY, required=False),
        ],
        responses={200: AdminBroadcastPageSerializer, 400: VALIDATION_400, **protected()},
    )
    def get(self, request: Any) -> Response:
        paginator = BroadcastCursorPage()
        rows = Broadcast.objects.select_related("actor").order_by("-created_at", "id")
        page = paginator.paginate_queryset(rows, request, view=self) or []
        return Response(paginator.get_paginated_payload([_broadcast_payload(b) for b in page]))


# ---------------------------------------------------------------- rejection templates


def _template_payload(template: RejectionTemplate) -> dict[str, Any]:
    return {
        "id": str(template.pk),
        "titleAr": template.title_ar,
        "bodyAr": template.body_ar,
        "active": template.active,
        "sortOrder": template.sort_order,
    }


def _template_snapshot(template: RejectionTemplate) -> dict[str, Any]:
    payload = _template_payload(template)
    payload.pop("id")
    return payload


TEMPLATE_FIELDS = {
    "titleAr": "title_ar",
    "bodyAr": "body_ar",
    "active": "active",
    "sortOrder": "sort_order",
}


class RejectionTemplateListView(AdminView):
    required_permission = "admin.reviews.read"

    @extend_schema(
        operation_id="adminRejectionTemplatesList",
        tags=["Admin Reviews"],
        summary="List rejection templates",
        description="Ordered by `sortOrder`. `active=true` keeps only the active ones.",
        parameters=[OpenApiParameter("active", bool, OpenApiParameter.QUERY, required=False)],
        responses={200: AdminRejectionTemplateListSerializer, **protected()},
    )
    def get(self, request: Any) -> Response:
        rows = RejectionTemplate.objects.order_by("sort_order", "title_ar")
        if request.query_params.get("active", "").lower() == "true":
            rows = rows.filter(active=True)
        return Response({"items": [_template_payload(row) for row in rows]})

    @extend_schema(
        operation_id="adminRejectionTemplateCreate",
        tags=["Admin Reviews"],
        summary="Create a rejection template",
        description="Requires admin.reviews.decide, re-checked inside the handler.",
        request=AdminRejectionTemplateRequestSerializer,
        responses={201: AdminRejectionTemplateSerializer, 400: VALIDATION_400, **protected()},
    )
    def post(self, request: Any) -> Response:
        require_permission(self, request, "admin.reviews.decide")
        payload = AdminRejectionTemplateRequestSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        template = RejectionTemplate(
            **{column: payload.validated_data[wire] for wire, column in TEMPLATE_FIELDS.items()}
        )
        template.full_clean()
        template.save()
        record_audit(
            actor=request.user,
            action="rejection_template.created",
            target=template,
            after_snapshot=_template_snapshot(template),
            request_id=_request_id(request),
        )
        return Response(_template_payload(template), status=201)


class RejectionTemplateDetailView(AdminView):
    required_permission = "admin.reviews.decide"

    @extend_schema(
        operation_id="adminRejectionTemplateUpdate",
        tags=["Admin Reviews"],
        summary="Edit, reorder or retire a rejection template",
        description="Omitted fields keep their value.",
        request=AdminRejectionTemplateRequestSerializer,
        responses={
            200: AdminRejectionTemplateSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def put(self, request: Any, template_id: Any) -> Response:
        template = get_object_or_404(RejectionTemplate, pk=template_id)
        payload = AdminRejectionTemplateRequestSerializer(data=request.data, partial=True)
        payload.is_valid(raise_exception=True)
        before = _template_snapshot(template)
        for wire, column in TEMPLATE_FIELDS.items():
            if wire in request.data and wire in payload.validated_data:
                setattr(template, column, payload.validated_data[wire])
        try:
            template.full_clean()
        except DjangoValidationError as exc:
            raise ValidationError(exc.message_dict) from exc
        template.save()
        record_audit(
            actor=request.user,
            action="rejection_template.updated",
            target=template,
            before_snapshot=before,
            after_snapshot=_template_snapshot(template),
            request_id=_request_id(request),
        )
        return Response(_template_payload(template))

    @extend_schema(
        operation_id="adminRejectionTemplateDelete",
        tags=["Admin Reviews"],
        summary="Delete a rejection template",
        description="Past rejections keep their text; a template is only a starting point.",
        responses={204: None, **protected(), 404: NOT_FOUND_404},
    )
    def delete(self, request: Any, template_id: Any) -> Response:
        template = get_object_or_404(RejectionTemplate, pk=template_id)
        record_audit(
            actor=request.user,
            action="rejection_template.deleted",
            target=template,
            before_snapshot=_template_snapshot(template),
            request_id=_request_id(request),
        )
        template.delete()
        return Response(status=204)


# ------------------------------------------------------------------------- ad images


class AdvertisementImage:
    """The audit target of an uploaded ad image, which has no row of its own."""

    def __init__(self, key: str) -> None:
        self.pk = key


class AdvertisementImageUploadView(AdminView):
    required_permission = "admin.ads.manage"
    parser_classes = [MultiPartParser, FormParser]

    @extend_schema(
        operation_id="adminAdImageUpload",
        tags=["Admin Ads"],
        summary="Upload an advertisement image",
        description=(
            "multipart/form-data with `file`. JPEG, PNG or WebP only, at most 2 MB, each "
            "side 100 to 4096 px. The image is re-encoded to JPEG (metadata stripped) and "
            "stored in public media under a random key. Pass the returned `imageKey` when "
            "creating or updating the advertisement."
        ),
        request={"multipart/form-data": AdminAdImageUploadSerializer},
        responses={201: AdminAdImageSerializer, 400: VALIDATION_400, **protected()},
    )
    def post(self, request: Any) -> Response:
        payload = AdminAdImageUploadSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        try:
            storage, key, width, height = save_ad_image(payload.validated_data["file"])
        except DjangoValidationError as exc:
            raise ValidationError({"file": exc.messages}) from exc
        record_audit(
            actor=request.user,
            action="advertisement.image.uploaded",
            target=AdvertisementImage(key),
            metadata={"width": width, "height": height},
            request_id=_request_id(request),
        )
        return Response(
            {"imageKey": key, "url": storage.url(key), "width": width, "height": height},
            status=201,
        )
