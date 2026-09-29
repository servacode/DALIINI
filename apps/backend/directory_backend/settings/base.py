from pathlib import Path

import dj_database_url
from celery.schedules import crontab  # type: ignore[import-untyped]

from .env import env, env_bool, env_csv

BASE_DIR = Path(__file__).resolve().parents[2]

SECRET_KEY = env("SECRET_KEY", "development-only-not-for-production")
ACCESS_TOKEN_SIGNING_KEY = env("ACCESS_TOKEN_SIGNING_KEY", "development-access-token-key")
REFRESH_HMAC_SECRET = env("REFRESH_HMAC_SECRET", "development-refresh-hmac-key")
RECOVERY_HMAC_SECRET = env("RECOVERY_HMAC_SECRET", "development-recovery-hmac-key")
OTP_PROVIDER = env("OTP_PROVIDER", "development")
DEBUG = False
ALLOWED_HOSTS = env_csv("ALLOWED_HOSTS", ["localhost", "127.0.0.1"])

INSTALLED_APPS = [
    "daphne",
    "django.contrib.auth",
    "django.contrib.contenttypes",
    "django.contrib.staticfiles",
    "django.contrib.gis",
    "corsheaders",
    "rest_framework",
    "django_filters",
    "drf_spectacular",
    "channels",
    "storages",
    "core",
    "health",
    "storage",
    "accounts",
    "sessions.apps.SessionsConfig",
    "audit",
    "locations",
    "directory",
    "facilities",
    "business_hours",
    "pharmacy_duty",
    "ratings",
    "favorites",
    "search",
    "realtime",
    "content_services",
    "notifications",
    "analytics",
    "platform_settings",
    "admin_console",
]

MIDDLEWARE = [
    "core.middleware.RequestIdMiddleware",
    "corsheaders.middleware.CorsMiddleware",
    "platform_settings.middleware.MaintenanceModeMiddleware",
    "django.middleware.security.SecurityMiddleware",
    "django.middleware.common.CommonMiddleware",
    "django.middleware.csrf.CsrfViewMiddleware",
]

ROOT_URLCONF = "directory_backend.urls"
WSGI_APPLICATION = "directory_backend.wsgi.application"
ASGI_APPLICATION = "directory_backend.asgi.application"

DATABASES = {
    "default": dj_database_url.config(
        default="postgresql://directory:directory@localhost:5432/directory",
        conn_max_age=60,
        conn_health_checks=True,
        engine="django.contrib.gis.db.backends.postgis",
    )
}

LANGUAGE_CODE = "ar"
TIME_ZONE = "Asia/Damascus"
USE_I18N = True
USE_TZ = True

STATIC_URL = "/static/"
DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"
AUTH_USER_MODEL = "accounts.User"

AUTH_PASSWORD_VALIDATORS = [
    {"NAME": "django.contrib.auth.password_validation.UserAttributeSimilarityValidator"},
    {"NAME": "django.contrib.auth.password_validation.MinimumLengthValidator"},
    {"NAME": "django.contrib.auth.password_validation.CommonPasswordValidator"},
    {"NAME": "django.contrib.auth.password_validation.NumericPasswordValidator"},
]
PASSWORD_HASHERS = [
    "django.contrib.auth.hashers.Argon2PasswordHasher",
    "django.contrib.auth.hashers.PBKDF2PasswordHasher",
]

