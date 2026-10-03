from __future__ import annotations

import pathlib
import re
from typing import Any

import pytest

yaml = pytest.importorskip("yaml")

"""
The production stack against the settings it has to satisfy (DECISION-080).

infrastructure/production/compose.yml is a promise that a deploy will boot. The settings module
decides whether it does, by refusing to import when something required is missing, so the two
drift apart silently until the day somebody deploys. This reads both, and the env example and
the Caddyfile beside them, and compares them.
"""

STACK = pathlib.Path("infrastructure") / "production"


def repo_root() -> pathlib.Path:
    here = pathlib.Path(__file__).resolve()
    for parent in here.parents:
        if (parent / STACK / "compose.yml").exists():
            return parent
    # Inside the API container only apps/backend is mounted, so the stack is out of reach and
    # this module has nothing to compare. CI checks out the whole repository.
    pytest.skip("infrastructure/production is not beside this checkout", allow_module_level=True)


ROOT = repo_root()
COMPOSE_TEXT = (ROOT / STACK / "compose.yml").read_text(encoding="utf-8")
COMPOSE: dict[str, Any] = yaml.safe_load(COMPOSE_TEXT)
SERVICES: dict[str, dict[str, Any]] = COMPOSE["services"]
ENV_EXAMPLE = (ROOT / STACK / "production.env.example").read_text(encoding="utf-8")
CADDYFILE = (ROOT / STACK / "Caddyfile").read_text(encoding="utf-8")
SETTINGS = (
    ROOT / "apps" / "backend" / "directory_backend" / "settings" / "production.py"
).read_text(encoding="utf-8")

BACKEND = ("migrate", "api", "worker", "beat")


def environment(name: str) -> dict[str, str]:
    return {key: str(value) for key, value in SERVICES[name]["environment"].items()}


def test_every_setting_production_requires_reaches_the_backend() -> None:
    # Only the unconditional ones: the WhatsApp and FCM keys are required by a branch, and the
    # stack carries them anyway.
    required = set(re.findall(r'env\("(\w+)",\s*required=True\)', SETTINGS))

    missing = required - environment("api").keys()

    assert not missing, f"compose.yml does not give the API {sorted(missing)}"


def test_the_api_worker_beat_and_migrations_read_the_same_settings() -> None:
    api = environment("api")
    for name in BACKEND:
        assert environment(name) == api, f"{name} reads different settings from the API"


def test_the_database_and_cache_are_this_stack_own() -> None:
    api = environment("api")

    assert "@postgres:5432/" in api["DATABASE_URL"]
    assert api["REDIS_URL"].startswith("redis://redis:6379/")


def test_no_secret_is_written_into_the_stack() -> None:
    # Everything secret comes from the env file on the server, never from the repository.
    secretish = re.compile(r"SECRET|KEY|TOKEN|DSN|PASSWORD", re.IGNORECASE)
    for name, service in SERVICES.items():
        for key, value in (service.get("environment") or {}).items():
            if secretish.search(key):
                assert "${" in str(value), f"{name}: {key} carries a literal value"


def test_every_value_the_stack_insists_on_is_in_the_env_example() -> None:
    insisted = set(re.findall(r"\$\{(\w+):\?\}", COMPOSE_TEXT))
    offered = set(re.findall(r"^(\w+)=", ENV_EXAMPLE, flags=re.MULTILINE))

    missing = insisted - offered

    assert not missing, f"production.env.example does not offer {sorted(missing)}"


def test_the_env_example_carries_no_real_value_for_a_secret() -> None:
    for line in ENV_EXAMPLE.splitlines():
        match = re.match(r"^(\w*(SECRET|KEY|TOKEN|PASSWORD|SALT|DSN)\w*)=(.*)$", line)
        if match and match.group(3):
            assert match.group(3).startswith("<"), f"{match.group(1)} has a value in the example"


def test_the_settings_module_follows_the_environment() -> None:
    # production or staging; staging inherits production's fail-closed posture.
    module = environment("api")["DJANGO_SETTINGS_MODULE"]
    assert module == "directory_backend.settings.${ENVIRONMENT:-production}"
    settings = ROOT / "apps" / "backend" / "directory_backend" / "settings"
    for name in ("production", "staging"):
        assert (settings / f"{name}.py").exists()


def test_only_caddy_is_reachable_from_outside() -> None:
    published = sorted(name for name, service in SERVICES.items() if service.get("ports"))
    assert published == ["caddy"]


def test_the_api_starts_only_after_its_migrations() -> None:
    dependency = SERVICES["api"]["depends_on"]["migrate"]
    assert dependency["condition"] == "service_completed_successfully"
    assert "check --deploy" in " ".join(SERVICES["migrate"]["command"])


