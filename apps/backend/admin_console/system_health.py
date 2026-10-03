"""What the console's system page checks, and what each answer means (DECISION-073).

Every dependency the platform needs is asked directly, with a short timeout, rather than
reported as "configured" because a setting exists:

* the database answers and has no migration waiting;
* Redis answers, and at least one Celery worker answers through it;
* the scheduler's heartbeat (`health.tasks.heartbeat`, every five minutes) is recent, which
  proves beat and a worker together;
* both storage buckets answer;
* the verification-code channel: the bot reports itself connected to WhatsApp, or the Cloud API
  is configured; and the codes sent in the last day, the codes used, and any failures since the
  last code that got through;
* push notifications: the provider, the devices that can receive, the last delivery and any
  failure after it;
* the hourly backup reported back within the last two hours (`scripts/db-backup.sh`,
  DECISION-081);
* the server's disk, which the database, each release's images and the logs share;
* error reporting, and maintenance mode.

A check answers with a status — `ok`, `warning` (working, but someone should look), `failed`
(not working) or `off` (not used by this deployment) — a sentence for the operator, and the
numbers behind it. Nothing returned is a host, a URL, a credential or an exception's text: a
failure is described by what it means, and the detail stays in the server log.
"""

import logging
import shutil
import time
import urllib.error
import urllib.request
from concurrent.futures import Future, ThreadPoolExecutor
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from typing import Any

from django.conf import settings
from django.db import connection
from django.db.migrations.executor import MigrationExecutor
from django.utils import timezone

from accounts.models import OTPChallenge
from health.beacons import BACKUP, OTP, PUSH, SCHEDULER
from health.models import ServiceSignal
from notifications.models import DevicePushToken, NotificationPushDelivery
from platform_settings.maintenance import get_maintenance_state

logger = logging.getLogger(__name__)

PROBE_TIMEOUT = 2.0
SCHEDULER_LATE = timedelta(minutes=15)
# Hourly dumps (DECISION-081): one missed run is a warning, a day of them a failure, because the
# promise is to lose at most an hour.
BACKUP_DUE = timedelta(hours=2)
BACKUP_LATE = timedelta(hours=26)
DAY = timedelta(hours=24)
# A full disk stops the database (DECISION-081): warn with room left to clean up, fail before it
# is too late to.
DISK_WARNING = 80
DISK_FAILED = 90
GIB = 1024**3
# Consecutive failed sends after which the channel reads as down rather than unsteady.
FAILING = 3

STATUS_ORDER = {"off": 0, "ok": 0, "warning": 1, "failed": 2}
DEVELOPMENT_ENVIRONMENTS = {"development", "test", "local", "e2e"}


@dataclass
class Check:
    key: str
    status: str
    summary: str
    latency_ms: int | None = None
    last_ok_at: datetime | None = None
    last_failure_at: datetime | None = None
    metrics: dict[str, int] = field(default_factory=dict)

    def payload(self) -> dict[str, Any]:
        return {
            "key": self.key,
            "status": self.status,
            "summary": self.summary,
            "latencyMs": self.latency_ms,
            "lastOkAt": self.last_ok_at.isoformat() if self.last_ok_at else None,
            "lastFailureAt": self.last_failure_at.isoformat() if self.last_failure_at else None,
            "metrics": [{"key": key, "value": value} for key, value in self.metrics.items()],
        }


def _ms(start: float) -> int:
    return round((time.perf_counter() - start) * 1000)


def _development() -> bool:
    return str(getattr(settings, "ENVIRONMENT_NAME", "")).lower() in DEVELOPMENT_ENVIRONMENTS


