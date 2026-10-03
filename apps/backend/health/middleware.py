from __future__ import annotations

from collections.abc import Callable

from django.http import HttpRequest, HttpResponse

from .views import liveness, readiness

PROBES: dict[str, Callable[[HttpRequest], HttpResponse]] = {
    "/health/live/": liveness,
    "/health/ready/": readiness,
}


class HealthProbeMiddleware:
    """Answer the two health probes before anything checks the Host header or the scheme.

    A probe comes from inside the machine: Docker's HEALTHCHECK asks 127.0.0.1, a load
    balancer asks the container's own address, and neither speaks HTTPS to it. Behind the rest
    of the stack that is a 400 for an unknown host in production, or a redirect to HTTPS, and a
    healthy server is reported dead or a dead one healthy. The probes build nothing from the
    host and return no data, so answering them first gives nothing away.
    """

    def __init__(self, get_response: Callable[[HttpRequest], HttpResponse]) -> None:
        self.get_response = get_response

    def __call__(self, request: HttpRequest) -> HttpResponse:
        probe = PROBES.get(request.path)
        if probe is not None:
            return probe(request)
        return self.get_response(request)
