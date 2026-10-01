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


class RejectedEvent(ValueError):
    """A rejection a caller may be told about, named rather than described.

    The reason is a code from REJECTIONS below, so what reaches a response is a sentence we
    wrote. The earlier messages interpolated the field names the client had just sent, which
    told the client nothing it did not know and made a response carry its own input back.
    """

    def __init__(self, code: str) -> None:
        super().__init__(code)
        self.code = code


REJECTIONS = {
    "unknown_event": "Unknown analytics event",
    "properties_not_object": "Analytics properties must be an object",
    "forbidden_fields": "The event carries a field this endpoint never accepts",
    "undocumented_fields": "The event carries a field the schema does not declare",
}


def validate_event(name: str, properties: dict[str, Any]) -> dict[str, Any]:
    spec = EVENT_REGISTRY.get(name)
    if spec is None:
        raise RejectedEvent("unknown_event")
    if not isinstance(properties, dict):
        raise RejectedEvent("properties_not_object")
    keys = set(properties)
    if keys & FORBIDDEN_FIELD_NAMES:
        raise RejectedEvent("forbidden_fields")
    if keys - set(spec.allowed_fields):
        raise RejectedEvent("undocumented_fields")
    return properties