def _ago(moment: datetime, now: datetime) -> str:
    minutes = max(0, int((now - moment).total_seconds() // 60))
    if minutes < 1:
        return "الآن"
    if minutes < 60:
        return f"قبل {minutes} دقيقة"
    hours = minutes // 60
    if hours < 48:
        return f"قبل {hours} ساعة"
    return f"قبل {hours // 24} يوماً"


# --------------------------------------------------------------------------------------
# Probes that leave the process. Each runs in its own thread and touches no ORM.
# --------------------------------------------------------------------------------------


def probe_queue() -> tuple[Check, Check]:
    """Redis, then the workers reached through it. A worker cannot answer if Redis does not."""
    from redis import Redis

    url = str(getattr(settings, "REDIS_URL", "") or "")
    if not url:
        missing = Check("redis", "failed", "Redis غير مهيأ.")
        return missing, Check("worker", "failed", "لا طابور للمهام بلا Redis.")
    start = time.perf_counter()
    try:
        Redis.from_url(url, socket_connect_timeout=1, socket_timeout=1).ping()
    except Exception:
        logger.warning("health.redis_unreachable", exc_info=True)
        return (
            Check("redis", "failed", "Redis لا يستجيب."),
            Check("worker", "failed", "لا يمكن الوصول إلى طابور المهام."),
        )
    redis = Check("redis", "ok", "يستجيب.", latency_ms=_ms(start))

    from directory_backend.celery import app

    # No latency for the workers: a ping waits out its whole timeout collecting replies, so the
    # time it takes says nothing about them.
    try:
        replies = app.control.ping(timeout=0.7) or []
    except Exception:
        logger.warning("health.worker_ping_failed", exc_info=True)
        replies = []
    workers = len(replies)
    if workers == 0:
        return redis, Check(
            "worker", "failed", "لا عامل يستجيب: المهام والإشعارات لا تُنفَّذ.", metrics={"workers": 0}
        )
    return redis, Check("worker", "ok", "يستجيب.", metrics={"workers": workers})


def probe_storage() -> Check:
    from botocore.config import Config

    from storage.backends import PrivateS3Storage, PublicS3Storage

    config = Config(
        connect_timeout=PROBE_TIMEOUT, read_timeout=PROBE_TIMEOUT, retries={"max_attempts": 1}
    )
    start = time.perf_counter()
    unreachable: list[str] = []
    for label, backend in (("العامة", PublicS3Storage), ("الخاصة", PrivateS3Storage)):
        try:
            storage = backend(client_config=config)
            storage.connection.meta.client.head_bucket(Bucket=storage.bucket_name)
        except Exception:
            logger.warning("health.bucket_unreachable", extra={"bucket": label}, exc_info=True)
            unreachable.append(label)
    if unreachable:
        return Check("storage", "failed", f"لا تستجيب مساحة الملفات {' و'.join(unreachable)}.")
    return Check("storage", "ok", "مساحتا الملفات العامة والخاصة تستجيبان.", latency_ms=_ms(start))


def probe_bot() -> tuple[str, int | None]:
    """The WhatsApp bot's own view of its session: connected, logged_out, down or unreachable."""
    import json

    base = str(getattr(settings, "WHATSAPP_BOT_URL", "") or "").rstrip("/")
    if not base:
        return "unconfigured", None
    start = time.perf_counter()
    try:
        with urllib.request.urlopen(f"{base}/health", timeout=PROBE_TIMEOUT) as response:
            body = json.loads(response.read() or b"{}")
            return ("connected" if body.get("connected") else "down"), _ms(start)
    except urllib.error.HTTPError as error:
        try:
            body = json.loads(error.read() or b"{}")
        except ValueError:
            body = {}
        return ("logged_out" if body.get("loggedOut") else "down"), _ms(start)
    except Exception:
        logger.warning("health.bot_unreachable", exc_info=True)
        return "unreachable", None


# --------------------------------------------------------------------------------------
# Checks answered from this process and the database.
# --------------------------------------------------------------------------------------


def check_database() -> Check:
    start = time.perf_counter()
    try:
        with connection.cursor() as cursor:
            cursor.execute("SELECT 1")
            cursor.fetchone()
    except Exception:
        logger.warning("health.database_unreachable", exc_info=True)
        return Check("database", "failed", "قاعدة البيانات لا تستجيب.")
    latency = _ms(start)
    executor = MigrationExecutor(connection)
    pending = len(executor.migration_plan(executor.loader.graph.leaf_nodes()))
    if pending:
        return Check(
            "database",
            "warning",
            f"تستجيب، وفيها {pending} ترحيلاً لم يُطبَّق بعد.",
            latency_ms=latency,
            metrics={"pendingMigrations": pending},
        )
    return Check("database", "ok", "تستجيب، وكل الترحيلات مطبّقة.", latency_ms=latency)


def _signals() -> dict[str, ServiceSignal]:
    return {signal.name: signal for signal in ServiceSignal.objects.all()}


def check_scheduler(signal: ServiceSignal | None, now: datetime) -> Check:
    if signal is None or signal.ok_at is None:
        return Check(
            "scheduler", "warning", "لم يُسجَّل نبض بعد. إن استمر هذا فالمجدول (beat) لا يعمل."
        )
    if now - signal.ok_at > SCHEDULER_LATE:
        return Check(
            "scheduler",
            "failed",
            f"آخر نبض {_ago(signal.ok_at, now)}: المهام المجدولة متوقفة.",
            last_ok_at=signal.ok_at,
        )
    return Check("scheduler", "ok", f"آخر نبض {_ago(signal.ok_at, now)}.", last_ok_at=signal.ok_at)


def check_otp(signal: ServiceSignal | None, bot: tuple[str, int | None], now: datetime) -> Check:
    provider = str(getattr(settings, "OTP_PROVIDER", "")).lower()
    since = now - DAY
    metrics = {
        "sent24h": OTPChallenge.objects.filter(created_at__gte=since).count(),
        "verified24h": OTPChallenge.objects.filter(verified_at__gte=since).count(),
        "failures": signal.failures if signal else 0,
    }
    latency: int | None = None
    if provider in {"development", "test"}:
        status, summary = (
            "off" if _development() else "failed",
            "وضع التطوير: لا تصل الرموز إلى الهواتف.",
        )
    elif provider == "whatsapp_bot":
        state, latency = bot
        status, summary = {
            "connected": ("ok", "البوت متصل بواتساب."),
            "logged_out": ("failed", "البوت خرج من واتساب: يحتاج إعادة ربط الرقم."),
            "down": ("failed", "البوت يعمل لكنه غير متصل بواتساب."),
            "unreachable": ("failed", "تعذّر الوصول إلى البوت."),
            "unconfigured": ("failed", "عنوان البوت غير مهيأ."),
        }[state]
    elif provider == "whatsapp":
        configured = all(
            str(getattr(settings, name, "") or "").strip()
            for name in (
                "WHATSAPP_PHONE_NUMBER_ID",
                "WHATSAPP_ACCESS_TOKEN",
                "WHATSAPP_TEMPLATE_NAME",
            )
        )
        status, summary = (
            ("ok", "واجهة واتساب الرسمية مهيأة.")
            if configured
            else ("failed", "واجهة واتساب الرسمية ناقصة الإعداد.")
        )
    else:
        status, summary = "failed", "مزوّد الرموز غير معروف."

    failing_now = bool(
        signal
        and signal.failed_at
        and signal.failures
        and (signal.ok_at is None or signal.failed_at > signal.ok_at)
    )
    if failing_now and signal is not None:
        worst = "failed" if signal.failures >= FAILING else "warning"
        if STATUS_ORDER[worst] > STATUS_ORDER[status]:
            status = worst
        summary = f"{summary} آخر {signal.failures} محاولة إرسال فشلت."
    return Check(
        "otp",
        status,
        summary,
        latency_ms=latency,
        last_ok_at=signal.ok_at if signal else None,
        last_failure_at=signal.failed_at if signal else None,
        metrics=metrics,
    )


def check_push(signal: ServiceSignal | None, now: datetime) -> Check:
    provider = str(getattr(settings, "PUSH_PROVIDER", "")).lower()
    last = (
        NotificationPushDelivery.objects.order_by("-delivered_at")
        .values_list("delivered_at", flat=True)
        .first()
    )
    metrics = {
        "activeDevices": DevicePushToken.objects.filter(active=True).count(),
        "deliveries24h": NotificationPushDelivery.objects.filter(
            delivered_at__gte=now - DAY
        ).count(),
    }
    if provider == "development":
        status, summary = (
            "off" if _development() else "failed",
            "وضع التطوير: لا تصل الإشعارات إلى الأجهزة.",
        )
    elif provider == "fcm":
        configured = bool(
            str(getattr(settings, "FCM_PROJECT_ID", "") or "").strip()
            and str(getattr(settings, "FCM_SERVICE_ACCOUNT_JSON", "") or "").strip()
        )
        status, summary = (
            ("ok", "Firebase مهيأ.") if configured else ("failed", "Firebase غير مربوط بحساب خدمة.")
        )
    else:
        status, summary = "failed", "مزوّد الإشعارات غير معروف."

    if signal and signal.failed_at and (last is None or signal.failed_at > last):
        if signal.failure == "misconfigured":
            status, summary = "failed", "آخر محاولة رُفضت لخلل في الإعداد."
        elif STATUS_ORDER[status] < STATUS_ORDER["warning"]:
            status, summary = (
                "warning",
                f"{summary} تعثّر الإرسال {_ago(signal.failed_at, now)} وسيُعاد.",
            )
    return Check(
        "push",
        status,
        summary,
        last_ok_at=last,
        last_failure_at=signal.failed_at if signal else None,
        metrics=metrics,
    )


def check_backup(signal: ServiceSignal | None, now: datetime) -> Check:
    if signal is None or (signal.ok_at is None and signal.failed_at is None):
        if _development():
            return Check("backup", "off", "لا نسخ احتياطي في بيئة التطوير.")
        return Check("backup", "failed", "لم تُسجَّل أي نسخة احتياطية بعد.")
    if signal.failed_at and (signal.ok_at is None or signal.failed_at > signal.ok_at):
        return Check(
            "backup",
            "failed",
            f"آخر محاولة نسخ فشلت {_ago(signal.failed_at, now)}.",
            last_ok_at=signal.ok_at,
            last_failure_at=signal.failed_at,
        )
    assert signal.ok_at is not None
    age = now - signal.ok_at
    status = "ok" if age <= BACKUP_DUE else "warning" if age <= BACKUP_LATE else "failed"
    summary = f"آخر نسخة {_ago(signal.ok_at, now)}."
    if status != "ok":
        summary = f"{summary} النسخ كل ساعة متأخر."
    return Check(
        "backup", status, summary, last_ok_at=signal.ok_at, last_failure_at=signal.failed_at
    )


def check_disk(path: str = "/") -> Check:
    """How full the server's disk is. Inside the API's container `/` is the filesystem Docker
    keeps everything on, the database's volume included."""
    try:
        usage = shutil.disk_usage(path)
    except OSError:
        logger.exception("health.disk_unreadable")
        return Check("disk", "failed", "تعذّر الفحص.")
    percent = round(usage.used * 100 / usage.total) if usage.total else 0
    free = usage.free // GIB
    metrics = {"diskUsedPercent": percent, "diskFreeGb": free}
    if _development():
        return Check("disk", "off", "قرص جهاز التطوير لا يُراقب.", metrics=metrics)
    summary = f"مستخدم {percent}٪، ومتاح {free} غيغابايت."
    if percent >= DISK_FAILED:
        return Check(
            "disk", "failed", f"{summary} قاعدة البيانات تتوقف حين يمتلئ.", metrics=metrics
        )
    if percent >= DISK_WARNING:
        return Check(
            "disk",
            "warning",
            f"{summary} احذف صور الإصدارات القديمة أو وسّع القرص.",
            metrics=metrics,
        )
    return Check("disk", "ok", summary, metrics=metrics)


def check_errors() -> Check:
    if getattr(settings, "SENTRY_ENABLED", False):
        return Check("errors", "ok", "تتبع الأخطاء مفعّل.")
    if _development():
        return Check("errors", "off", "تتبع الأخطاء غير مفعّل في بيئة التطوير.")
    return Check(
        "errors", "warning", "تتبع الأخطاء (Sentry) غير مفعّل: لن نعرف بالأعطال إلا من الناس."
    )


def check_maintenance() -> Check:
    if get_maintenance_state().enabled:
        return Check("maintenance", "warning", "وضع الصيانة مفعّل: الواجهات العامة ترد بـ 503.")
    return Check("maintenance", "ok", "الواجهات العامة تعمل.")


def _settled(future: Future[Any], fallback: Any) -> Any:
    """A probe's answer, or `fallback` if it raised: one broken probe never breaks the page."""
    try:
        return future.result()
    except Exception:
        logger.exception("health.probe_crashed")
        return fallback


def run_checks() -> dict[str, Any]:
    """Every check, in the order the page shows them, and the worst status among them."""
    now = timezone.now()
    with ThreadPoolExecutor(max_workers=3, thread_name_prefix="health") as pool:
        queue = pool.submit(probe_queue)
        storage = pool.submit(probe_storage)
        bot = pool.submit(probe_bot)
        database = check_database()
        signals = _signals()
        scheduler = check_scheduler(signals.get(SCHEDULER), now)
        push = check_push(signals.get(PUSH), now)
        backup = check_backup(signals.get(BACKUP), now)
        redis, worker = _settled(
            queue,
            (
                Check("redis", "failed", "تعذّر الفحص."),
                Check("worker", "failed", "تعذّر الفحص."),
            ),
        )
        otp = check_otp(signals.get(OTP), _settled(bot, ("unreachable", None)), now)
        files = _settled(storage, Check("storage", "failed", "تعذّر الفحص."))
    checks = [
        database,
        redis,
        worker,
        scheduler,
        files,
        check_disk(),
        otp,
        push,
        backup,
        check_errors(),
        check_maintenance(),
    ]
    overall = max((check.status for check in checks), key=lambda status: STATUS_ORDER[status])
    return {
        "overall": "ok" if overall == "off" else overall,
        "checks": [check.payload() for check in checks],
    }
