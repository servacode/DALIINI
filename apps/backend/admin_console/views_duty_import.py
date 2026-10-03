"""Duty in bulk: a roster read from a spreadsheet, and rotations that generate whole months.

Both are previewed first: every row is checked against the province's pharmacies and the shifts
already stored, and nothing is written until the operator applies a preview with no errors.
Applying writes all of it or none of it, and re-applying the same input changes nothing.
"""

from __future__ import annotations

from collections import defaultdict
from typing import Any
from uuid import UUID

from django.shortcuts import get_object_or_404
from drf_spectacular.utils import extend_schema
from rest_framework import serializers
from rest_framework.exceptions import ValidationError
from rest_framework.parsers import FormParser, MultiPartParser
from rest_framework.response import Response

from accounts.authentication import AuthenticatedRequest
from audit.services import record_audit
from core.openapi import NOT_FOUND_404, VALIDATION_400, protected
from facilities.models import Facility, FacilityMembership
from facilities.serializers import UploadedFileField
from locations.models import Province
from notifications.models import Notification
from notifications.services import notify
from pharmacy_duty import importer, rotation
from pharmacy_duty.importer import RowResult
from pharmacy_duty.models import DutyRotation

from .views import AdminView, _request_id
from .views_smart import require_permission

MANAGE = "admin.duty.manage"


class DutyImportRequestSerializer(serializers.Serializer[Any]):
    file = UploadedFileField(help_text="CSV (UTF-8) or XLSX, first sheet, header row first.")
    provinceId = serializers.UUIDField()
    apply = serializers.BooleanField(
        default=False, help_text="False previews; true writes, refused if any row has an error."
    )


class DutyImportRowSerializer(serializers.Serializer[Any]):
    line = serializers.IntegerField(help_text="Row number in the file (the header is line 1).")
    facilityId = serializers.UUIDField(allow_null=True)
    facilityNameAr = serializers.CharField(allow_null=True)
    startsAt = serializers.DateTimeField(allow_null=True)
    endsAt = serializers.DateTimeField(allow_null=True)
    outcome = serializers.ChoiceField(
        choices=[("CREATED", "CREATED"), ("UPDATED", "UPDATED"), ("UNCHANGED", "UNCHANGED")],
        allow_null=True,
    )
    problems = serializers.ListField(
        child=serializers.CharField(), help_text="Why the row cannot be applied; empty when it can."
    )


class DutyImportResultSerializer(serializers.Serializer[Any]):
    applied = serializers.BooleanField()
    rows = DutyImportRowSerializer(many=True)
    errorCount = serializers.IntegerField()
    created = serializers.IntegerField()
    unchanged = serializers.IntegerField()


class DutyRotationRequestSerializer(serializers.Serializer[Any]):
    name = serializers.CharField(max_length=120)
    provinceId = serializers.UUIDField()
    facilityIds = serializers.ListField(child=serializers.UUIDField(), min_length=1, max_length=400)
    startsAt = serializers.TimeField(help_text="Damascus time, for example 20:00.")
    endsAt = serializers.TimeField(help_text="At or before the start means the next morning.")
    perDay = serializers.IntegerField(min_value=1, max_value=10, default=1)
    anchorDate = serializers.DateField(
        help_text="The day the first pharmacy of the list is on duty."
    )


class DutyRotationSerializer(DutyRotationRequestSerializer):
    id = serializers.UUIDField()


class DutyRotationListSerializer(serializers.Serializer[Any]):
    items = DutyRotationSerializer(many=True)


class DutyRotationGenerateSerializer(serializers.Serializer[Any]):
    fromDate = serializers.DateField()
    toDate = serializers.DateField()
    apply = serializers.BooleanField(default=False)

    def validate(self, attrs: dict[str, Any]) -> dict[str, Any]:
        if problem := rotation.period_problem(attrs["fromDate"], attrs["toDate"]):
            raise ValidationError({"toDate": [problem]})
        return attrs


def _rotation_payload(item: DutyRotation) -> dict[str, Any]:
    return {
        "id": str(item.pk),
        "name": item.name,
        "provinceId": str(item.province_id),
        "facilityIds": list(item.facility_ids),
        "startsAt": item.starts_at_time.strftime("%H:%M"),
        "endsAt": item.ends_at_time.strftime("%H:%M"),
        "perDay": item.per_day,
        "anchorDate": item.anchor_date.isoformat(),
    }


