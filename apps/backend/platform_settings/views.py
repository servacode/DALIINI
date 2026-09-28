from drf_spectacular.utils import extend_schema
from rest_framework import serializers
from rest_framework.request import Request
from rest_framework.response import Response
from rest_framework.views import APIView

from .maintenance import get_maintenance_state


class PlatformStatusSerializer(serializers.Serializer[dict[str, object]]):
    maintenance = serializers.BooleanField()
    messageAr = serializers.CharField()
    retryAfterSeconds = serializers.IntegerField(min_value=0)


class PlatformStatusView(APIView):
    authentication_classes = []
    permission_classes = []

    @extend_schema(
        operation_id="publicPlatformStatusRetrieve",
        tags=["Public Platform"],
        summary="Platform availability (maintenance mode)",
        description=(
            "Always served, even during maintenance. While `maintenance` is true every other "
            "public and owner API answers 503 with code MAINTENANCE, details "
            "{retryAfterSeconds} and a Retry-After header."
        ),
        responses={200: PlatformStatusSerializer},
    )
    def get(self, request: Request) -> Response:
        response = Response(get_maintenance_state().as_payload())
        response["Cache-Control"] = "no-store"
        return response
