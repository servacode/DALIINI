"""Public "report a problem" about a facility."""

from __future__ import annotations

from typing import Any

from django.shortcuts import get_object_or_404
from drf_spectacular.utils import extend_schema
from rest_framework import serializers
from rest_framework.permissions import AllowAny
from rest_framework.request import Request
from rest_framework.response import Response
from rest_framework.views import APIView

from accounts.models import User
from core.openapi import NOT_FOUND_404, VALIDATION_400
from core.throttles import FacilityReportThrottle
from search.selectors import public_facilities

from .models import FacilityReport


class FacilityReportRequestSerializer(serializers.Serializer[dict[str, Any]]):
    reason = serializers.ChoiceField(choices=FacilityReport.Reason.choices)
    note = serializers.CharField(max_length=500, required=False, allow_blank=True)


class FacilityReportCreatedSerializer(serializers.Serializer[dict[str, Any]]):
    id = serializers.UUIDField()
    status = serializers.ChoiceField(choices=FacilityReport.Status.choices)
    createdAt = serializers.DateTimeField()


class PublicFacilityReportView(APIView):
    permission_classes = [AllowAny]
    throttle_classes = [FacilityReportThrottle]

    @extend_schema(
        operation_id="publicFacilityReportCreate",
        tags=["Public Facilities"],
        summary="Report a problem with a facility's listing",
        description=(
            "Anonymous callers are allowed; a signed-in caller is recorded as the reporter. "
            "Strictly throttled per account or IP. Only publicly visible facilities accept "
            "reports."
        ),
        request=FacilityReportRequestSerializer,
        responses={201: FacilityReportCreatedSerializer, 400: VALIDATION_400, 404: NOT_FOUND_404},
    )
    def post(self, request: Request, facility_id: str) -> Response:
        facility = get_object_or_404(public_facilities().filter(pk=facility_id))  # type: ignore[no-untyped-call]
        serializer = FacilityReportRequestSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user = request.user if isinstance(request.user, User) else None
        report = FacilityReport.objects.create(
            facility=facility,
            reporter=user,
            reason=serializer.validated_data["reason"],
            note=serializer.validated_data.get("note", "").strip(),
        )
        return Response(
            {
                "id": str(report.pk),
                "status": report.status,
                "createdAt": report.created_at.isoformat(),
            },
            status=201,
        )
