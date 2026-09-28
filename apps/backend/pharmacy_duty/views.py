from django.db import transaction
from django.shortcuts import get_object_or_404
from drf_spectacular.utils import extend_schema
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from business_hours.permissions import require_facility_manager
from core.openapi import CONFLICT_409, NOT_FOUND_404, VALIDATION_400, protected
from facilities.models import Facility

from .models import DutyShift
from .schemas import DutyShiftListSerializer
from .serializers import DutyShiftInputSerializer, DutyShiftSerializer
from .services import require_duty_capability, save_shift


class DutyListCreateView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="ownerFacilityDutyList",
        tags=["Duty"],
        summary="List duty shifts of a facility",
        responses={200: DutyShiftListSerializer, **protected(), 404: NOT_FOUND_404},
    )
    def get(self, request, facility_id):
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
    def post(self, request, facility_id):
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
    def patch(self, request, facility_id, shift_id):
        facility = get_object_or_404(Facility, pk=facility_id)
        require_facility_manager(request.user, facility)
        row = get_object_or_404(
            DutyShift.objects.select_for_update(),
            pk=shift_id,
            facility=facility,
        )
        serializer = DutyShiftInputSerializer(row, data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        for key, value in serializer.validated_data.items():
            setattr(row, key, value)
        save_shift(row)
        return Response(DutyShiftSerializer(row).data)

    @extend_schema(
        operation_id="ownerFacilityDutyDelete",
        tags=["Duty"],
        summary="Remove a duty shift",
        responses={204: None, **protected(), 404: NOT_FOUND_404},
    )
    def delete(self, request, facility_id, shift_id):
        facility = get_object_or_404(Facility, pk=facility_id)
        require_facility_manager(request.user, facility)
        row = get_object_or_404(DutyShift, pk=shift_id, facility=facility)
        row.delete()
        return Response(status=204)