def test_every_long_running_service_restarts_and_rotates_its_log() -> None:
    for name, service in SERVICES.items():
        if name == "migrate" or "tools" in service.get("profiles", []):
            continue
        assert service.get("restart") == "unless-stopped", f"{name} does not come back by itself"
        options = service.get("logging", {}).get("options", {})
        assert options.get("max-size") and options.get("max-file"), f"{name} logs grow forever"


def test_redis_refuses_writes_rather_than_dropping_work() -> None:
    command = SERVICES["redis"]["command"]
    assert command[command.index("--maxmemory-policy") + 1] == "noeviction"


def test_the_site_is_told_where_its_photographs_live() -> None:
    """The media origin must reach the web build, or the site blocks its own images.

    `apps/web/next.config.ts` puts this origin into the Content-Security-Policy's img-src at
    build time. Without it the policy allows the API and nothing else, every facility
    photograph is refused by the browser, and the page renders alt text where a picture should
    be. It has happened once, and a browser test against a real stack is what noticed.
    """
    assert "NEXT_PUBLIC_MEDIA_ORIGIN" in SERVICES["web"]["build"]["args"]


def test_the_console_knows_the_address_operators_type() -> None:
    # Its session cookies are derived from it, and it refuses to start without it.
    assert environment("admin")["ADMIN_PUBLIC_ORIGIN"] == "https://admin.${ROOT_DOMAIN:?}"


def test_the_api_sees_one_proxy_and_the_visitor_behind_it() -> None:
    """Every rate limit tells visitors apart by address, so the address must be theirs.

    Caddy is the one proxy in front of the API (DRF_NUM_PROXIES=1) and hands over exactly one
    address: Cloudflare's CF-Connecting-IP when the request came from Cloudflare's own ranges,
    the connection's address otherwise. Anything else and every visitor in the country shares
    one limit, or anyone can choose their own.
    """
    assert environment("api")["DRF_NUM_PROXIES"] == "1"
    assert "client_ip_headers CF-Connecting-IP" in CADDYFILE
    assert re.search(r"trusted_proxies static (\S+/\d+ ?)+", CADDYFILE)
    api_block = CADDYFILE[CADDYFILE.index("api.{$ROOT_DOMAIN}") :]
    assert "header_up X-Forwarded-For {client_ip}" in api_block.split("\n}\n", 1)[0]


def test_no_access_log_writes_a_request_line() -> None:
    """Neither Caddy nor the backend's loggers write the request line.

    The request line is the whole URL, query string included, and some of this API's queries
    carry a person's position. A log that records where somebody stood must not exist, so this
    is pinned rather than left to a comment.
    """
    lines = (line.strip() for line in CADDYFILE.splitlines())
    directives = [line for line in lines if not line.startswith("#")]
    assert not [line for line in directives if line == "log" or line.startswith("log ")]

    settings_base = (
        ROOT / "apps" / "backend" / "directory_backend" / "settings" / "base.py"
    ).read_text(encoding="utf-8")
    for logger in ("django.server", "django.channels.server", "uvicorn.access"):
        assert f'"{logger}": {{"level": "WARNING"}}' in settings_base, (
            f"{logger} would write a request line, and some carry coordinates"
        )


def test_every_server_command_keeps_the_access_log_off() -> None:
    """Uvicorn writes its request line unless told not to, whatever LOGGING says first."""
    commands = [
        " ".join(SERVICES["api"]["command"]),
        (ROOT / "apps" / "backend" / "Dockerfile").read_text(encoding="utf-8"),
        (ROOT / "infrastructure" / "docker" / "compose.yml").read_text(encoding="utf-8"),
    ]
    for command in commands:
        if "uvicorn" in command:
            assert "--no-access-log" in command, command[:120]
        assert "daphne -b" not in command, "production is served by Uvicorn"


def test_backups_leave_the_server_with_their_own_key() -> None:
    backup = environment("backup")
    assert backup["AWS_ACCESS_KEY_ID"] == "${BACKUP_S3_ACCESS_KEY_ID:?}"
    assert backup["AWS_ACCESS_KEY_ID"] != environment("api")["S3_ACCESS_KEY_ID"]


def test_the_site_and_console_reach_the_api_through_caddy() -> None:
    """Server-side calls use the API's public name, which on the stack's network is Caddy.

    `http://api:8000` would be refused by ALLOWED_HOSTS and redirected by the production
    settings' SECURE_SSL_REDIRECT, so the site would render without data and the console could
    not sign anyone in.
    """
    public = "https://api.${ROOT_DOMAIN:?}"
    assert environment("web")["PUBLIC_API_ORIGIN"] == public
    assert SERVICES["web"]["build"]["args"]["PUBLIC_API_ORIGIN"] == public
    assert environment("admin")["ADMIN_API_ORIGIN"] == public
    assert "api.${ROOT_DOMAIN:?}" in SERVICES["caddy"]["networks"]["default"]["aliases"]


