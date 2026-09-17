from pathlib import Path

import dj_database_url

from .env import env, env_csv

BASE_DIR = Path(__file__).resolve().parents[2]

SECRET_KEY = env("SECRET_KEY", "development-only-not-for-production")
ACCESS_TOKEN_SIGNING_KEY = env("ACCESS_TOKEN_SIGNING_KEY", "development-access-token-key")
DEBUG = False
ALLOWED_HOSTS = env_csv("ALLOWED_HOSTS", ["localhost", "127.0.0.1"])

INSTALLED_APPS = [
    "daphne",
    "django.contrib.auth",
    "django.contrib.contenttypes",
    "django.contrib.messages",
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
]

MIDDLEWARE = [
    "core.middleware.RequestIdMiddleware",
    "django.middleware.security.SecurityMiddleware",
    "corsheaders.middleware.CorsMiddleware",
    "django.middleware.common.CommonMiddleware",
    "django.middleware.csrf.CsrfViewMiddleware",
    "django.contrib.messages.middleware.MessageMiddleware",
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
}
SPECTACULAR_SETTINGS = {
    "TITLE": "Directory Platform API",
    "VERSION": "1.0.0",
    "SERVE_INCLUDE_SCHEMA": False,
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