REST_FRAMEWORK = {
    "DEFAULT_SCHEMA_CLASS": "drf_spectacular.openapi.AutoSchema",
    "EXCEPTION_HANDLER": "core.exceptions.exception_handler",
    "DEFAULT_FILTER_BACKENDS": ["django_filters.rest_framework.DjangoFilterBackend"],
    "DEFAULT_RENDERER_CLASSES": ["rest_framework.renderers.JSONRenderer"],
    "DEFAULT_AUTHENTICATION_CLASSES": [
        "accounts.authentication.BearerAccessTokenAuthentication",
    ],
    "DEFAULT_THROTTLE_RATES": {
        "otp_start": "5/hour",
        "otp_verify": "10/hour",
        "login": "10/minute",
        "recovery": "5/hour",
        # Abuse protection for public and account writes; override per environment.
        "ratings_write": env("THROTTLE_RATINGS_WRITE", "30/hour"),
        "favorites_write": env("THROTTLE_FAVORITES_WRITE", "60/hour"),
        "push_token": env("THROTTLE_PUSH_TOKEN", "20/hour"),
        "analytics_ingest": env("THROTTLE_ANALYTICS_INGEST", "600/hour"),
        "search": env("THROTTLE_SEARCH", "120/minute"),
        "owner_submit": env("THROTTLE_OWNER_SUBMIT", "10/hour"),
        "evidence_upload": env("THROTTLE_EVIDENCE_UPLOAD", "30/hour"),
        "facility_report": env("THROTTLE_FACILITY_REPORT", "5/hour"),
        "contact": env("THROTTLE_CONTACT", "3/hour"),
        # Anonymous public reads from the website server (see WEB_SERVER_API_KEY).
        "web_server": env("THROTTLE_WEB_SERVER", "3000/minute"),
    },
    # How many reverse proxies sit in front of the app. Unset (the default), `ContactThrottle`
    # identifies an anonymous caller by REMOTE_ADDR alone and ignores X-Forwarded-For, which a
    # client could otherwise forge to escape the limit. Set it to the real proxy count (for
    # example 1 behind one load balancer) to take the client address from X-Forwarded-For.
    "NUM_PROXIES": int(env("DRF_NUM_PROXIES")) if env("DRF_NUM_PROXIES") else None,
}
# Interactive schema exposure. Safe default; development widens it and production
# disables it. CI never needs the route: it uses the spectacular management command.
OPENAPI_SCHEMA_EXPOSURE = "privileged"

SPECTACULAR_SETTINGS = {
    "TITLE": "Directory Platform API",
    "VERSION": "1.0.0",
    "DESCRIPTION": (
        "Canonical contract for the Serva Code Directory Platform. Generated from Django "
        "and DRF; it is never hand-authored. Generated TypeScript, Kotlin and Swift "
        "clients are produced from this document, so no client hand-writes transport DTOs."
    ),
    "SERVE_INCLUDE_SCHEMA": False,
    "SCHEMA_PATH_PREFIX": "/api/v1",
    "SORT_OPERATIONS": True,
    # Several domains legitimately declare a field called "status" or "type", and a few
    # choice sets are reused under different field names. Without explicit names
    # drf-spectacular invents hash-suffixed component names such as "Status652Enum",
    # which are unstable across runs and leak into every generated client.
    "ENUM_NAME_OVERRIDES": {
        "FacilityStatusEnum": "core.enums.FACILITY_STATUS",
        "FacilityApplicationStatusEnum": "core.enums.FACILITY_APPLICATION_STATUS",
        "FacilityApplicationKindEnum": "core.enums.FACILITY_APPLICATION_KIND",
        "FacilityMemberRoleEnum": "core.enums.FACILITY_MEMBER_ROLE",
        "AccountDeletionStatusEnum": "core.enums.ACCOUNT_DELETION_STATUS",
        "CategorySpecializationEnum": "core.enums.CATEGORY_SPECIALIZATION",
        "AdvertisementActionTypeEnum": "core.enums.ADVERTISEMENT_ACTION_TYPE",
        "AdvertisementTargetScopeEnum": "core.enums.ADVERTISEMENT_TARGET_SCOPE",
        "AvailabilityStateEnum": "core.enums.AVAILABILITY_STATE",
        "DependencyConfiguredEnum": "core.enums.DEPENDENCY_CONFIGURED",
        "DatabaseHealthEnum": "core.enums.DATABASE_HEALTH",
        "OwnerRequiredActionEnum": "core.enums.OWNER_REQUIRED_ACTION",
        "PushPlatformEnum": "notifications.models.DevicePushToken.Platform",
        "FacilityReportReasonEnum": "facilities.models.FacilityReport.Reason",
        "FacilityReportStatusEnum": "facilities.models.FacilityReport.Status",
        "DuplicateReasonEnum": "admin_console.review.DUPLICATE_REASON_CHOICES",
        "FacilityQualityIssueEnum": "admin_console.quality.QUALITY_ISSUE_CHOICES",
        "AdminAlertKindEnum": "admin_console.insights.ALERT_KINDS",
        "AdminAlertSeverityEnum": "admin_console.insights.ALERT_SEVERITIES",
        "AdminLinkEntityTypeEnum": "admin_console.insights.LINK_ENTITY_TYPES",
        "AdminReadinessCodeEnum": "admin_console.insights.READINESS_CODES",
        "AdminTimelineEventKindEnum": "admin_console.schemas_smart.TIMELINE_KINDS",
        "AdminSearchHitTypeEnum": "admin_console.schemas_smart.SEARCH_HIT_TYPES",
        "AdminReportBulkActionEnum": "admin_console.schemas_smart.BULK_REPORT_ACTIONS",
        "AdminReportBulkOutcomeEnum": "admin_console.schemas_smart.BULK_OUTCOMES",
        "DutyShiftStatusEnum": "admin_console.schemas_smart.SHIFT_STATUSES",
        "DutyShiftSourceEnum": "pharmacy_duty.models.DutyShift.Source",
        "BroadcastAudienceEnum": "notifications.models.Broadcast.Audience",
        "ContentPageKindEnum": "content_services.models.LegalDocument.Kind",
        "EmergencyNumberKindEnum": "content_services.models.EmergencyNumber.Kind",
        "EmergencyNumberScopeEnum": "content_services.content_schemas.EMERGENCY_SCOPES",
        "ContactMessageKindEnum": "content_services.models.ContactMessage.Kind",
    },
    # A nullable choice field is otherwise described as `oneOf: [<Enum>, NullEnum]`, where
    # NullEnum is an enum whose only value is null. The Kotlin generator renders that as an
    # enum class with no entries, which does not compile (INT-052). Marking the field
    # nullable says the same thing in a form every generator understands.
    "ENUM_ADD_EXPLICIT_BLANK_NULL_CHOICE": False,
}

