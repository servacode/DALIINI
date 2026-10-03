"""How often each advertisement was seen and opened, from the apps' own analytics events.

The apps already report `ad_impression` and `ad_click` (analytics registry); this counts them
per advertisement over a period. Counts are of events, not of people: analytics is
pseudonymous by design and nothing here tries to tell readers apart.
"""

from __future__ import annotations

from datetime import datetime, timedelta
from typing import Any

from django.db.models import Count
from django.db.models.fields.json import KeyTextTransform
from django.utils import timezone
from django.utils.dateparse import parse_date
from drf_spectacular.utils import OpenApiParameter, extend_schema
from rest_framework import serializers
from rest_framework.exceptions import ValidationError
from rest_framework.response import Response

from accounts.authentication import AuthenticatedRequest
from analytics.models import ProductAnalyticsEvent
from business_hours.services import DAMASCUS
from content_services.models import Advertisement
from core.openapi import VALIDATION_400, protected

from .views import AdminView

DEFAULT_DAYS = 30
LONGEST = timedelta(days=366)


class AdStatSerializer(serializers.Serializer[Any]):
    adId = serializers.UUIDField()
    titleAr = serializers.CharField()
    impressions = serializers.IntegerField(min_value=0)
    clicks = serializers.IntegerField(min_value=0)
    clickRate = serializers.FloatField(
        allow_null=True, help_text="Clicks per impression, 0..1; null with no impressions."
    )


class AdStatsSerializer(serializers.Serializer[Any]):
    fromDate = serializers.DateField()
    toDate = serializers.DateField()
    items = AdStatSerializer(many=True)


def _day(value: str | None, name: str) -> Any:
    if not value:
        return None
    day = parse_date(value)
    if day is None:
        raise ValidationError({name: ["Expected YYYY-MM-DD."]})
    return day


class AdStatsView(AdminView):
    required_permission = "admin.ads.read"

    @extend_schema(
        operation_id="adminAdStatsRetrieve",
        tags=["Admin Ads"],
        summary="Impressions and clicks of each advertisement over a period",
        description=(
            "Counted from the apps' `ad_impression` and `ad_click` events, by Damascus day. "
            f"The default is the last {DEFAULT_DAYS} days; at most a year. Every advertisement "
            "is listed, those never shown with zeros."
        ),
        parameters=[
            OpenApiParameter("from", str, OpenApiParameter.QUERY, required=False),
            OpenApiParameter("to", str, OpenApiParameter.QUERY, required=False),
        ],
        responses={200: AdStatsSerializer, 400: VALIDATION_400, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        today = timezone.now().astimezone(DAMASCUS).date()
        last = _day(request.query_params.get("to"), "to") or today
        first = _day(request.query_params.get("from"), "from") or last - timedelta(
            days=DEFAULT_DAYS - 1
        )
        if last < first:
            raise ValidationError({"to": ["The end is before the start."]})
        if last - first > LONGEST:
            raise ValidationError({"from": ["At most a year at a time."]})
        since = timezone.make_aware(datetime.combine(first, datetime.min.time()), DAMASCUS)
        until = timezone.make_aware(
            datetime.combine(last + timedelta(days=1), datetime.min.time()), DAMASCUS
        )
        counts: dict[tuple[str, str], int] = {}
        rows = (
            ProductAnalyticsEvent.objects.filter(
                name__in=("ad_impression", "ad_click"),
                occurred_at__gte=since,
                occurred_at__lt=until,
            )
            .annotate(ad=KeyTextTransform("adId", "properties"))
            .values("name", "ad")
            .annotate(total=Count("id"))
        )
        for row in rows:
            counts[(str(row["ad"]), row["name"])] = row["total"]
        items = []
        for ad in Advertisement.objects.order_by("-created_at"):
            seen = counts.get((str(ad.pk), "ad_impression"), 0)
            opened = counts.get((str(ad.pk), "ad_click"), 0)
            items.append(
                {
                    "adId": str(ad.pk),
                    "titleAr": ad.title_ar,
                    "impressions": seen,
                    "clicks": opened,
                    "clickRate": round(opened / seen, 4) if seen else None,
                }
            )
        return Response({"fromDate": first.isoformat(), "toDate": last.isoformat(), "items": items})
