from django.core.exceptions import ImproperlyConfigured

from .base import *  # noqa: F403
from .env import env, env_bool, env_csv

DEBUG = env_bool("DEBUG", False)
SECRET_KEY = env("SECRET_KEY", required=True)
ACCESS_TOKEN_SIGNING_KEY = env("ACCESS_TOKEN_SIGNING_KEY", required=True)
REFRESH_HMAC_SECRET = env("REFRESH_HMAC_SECRET", required=True)
RECOVERY_HMAC_SECRET = env("RECOVERY_HMAC_SECRET", required=True)
ALLOWED_HOSTS = env_csv("ALLOWED_HOSTS")
CORS_ALLOWED_ORIGINS = env_csv("CORS_ALLOWED_ORIGINS")
CSRF_TRUSTED_ORIGINS = env_csv("CSRF_TRUSTED_ORIGINS")
DATABASE_URL = env("DATABASE_URL", required=True)
REDIS_URL = env("REDIS_URL", required=True)
S3_ENDPOINT_URL = env("S3_ENDPOINT_URL", required=True)
S3_REGION = env("S3_REGION", required=True)
S3_ACCESS_KEY_ID = env("S3_ACCESS_KEY_ID", required=True)
S3_SECRET_ACCESS_KEY = env("S3_SECRET_ACCESS_KEY", required=True)
S3_PUBLIC_MEDIA_BASE_URL = env("S3_PUBLIC_MEDIA_BASE_URL", required=True)
S3_PUBLIC_BUCKET = env("S3_PUBLIC_BUCKET", required=True)
S3_PRIVATE_BUCKET = env("S3_PRIVATE_BUCKET", required=True)
OTP_PROVIDER = env("OTP_PROVIDER", required=True)
PUSH_PROVIDER = env("PUSH_PROVIDER", required=True)
PUSH_TOKEN_ENCRYPTION_KEY = env("PUSH_TOKEN_ENCRYPTION_KEY", required=True)
ANALYTICS_HASH_SALT = env("ANALYTICS_HASH_SALT", required=True)
FCM_PROJECT_ID = env("FCM_PROJECT_ID", "")

if DEBUG:
    raise ImproperlyConfigured("Production DEBUG must be false")
if SECRET_KEY.startswith("CHANGE_ME") or SECRET_KEY == "development-only-not-for-production":
    raise ImproperlyConfigured("Production SECRET_KEY is insecure")
if ACCESS_TOKEN_SIGNING_KEY.startswith("CHANGE_ME") or len(ACCESS_TOKEN_SIGNING_KEY) < 32:
    raise ImproperlyConfigured("Production ACCESS_TOKEN_SIGNING_KEY is insecure")
if REFRESH_HMAC_SECRET.startswith("CHANGE_ME") or len(REFRESH_HMAC_SECRET) < 32:
    raise ImproperlyConfigured("Production REFRESH_HMAC_SECRET is insecure")
if RECOVERY_HMAC_SECRET.startswith("CHANGE_ME") or len(RECOVERY_HMAC_SECRET) < 32:
    raise ImproperlyConfigured("Production RECOVERY_HMAC_SECRET is insecure")
if not ALLOWED_HOSTS or "*" in ALLOWED_HOSTS:
    raise ImproperlyConfigured("Production ALLOWED_HOSTS must be explicit")
if OTP_PROVIDER.lower() in {"development", "test", "console"}:
    raise ImproperlyConfigured("Production OTP provider cannot be a test provider")
if PUSH_PROVIDER.lower() in {"development", "test", "console"}:
    raise ImproperlyConfigured("Production push provider cannot be a test provider")
if len(PUSH_TOKEN_ENCRYPTION_KEY) < 32:
    raise ImproperlyConfigured("Production PUSH_TOKEN_ENCRYPTION_KEY is too short")
if len(ANALYTICS_HASH_SALT) < 32:
    raise ImproperlyConfigured("Production ANALYTICS_HASH_SALT is too short")
if PUSH_PROVIDER.lower() == "fcm" and not FCM_PROJECT_ID:
    raise ImproperlyConfigured("FCM_PROJECT_ID is required when PUSH_PROVIDER=fcm")

CHANNEL_LAYERS["default"]["CONFIG"]["hosts"] = [REDIS_URL]
CELERY_BROKER_URL = REDIS_URL
CELERY_RESULT_BACKEND = REDIS_URL

# 07-BACKEND-DJANGO: the interactive schema route is not served in production.
OPENAPI_SCHEMA_EXPOSURE = "disabled"

SECURE_PROXY_SSL_HEADER = ("HTTP_X_FORWARDED_PROTO", "https")
SESSION_COOKIE_SECURE = True
CSRF_COOKIE_SECURE = True
SECURE_SSL_REDIRECT = True
SECURE_HSTS_SECONDS = 31536000
SECURE_HSTS_INCLUDE_SUBDOMAINS = True
SECURE_CONTENT_TYPE_NOSNIFF = True
SECURE_REFERRER_POLICY = "strict-origin-when-cross-origin"

STORAGES = {
    "default": {
        "BACKEND": "storage.backends.PrivateS3Storage",
    },
    "staticfiles": {"BACKEND": "django.contrib.staticfiles.storage.StaticFilesStorage"},
}