CORS_ALLOWED_ORIGINS = env_csv("CORS_ALLOWED_ORIGINS")
CSRF_TRUSTED_ORIGINS = env_csv("CSRF_TRUSTED_ORIGINS")

REDIS_URL = env("REDIS_URL", "redis://localhost:6379/0")
CHANNEL_LAYERS = {
    "default": {
        "BACKEND": "channels_redis.core.RedisChannelLayer",
        "CONFIG": {"hosts": [REDIS_URL]},
    }
}
CELERY_BROKER_URL = REDIS_URL
CELERY_RESULT_BACKEND = REDIS_URL
CELERY_TASK_SERIALIZER = "json"
CELERY_ACCEPT_CONTENT = ["json"]
CELERY_RESULT_SERIALIZER = "json"
CELERY_TIMEZONE = TIME_ZONE
CELERY_TASK_ACKS_LATE = True
CELERY_WORKER_PREFETCH_MULTIPLIER = 1
# Periodic maintenance, run by `celery -A directory_backend beat`. Times are Damascus
# local (CELERY_TIMEZONE) and sit in the quiet night hours.
CELERY_BEAT_SCHEDULE = {
    "analytics-retention-purge": {
        "task": "analytics.tasks.purge_analytics_retention",
        "schedule": crontab(hour=3, minute=17),
    },
    "sessions-purge-ended": {
        "task": "sessions.tasks.purge_ended_sessions",
        "schedule": crontab(hour=3, minute=37),
    },
    "otp-purge-expired": {
        "task": "accounts.tasks.purge_expired_otp_challenges",
        "schedule": crontab(hour=3, minute=47),
    },
    # Daytime, because these reach people: ask pharmacists to cover uncovered duty days.
    "duty-gap-nudges": {
        "task": "pharmacy_duty.tasks.nudge_uncovered_duty_days",
        "schedule": crontab(hour=10, minute=7),
    },
    # Mondays (the ISO week the reminder is idempotent over): "are your hours still right?"
    "hours-confirmation-reminder": {
        "task": "facilities.tasks.remind_hours_confirmation",
        "schedule": crontab(day_of_week="mon", hour=10, minute=17),
    },
}

