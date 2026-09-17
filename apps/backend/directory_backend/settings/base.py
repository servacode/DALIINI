from pathlib import Path

import dj_database_url

from .env import env, env_csv

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
    "django.middleware.security.SecurityMiddleware",
    "corsheaders.middleware.CorsMiddleware",
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
    },
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
    },
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

S3_ENDPOINT_URL = env("S3_ENDPOINT_URL", "http://localhost:9000")
S3_REGION = env("S3_REGION", "auto")
S3_ACCESS_KEY_ID = env("S3_ACCESS_KEY_ID", "development")
S3_SECRET_ACCESS_KEY = env("S3_SECRET_ACCESS_KEY", "development")
S3_PUBLIC_BUCKET = env("S3_PUBLIC_BUCKET", "directory-public")
S3_PRIVATE_BUCKET = env("S3_PRIVATE_BUCKET", "directory-private")
PUSH_PROVIDER = env("PUSH_PROVIDER", "development")
PUSH_TOKEN_ENCRYPTION_KEY = env("PUSH_TOKEN_ENCRYPTION_KEY", "development-push-token-key")
FCM_PROJECT_ID = env("FCM_PROJECT_ID", "")
ANALYTICS_HASH_SALT = env("ANALYTICS_HASH_SALT", "development-analytics-salt")
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
    "handlers": {
        "console": {"class": "logging.StreamHandler", "formatter": "json"},
    },
    "root": {"handlers": ["console"], "level": "INFO"},
}
