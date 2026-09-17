from django.core.exceptions import ValidationError as DjangoValidationError
from django.db import IntegrityError, transaction
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
from .serializers import DutyShiftSerializer


def _duty_error():
    return Response(
        {"error": {"code": "DUTY_OVERLAP_OR_INVALID"}},
        status=409,
    )


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
            "declare the duty capability accept this."
        ),
        request=DutyShiftSerializer,
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
        if not facility.category.capabilities.supports_duty:
            return Response({"error": {"code": "DUTY_NOT_SUPPORTED"}}, status=409)
        serializer = DutyShiftSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        row = DutyShift(facility=facility, **serializer.validated_data)
        try:
            row.full_clean()
            row.save()
        except (DjangoValidationError, IntegrityError):
            transaction.set_rollback(True)
            return _duty_error()
        return Response(DutyShiftSerializer(row).data, status=201)


class DutyDetailView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="ownerFacilityDutyUpdate",
        tags=["Duty"],
        summary="Adjust a duty shift",
        request=DutyShiftSerializer,
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
        serializer = DutyShiftSerializer(row, data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        for key, value in serializer.validated_data.items():
            setattr(row, key, value)
        try:
            row.full_clean()
            row.save()
        except (DjangoValidationError, IntegrityError):
            transaction.set_rollback(True)
            return _duty_error()
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