S3_ENDPOINT_URL = env("S3_ENDPOINT_URL", "http://localhost:9000")
S3_REGION = env("S3_REGION", "auto")
S3_ACCESS_KEY_ID = env("S3_ACCESS_KEY_ID", "development")
S3_SECRET_ACCESS_KEY = env("S3_SECRET_ACCESS_KEY", "development")
S3_PUBLIC_BUCKET = env("S3_PUBLIC_BUCKET", "directory-public")
S3_PRIVATE_BUCKET = env("S3_PRIVATE_BUCKET", "directory-private")
# Where anyone reads public media from: a CDN in production, the public bucket path-style in
# development. Only the public bucket is ever addressed; see storage/public_media.py.
S3_PUBLIC_MEDIA_BASE_URL = env(
    "S3_PUBLIC_MEDIA_BASE_URL", f"{S3_ENDPOINT_URL.rstrip('/')}/{S3_PUBLIC_BUCKET}"
)
PUSH_PROVIDER = env("PUSH_PROVIDER", "development")
PUSH_TOKEN_ENCRYPTION_KEY = env("PUSH_TOKEN_ENCRYPTION_KEY", "development-push-token-key")
FCM_PROJECT_ID = env("FCM_PROJECT_ID", "")
# The Firebase service account key (its JSON, or that JSON in base64) the FCM transport signs
# in with; see notifications/providers/fcm_http.py. A secret: set it, never commit it.
FCM_SERVICE_ACCOUNT_JSON = env("FCM_SERVICE_ACCOUNT_JSON", "")
ANALYTICS_HASH_SALT = env("ANALYTICS_HASH_SALT", "development-analytics-salt")
# Optional shared secret of the public website's server. Requests carrying it in
# `X-Daliini-Web-Key` have their anonymous public reads counted under the `web_server`
# throttle instead of per address; it grants no data or permission. Empty disables it.
WEB_SERVER_API_KEY = env("WEB_SERVER_API_KEY", "")
ANALYTICS_RETENTION_DAYS = 180

STORAGES = {
    "default": {"BACKEND": "django.core.files.storage.FileSystemStorage"},
    "staticfiles": {"BACKEND": "django.contrib.staticfiles.storage.StaticFilesStorage"},
}

LOGGING = {
    "version": 1,
    "disable_existing_loggers": False,
    "formatters": {
        "json": {"()": "core.logging.JsonFormatter"},
    },
    "filters": {
        "request_id": {"()": "core.logging.RequestIdFilter"},
    },
    "handlers": {
        "console": {
            "class": "logging.StreamHandler",
            "formatter": "json",
            "filters": ["request_id"],
        },
    },
    "root": {"handlers": ["console"], "level": env("LOG_LEVEL", "INFO")},
    "loggers": {
        "django.server": {"level": "WARNING"},
        "celery": {"level": "INFO"},
    },
}

# Optional error reporting. Nothing is sent unless SENTRY_DSN is set; personal data is
# never attached (send_default_pii=False).
SENTRY_DSN = env("SENTRY_DSN", "")
SENTRY_TRACES_SAMPLE_RATE = float(env("SENTRY_TRACES_SAMPLE_RATE", "0") or 0)
SENTRY_ENVIRONMENT = env("SENTRY_ENVIRONMENT", "")
SENTRY_ENABLED = bool(SENTRY_DSN) and env_bool("SENTRY_ENABLED", True)
if SENTRY_ENABLED:
    from core.observability import init_sentry

    init_sentry(
        dsn=SENTRY_DSN,
        traces_sample_rate=SENTRY_TRACES_SAMPLE_RATE,
        environment=SENTRY_ENVIRONMENT or None,
    )
