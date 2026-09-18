from django.core.exceptions import ValidationError
from django.shortcuts import get_object_or_404
from drf_spectacular.utils import extend_schema
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from core.exceptions import ConflictError, DomainError
from core.openapi import CONFLICT_409, DOMAIN_400, NOT_FOUND_404, VALIDATION_400, protected
from facilities.models import Facility

from .models import TemporaryClosure
from .permissions import require_facility_manager
from .schemas import BusinessHoursListSerializer, TemporaryClosureListSerializer
from .serializers import (
    BusinessHourInputSerializer,
    TemporaryClosureSerializer,
    serialize_hours,
)
from .services_write import replace_business_hours


class FacilityHoursView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="ownerFacilityHoursReplace",
        tags=["Availability"],
        summary="Replace the weekly opening hours of a facility",
        description=(
            "The whole week is replaced in one call. Overnight spans are supported and "
            "same-day overlaps are rejected. Only categories that declare the hours "
            "capability accept this."
        ),
        request=BusinessHourInputSerializer(many=True),
        responses={
            200: BusinessHoursListSerializer,
            400: DOMAIN_400,
            **protected(),
            404: NOT_FOUND_404,
            409: CONFLICT_409,
        },
    )
    def put(self, request, facility_id):
        facility = get_object_or_404(Facility, pk=facility_id)
        require_facility_manager(request.user, facility)
        if not facility.category.capabilities.supports_hours:
            raise ConflictError(
                "HOURS_NOT_SUPPORTED",
                message="هذا التصنيف لا يدعم أوقات الدوام.",
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
            raise DomainError(
                "INVALID_HOURS",
                message="أوقات الدوام المرسلة غير صالحة.",
                details=getattr(exc, "message_dict", None) or exc.messages,
            ) from exc
        return Response({"items": serialize_hours(created)})


class TemporaryClosureListCreateView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="ownerFacilityTemporaryClosuresList",
        tags=["Availability"],
        summary="List temporary closures of a facility",
        responses={
            200: TemporaryClosureListSerializer,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def get(self, request, facility_id):
        facility = get_object_or_404(Facility, pk=facility_id)
        require_facility_manager(request.user, facility)
        queryset = facility.temporary_closures.order_by("-starts_at")
        return Response(
            {"items": TemporaryClosureSerializer(queryset, many=True).data}
        )

    @extend_schema(
        operation_id="ownerFacilityTemporaryClosureCreate",
        tags=["Availability"],
        summary="Open a temporary closure window",
        description="A temporary closure overrides both regular hours and duty.",
        request=TemporaryClosureSerializer,
        responses={
            201: TemporaryClosureSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
            409: CONFLICT_409,
        },
    )
    def post(self, request, facility_id):
        facility = get_object_or_404(Facility, pk=facility_id)
        require_facility_manager(request.user, facility)
        if not facility.category.capabilities.supports_temporary_closure:
            raise ConflictError(
                "TEMPORARY_CLOSURE_NOT_SUPPORTED",
                message="هذا التصنيف لا يدعم الإغلاق المؤقت.",
            )
        serializer = TemporaryClosureSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        obj = serializer.save(facility=facility)
        return Response(TemporaryClosureSerializer(obj).data, status=201)


class TemporaryClosureDeleteView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="ownerFacilityTemporaryClosureCancel",
        tags=["Availability"],
        summary="Cancel a temporary closure",
        responses={204: None, **protected(), 404: NOT_FOUND_404},
    )
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
