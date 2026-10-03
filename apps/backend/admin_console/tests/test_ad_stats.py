"""Impressions and clicks per advertisement, from the apps' own analytics events."""

from datetime import timedelta
from typing import Any

import pytest
from django.utils import timezone

from analytics.models import ProductAnalyticsEvent
from content_services.models import Advertisement

STATS = "/api/v1/admin/ads/stats/"


def _event(name: str, ad: Advertisement, days_ago: int = 0) -> None:
    ProductAnalyticsEvent.objects.create(
        name=name,
        properties={"adId": str(ad.pk)},
        occurred_at=timezone.now() - timedelta(days=days_ago),
    )


@pytest.mark.django_db
def test_each_ad_is_counted_within_the_period(admin_api: Any) -> None:
    shown = Advertisement.objects.create(image_key="ads/a.jpg", title_ar="عرض الشتاء")
    never = Advertisement.objects.create(image_key="ads/b.jpg", title_ar="لم يظهر")
    for _ in range(4):
        _event("ad_impression", shown)
    _event("ad_click", shown)
    _event("ad_impression", shown, days_ago=60)

    body = admin_api("admin.ads.read").get(STATS).json()
    rows = {row["adId"]: row for row in body["items"]}

    assert rows[str(shown.pk)]["impressions"] == 4
    assert rows[str(shown.pk)]["clicks"] == 1
    assert rows[str(shown.pk)]["clickRate"] == 0.25
    assert rows[str(never.pk)] == {
        "adId": str(never.pk),
        "titleAr": "لم يظهر",
        "impressions": 0,
        "clicks": 0,
        "clickRate": None,
    }


@pytest.mark.django_db
def test_the_period_is_checked(admin_api: Any) -> None:
    client = admin_api("admin.ads.read")

    assert client.get(STATS, {"from": "2026-10-10", "to": "2026-10-01"}).status_code == 400
    assert client.get(STATS, {"from": "2024-01-01", "to": "2026-01-01"}).status_code == 400
    assert client.get(STATS, {"from": "not-a-day"}).status_code == 400
