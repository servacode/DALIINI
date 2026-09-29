import os
import subprocess
import sys


def test_production_settings_fail_closed_without_required_environment() -> None:
    env = os.environ.copy()
    for key in [
        "SECRET_KEY", "REDIS_URL", "S3_ENDPOINT_URL", "S3_REGION", "S3_ACCESS_KEY_ID",
        "S3_SECRET_ACCESS_KEY", "S3_PUBLIC_BUCKET", "S3_PRIVATE_BUCKET", "OTP_PROVIDER",
        "ALLOWED_HOSTS",
    ]:
        env.pop(key, None)
    env["DJANGO_SETTINGS_MODULE"] = "directory_backend.settings.production"
    result = subprocess.run(
        [sys.executable, "-c", "import django; django.setup()"],
        env=env,
        capture_output=True,
        text=True,
        check=False,
    )
    assert result.returncode != 0
    assert "Required environment variable is missing" in result.stderr


PRODUCTION_ENV = {
    "SECRET_KEY": "p" * 50,
    "ACCESS_TOKEN_SIGNING_KEY": "a" * 40,
    "REFRESH_HMAC_SECRET": "r" * 40,
    "RECOVERY_HMAC_SECRET": "c" * 40,
    "ALLOWED_HOSTS": "api.example.test",
    "DATABASE_URL": "postgresql://directory:directory@localhost:5432/directory",
    "REDIS_URL": "redis://localhost:6379/0",
    "S3_ENDPOINT_URL": "https://storage.example.test",
    "S3_REGION": "auto",
    "S3_ACCESS_KEY_ID": "key",
    "S3_SECRET_ACCESS_KEY": "secret",
    "S3_PUBLIC_MEDIA_BASE_URL": "https://media.example.test",
    "S3_PUBLIC_BUCKET": "public",
    "S3_PRIVATE_BUCKET": "private",
    "OTP_PROVIDER": "sms-gateway",
    "PUSH_PROVIDER": "fcm",
    "PUSH_TOKEN_ENCRYPTION_KEY": "k" * 40,
    "ANALYTICS_HASH_SALT": "s" * 40,
    "FCM_PROJECT_ID": "daliini",
    "DJANGO_SETTINGS_MODULE": "directory_backend.settings.production",
}


def _setup(extra: dict[str, str]) -> subprocess.CompletedProcess[str]:
    env = {**os.environ, **PRODUCTION_ENV}
    env.pop("FCM_SERVICE_ACCOUNT_JSON", None)
    env.update(extra)
    return subprocess.run(
        [sys.executable, "-c", "import django; django.setup()"],
        env=env,
        capture_output=True,
        text=True,
        check=False,
    )


def test_fcm_push_needs_its_service_account_key() -> None:
    refused = _setup({})
    accepted = _setup({"FCM_SERVICE_ACCOUNT_JSON": '{"client_email": "x"}'})

    assert refused.returncode != 0
    assert "FCM_SERVICE_ACCOUNT_JSON is required" in refused.stderr
    assert accepted.returncode == 0, accepted.stderr
