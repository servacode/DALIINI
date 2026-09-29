"""Owner-facing engagement counts, derived from the product analytics registry."""

from __future__ import annotations

from datetime import timedelta
from typing import Any

from django.db.models import Count
from django.shortcuts import get_object_or_404
from django.utils import timezone
from drf_spectacular.utils import extend_schema
from rest_framework import serializers
from rest_framework.permissions import IsAuthenticated
from rest_framework.request import Request
from rest_framework.response import Response
from rest_framework.views import APIView

from analytics.models import ProductAnalyticsEvent
from core.openapi import NOT_FOUND_404, protected

from .models import Facility
from .permissions import require_facility_member

WINDOW_DAYS = 30

# Wire name -> analytics registry event name. Only events the registry records are counted;
# there is no favorites event, so favorites are not reported.
INSIGHT_EVENTS = {
    "views": "facility_view",
    "calls": "phone_tap",
    "directions": "directions_start",
}


class OwnerFacilityInsightsSerializer(serializers.Serializer[dict[str, Any]]):
    facilityId = serializers.UUIDField()
    windowDays = serializers.IntegerField()
    since = serializers.DateTimeField()
    views = serializers.IntegerField(help_text="facility_view events.")
    calls = serializers.IntegerField(help_text="phone_tap events.")
    directions = serializers.IntegerField(help_text="directions_start events.")


class OwnerFacilityInsightsView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="ownerFacilityInsightsRetrieve",
        tags=["Owner"],
        summary="Engagement with a facility over the last 30 days",
        description="Counts of product analytics events that reference this facility.",
        responses={200: OwnerFacilityInsightsSerializer, **protected(), 404: NOT_FOUND_404},
    )
    def get(self, request: Request, facility_id: str) -> Response:
        facility = get_object_or_404(
            Facility.objects.filter(memberships__user=request.user.pk).distinct(),
            pk=facility_id,
        )
        require_facility_member(request.user, facility)
        since = timezone.now() - timedelta(days=WINDOW_DAYS)
        counts = dict(
            ProductAnalyticsEvent.objects.filter(
                occurred_at__gte=since,
                name__in=INSIGHT_EVENTS.values(),
                properties__facilityId=str(facility.pk),
            )
            .values_list("name")
            .annotate(count=Count("id"))
            .values_list("name", "count")
        )
        return Response(
            {
                "facilityId": str(facility.pk),
                "windowDays": WINDOW_DAYS,
                "since": since.isoformat(),
                **{key: counts.get(name, 0) for key, name in INSIGHT_EVENTS.items()},
            }
        )
