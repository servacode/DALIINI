"""CSV exports of the facility list, problem reports and the audit trail.

Each export takes the same filters and needs the same permission as the list it mirrors, is
streamed row by row from a server-side cursor (a large export never sits in memory), and
stops at `MAX_EXPORT_ROWS`. The body is UTF-8 with a byte-order mark so Excel opens Arabic
text correctly. Cells that a spreadsheet would read as a formula are prefixed with an
apostrophe (CSV injection).
"""

from __future__ import annotations

import csv
from collections.abc import Iterable, Iterator
from typing import Any

from django.http import StreamingHttpResponse
from django.utils import timezone
from drf_spectacular.types import OpenApiTypes
from drf_spectacular.utils import OpenApiParameter, OpenApiResponse, extend_schema
from rest_framework.renderers import BaseRenderer, JSONRenderer
from rest_framework.response import Response

from core.openapi import FORBIDDEN_403, UNAUTHENTICATED_401, VALIDATION_400

from .quality import QUALITY_ISSUES, quality_payload
from .views import AdminView, filtered_audit, filtered_facilities, filtered_reports

MAX_EXPORT_ROWS = 50_000
CHUNK = 2000
BOM = "﻿"
FORMULA_PREFIXES = ("=", "+", "-", "@", "\t", "\r")


class CsvRenderer(BaseRenderer):
    """Lets a client send `Accept: text/csv`. It never renders: see `finalize_response`."""

    media_type = "text/csv"
    format = "csv"
    charset = "utf-8"

    def render(
        self, data: Any, accepted_media_type: Any = None, renderer_context: Any = None
    ) -> bytes:
        rendered: bytes = JSONRenderer().render(data, "application/json", renderer_context)
        return rendered


class _Echo:
    def write(self, value: str) -> str:
        return value


def _cell(value: Any) -> str:
    if value is None:
        return ""
    text = value.isoformat() if hasattr(value, "isoformat") else str(value)
    if text.startswith(FORMULA_PREFIXES):
        return "'" + text
    return text


# The files are opened by the platform's own staff, in Excel, in Arabic: the columns are named
# and the codes said in their words. The audit keeps its action codes, which are what support
# searches for.
FACILITY_STATUS_AR = {
    "DRAFT": "مسودة",
    "SUBMITTED": "قيد المراجعة",
    "ACTIVE": "فعّالة",
    "REVERIFICATION_REQUIRED": "تحتاج إعادة تحقق",
    "SUSPENDED": "موقوفة",
    "CLOSED": "مغلقة نهائياً",
}
QUALITY_ISSUE_AR = {
    "NO_PHOTOS": "بلا صور",
    "NO_HOURS": "بلا أوقات دوام",
    "NO_LOCATION": "بلا موقع",
    "NO_PHONE": "بلا هاتف",
    "STALE": "لم تُحدَّث منذ 90 يوماً",
    "OPEN_REPORTS": "عليها بلاغات",
    "NOT_VERIFIED_RECENTLY": "لم يُتحقق منها مؤخراً",
}
REPORT_REASON_AR = {
    "WRONG_INFO": "معلومات خاطئة",
    "CLOSED_PERMANENTLY": "مغلقة نهائياً",
    "WRONG_LOCATION": "الموقع خاطئ",
    "WRONG_HOURS": "أوقات الدوام خاطئة",
    "NOT_ON_DUTY": "ليست مناوبة",
    "OTHER": "أخرى",
}
REPORT_STATUS_AR = {"OPEN": "مفتوح", "RESOLVED": "عولج", "DISMISSED": "مرفوض"}


def _stream(header: list[str], rows: Iterable[list[Any]]) -> Iterator[str]:
    writer = csv.writer(_Echo())
    yield BOM + writer.writerow(header)
    for count, row in enumerate(rows):
        if count >= MAX_EXPORT_ROWS:
            break
        yield writer.writerow([_cell(value) for value in row])


def _csv_response(name: str, header: list[str], rows: Iterable[list[Any]]) -> Any:
    stamp = timezone.now().strftime("%Y%m%d-%H%M")
    response = StreamingHttpResponse(_stream(header, rows), content_type="text/csv; charset=utf-8")
    response["Content-Disposition"] = f'attachment; filename="{name}-{stamp}.csv"'
    response["Cache-Control"] = "private, no-store"
    response["X-Content-Type-Options"] = "nosniff"
    return response


def _filter(name: str, description: str) -> OpenApiParameter:
    return OpenApiParameter(
        name, str, OpenApiParameter.QUERY, required=False, description=description
    )


CSV_200 = OpenApiResponse(
    response=OpenApiTypes.STR,
    description=(
        "text/csv; charset=utf-8, starting with a UTF-8 byte-order mark, sent as an "
        f"attachment. At most {MAX_EXPORT_ROWS} data rows."
    ),
)
# Success is CSV; every failure is still the JSON error envelope.
CSV_RESPONSES: dict[Any, OpenApiResponse] = {
    (200, "text/csv"): CSV_200,
    (400, "application/json"): VALIDATION_400,
    (401, "application/json"): UNAUTHENTICATED_401,
    (403, "application/json"): FORBIDDEN_403,
}


