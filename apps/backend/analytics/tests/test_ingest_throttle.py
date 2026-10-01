from typing import Any

import pytest
from rest_framework.test import APIClient

from core.throttles import AnalyticsIngestThrottle


@pytest.mark.django_db
def test_analytics_ingest_is_throttled_per_ip(monkeypatch: Any) -> None:
    monkeypatch.setattr(AnalyticsIngestThrottle, "THROTTLE_RATES", {"analytics_ingest": "3/hour"})
    client = APIClient()
    body = {"name": "app_open", "properties": {"platform": "ANDROID"}}
    for _ in range(3):
        assert client.post("/api/v1/analytics/events/", body, format="json").status_code == 202
    response = client.post("/api/v1/analytics/events/", body, format="json")
    assert response.status_code == 429
    assert response.json()["code"] == "THROTTLED"
