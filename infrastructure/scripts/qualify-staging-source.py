from __future__ import annotations

from pathlib import Path
import re
import yaml

ROOT = Path(__file__).resolve().parents[2]
blueprint = yaml.safe_load((ROOT / "render.yaml").read_text())
services = {item["name"]: item for item in blueprint["services"]}
databases = {item["name"]: item for item in blueprint["databases"]}
required = {
    "directory-v3-staging-api",
    "directory-v3-staging-worker",
    "directory-v3-staging-www",
    "directory-v3-staging-admin",
    "directory-v3-staging-redis",
}
assert required <= services.keys()
assert "directory-v3-staging-db" in databases
assert services["directory-v3-staging-api"]["healthCheckPath"] == "/health/ready/"
assert "check --deploy" in services["directory-v3-staging-api"]["preDeployCommand"]
assert services["directory-v3-staging-api"]["preDeployCommand"].endswith("migrate --noinput")
assert services["directory-v3-staging-redis"]["maxmemoryPolicy"] == "noeviction"
assert services["directory-v3-staging-redis"]["ipAllowList"] == []
assert databases["directory-v3-staging-db"]["postgresMajorVersion"] == "17"
assert databases["directory-v3-staging-db"]["ipAllowList"] == []
regional_services = [item for item in blueprint["services"] if item["type"] != "keyvalue"]
assert all(item.get("region") == "frankfurt" for item in regional_services)

raw=(ROOT / "render.yaml").read_text()
for forbidden in [
    "directory-platform-v2",
    "directory-free-api",
    "directory-free-admin",
    "directory-free-db",
    "directory-free-redis",
]:
    assert forbidden not in raw
assert not re.search(r"(?i)(password|secret|token):\s+[^<{\s][^\n]+", raw)

staging=(ROOT / "apps/backend/directory_backend/settings/staging.py").read_text()
assert "from .production import *" in staging
assert "from .base import *" not in staging
migration=(ROOT / "apps/backend/locations/migrations/0001_initial.py").read_text()
assert "CreateExtension('postgis')" in migration

production=(ROOT / "apps/backend/directory_backend/settings/production.py").read_text()
assert 'DATABASE_URL = env("DATABASE_URL", required=True)' in production
assert 'SECURE_PROXY_SSL_HEADER = ("HTTP_X_FORWARDED_PROTO", "https")' in production
assert "|| true" not in (ROOT / "apps/backend/Dockerfile").read_text()
for key in [
    "REFRESH_HMAC_SECRET",
    "RECOVERY_HMAC_SECRET",
    "PUSH_TOKEN_ENCRYPTION_KEY",
    "ANALYTICS_HASH_SALT",
]:
    assert f'{key} = env("{key}", required=True)' in production

for path in [
    "docs/runbooks/staging-deploy.md",
    "docs/runbooks/rollback.md",
    "docs/runbooks/backup-restore.md",
    "docs/runbooks/incident-response.md",
    "docs/runbooks/dns-tls.md",
    "docs/runbooks/monitoring.md",
]:
    assert (ROOT / path).is_file(), path
print("P19 staging source qualification PASS")
