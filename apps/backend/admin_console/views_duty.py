"""The duty roster as operators see and repair it.

Pharmacists enter their own duty shifts in the app. Operators see the roster per province
(or city) and day, with the days nobody covers flagged by the same rule as the DUTY_GAP alert
(`pharmacy_duty.coverage`), and can add, move or cancel a shift on a pharmacy's behalf. Those
writes go through `pharmacy_duty.services.upsert_duty_shifts`, the same validation the owner
flow uses, are recorded with source ADMIN, are audited, and tell the pharmacy's owners.
"""

from __future__ import annotations

import uuid
from datetime import date, datetime, timedelta
from typing import Any

from django.core.exceptions import ObjectDoesNotExist
from django.shortcuts import get_object_or_404
from django.utils import timezone
from django.utils.dateparse import parse_date
from drf_spectacular.utils import OpenApiParameter, extend_schema
from rest_framework.exceptions import ValidationError
from rest_framework.response import Response

from audit.services import record_audit
from business_hours.services import DAMASCUS
from core.openapi import CONFLICT_409, NOT_FOUND_404, VALIDATION_400, protected
from facilities.models import Facility, FacilityMembership
from locations.models import City, Province
from notifications.models import Notification
from notifications.services import notify
from pharmacy_duty.coverage import (
    DEFAULT_WINDOW_DAYS,
    day_bounds,
    duty_gaps,
    local_today,
    shifts_in,
    window,
)
from pharmacy_duty.models import DutyShift
from pharmacy_duty.nudges import day_label_ar
from pharmacy_duty.services import shift_snapshot, upsert_duty_shifts

from .schemas_smart import (
    AdminDutyRosterSerializer,
    AdminDutyShiftCreateRequestSerializer,
    AdminDutyShiftSerializer,
    AdminDutyShiftUpdateRequestSerializer,
)
from .views import AdminView
from .views_smart import require_permission

MAX_ROSTER_DAYS = 62
NOTIFICATION_TYPE = "duty.shift.admin_changed"
NOTIFICATION_TITLE_AR = "تغيير في مناوبة صيدليتك"


def _request_id(request: Any) -> str:
    return getattr(request, "request_id", "")


def _status(shift: DutyShift, now: datetime) -> str:
    if now < shift.starts_at:
        return "UPCOMING"
    if now < shift.ends_at:
        return "ONGOING"
    return "ENDED"


def shift_payload(shift: DutyShift, now: datetime | None = None) -> dict[str, Any]:
    now = now or timezone.now()
    return {
        "id": str(shift.pk),
        "facilityId": str(shift.facility_id),
        "facilityNameAr": shift.facility.name_ar,
        "cityId": str(shift.facility.city_id) if shift.facility.city_id else None,
        "startsAt": shift.starts_at.isoformat(),
        "endsAt": shift.ends_at.isoformat(),
        "createdBy": shift.source,
        "status": _status(shift, now),
    }


def _when_ar(starts_at: datetime, ends_at: datetime) -> str:
    start = starts_at.astimezone(DAMASCUS)
    end = ends_at.astimezone(DAMASCUS)
    return f"يوم {day_label_ar(start.date())} من {start:%H:%M} إلى {end:%H:%M}"


def _tell_owners(facility: Facility, body_ar: str, shift_id: str) -> None:
    owners = FacilityMembership.objects.filter(
        facility=facility, role=FacilityMembership.Role.OWNER, user__is_active=True
    ).select_related("user")
    for membership in owners:
        notify(
            user=membership.user,
            type=NOTIFICATION_TYPE,
            title_ar=NOTIFICATION_TITLE_AR,
            body_ar=body_ar[:400],
            destination=Notification.Destination.OWNER_FACILITIES,
            payload={"facilityId": str(facility.pk), "shiftId": shift_id},
        )


def _uuid_param(value: str | None, name: str) -> uuid.UUID:
    if not value:
        raise ValidationError({name: ["Required."]})
    try:
        return uuid.UUID(value)
    except ValueError as exc:
        raise ValidationError({name: ["Must be a valid UUID."]}) from exc


def _parse_day(value: str | None, name: str) -> date | None:
    if not value:
        return None
    parsed = parse_date(value)
    if parsed is None:
        raise ValidationError({name: ["Expected a date, YYYY-MM-DD."]})
    return parsed