def _run(rows: list[RowResult], *, apply: bool) -> dict[str, Any]:
    if not apply:
        return {"applied": False, **importer.trial(rows)}
    if any(row.errors for row in rows):
        return {"applied": False, **importer.trial(rows)}
    checked = importer.trial(rows)
    if checked["errorCount"]:
        return {"applied": False, **checked}
    return {"applied": True, **importer.apply(rows)}


def _tell_owners(result: dict[str, Any]) -> None:
    """One message per pharmacy that gained shifts, rather than one per shift."""
    gained: dict[str, int] = defaultdict(int)
    for row in result["rows"]:
        if row["outcome"] == "CREATED" and row["facilityId"]:
            gained[row["facilityId"]] += 1
    if not gained:
        return
    facilities = {str(item.pk): item for item in Facility.objects.filter(pk__in=list(gained))}
    owners = FacilityMembership.objects.filter(
        facility_id__in=list(gained),
        role=FacilityMembership.Role.OWNER,
        user__is_active=True,
    ).select_related("user")
    for membership in owners:
        facility = facilities.get(str(membership.facility_id))
        if facility is None:
            continue
        count = gained[str(facility.pk)]
        notify(
            user=membership.user,
            type="duty.shift.admin_changed",
            title_ar="ورديات مناوبة جديدة",
            body_ar=(
                f"أضافت إدارة الدليل {count} وردية مناوبة لـ{facility.name_ar} من جدول المناوبات."
            ),
            destination=Notification.Destination.OWNER_FACILITIES,
            payload={"facilityId": str(facility.pk)},
        )


class DutyImportView(AdminView):
    required_permission = MANAGE
    parser_classes = [MultiPartParser, FormParser]

    @extend_schema(
        operation_id="adminDutyImport",
        tags=["Admin Duty"],
        summary="Read a duty roster from a spreadsheet; preview it, or apply it",
        description=(
            "Columns in Arabic or English: the pharmacy (`facilityId`, `pharmacy`/`الصيدلية` "
            "by name, or `phone`/`الهاتف`) and either `date`/`التاريخ` with `from`/`من` and "
            "`to`/`إلى` in Damascus time (an end at or before the start is the next morning), "
            "or `startsAt` and `endsAt`. Every row is checked against the province's "
            "pharmacies and the stored shifts. `apply` writes all rows or none, and only when no "
            "row has an error; re-applying the same file changes nothing. At most 2000 rows."
        ),
        request={"multipart/form-data": DutyImportRequestSerializer},
        responses={200: DutyImportResultSerializer, 400: VALIDATION_400, **protected()},
    )
    def post(self, request: AuthenticatedRequest) -> Response:
        payload = DutyImportRequestSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        province = get_object_or_404(Province, pk=payload.validated_data["provinceId"])
        upload = payload.validated_data["file"]
        try:
            table, problem = importer.read_table(upload, upload.name or "")
        except UnicodeDecodeError as exc:
            raise ValidationError({"file": ["الملف ليس بترميز UTF-8."]}) from exc
        except Exception as exc:  # a corrupt workbook raises whatever its parser raises
            raise ValidationError(
                {"file": ["تعذرت قراءة الملف. احفظه بصيغة CSV أو XLSX."]}
            ) from exc
        if problem:
            raise ValidationError({"file": [problem]})
        rows = importer.check(table, province.pk)
        result = _run(rows, apply=payload.validated_data["apply"])
        if result["applied"]:
            record_audit(
                actor=request.user,
                action="duty.imported",
                target=province,
                metadata={
                    "file": (upload.name or "")[:120],
                    "rows": len(rows),
                    "created": result["created"],
                    "unchanged": result["unchanged"],
                },
                request_id=_request_id(request),
            )
            _tell_owners(result)
        return Response(result)