def test_a_released_site_is_built_with_the_arguments_the_stack_builds_it_with() -> None:
    """The site's public addresses are fixed when its image is built. The release workflow
    passes the same arguments the stack does, read from the environment's variables of the same
    names, so a pulled image and a built one are the same site (DECISION-081)."""
    workflow = yaml.safe_load(
        (ROOT / ".github" / "workflows" / "release-images.yml").read_text(encoding="utf-8")
    )
    build = next(
        step
        for step in workflow["jobs"]["image"]["steps"]
        if str(step.get("uses", "")).startswith("docker/build-push-action")
    )
    released = dict(
        line.strip().split("=", 1) for line in build["with"]["build-args"].splitlines() if line
    )
    stack = {key: str(value) for key, value in SERVICES["web"]["build"]["args"].items()}
    assert set(released) == set(stack)
    for key, value in stack.items():
        assert re.findall(r"\$\{(\w+)", value) == re.findall(r"vars\.(\w+)", released[key]), key
        assert re.sub(r"\$\{\w+:?[?-]?\}", "X", value) == re.sub(
            r"\$\{\{ vars\.\w+ \}\}", "X", released[key]
        ), key


def _map_module(name: str) -> Any:
    import importlib.util

    path = ROOT / STACK / "map" / f"{name}.py"
    spec = importlib.util.spec_from_file_location(f"map_{name.replace('-', '_')}", path)
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_the_bound_style_reads_everything_from_the_map_host(tmp_path: pathlib.Path) -> None:
    """DECISION-082: tiles, glyphs and icons from maps.<ROOT>, in the brand's font; the layers
    the app inserts its route and markers between are the reference's own."""
    archive = tmp_path / "syria.pmtiles"
    archive.write_bytes(_map_module("fixture").archive())
    style = _map_module("bind-style").bind("https://maps.example.org", archive)
    reference = __import__("json").loads((ROOT / "maps" / "raqqa.style.json").read_text("utf-8"))

    source = style["sources"]["base"]
    assert source["tiles"] == ["https://maps.example.org/syria/{z}/{x}/{y}"]
    assert (source["minzoom"], source["maxzoom"]) == (12, 12)
    assert style["glyphs"] == "https://maps.example.org/font/{fontstack}/{range}"
    assert style["sprite"] == "https://maps.example.org/sprite/daliini"
    assert [layer["id"] for layer in style["layers"]] == [
        layer["id"] for layer in reference["layers"]
    ]
    assert not [key for key in style if key.startswith("_")]
    fonts = {
        font for layer in style["layers"] for font in layer.get("layout", {}).get("text-font", [])
    }
    assert fonts == {"IBM Plex Sans Arabic Regular", "IBM Plex Sans Arabic Bold"}
    assert "rahalgo.com" not in __import__("json").dumps(style)


def test_every_icon_the_style_names_is_drawn() -> None:
    def strings(expression: Any) -> list[str]:
        if isinstance(expression, str):
            return [expression]
        if isinstance(expression, list):
            return [text for item in expression for text in strings(item)]
        return []

    reference = __import__("json").loads((ROOT / "maps" / "raqqa.style.json").read_text("utf-8"))
    named = {
        text
        for layer in reference["layers"]
        for text in strings(layer.get("layout", {}).get("icon-image"))
        if text.startswith("poi-")
    }
    drawn = {path.stem for path in (ROOT / "maps" / "sprite" / "daliini").glob("*.svg")}
    assert named == {"poi-fuel", "poi-pharmacy", "poi-hospital", "poi-landmark"}
    assert named <= drawn, named - drawn


def test_the_map_host_runs_as_ci_and_local_checks_run_it() -> None:
    martin = SERVICES["martin"]
    assert martin["command"] == [
        "/map/syria.pmtiles",
        "--font",
        "/fonts",
        "--sprite",
        "/sprites/daliini",
        "--style",
        "/map/styles",
    ]
    serve = (ROOT / STACK / "map" / "serve.sh").read_text("utf-8")
    assert "/map/syria.pmtiles --font /fonts --sprite /sprites/daliini --style /map/styles" in serve
    for name in ("martin", "valhalla"):
        image = SERVICES[name]["image"]
        assert ":" in image and not image.endswith(":latest"), image
        assert image in serve, f"serve.sh runs another {name}: {image}"
    assert "ports" not in martin and "ports" not in SERVICES["valhalla"]


def test_routing_takes_a_route_by_post_and_nothing_else() -> None:
    """A route's positions travel in the body; a GET would put them in Valhalla's log."""
    site = CADDYFILE[CADDYFILE.index("maps.{$ROOT_DOMAIN}") :]
    routing = site[site.index("handle_path /routing/*") : site.index("handle /style/*")]
    assert "method POST\n\t\t\tpath /route" in routing
    assert "method GET\n\t\t\tpath /status" in routing
    assert "respond 404" in routing