class DutyRosterView(AdminView):
    required_permission = "admin.duty.read"

    @extend_schema(
        operation_id="adminDutyRosterRetrieve",
        tags=["Admin Duty"],
        summary="The duty roster of a province (or city), day by day",
        description=(
            "Days are Damascus calendar days from `from` to `to` inclusive, by default today "
            f"and the next {DEFAULT_WINDOW_DAYS - 1} days, at most {MAX_ROSTER_DAYS}. Each day "
            "lists the shifts of ACTIVE duty pharmacies overlapping it, and `gap` is true "
            "when there is none: the rule behind the DUTY_GAP alert. With `cityId`, shifts "
            "and gaps are those of that city's pharmacies."
        ),
        parameters=[
            OpenApiParameter("provinceId", str, OpenApiParameter.QUERY, required=True),
            OpenApiParameter("cityId", str, OpenApiParameter.QUERY, required=False),
            OpenApiParameter(
                "from", str, OpenApiParameter.QUERY, required=False, description="YYYY-MM-DD"
            ),
            OpenApiParameter(
                "to", str, OpenApiParameter.QUERY, required=False, description="YYYY-MM-DD"
            ),
        ],
        responses={
            200: AdminDutyRosterSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def get(self, request: Any) -> Response:
        province = get_object_or_404(
            Province, pk=_uuid_param(request.query_params.get("provinceId"), "provinceId")
        )
        city = None
        if request.query_params.get("cityId"):
            city_id = _uuid_param(request.query_params.get("cityId"), "cityId")
            city = City.objects.filter(pk=city_id, province=province).first()
            if city is None:
                raise ValidationError({"cityId": ["Not a city of this province."]})
        start = _parse_day(request.query_params.get("from"), "from") or local_today()
        end = _parse_day(request.query_params.get("to"), "to") or (
            start + timedelta(days=DEFAULT_WINDOW_DAYS - 1)
        )
        if end < start:
            raise ValidationError({"to": ["Must not be before `from`."]})
        count = (end - start).days + 1
        if count > MAX_ROSTER_DAYS:
            raise ValidationError({"to": [f"At most {MAX_ROSTER_DAYS} days at once."]})
        days = window(start, count)
        city_filter = city.pk if city else None
        gaps = set(
            duty_gaps(days, province_ids=[province.pk], city_id=city_filter)[str(province.pk)]
        )
        shifts = list(
            shifts_in(days, province_ids=[province.pk], city_id=city_filter)
            .select_related("facility")
            .order_by("starts_at", "id")
        )
        now = timezone.now()
        payload_days = []
        for day in days:
            day_start, day_end = day_bounds(day)
            payload_days.append(
                {
                    "date": day.isoformat(),
                    "gap": day in gaps,
                    "shifts": [
                        shift_payload(shift, now)
                        for shift in shifts
                        if shift.starts_at < day_end and shift.ends_at > day_start
                    ],
                }
            )
        return Response(
            {
                "provinceId": str(province.pk),
                "cityId": str(city.pk) if city else None,
                "from": start.isoformat(),
                "to": end.isoformat(),
                "days": payload_days,
            }
        )

    @extend_schema(
        operation_id="adminDutyShiftCreate",
        tags=["Admin Duty"],
        summary="Put a duty shift on a pharmacy's roster",
        description=(
            "Same rules as the owner endpoint: the category must support duty (409 "
            "DUTY_NOT_SUPPORTED), the shift must not overlap another of the same pharmacy or "
            "be invalid (409 DUTY_OVERLAP_OR_INVALID) and must not fall in a temporary "
            "closure (409 DUTY_DURING_CLOSURE). Recorded with `createdBy` ADMIN, audited, "
            "and the pharmacy's owners are notified. Sending a shift identical to an "
            "existing one returns it with 200 and changes nothing."
        ),
        request=AdminDutyShiftCreateRequestSerializer,
        responses={
            201: AdminDutyShiftSerializer,
            200: AdminDutyShiftSerializer,
            400: VALIDATION_400,
            **protected(),
            409: CONFLICT_409,
        },
    )
    def post(self, request: Any) -> Response:
        require_permission(self, request, "admin.duty.manage")
        payload = AdminDutyShiftCreateRequestSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        data = payload.validated_data
        if data["endsAt"] <= data["startsAt"]:
            raise ValidationError({"endsAt": ["Must be after startsAt."]})
        if not Facility.objects.filter(pk=data["facilityId"]).exists():
            raise ValidationError({"facilityId": ["Unknown facility."]})
        (result,) = upsert_duty_shifts(
            [
                {
                    "facility_id": data["facilityId"],
                    "starts_at": data["startsAt"],
                    "ends_at": data["endsAt"],
                }
            ],
            source=DutyShift.Source.ADMIN,
        )
        shift = result.shift
        if result.outcome == "UNCHANGED":
            return Response(shift_payload(shift))
        record_audit(
            actor=request.user,
            action="duty_shift.created",
            target=shift,
            after_snapshot=shift_snapshot(shift),
            metadata={"facilityId": str(shift.facility_id)},
            request_id=_request_id(request),
        )
        _tell_owners(
            shift.facility,
            f"أضافت إدارة الدليل وردية مناوبة لـ{shift.facility.name_ar} "
            f"{_when_ar(shift.starts_at, shift.ends_at)}.",
            str(shift.pk),
        )
        return Response(shift_payload(shift), status=201)


class DutyShiftDetailView(AdminView):
    required_permission = "admin.duty.manage"

    @extend_schema(
        operation_id="adminDutyShiftUpdate",
        tags=["Admin Duty"],
        summary="Move a duty shift",
        description=(
            "Same validation as creating one. `createdBy` keeps who created the shift. "
            "Audited; the pharmacy's owners are notified when the times change."
        ),
        request=AdminDutyShiftUpdateRequestSerializer,
        responses={
            200: AdminDutyShiftSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
            409: CONFLICT_409,
        },
    )
    def patch(self, request: Any, shift_id: Any) -> Response:
        shift = get_object_or_404(DutyShift.objects.select_related("facility"), pk=shift_id)
        payload = AdminDutyShiftUpdateRequestSerializer(data=request.data, partial=True)
        payload.is_valid(raise_exception=True)
        starts_at = payload.validated_data.get("startsAt", shift.starts_at)
        ends_at = payload.validated_data.get("endsAt", shift.ends_at)
        if ends_at <= starts_at:
            raise ValidationError({"endsAt": ["Must be after startsAt."]})
        try:
            (result,) = upsert_duty_shifts(
                [
                    {
                        "id": shift.pk,
                        "facility_id": shift.facility_id,
                        "starts_at": starts_at,
                        "ends_at": ends_at,
                    }
                ],
                source=DutyShift.Source.ADMIN,
            )
        except ObjectDoesNotExist as exc:
            raise ValidationError({"id": ["The shift no longer exists."]}) from exc
        updated = result.shift
        if result.outcome == "UPDATED":
            record_audit(
                actor=request.user,
                action="duty_shift.updated",
                target=updated,
                before_snapshot=result.before,
                after_snapshot=shift_snapshot(updated),
                metadata={"facilityId": str(updated.facility_id)},
                request_id=_request_id(request),
            )
            _tell_owners(
                updated.facility,
                f"عدّلت إدارة الدليل وردية مناوبة {updated.facility.name_ar}، وصارت "
                f"{_when_ar(updated.starts_at, updated.ends_at)}.",
                str(updated.pk),
            )
        return Response(shift_payload(updated))

    @extend_schema(
        operation_id="adminDutyShiftDelete",
        tags=["Admin Duty"],
        summary="Cancel a duty shift",
        description="Audited; the pharmacy's owners are notified.",
        responses={204: None, **protected(), 404: NOT_FOUND_404},
    )
    def delete(self, request: Any, shift_id: Any) -> Response:
        shift = get_object_or_404(DutyShift.objects.select_related("facility"), pk=shift_id)
        record_audit(
            actor=request.user,
            action="duty_shift.deleted",
            target=shift,
            before_snapshot=shift_snapshot(shift),
            metadata={"facilityId": str(shift.facility_id)},
            request_id=_request_id(request),
        )
        facility, shift_ref = shift.facility, str(shift.pk)
        body = (
            f"ألغت إدارة الدليل وردية مناوبة {facility.name_ar} "
            f"{_when_ar(shift.starts_at, shift.ends_at)}."
        )
        shift.delete()
        _tell_owners(facility, body, shift_ref)
        return Response(status=204)