class DutyRotationListView(AdminView):
    required_permission = "admin.duty.read"

    @extend_schema(
        operation_id="adminDutyRotationsList",
        tags=["Admin Duty"],
        summary="Saved duty rotations",
        responses={200: DutyRotationListSerializer, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        items = DutyRotation.objects.order_by("province__name_ar", "name")
        if value := request.query_params.get("provinceId"):
            items = items.filter(province_id=value)
        return Response({"items": [_rotation_payload(item) for item in items[:200]]})

    @extend_schema(
        operation_id="adminDutyRotationCreate",
        tags=["Admin Duty"],
        summary="Save a duty rotation",
        request=DutyRotationRequestSerializer,
        responses={201: DutyRotationSerializer, 400: VALIDATION_400, **protected()},
    )
    def post(self, request: AuthenticatedRequest) -> Response:
        require_permission(self, request, MANAGE)
        payload = DutyRotationRequestSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        item = DutyRotation(created_by=request.user)
        _fill(item, payload.validated_data)
        item.save()
        record_audit(
            actor=request.user,
            action="duty_rotation.created",
            target=item,
            after_snapshot=_rotation_payload(item),
            request_id=_request_id(request),
        )
        return Response(_rotation_payload(item), status=201)


def _fill(item: DutyRotation, data: dict[str, Any]) -> None:
    province = get_object_or_404(Province, pk=data.get("provinceId", item.province_id))
    ids = [str(value) for value in data.get("facilityIds", item.facility_ids)]
    if len(set(ids)) != len(ids):
        raise ValidationError({"facilityIds": ["A pharmacy appears twice in the rotation."]})
    known = set(
        str(pk)
        for pk in Facility.objects.filter(
            pk__in=ids, province=province, category__capabilities__supports_duty=True
        ).values_list("pk", flat=True)
    )
    if missing := [value for value in ids if value not in known]:
        raise ValidationError(
            {"facilityIds": [f"Not a pharmacy of this province: {', '.join(missing[:5])}"]}
        )
    item.province = province
    item.facility_ids = ids
    for key, attribute in (
        ("name", "name"),
        ("startsAt", "starts_at_time"),
        ("endsAt", "ends_at_time"),
        ("perDay", "per_day"),
        ("anchorDate", "anchor_date"),
    ):
        if key in data:
            setattr(item, attribute, data[key])


class DutyRotationDetailView(AdminView):
    required_permission = MANAGE

    @extend_schema(
        operation_id="adminDutyRotationUpdate",
        tags=["Admin Duty"],
        summary="Change a saved duty rotation",
        request=DutyRotationRequestSerializer,
        responses={
            200: DutyRotationSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def patch(self, request: AuthenticatedRequest, rotation_id: UUID) -> Response:
        item = get_object_or_404(DutyRotation, pk=rotation_id)
        before = _rotation_payload(item)
        payload = DutyRotationRequestSerializer(data=request.data, partial=True)
        payload.is_valid(raise_exception=True)
        _fill(item, payload.validated_data)
        item.save()
        record_audit(
            actor=request.user,
            action="duty_rotation.updated",
            target=item,
            before_snapshot=before,
            after_snapshot=_rotation_payload(item),
            request_id=_request_id(request),
        )
        return Response(_rotation_payload(item))

    @extend_schema(
        operation_id="adminDutyRotationDelete",
        tags=["Admin Duty"],
        summary="Delete a saved duty rotation",
        description="The shifts it generated stay; only the template goes.",
        responses={204: None, **protected(), 404: NOT_FOUND_404},
    )
    def delete(self, request: AuthenticatedRequest, rotation_id: UUID) -> Response:
        item = get_object_or_404(DutyRotation, pk=rotation_id)
        record_audit(
            actor=request.user,
            action="duty_rotation.deleted",
            target=item,
            before_snapshot=_rotation_payload(item),
            request_id=_request_id(request),
        )
        item.delete()
        return Response(status=204)


class DutyRotationGenerateView(AdminView):
    required_permission = MANAGE

    @extend_schema(
        operation_id="adminDutyRotationGenerate",
        tags=["Admin Duty"],
        summary="Generate a period's shifts from a rotation; preview them, or apply them",
        description=(
            "Up to three months at a time. The same checks and all-or-nothing writing as an "
            "import; applying a period twice changes nothing."
        ),
        request=DutyRotationGenerateSerializer,
        responses={
            200: DutyImportResultSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def post(self, request: AuthenticatedRequest, rotation_id: UUID) -> Response:
        item = get_object_or_404(DutyRotation, pk=rotation_id)
        payload = DutyRotationGenerateSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        rows = rotation.rows_for(
            item, payload.validated_data["fromDate"], payload.validated_data["toDate"]
        )
        result = _run(rows, apply=payload.validated_data["apply"])
        if result["applied"]:
            record_audit(
                actor=request.user,
                action="duty_rotation.applied",
                target=item,
                metadata={
                    "from": payload.validated_data["fromDate"].isoformat(),
                    "to": payload.validated_data["toDate"].isoformat(),
                    "created": result["created"],
                    "unchanged": result["unchanged"],
                },
                request_id=_request_id(request),
            )
            _tell_owners(result)
        return Response(result)
