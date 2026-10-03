"""The charts' numbers: every Damascus day of the period, a quiet day as zeros (DECISION-075)."""

from datetime import datetime
from typing import Any
from zoneinfo import ZoneInfo

import pytest
from django.utils import timezone

from accounts.models import User
from analytics.models import ProductAnalyticsEvent
from facilities.models import Facility, FacilityReport

DAMASCUS = ZoneInfo("Asia/Damascus")
SERIES = "/api/v1/admin/analytics/series/"


def _at(day: int, hour: int) -> datetime:
    return datetime(2026, 9, day, hour, 0, tzinfo=DAMASCUS)


@pytest.mark.django_db
def test_every_day_is_present_and_counted_on_its_damascus_date(
    admin_api: Any, facility: Facility
) -> None:
    client = admin_api("admin.analytics.read")
    for when, name in (
        (_at(1, 10), "search_submitted"),
        (_at(1, 23), "search_submitted"),
        # 01:30 in Damascus is still the previous evening in UTC; it belongs to the 3rd here.
        (_at(3, 1), "search_submitted"),
        (_at(3, 12), "facility_view"),
        (_at(3, 12), "not_a_kpi"),
    ):
        ProductAnalyticsEvent.objects.create(name=name, properties={}, occurred_at=when)
    report = FacilityReport.objects.create(facility=facility, reason="WRONG_HOURS")
    FacilityReport.objects.filter(pk=report.pk).update(created_at=_at(2, 9))
    user = User.objects.create_user(phone="+963900000091", password="x" * 12, name="جديد")
    User.objects.filter(pk=user.pk).update(created_at=_at(3, 20))

    response = client.get(SERIES, {"from": "2026-09-01", "to": "2026-09-03"})

    assert response.status_code == 200, response.content
    days = response.json()["days"]
    assert [day["date"] for day in days] == ["2026-09-01", "2026-09-02", "2026-09-03"]
    assert [day["searches"] for day in days] == [2, 0, 1]
    assert [day["facilityViews"] for day in days] == [0, 0, 1]
    assert [day["reports"] for day in days] == [0, 1, 0]
    assert [day["newUsers"] for day in days] == [0, 0, 1]
    assert set(days[0]) == {
        "date",
        "searches",
        "zeroResultSearches",
        "facilityViews",
        "directionsRequests",
        "newUsers",
        "approvals",
        "reports",
    }


@pytest.mark.django_db
def test_the_default_period_is_the_last_thirty_days(admin_api: Any) -> None:
    client = admin_api("admin.analytics.read")
    days = client.get(SERIES).json()["days"]
    assert len(days) in (30, 31)  # Thirty days back from now, both ends' dates included.
    assert days[-1]["date"] == timezone.now().astimezone(DAMASCUS).date().isoformat()


@pytest.mark.django_db
def test_a_bad_period_is_refused_and_the_permission_is_needed(admin_api: Any) -> None:
    assert (
        admin_api("admin.analytics.read")
        .get(SERIES, {"from": "2026-09-05", "to": "2026-09-01"})
        .status_code
        == 400
    )
    assert admin_api("admin.dashboard.read").get(SERIES).status_code == 403
