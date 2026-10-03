#!/usr/bin/env python3
"""The production stack, read as source (DECISION-080).

The backend's test_production_stack.py holds the stack to the settings it must satisfy; this is
the part a release check can run without the backend's environment: the shape of the stack,
and the fail-closed rules around it.
"""
from __future__ import annotations

import re
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[2]
STACK = ROOT / "infrastructure" / "production"

compose = yaml.safe_load((STACK / "compose.yml").read_text(encoding="utf-8"))
services = compose["services"]
for name in ("caddy", "postgres", "redis", "migrate", "api", "worker", "beat", "web", "admin",
             "whatsapp-bot", "backup"):
    assert name in services, f"the stack has no {name}"
assert sorted(n for n, s in services.items() if s.get("ports")) == ["caddy"], "only Caddy is published"
assert services["postgres"]["image"].startswith("postgis/postgis:17"), "PostgreSQL 17 with PostGIS"
assert "noeviction" in services["redis"]["command"], "Redis must refuse writes, not drop work"
assert services["api"]["depends_on"]["migrate"]["condition"] == "service_completed_successfully"
assert "check --deploy" in " ".join(services["migrate"]["command"])
assert "--no-access-log" in " ".join(services["api"]["command"])

raw = (STACK / "compose.yml").read_text(encoding="utf-8")
settings_lines = [line for line in raw.splitlines() if not line.strip().startswith("#")]
for line in settings_lines:
    assert not re.search(r"(?i)(password|secret|token|key)\w*:\s+(?!\$\{)[^\s#]", line), (
        f"a credential is written into compose.yml: {line.strip()}"
    )
caddyfile = (STACK / "Caddyfile").read_text(encoding="utf-8")
assert "client_ip_headers CF-Connecting-IP" in caddyfile
for site in ("api.{$ROOT_DOMAIN}", "{$ROOT_DOMAIN}, www.{$ROOT_DOMAIN}", "admin.{$ROOT_DOMAIN}"):
    assert site in caddyfile, f"the Caddyfile does not serve {site}"
assert (STACK / "production.env.example").is_file()
assert not (ROOT / "render.yaml").exists() and not (ROOT / "render.production.yaml").exists(), (
    "Render is not used (ROADMAP.md, owner's decision of 2026-10-03)"
)

staging = (ROOT / "apps/backend/directory_backend/settings/staging.py").read_text()
assert "from .production import *" in staging
assert "from .base import *" not in staging
migration = (ROOT / "apps/backend/locations/migrations/0001_initial.py").read_text()
assert "CreateExtension('postgis')" in migration

production = (ROOT / "apps/backend/directory_backend/settings/production.py").read_text()
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
    "infrastructure/production/README.md",
    "docs/runbooks/deploy.md",
    "docs/runbooks/rollback.md",
    "docs/runbooks/backup-restore.md",
    "docs/runbooks/incident-response.md",
    "docs/runbooks/dns-tls.md",
    "docs/runbooks/monitoring.md",
]:
    assert (ROOT / path).is_file(), path
print("production stack source qualification PASS")
