from __future__ import annotations

import pathlib
import re
from typing import Any

import pytest

yaml = pytest.importorskip("yaml")

"""
The production blueprint against the settings it has to satisfy.

render.production.yaml is a promise that a deploy will boot. The settings module decides
whether it does, by refusing to import when something required is missing — so the two drift
apart silently until the day somebody deploys. This reads both and compares them.

It caught a real absence when it was written: S3_PUBLIC_MEDIA_BASE_URL is required in
production and the staging blueprint beside it does not carry one.
"""


def repo_root() -> pathlib.Path:
    here = pathlib.Path(__file__).resolve()
    for parent in here.parents:
        if (parent / "render.production.yaml").exists():
            return parent
    # Inside the API container only apps/backend is mounted, so the blueprint is out of
    # reach and this module has nothing to compare. CI checks out the whole repository.
    pytest.skip("render.production.yaml is not beside this checkout", allow_module_level=True)


ROOT = repo_root()
BLUEPRINT = yaml.safe_load((ROOT / "render.production.yaml").read_text(encoding="utf-8"))
SETTINGS = (
    ROOT / "apps" / "backend" / "directory_backend" / "settings" / "production.py"
).read_text(encoding="utf-8")


def group_keys() -> set[str]:
    group = next(g for g in BLUEPRINT["envVarGroups"] if g["name"] == "directory-v3-backend")
    return {entry["key"] for entry in group["envVars"]}


def service(name: str) -> dict[str, Any]:
    return next(s for s in BLUEPRINT["services"] if s["name"] == name)


def test_every_setting_production_requires_is_in_the_blueprint() -> None:
    # Only the unconditional ones: the WhatsApp and FCM keys are required by a branch, and the
    # blueprint carries them anyway.
    required = set(re.findall(r'env\("(\w+)",\s*required=True\)', SETTINGS))
    # These two cannot live in an env var group; each service takes them from the database and
    # the key-value store directly.
    from_other_services = {"DATABASE_URL", "REDIS_URL"}

    missing = required - group_keys() - from_other_services

    assert not missing, f"render.production.yaml is missing {sorted(missing)}"


def test_the_api_worker_and_beat_all_read_the_same_group() -> None:
    for name in ("directory-v3-api", "directory-v3-worker", "directory-v3-beat"):
        groups = [e.get("fromGroup") for e in service(name)["envVars"]]
        assert "directory-v3-backend" in groups, f"{name} does not read the backend group"


def test_the_database_and_cache_are_wired_to_this_blueprint_own_instances() -> None:
    api = service("directory-v3-api")
    database = next(e for e in api["envVars"] if e["key"] == "DATABASE_URL")
    redis = next(e for e in api["envVars"] if e["key"] == "REDIS_URL")

    assert database["fromDatabase"]["name"] == BLUEPRINT["databases"][0]["name"]
    assert redis["fromService"]["name"] == "directory-v3-redis"


def test_nothing_serving_traffic_deploys_itself() -> None:
    # A green pipeline is a reason to deploy, not a decision to.
    for entry in BLUEPRINT["services"]:
        if entry["type"] in {"keyvalue", "cron"}:
            continue
        assert entry.get("autoDeployTrigger") == "off", (
            f"{entry['name']} deploys on its own; note that an unquoted `off` is YAML's false"
        )


def test_no_secret_carries_a_value() -> None:
    # Everything secret is `sync: false` — set in the dashboard, never in the repository.
    secretish = re.compile(r"SECRET|KEY|TOKEN|DSN|PASSWORD", re.IGNORECASE)
    for group in BLUEPRINT["envVarGroups"]:
        for entry in group["envVars"]:
            if secretish.search(entry["key"]):
                assert "value" not in entry, f"{entry['key']} carries a literal value"
                assert entry.get("sync") is False, f"{entry['key']} is not marked sync: false"


def test_production_is_named_apart_from_staging() -> None:
    # One leaked staging credential must not reach anything here, which starts with not
    # sharing a single name.
    names = [s["name"] for s in BLUEPRINT["services"]] + [
        d["name"] for d in BLUEPRINT["databases"]
    ]
    assert not [name for name in names if "staging" in name]


def test_the_settings_module_is_the_production_one() -> None:
    group = next(g for g in BLUEPRINT["envVarGroups"] if g["name"] == "directory-v3-backend")
    module = next(e for e in group["envVars"] if e["key"] == "DJANGO_SETTINGS_MODULE")
    assert module["value"] == "directory_backend.settings.production"


def test_no_access_log_writes_a_request_line() -> None:
    """Both access loggers stay at WARNING, whatever else changes around them.

    Their INFO line is the whole request line, query string included, and some of this API's
    queries carry a person's position. A log that records where somebody stood is a log that
    must not exist, so this is pinned rather than left to a comment.
    """
    settings_base = (
        ROOT / "apps" / "backend" / "directory_backend" / "settings" / "base.py"
    ).read_text(encoding="utf-8")

    for logger in ("django.server", "django.channels.server", "uvicorn.access"):
        assert f'"{logger}": {{"level": "WARNING"}}' in settings_base, (
            f"{logger} would write a request line, and some carry coordinates"
        )


def test_the_site_is_told_where_its_photographs_live() -> None:
    """The media origin must reach the web build, or the site blocks its own images.

    `apps/web/next.config.ts` puts this origin into the Content-Security-Policy's img-src at
    build time. Without it the policy allows the API and nothing else, every facility
    photograph is refused by the browser, and the page renders alt text where a picture should
    be. It has happened once; the blueprints did not carry the variable, and a browser test
    against a real stack is what noticed.
    """
    www = next(s for s in BLUEPRINT["services"] if s["name"].endswith("www"))
    keys = {entry.get("key") for entry in www["envVars"]}

    assert "NEXT_PUBLIC_MEDIA_ORIGIN" in keys, (
        "the site would block every facility photograph"
    )


def test_every_server_command_keeps_the_access_log_off() -> None:
    """Uvicorn writes its request line unless told not to, whatever LOGGING says first."""
    commands = [
        service["dockerCommand"]
        for blueprint in ("render.yaml", "render.production.yaml")
        for service in yaml.safe_load((ROOT / blueprint).read_text(encoding="utf-8"))["services"]
        if service.get("type") == "web" and "dockerCommand" in service
    ]
    commands.append((ROOT / "apps" / "backend" / "Dockerfile").read_text(encoding="utf-8"))
    compose = ROOT / "infrastructure" / "docker" / "compose.yml"
    commands.append(compose.read_text(encoding="utf-8"))

    assert len(commands) >= 4
    for command in commands:
        if "uvicorn" in command:
            assert "--no-access-log" in command, command[:120]
        assert "daphne -b" not in command, "production is served by Uvicorn"