class CsvExportView(AdminView):
    renderer_classes = [JSONRenderer, CsvRenderer]

    def perform_content_negotiation(self, request: Any, force: bool = False) -> Any:
        # An `Accept: text/csv` client must get the CSV, or a JSON error, never a 406.
        return super().perform_content_negotiation(request, force=True)

    def finalize_response(self, request: Any, response: Any, *args: Any, **kwargs: Any) -> Any:
        # The CSV itself is a StreamingHttpResponse and bypasses renderers; anything DRF
        # renders here is an error envelope, which is always JSON.
        if isinstance(response, Response):
            request.accepted_renderer = JSONRenderer()
            request.accepted_media_type = "application/json"
        return super().finalize_response(request, response, *args, **kwargs)


class FacilitiesCsvView(CsvExportView):
    required_permission = "admin.facilities.read"

    @extend_schema(
        operation_id="adminExportFacilitiesCsv",
        tags=["Admin Exports"],
        summary="Export the facility list as CSV",
        description="Same filters and ordering as the facility list, without its 250 cap.",
        parameters=[
            _filter("status", "Facility status."),
            _filter("province", "Province id."),
            _filter("category", "Category id."),
            _filter("q", "Free text matched against the facility names."),
            _filter("issue", f"One of {', '.join(QUALITY_ISSUES)}."),
            _filter("ordering", "qualityScore, -qualityScore, updatedAt or -updatedAt."),
        ],
        responses=CSV_RESPONSES,
    )
    def get(self, request: Any) -> Any:
        queryset = filtered_facilities(request.query_params)
        header = [
            "المعرّف",
            "الاسم",
            "الاسم بالإنكليزية",
            "الحالة",
            "التصنيف",
            "المحافظة",
            "الهاتف",
            "خط العرض",
            "خط الطول",
            "اكتمال البيانات",
            "ما ينقصها",
            "آخر تحقق",
            "آخر تأكيد للدوام",
            "آخر تحديث",
        ]

        def rows() -> Iterator[list[Any]]:
            for item in queryset.iterator(chunk_size=CHUNK):
                quality = quality_payload(item)
                yield [
                    item.pk,
                    item.name_ar,
                    item.name_en,
                    FACILITY_STATUS_AR.get(item.status, item.status),
                    item.category.name_ar,
                    item.province.name_ar,
                    item.phone,
                    item.location.y if item.location else None,
                    item.location.x if item.location else None,
                    quality["qualityScore"],
                    "، ".join(QUALITY_ISSUE_AR.get(i, i) for i in quality["qualityIssues"]),
                    item.last_verified_at,
                    item.hours_confirmed_at,
                    item.updated_at,
                ]

        return _csv_response("facilities", header, rows())


class ReportsCsvView(CsvExportView):
    required_permission = "admin.reports.read"

    @extend_schema(
        operation_id="adminExportReportsCsv",
        tags=["Admin Exports"],
        summary="Export problem reports as CSV",
        description="Same filters as the report list, newest first, without its 250 cap.",
        parameters=[
            _filter("status", "OPEN, RESOLVED or DISMISSED."),
            _filter("facility", "Facility id."),
        ],
        responses=CSV_RESPONSES,
    )
    def get(self, request: Any) -> Any:
        queryset = filtered_reports(request.query_params)
        header = [
            "المعرّف",
            "معرّف المنشأة",
            "المنشأة",
            "السبب",
            "الملاحظة",
            "الحالة",
            "تاريخ البلاغ",
            "معرّف من حسمه",
            "تاريخ الحسم",
        ]

        def rows() -> Iterator[list[Any]]:
            for item in queryset.iterator(chunk_size=CHUNK):
                yield [
                    item.pk,
                    item.facility_id,
                    item.facility.name_ar,
                    REPORT_REASON_AR.get(item.reason, item.reason),
                    item.note,
                    REPORT_STATUS_AR.get(item.status, item.status),
                    item.created_at,
                    item.resolved_by_id,
                    item.resolved_at,
                ]

        return _csv_response("reports", header, rows())


class AuditCsvView(CsvExportView):
    required_permission = "admin.audit.read"

    @extend_schema(
        operation_id="adminExportAuditCsv",
        tags=["Admin Exports"],
        summary="Export the audit trail as CSV",
        description=(
            "Same filters as the audit search, newest first, without its 250 cap. Snapshots "
            "are left out; metadata is included as recorded (already redacted)."
        ),
        parameters=[
            _filter("actor", "Actor user id."),
            _filter("action", "Substring of the action code."),
            _filter("resource", "Substring of the target type, or an exact target id."),
            _filter("requestId", "Exact request correlation id."),
            _filter("from", "ISO date or datetime."),
            _filter("to", "ISO date or datetime; a bare date includes that whole day."),
        ],
        responses=CSV_RESPONSES,
    )
    def get(self, request: Any) -> Any:
        queryset = filtered_audit(request.query_params).values_list(
            "id",
            "created_at",
            "actor_id",
            "actor__name",
            "action",
            "target_type",
            "target_id",
            "request_id",
            "metadata",
        )
        header = [
            "المعرّف",
            "الوقت",
            "معرّف المنفّذ",
            "المنفّذ",
            "الإجراء",
            "نوع العنصر",
            "معرّف العنصر",
            "معرّف الطلب",
            "التفاصيل",
        ]

        def rows() -> Iterator[list[Any]]:
            import json

            for row in queryset.iterator(chunk_size=CHUNK):
                *head, metadata = row
                yield [*head, json.dumps(metadata or {}, ensure_ascii=False, sort_keys=True)]

        return _csv_response("audit", header, rows())
