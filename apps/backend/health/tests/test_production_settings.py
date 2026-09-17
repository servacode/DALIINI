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
