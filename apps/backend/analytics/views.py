from django.utils.dateparse import parse_datetime
from drf_spectacular.utils import OpenApiParameter, extend_schema
from rest_framework import status
from rest_framework.exceptions import ValidationError
from rest_framework.response import Response
from rest_framework.views import APIView

from core.openapi import VALIDATION_400
from core.throttles import AnalyticsIngestThrottle

from .schemas import AnalyticsEventAcceptedSerializer, AnalyticsEventRequestSerializer
from .services import record_product_event


class AnalyticsEventView(APIView):
    throttle_classes = [AnalyticsIngestThrottle]
    authentication_classes = []
    permission_classes = []

    @extend_schema(
        operation_id="analyticsEventCreate",
        tags=["Analytics"],
        summary="Record a product analytics event",
        description=(
            "The event name must exist in the central registry and its properties are "
            "checked against the keys declared for that event. Forbidden keys such as raw "
            "coordinates, phone numbers and tokens are rejected rather than stored."
        ),
        parameters=[
            OpenApiParameter(
                "X-Anonymous-Id",
                str,
                OpenApiParameter.HEADER,
                required=False,
                description="Client-generated pseudonymous id for unauthenticated callers.",
            )
        ],
        request=AnalyticsEventRequestSerializer,
        responses={202: AnalyticsEventAcceptedSerializer, 400: VALIDATION_400},
    )
    def post(self, request):
        body = request.data if isinstance(request.data, dict) else {}
        occurred_at = parse_datetime(body.get("occurredAt")) if body.get("occurredAt") else None
        try:
            event = record_product_event(
                name=body.get("name", ""),
                properties=body.get("properties", {}),
                occurred_at=occurred_at,
                user=request.user,
                anonymous_id=request.headers.get("X-Anonymous-Id", "")[:128],
            )
        except ValueError as exc:
            raise ValidationError({"event": str(exc)}) from exc
        return Response({"accepted": True, "id": str(event.id)}, status=status.HTTP_202_ACCEPTED)
