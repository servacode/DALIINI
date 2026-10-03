"""The system page asks each dependency directly and says what the answer means (DECISION-073)."""

import io
import json
import shutil
import urllib.error
from datetime import timedelta
from email.message import Message
from types import SimpleNamespace
from typing import Any

import pytest
from django.test import override_settings
from django.utils import timezone

from accounts.models import OTPChallenge
from admin_console import system_health
from admin_console.system_health import (
    Check,
    check_backup,
    check_disk,
    check_otp,
    check_push,
    check_scheduler,
    probe_bot,
    probe_map,
)
from health.beacons import BACKUP, OTP, PUSH, SCHEDULER, record_failure, record_ok
from health.models import ServiceSignal
from health.tasks import heartbeat

NOW = timezone.now()


def _signal(name: str, **fields: Any) -> ServiceSignal:
    return ServiceSignal.objects.create(name=name, **fields)


@pytest.mark.django_db
def test_recording_counts_failures_until_the_next_success() -> None:
    record_failure(OTP, "unavailable")
    record_failure(OTP, "unavailable")
    signal = ServiceSignal.objects.get(name=OTP)
    assert (signal.failures, signal.failure, signal.ok_at) == (2, "unavailable", None)

    record_ok(OTP)
    signal.refresh_from_db()
    assert signal.failures == 0
    assert signal.ok_at is not None
    assert signal.failed_at is not None  # The last failure stays on record.


@pytest.mark.django_db
def test_the_heartbeat_task_marks_the_scheduler_alive() -> None:
    heartbeat()
    assert ServiceSignal.objects.get(name=SCHEDULER).ok_at is not None


@pytest.mark.django_db
def test_the_scheduler_reads_from_its_heartbeat() -> None:
    assert check_scheduler(None, NOW).status == "warning"
    fresh = _signal(SCHEDULER, ok_at=NOW - timedelta(minutes=4))
    assert check_scheduler(fresh, NOW).status == "ok"
    fresh.ok_at = NOW - timedelta(hours=2)
    stale = check_scheduler(fresh, NOW)
    assert stale.status == "failed"
    assert "متوقفة" in stale.summary


@pytest.mark.django_db
@override_settings(ENVIRONMENT_NAME="production")
def test_a_backup_is_due_hourly_and_a_failed_one_is_said_so() -> None:
    assert check_backup(None, NOW).status == "failed"
    signal = _signal(BACKUP, ok_at=NOW - timedelta(minutes=50))
    assert check_backup(signal, NOW).status == "ok"
    signal.ok_at = NOW - timedelta(hours=3)
    late = check_backup(signal, NOW)
    assert late.status == "warning"
    assert "متأخر" in late.summary
    signal.ok_at = NOW - timedelta(hours=30)
    assert check_backup(signal, NOW).status == "failed"
    signal.ok_at = NOW - timedelta(minutes=50)
    signal.failed_at = NOW - timedelta(minutes=10)
    failed = check_backup(signal, NOW)
    assert failed.status == "failed"
    assert "فشلت" in failed.summary


@pytest.mark.django_db
@override_settings(ENVIRONMENT_NAME="development")
def test_no_backup_on_a_development_stack_is_not_an_alarm() -> None:
    assert check_backup(None, NOW).status == "off"


@pytest.mark.django_db
@override_settings(OTP_PROVIDER="whatsapp_bot", ENVIRONMENT_NAME="production")
def test_the_code_channel_reads_the_bot_and_the_failures_since_the_last_code() -> None:
    OTPChallenge.objects.create(
        phone="+963900000071", purpose="REGISTER", expires_at=NOW, verified_at=NOW
    )
    OTPChallenge.objects.create(phone="+963900000072", purpose="REGISTER", expires_at=NOW)

    connected = check_otp(None, ("connected", 40), NOW)
    assert connected.status == "ok"
    assert connected.latency_ms == 40
    assert connected.metrics == {"sent24h": 2, "verified24h": 1, "failures": 0}

    assert check_otp(None, ("logged_out", 40), NOW).status == "failed"
    assert "إعادة ربط" in check_otp(None, ("logged_out", 40), NOW).summary
    assert check_otp(None, ("unreachable", None), NOW).status == "failed"

    signal = _signal(OTP, ok_at=NOW - timedelta(hours=1), failed_at=NOW, failures=1)
    assert check_otp(signal, ("connected", 40), NOW).status == "warning"
    signal.failures = 3
    assert check_otp(signal, ("connected", 40), NOW).status == "failed"
    signal.ok_at = NOW + timedelta(seconds=1)  # A code got through after the failures.
    signal.failures = 0
    assert check_otp(signal, ("connected", 40), NOW).status == "ok"


