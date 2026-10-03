"""Reports people file against a listing, and an operator's decision on each."""

from typing import Any

from django.core.exceptions import ValidationError as DjangoValidationError
from django.db import transaction
from django.shortcuts import get_object_or_404
from drf_spectacular.utils import (
    extend_schema,
    extend_schema_view,
)
from rest_framework.response import Response

from core.openapi import NOT_FOUND_404, VALIDATION_400, protected
from core.pagination import QueryOrderedCursorPage, page_parameters
from facilities.models import FacilityReport

from .schemas import (
    AdminFacilityReportListSerializer,
    AdminFacilityReportSerializer,
    AdminReportDecisionRequestSerializer,
)
from .services import (
    decide_report,
)
from .views import (
    AdminView,
    _filter,
    _page,
    _request_id,
    _validation_error,
    filtered_reports,
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
        description="Newest first, in cursor pages.",
        parameters=[
            *page_parameters(QueryOrderedCursorPage),
            _filter("status", "OPEN, RESOLVED or DISMISSED."),
            _filter("facility", "Facility id."),
        ],
        responses={200: AdminFacilityReportListSerializer, **protected()},
    )
    def get(self, request: Any) -> Response:
        return _page(request, filtered_reports(request.query_params), _report_payload)


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
