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

# Deploys and backups (DECISION-081): images published per environment, a deploy started by a
# person, an hourly backup and a monthly drill that proves it restores.
for name in ("migrate", "api", "worker", "beat", "web", "admin", "whatsapp-bot", "backup"):
    image = services[name]["image"]
    assert image.startswith("${IMAGE_REGISTRY:-}daliini-"), f"{name} runs another image: {image}"
    assert image.endswith(":${RELEASE:-local}"), f"{name} must run the release deployed: {image}"
workflows = ROOT / ".github" / "workflows"
release = yaml.safe_load((workflows / "release-images.yml").read_text(encoding="utf-8"))
deploy = yaml.safe_load((workflows / "deploy.yml").read_text(encoding="utf-8"))
for workflow in (release, deploy):
    # PyYAML reads the key `on` as True.
    assert list(workflow[True]) == ["workflow_dispatch"], "nothing deploys by itself"
released = {entry["name"] for entry in release["jobs"]["image"]["strategy"]["matrix"]["include"]}
assert released == {"backend", "web", "admin", "whatsapp-bot", "backup"}, released
deploy_text = (workflows / "deploy.yml").read_text(encoding="utf-8")
assert "StrictHostKeyChecking=yes" in deploy_text, "the deploy must know the server's key"
assert "infrastructure/production/deploy.sh" in deploy_text
timer = (STACK / "systemd" / "daliini-backup.timer").read_text(encoding="utf-8")
assert "OnCalendar=*-*-* *:17:00 UTC" in timer, "backups run every hour (RPO 60 minutes)"
drill = (STACK / "systemd" / "daliini-restore-drill.timer").read_text(encoding="utf-8")
assert "OnCalendar=Sun *-*-01..07" in drill, "the restore drill runs on the first Sunday of a month"
restore = (STACK / "restore-drill.sh").read_text(encoding="utf-8")
assert "--max-rpo-minutes 60" in restore and "restore_evidence.py" in restore
for script in ("deploy.sh", "restore-drill.sh", "smoke.sh"):
    assert (STACK / script).stat().st_mode & 0o111, f"{script} is not executable"

# The map host (DECISION-082): Martin and Valhalla, pinned, behind Caddy's fifth name; routes
# only by POST; the build, the binding, the fixture and the local server beside the stack.
for name in ("martin", "valhalla"):
    assert name in services, f"the stack has no {name}"
    assert not services[name]["image"].endswith(":latest"), f"{name} must be pinned"
assert "maps.{$ROOT_DOMAIN}" in caddyfile, "the Caddyfile does not serve maps.<ROOT>"
assert "method POST\n\t\t\tpath /route" in caddyfile, "routes are taken by POST only"
for script in ("map/build-map.sh", "map/serve.sh", "map/bind-style.py", "map/fixture.py"):
    assert (STACK / script).stat().st_mode & 0o111, f"{script} is not executable"
assert "OnCalendar=Sun *-*-08..14" in (STACK / "systemd" / "daliini-map.timer").read_text()
assert (ROOT / "maps" / "sprite" / "daliini" / "poi-pharmacy.svg").is_file()

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
