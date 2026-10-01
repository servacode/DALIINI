from __future__ import annotations

from collections.abc import Callable

from django.http import HttpRequest, HttpResponse, JsonResponse

from .maintenance import get_maintenance_state, is_gated_path

MAINTENANCE = "MAINTENANCE"


class MaintenanceModeMiddleware:
    """Refuse gated API requests with 503 while `maintenance.enabled` is true."""

    def __init__(self, get_response: Callable[[HttpRequest], HttpResponse]) -> None:
        self.get_response = get_response

    def __call__(self, request: HttpRequest) -> HttpResponse:
        if request.method != "OPTIONS" and is_gated_path(request.path):
            state = get_maintenance_state()
            if state.enabled:
                response = JsonResponse(
                    {
                        "code": MAINTENANCE,
                        "message": state.message_ar,
                        "details": {"retryAfterSeconds": state.retry_after_seconds},
                        "requestId": getattr(request, "request_id", ""),
                    },
                    status=503,
                    json_dumps_params={"ensure_ascii": False},
                )
                response["Retry-After"] = str(state.retry_after_seconds)
                response["Cache-Control"] = "no-store"
                return response
        return self.get_response(request)
