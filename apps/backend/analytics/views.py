from django.utils.dateparse import parse_datetime
from rest_framework import status
from rest_framework.exceptions import ValidationError
from rest_framework.response import Response
from rest_framework.views import APIView

from .services import record_product_event


class AnalyticsEventView(APIView):
    authentication_classes = []
    permission_classes = []

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
