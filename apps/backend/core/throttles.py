"""Scoped abuse throttles. Rates live in REST_FRAMEWORK.DEFAULT_THROTTLE_RATES (env-overridable).

A signed-in caller is limited per account, an anonymous one per client IP. Write throttles
leave reads alone, so a view can list and write under one class.

Every route has a limit. A view that names its own `throttle_classes` keeps exactly those;
every other view gets the defaults in settings: `AnonDefaultThrottle`, `UserDefaultThrottle`
and `WebServerThrottle`.
"""

from __future__ import annotations

import hmac
from typing import Any

from django.conf import settings
from rest_framework.request import Request
from rest_framework.throttling import SimpleRateThrottle

SAFE_METHODS = ("GET", "HEAD", "OPTIONS")
WEB_KEY_HEADER = "HTTP_X_DALIINI_WEB_KEY"


def is_trusted_web_server(request: Request) -> bool:
    """The request comes from the platform's own website server (`X-Daliini-Web-Key`).

    The public website renders on its server, so every visitor reaches the API from the same
    address, and a per-address limit meant for one person would throttle the whole site. A
    request carrying the shared key is counted under the separate `web_server` scope instead.
    It grants nothing else: no data, permission or identity comes with the key. Compared in
    constant time; with `WEB_SERVER_API_KEY` unset no request is ever trusted.
    """
    expected = str(getattr(settings, "WEB_SERVER_API_KEY", "") or "")
    supplied = str(request.META.get(WEB_KEY_HEADER, "") or "")
    if not expected or not supplied:
        return False
    return hmac.compare_digest(supplied.encode(), expected.encode())


class UserOrIpThrottle(SimpleRateThrottle):
    only_writes = False

    def get_ident(self, request: Request) -> str:
        """The caller's address, which a client cannot choose.

        DRF trusts X-Forwarded-For whenever NUM_PROXIES is unset, so a client could send a
        new forged address with every request and never be limited. Here the header is
        honoured only when NUM_PROXIES says how many proxies to trust; otherwise the
        connection's own address is the identity.
        """
        from rest_framework.settings import api_settings

        if api_settings.NUM_PROXIES is None:
            return str(request.META.get("REMOTE_ADDR") or "")
        return str(super().get_ident(request))

    def get_cache_key(self, request: Request, view: Any) -> str | None:
        if self.only_writes and request.method in SAFE_METHODS:
            return None
        user = getattr(request, "user", None)
        if user is not None and getattr(user, "is_authenticated", False):
            ident = f"user:{user.pk}"
        else:
            if request.method in SAFE_METHODS and is_trusted_web_server(request):
                # Anonymous reads from the website server: `WebServerThrottle` counts these.
                return None
            ident = f"ip:{self.get_ident(request)}"
        return self.cache_format % {"scope": self.scope, "ident": ident}


class WebServerThrottle(SimpleRateThrottle):
    """The website server's own, much higher limit on anonymous public reads.

    Applies only to requests carrying a valid `X-Daliini-Web-Key`; every other request is
    left to the per-account or per-address throttles beside it. Writes are never moved
    here: a write from the site is still limited per address.
    """

    scope = "web_server"

    def get_cache_key(self, request: Request, view: Any) -> str | None:
        if request.method not in SAFE_METHODS or not is_trusted_web_server(request):
            return None
        user = getattr(request, "user", None)
        if user is not None and getattr(user, "is_authenticated", False):
            return None
        return self.cache_format % {"scope": self.scope, "ident": "site"}


class AnonDefaultThrottle(UserOrIpThrottle):
    """The backstop for anonymous callers on a route with no limit of its own.

    Generous on purpose: Syrian mobile carriers put many subscribers behind one address,
    and this is there to stop a flood, not to ration a person. The website server's reads
    are counted by `WebServerThrottle` instead.
    """

    scope = "anon_default"

    def get_cache_key(self, request: Request, view: Any) -> str | None:
        user = getattr(request, "user", None)
        if user is not None and getattr(user, "is_authenticated", False):
            return None
        return super().get_cache_key(request, view)


class UserDefaultThrottle(UserOrIpThrottle):
    """The backstop for a signed-in account on a route with no limit of its own."""

    scope = "user_default"

    def get_cache_key(self, request: Request, view: Any) -> str | None:
        user = getattr(request, "user", None)
        if user is None or not getattr(user, "is_authenticated", False):
            return None
        return super().get_cache_key(request, view)


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


class ContactThrottle(UserOrIpThrottle):
    """The public contact form: strict, and like every throttle here not escapable by
    forging X-Forwarded-For (see `UserOrIpThrottle.get_ident`)."""

    scope = "contact"
    only_writes = True
