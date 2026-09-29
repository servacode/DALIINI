from __future__ import annotations

from dataclasses import dataclass
from typing import Any


@dataclass(frozen=True)
class AnalyticsEventSpec:
    name: str
    allowed_fields: frozenset[str]


EVENT_REGISTRY = {
    spec.name: spec
    for spec in [
        AnalyticsEventSpec("app_open", frozenset({"platform", "appVersion"})),
        AnalyticsEventSpec("home_view", frozenset({"provinceId"})),
        AnalyticsEventSpec("province_selected", frozenset({"provinceId"})),
        AnalyticsEventSpec("location_permission_result", frozenset({"result", "precision"})),
        AnalyticsEventSpec("category_open", frozenset({"provinceId", "categoryId"})),
        AnalyticsEventSpec(
            "search_submitted", frozenset({"provinceId", "categoryId", "queryLength"})
        ),
        AnalyticsEventSpec(
            "search_zero_results", frozenset({"provinceId", "categoryId", "queryLength"})
        ),
        AnalyticsEventSpec(
            "facility_view", frozenset({"facilityId", "provinceId", "categoryId"})
        ),
        AnalyticsEventSpec("map_open", frozenset({"provinceId", "categoryId"})),
        AnalyticsEventSpec("marker_open", frozenset({"facilityId"})),
        AnalyticsEventSpec("phone_tap", frozenset({"facilityId"})),
        AnalyticsEventSpec("directions_start", frozenset({"facilityId", "routingProvider"})),
        AnalyticsEventSpec("rating_submit", frozenset({"facilityId", "stars"})),
        AnalyticsEventSpec("owner_draft_create", frozenset({"facilityId", "categoryId"})),
        AnalyticsEventSpec("owner_submit", frozenset({"facilityId", "applicationKind"})),
        AnalyticsEventSpec("application_status_view", frozenset({"applicationId", "status"})),
        AnalyticsEventSpec("ad_impression", frozenset({"adId", "provinceId"})),
        AnalyticsEventSpec("ad_click", frozenset({"adId", "actionType"})),
    ]
}

FORBIDDEN_FIELD_NAMES = frozenset(
    {
        "latitude",
        "longitude",
        "coordinates",
        "phone",
        "fullPhone",
        "otp",
        "password",
        "token",
        "accessToken",
        "refreshToken",
        "evidenceId",
        "storageKey",
    }
)


def validate_event(name: str, properties: dict[str, Any]) -> dict[str, Any]:
    spec = EVENT_REGISTRY.get(name)
    if spec is None:
        raise ValueError("Unknown analytics event")
    if not isinstance(properties, dict):
        raise ValueError("Analytics properties must be an object")
    keys = set(properties)
    forbidden = keys & FORBIDDEN_FIELD_NAMES
    if forbidden:
        raise ValueError(f"Forbidden analytics fields: {sorted(forbidden)}")
    extra = keys - set(spec.allowed_fields)
    if extra:
        raise ValueError(f"Undocumented analytics fields: {sorted(extra)}")
    return properties
