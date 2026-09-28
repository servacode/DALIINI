"""Scoped abuse throttles. Rates live in REST_FRAMEWORK.DEFAULT_THROTTLE_RATES (env-overridable).

A signed-in caller is limited per account, an anonymous one per client IP. Write throttles
leave reads alone, so a view can list and write under one class.
"""

from __future__ import annotations

from typing import Any

from rest_framework.request import Request
from rest_framework.throttling import SimpleRateThrottle

SAFE_METHODS = ("GET", "HEAD", "OPTIONS")


class UserOrIpThrottle(SimpleRateThrottle):
    only_writes = False

    def get_cache_key(self, request: Request, view: Any) -> str | None:
        if self.only_writes and request.method in SAFE_METHODS:
            return None
        user = getattr(request, "user", None)
        if user is not None and getattr(user, "is_authenticated", False):
            ident = f"user:{user.pk}"
        else:
            ident = f"ip:{self.get_ident(request)}"
        return self.cache_format % {"scope": self.scope, "ident": ident}


class RatingsWriteThrottle(UserOrIpThrottle):
    scope = "ratings_write"
    only_writes = True


class FavoritesWriteThrottle(UserOrIpThrottle):
    scope = "favorites_write"
    only_writes = True


class PushTokenThrottle(UserOrIpThrottle):
    scope = "push_token"
    only_writes = True


class AnalyticsIngestThrottle(UserOrIpThrottle):
    scope = "analytics_ingest"


class SearchThrottle(UserOrIpThrottle):
    scope = "search"


class OwnerSubmitThrottle(UserOrIpThrottle):
    scope = "owner_submit"
    only_writes = True


class EvidenceUploadThrottle(UserOrIpThrottle):
    scope = "evidence_upload"
    only_writes = True


class FacilityReportThrottle(UserOrIpThrottle):
    scope = "facility_report"
    only_writes = True
