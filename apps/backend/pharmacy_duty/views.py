from django.core.exceptions import ValidationError as DjangoValidationError
from django.db import IntegrityError, transaction
from django.shortcuts import get_object_or_404
from rest_framework.response import Response
from rest_framework.views import APIView

from business_hours.permissions import require_facility_manager
from facilities.models import Facility

from .models import DutyShift
from .serializers import DutyShiftSerializer


def _duty_error():
    return Response(
        {"error": {"code": "DUTY_OVERLAP_OR_INVALID"}},
        status=409,
    )


class DutyListCreateView(APIView):
    def get(self, request, facility_id):
        facility = get_object_or_404(Facility, pk=facility_id)
        require_facility_manager(request.user, facility)
        shifts = facility.duty_shifts.order_by("starts_at")
        return Response({"items": DutyShiftSerializer(shifts, many=True).data})

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

    def delete(self, request, facility_id, shift_id):
        facility = get_object_or_404(Facility, pk=facility_id)
        require_facility_manager(request.user, facility)
        row = get_object_or_404(DutyShift, pk=shift_id, facility=facility)
        row.delete()
        return Response(status=204)