@pytest.mark.django_db
@override_settings(OTP_PROVIDER="development", ENVIRONMENT_NAME="production")
def test_development_codes_in_production_are_a_failure() -> None:
    assert check_otp(None, ("unconfigured", None), NOW).status == "failed"


@pytest.mark.django_db
@override_settings(PUSH_PROVIDER="fcm", FCM_PROJECT_ID="p", FCM_SERVICE_ACCOUNT_JSON="{}")
def test_push_reads_its_provider_and_a_failure_after_the_last_delivery() -> None:
    assert check_push(None, NOW).status == "ok"
    signal = _signal(PUSH, failed_at=NOW, failure="transient", failures=1)
    assert check_push(signal, NOW).status == "warning"
    signal.failure = "misconfigured"
    assert check_push(signal, NOW).status == "failed"
    with override_settings(FCM_SERVICE_ACCOUNT_JSON=""):
        assert check_push(None, NOW).status == "failed"


@pytest.mark.django_db
def test_a_failed_code_send_is_recorded(monkeypatch: pytest.MonkeyPatch) -> None:
    from rest_framework.test import APIClient

    from accounts.providers.base import TransientOtpError
    from locations.models import Province

    province = Province.objects.create(code="health-p", name_ar="محافظة", active=True)

    def fail(*, phone: str, code: str) -> None:
        raise TransientOtpError("bot disconnected")

    monkeypatch.setattr("accounts.services.deliver_otp", fail)
    APIClient().post(
        "/api/v1/auth/register/start/",
        {"phone": "+963900000073", "provinceId": str(province.pk)},
        format="json",
    )
    signal = ServiceSignal.objects.get(name=OTP)
    assert (signal.failures, signal.failure) == (1, "unavailable")

    monkeypatch.setattr("accounts.services.deliver_otp", lambda *, phone, code: None)
    APIClient().post(
        "/api/v1/auth/register/start/",
        {"phone": "+963900000074", "provinceId": str(province.pk)},
        format="json",
    )
    signal.refresh_from_db()
    assert signal.failures == 0
    assert signal.ok_at is not None


class _Response(io.BytesIO):
    def __enter__(self) -> "_Response":
        return self

    def __exit__(self, *args: object) -> None:
        return None


