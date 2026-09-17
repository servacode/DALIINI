from django.core.exceptions import ValidationError
from django.shortcuts import get_object_or_404
from rest_framework.response import Response
from rest_framework.views import APIView

from facilities.models import Facility

from .models import TemporaryClosure
from .permissions import require_facility_manager
from .serializers import (
    BusinessHourInputSerializer,
    TemporaryClosureSerializer,
    serialize_hours,
)
from .services_write import replace_business_hours


class FacilityHoursView(APIView):
    def put(self, request, facility_id):
        facility = get_object_or_404(Facility, pk=facility_id)
        require_facility_manager(request.user, facility)
        if not facility.category.capabilities.supports_hours:
            return Response(
                {"error": {"code": "HOURS_NOT_SUPPORTED"}},
                status=409,
            )

        items = BusinessHourInputSerializer(data=request.data, many=True)
        items.is_valid(raise_exception=True)
        try:
            created = replace_business_hours(
                actor=request.user,
                facility=facility,
                rows=list(items.validated_data),
            )
        except ValidationError as exc:
            details = (
                exc.message_dict
                if hasattr(exc, "message_dict")
                else exc.messages
            )
            return Response(
                {"error": {"code": "INVALID_HOURS", "details": details}},
                status=400,
            )
        return Response({"items": serialize_hours(created)})


class TemporaryClosureListCreateView(APIView):
    def get(self, request, facility_id):
        facility = get_object_or_404(Facility, pk=facility_id)
        require_facility_manager(request.user, facility)
        queryset = facility.temporary_closures.order_by("-starts_at")
        return Response(
            {"items": TemporaryClosureSerializer(queryset, many=True).data}
        )

    def post(self, request, facility_id):
        facility = get_object_or_404(Facility, pk=facility_id)
        require_facility_manager(request.user, facility)
        if not facility.category.capabilities.supports_temporary_closure:
            return Response(
                {"error": {"code": "TEMPORARY_CLOSURE_NOT_SUPPORTED"}},
                status=409,
            )
        serializer = TemporaryClosureSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        obj = serializer.save(facility=facility)
        return Response(TemporaryClosureSerializer(obj).data, status=201)


class TemporaryClosureDeleteView(APIView):
    def delete(self, request, facility_id, closure_id):
        facility = get_object_or_404(Facility, pk=facility_id)
        require_facility_manager(request.user, facility)
        closure = get_object_or_404(
            TemporaryClosure,
            pk=closure_id,
            facility=facility,
        )
        closure.delete()
        return Response(status=204)
