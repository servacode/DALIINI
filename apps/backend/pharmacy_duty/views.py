from uuid import UUID

from django.db import transaction
from django.shortcuts import get_object_or_404
from drf_spectacular.utils import extend_schema
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from accounts.authentication import AuthenticatedRequest
from audit.services import record_audit
from business_hours.permissions import require_facility_manager
from core.openapi import CONFLICT_409, NOT_FOUND_404, VALIDATION_400, protected
from facilities.models import Facility

from .models import DutyShift
from .schemas import DutyShiftListSerializer
from .serializers import DutyShiftInputSerializer, DutyShiftSerializer
from .services import require_duty_capability, save_shift, shift_snapshot


class DutyListCreateView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="ownerFacilityDutyList",
        tags=["Duty"],
        summary="List duty shifts of a facility",
        responses={200: DutyShiftListSerializer, **protected(), 404: NOT_FOUND_404},
    )
    def get(self, request: AuthenticatedRequest, facility_id: UUID) -> Response:
        facility = get_object_or_404(Facility, pk=facility_id)
        require_facility_manager(request.user, facility)
        shifts = facility.duty_shifts.order_by("starts_at")
        return Response({"items": DutyShiftSerializer(shifts, many=True).data})

    @extend_schema(
        operation_id="ownerFacilityDutyCreate",
        tags=["Duty"],
        summary="Schedule a duty shift",
        description=(
            "Overlapping shifts for the same facility are refused by a PostgreSQL "
            "exclusion constraint, not only by application code. Only categories that "
            "declare the duty capability accept this, and a shift may not overlap a "
            "temporary closure of the facility (409 DUTY_DURING_CLOSURE)."
        ),
        request=DutyShiftInputSerializer,
        responses={
            201: DutyShiftSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
            409: CONFLICT_409,
        },
    )
    @transaction.atomic
    def post(self, request: AuthenticatedRequest, facility_id: UUID) -> Response:
        facility = get_object_or_404(Facility, pk=facility_id)
        require_facility_manager(request.user, facility)
        require_duty_capability(facility)
        serializer = DutyShiftInputSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        row = save_shift(
            DutyShift(
                facility=facility, source=DutyShift.Source.OWNER, **serializer.validated_data
            )
        )
        record_audit(
            actor=request.user,
            action="duty_shift.created",
            target=row,
            after_snapshot=shift_snapshot(row),
            metadata={"facilityId": str(facility.pk)},
            request_id=getattr(request, "request_id", ""),
        )
        return Response(DutyShiftSerializer(row).data, status=201)


class DutyDetailView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="ownerFacilityDutyUpdate",
        tags=["Duty"],
        summary="Adjust a duty shift",
        request=DutyShiftInputSerializer,
        responses={
            200: DutyShiftSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
            409: CONFLICT_409,
        },
    )
    @transaction.atomic
    def patch(self, request: AuthenticatedRequest, facility_id: UUID, shift_id: UUID) -> Response:
        facility = get_object_or_404(Facility, pk=facility_id)
        require_facility_manager(request.user, facility)
        row = get_object_or_404(
            DutyShift.objects.select_for_update(),
            pk=shift_id,
            facility=facility,
        )
        serializer = DutyShiftInputSerializer(row, data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        before, was = shift_snapshot(row), (row.starts_at, row.ends_at)
        for key, value in serializer.validated_data.items():
            setattr(row, key, value)
        save_shift(row)
        # Compared as instants: the same time sent with another offset is not a change.
        if (row.starts_at, row.ends_at) != was:
            record_audit(
                actor=request.user,
                action="duty_shift.updated",
                target=row,
                before_snapshot=before,
                after_snapshot=shift_snapshot(row),
                metadata={"facilityId": str(facility.pk)},
                request_id=getattr(request, "request_id", ""),
            )
        return Response(DutyShiftSerializer(row).data)

    @extend_schema(
        operation_id="ownerFacilityDutyDelete",
        tags=["Duty"],
        summary="Remove a duty shift",
        responses={204: None, **protected(), 404: NOT_FOUND_404},
    )
    @transaction.atomic
    def delete(self, request: AuthenticatedRequest, facility_id: UUID, shift_id: UUID) -> Response:
        facility = get_object_or_404(Facility, pk=facility_id)
        require_facility_manager(request.user, facility)
        row = get_object_or_404(DutyShift, pk=shift_id, facility=facility)
        record_audit(
            actor=request.user,
            action="duty_shift.deleted",
            target=row,
            before_snapshot=shift_snapshot(row),
            metadata={"facilityId": str(facility.pk)},
            request_id=getattr(request, "request_id", ""),
        )
        row.delete()
        return Response(status=204)