@override_settings(WHATSAPP_BOT_URL="http://bot.internal:3100")
def test_the_bot_probe_tells_a_lost_session_from_an_unreachable_bot(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    def answer(body: dict[str, Any]) -> Any:
        return lambda url, timeout: _Response(json.dumps(body).encode())

    def refuse(body: dict[str, Any]) -> Any:
        def open_(url: str, timeout: float) -> Any:
            raise urllib.error.HTTPError(
                url, 503, "down", Message(), io.BytesIO(json.dumps(body).encode())
            )

        return open_

    def unreachable(url: str, timeout: float) -> Any:
        raise OSError("connection refused")

    monkeypatch.setattr("urllib.request.urlopen", answer({"connected": True}))
    assert probe_bot()[0] == "connected"
    monkeypatch.setattr("urllib.request.urlopen", refuse({"connected": False, "loggedOut": True}))
    assert probe_bot()[0] == "logged_out"
    monkeypatch.setattr("urllib.request.urlopen", refuse({"connected": False}))
    assert probe_bot()[0] == "down"
    monkeypatch.setattr("urllib.request.urlopen", unreachable)
    assert probe_bot() == ("unreachable", None)
    with override_settings(WHATSAPP_BOT_URL=""):
        assert probe_bot()[0] == "unconfigured"


@pytest.mark.django_db
@override_settings(
    REDIS_URL="redis://:hunter2@cache.internal:6379/0", ENVIRONMENT_NAME="production"
)
def test_the_endpoint_reports_every_check_and_the_worst_status_without_secrets(
    admin_api: Any, monkeypatch: pytest.MonkeyPatch
) -> None:
    monkeypatch.setattr(
        system_health,
        "probe_queue",
        lambda: (Check("redis", "ok", "يستجيب.", 2), Check("worker", "failed", "لا عامل.")),
    )
    monkeypatch.setattr(system_health, "probe_storage", lambda: Check("storage", "ok", "تستجيب."))
    monkeypatch.setattr(system_health, "probe_bot", lambda: ("unconfigured", None))

    response = admin_api("admin.system.read").get("/api/v1/admin/system/status/")

    assert response.status_code == 200, response.content
    body = response.json()
    assert [check["key"] for check in body["checks"]] == [
        "database",
        "redis",
        "worker",
        "scheduler",
        "storage",
        "disk",
        "map",
        "otp",
        "push",
        "backup",
        "errors",
        "maintenance",
    ]
    assert body["overall"] == "failed"
    database = body["checks"][0]
    assert database["status"] == "ok", database
    assert isinstance(database["latencyMs"], int)
    assert "hunter2" not in response.content.decode()
    assert "cache.internal" not in response.content.decode()


@pytest.mark.django_db
def test_the_endpoint_needs_its_permission(admin_api: Any) -> None:
    response = admin_api("admin.dashboard.read").get("/api/v1/admin/system/status/")
    assert response.status_code == 403


@pytest.mark.parametrize(
    ("used", "status", "words"),
    [(50, "ok", "مستخدم 50٪"), (85, "warning", "احذف صور الإصدارات"), (95, "failed", "تتوقف")],
)
@override_settings(ENVIRONMENT_NAME="production")
def test_the_disk_warns_with_room_left_and_fails_before_it_is_full(
    used: int, status: str, words: str, monkeypatch: pytest.MonkeyPatch
) -> None:
    gib = 1024**3
    usage = SimpleNamespace(total=100 * gib, used=used * gib, free=(100 - used) * gib)
    monkeypatch.setattr(shutil, "disk_usage", lambda path: usage)

    check = check_disk()

    assert check.status == status
    assert words in check.summary
    assert check.metrics == {"diskUsedPercent": used, "diskFreeGb": 100 - used}


@override_settings(ENVIRONMENT_NAME="development")
def test_a_development_disk_is_reported_but_not_judged() -> None:
    check = check_disk()
    assert check.status == "off"
    assert set(check.metrics) == {"diskUsedPercent", "diskFreeGb"}


def test_an_unreadable_disk_is_said_so(monkeypatch: pytest.MonkeyPatch) -> None:
    def unreadable(path: str) -> None:
        raise OSError("no")

    monkeypatch.setattr(shutil, "disk_usage", unreadable)
    assert check_disk().status == "failed"


@override_settings(
    MAP_TILES_INTERNAL_URL="http://martin:3000", ROUTING_INTERNAL_URL="http://valhalla:8002"
)
def test_the_map_is_drawn_routed_and_rebuilt_monthly(monkeypatch: pytest.MonkeyPatch) -> None:
    def serve(catalog: dict[str, Any] | None, status: dict[str, Any] | None) -> Any:
        def open_(url: str, timeout: float) -> Any:
            body = catalog if url.endswith("/catalog") else status
            if body is None:
                raise OSError("connection refused")
            return _Response(json.dumps(body).encode())

        return open_

    syria: dict[str, Any] = {"tiles": {"syria": {}}}
    fresh = {"tileset_last_modified": int((NOW - timedelta(days=3)).timestamp())}
    stale = {"tileset_last_modified": int((NOW - timedelta(days=60)).timestamp())}

    monkeypatch.setattr("urllib.request.urlopen", serve(syria, fresh))
    check = probe_map()
    assert (check.status, check.metrics) == ("ok", {"mapAgeDays": 3}), check
    monkeypatch.setattr("urllib.request.urlopen", serve(syria, stale))
    assert probe_map().status == "warning"
    assert "الشهري" in probe_map().summary
    monkeypatch.setattr("urllib.request.urlopen", serve(syria, {"tileset_last_modified": 0}))
    assert probe_map().status == "warning"
    monkeypatch.setattr("urllib.request.urlopen", serve(syria, None))
    assert "التوجيه متوقف" in probe_map().summary
    monkeypatch.setattr("urllib.request.urlopen", serve({"tiles": {}}, fresh))
    assert probe_map().status == "failed"
    monkeypatch.setattr("urllib.request.urlopen", serve(None, fresh))
    assert probe_map().status == "failed"
    with override_settings(MAP_TILES_INTERNAL_URL="", ROUTING_INTERNAL_URL=""):
        assert probe_map().status == "off"
