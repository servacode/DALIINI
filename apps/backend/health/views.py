from django.conf import settings
from django.db import connections
from django.http import JsonResponse
from django.views.decorators.http import require_safe
from redis import Redis


@require_safe
def liveness(request):  # type: ignore[no-untyped-def]
    return JsonResponse({"status": "ok"})


@require_safe
def readiness(request):  # type: ignore[no-untyped-def]
    checks: dict[str, str] = {}
    try:
        with connections["default"].cursor() as cursor:
            cursor.execute("SELECT 1")
            cursor.fetchone()
        checks["database"] = "ok"
    except Exception:
        checks["database"] = "failed"

    try:
        if Redis.from_url(settings.REDIS_URL, socket_connect_timeout=1).ping():
            checks["redis"] = "ok"
        else:
            checks["redis"] = "failed"
    except Exception:
        checks["redis"] = "failed"

    if any(value != "ok" for value in checks.values()):
        return JsonResponse({"status": "not_ready", "checks": checks}, status=503)
    return JsonResponse({"status": "ready", "checks": checks})
