"""Day-by-day numbers for the console's charts (DECISION-075).

The analytics screen had totals for a period and arrows against the period before. A total
cannot show a bad Tuesday, a slow start or a weekend dip; a line can. This answers the same
period one Damascus day at a time, with every day present: a day nothing happened is a zero,
not a gap the chart has to guess about.
"""

from datetime import date, datetime, timedelta
from typing import Any
from zoneinfo import ZoneInfo

from django.conf import settings
from django.db.models import Count, QuerySet
from django.db.models.functions import TruncDate
from drf_spectacular.utils import extend_schema
from rest_framework.response import Response

from accounts.authentication import AuthenticatedRequest
from accounts.models import User
from analytics.models import ProductAnalyticsEvent
from core.openapi import VALIDATION_400, protected
from facilities.models import FacilityApplication, FacilityReport

from .schemas import AdminAnalyticsSeriesSerializer
from .views import KPI_EVENTS, AdminView, _filter, analytics_period

SERIES = (
    *KPI_EVENTS.keys(),
    "newUsers",
    "approvals",
    "reports",
)


def _local_days(start: datetime, end: datetime, zone: ZoneInfo) -> list[date]:
    first = start.astimezone(zone).date()
    last = (end - timedelta(microseconds=1)).astimezone(zone).date()
    return [first + timedelta(days=offset) for offset in range((last - first).days + 1)]


def _per_day(qs: QuerySet[Any], field: str, zone: ZoneInfo) -> dict[date, int]:
    rows = (
        qs.annotate(day=TruncDate(field, tzinfo=zone))
        .values("day")
        .annotate(count=Count("pk"))
        .values_list("day", "count")
    )
    return dict(rows)


def daily_series(start: datetime, end: datetime) -> list[dict[str, Any]]:
    zone = ZoneInfo(settings.TIME_ZONE)
    events = ProductAnalyticsEvent.objects.filter(occurred_at__gte=start, occurred_at__lt=end)
    named: dict[str, dict[date, int]] = {key: {} for key in KPI_EVENTS}
    by_name = {name: key for key, name in KPI_EVENTS.items()}
    for name, day, count in (
        events.filter(name__in=KPI_EVENTS.values())
        .annotate(day=TruncDate("occurred_at", tzinfo=zone))
        .values("name", "day")
        .annotate(count=Count("pk"))
        .values_list("name", "day", "count")
    ):
        named[by_name[name]][day] = count
    counts: dict[str, dict[date, int]] = {
        **named,
        "newUsers": _per_day(
            User.objects.filter(created_at__gte=start, created_at__lt=end), "created_at", zone
        ),
        "approvals": _per_day(
            FacilityApplication.objects.filter(
                status=FacilityApplication.Status.APPROVED,
                reviewed_at__gte=start,
                reviewed_at__lt=end,
            ),
            "reviewed_at",
            zone,
        ),
        "reports": _per_day(
            FacilityReport.objects.filter(created_at__gte=start, created_at__lt=end),
            "created_at",
            zone,
        ),
    }
    return [
        {"date": day.isoformat(), **{key: counts[key].get(day, 0) for key in SERIES}}
        for day in _local_days(start, end, zone)
    ]


class AnalyticsSeriesView(AdminView):
    required_permission = "admin.analytics.read"

    @extend_schema(
        operation_id="adminAnalyticsSeriesRetrieve",
        tags=["Admin Analytics"],
        summary="The period's numbers, one Damascus day at a time",
        description=(
            "Every day from `from` to `to` is present, a quiet day as zeros. The event series "
            "are the same four the period totals count; `newUsers` are accounts created, "
            "`approvals` applications approved and `reports` problem reports received."
        ),
        parameters=[
            _filter("from", "ISO date or datetime; default 30 days before `to`."),
            _filter("to", "ISO date or datetime; a bare date includes that whole day."),
        ],
        responses={200: AdminAnalyticsSeriesSerializer, 400: VALIDATION_400, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        start, end = analytics_period(request)
        return Response(
            {"from": start.isoformat(), "to": end.isoformat(), "days": daily_series(start, end)}
        )
