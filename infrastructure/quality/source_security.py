#!/usr/bin/env python3
from __future__ import annotations

import ast
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BACKEND = ROOT / "apps/backend"
ANDROID = ROOT / "apps/android"


def require(ok: bool, message: str) -> None:
    if not ok:
        raise AssertionError(message)


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def main() -> int:
    python_files = [p for p in BACKEND.rglob("*.py") if ".venv" not in p.parts]
    for path in python_files:
        ast.parse(path.read_text(encoding="utf-8"), filename=str(path))

    urls = read("apps/backend/accounts/urls.py")
    for route in (
        "auth/register/start/",
        "auth/register/verify/",
        "auth/register/complete/",
        "auth/login/",
        "auth/refresh/",
        "auth/logout/",
        "auth/logout-all/",
        "auth/sessions/",
        "auth/recovery/start/",
        "auth/recovery/verify/",
        "auth/recovery/reset/",
        "account/profile/",
        "account/deletion-request/",
    ):
        require(route in urls, f"missing auth/account route: {route}")

    models = read("apps/backend/accounts/models.py")
    require("otp_digest" in models and "OTPChallenge" in models, "OTP digest model missing")
    require("raw_otp" not in models.lower(), "raw OTP field must not exist")

    session_models = read("apps/backend/sessions/models.py")
    for field in ("previous_refresh_digest", "previous_valid_until", "compromised_at"):
        require(field in session_models, f"session security field missing: {field}")

    services = read("apps/backend/accounts/services.py")
    require("hmac.compare_digest" in services, "constant-time digest comparison missing")
    require("select_for_update" in services, "session/deletion concurrency locking missing")
    require("revoke_all_sessions" in services, "session revocation missing")
    require("another_owner" in services, "last-owner deletion protection missing")
    require("Deleted user" in services, "account anonymization missing")

    facility_permissions = read("apps/backend/facilities/permissions.py")
    require(
        "FacilityMembership.objects.filter" in facility_permissions,
        "owner IDOR membership check missing",
    )

    admin_views = read("apps/backend/admin_console/views.py")
    require(
        "Cache-Control" in admin_views and "no-store" in admin_views,
        "evidence no-store missing",
    )
    require("verification_evidence.viewed" in admin_views, "evidence access audit missing")

    publisher = read("apps/backend/realtime/publisher.py")
    require("transaction.on_commit" in publisher, "realtime must publish after commit")

    analytics = read("apps/backend/analytics/registry.py")
    for forbidden in ("latitude", "longitude", "refreshToken", "evidenceId"):
        require(forbidden in analytics, f"analytics privacy denylist missing: {forbidden}")

    if ANDROID.exists():
        manifest = read("apps/android/app/src/main/AndroidManifest.xml")
        require(
            "ACCESS_BACKGROUND_LOCATION" not in manifest,
            "background location permission forbidden",
        )
        auth = read(
            "apps/android/core/auth/src/main/kotlin/com/servacode/directory/core/auth/"
            "AndroidKeyStoreRefreshTokenVault.kt"
        )
        require("AES/GCM" in auth, "Android refresh vault must use AES/GCM")

    secret_pattern = re.compile(
        r"(?i)(AKIA[0-9A-Z]{16}|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|"
        r"sk_live_[A-Za-z0-9]+)"
    )
    scanned = 0
    for path in ROOT.rglob("*"):
        if not path.is_file() or ".git" in path.parts or ".venv" in path.parts:
            continue
        if path.stat().st_size > 2_000_000:
            continue
        if path.suffix.lower() in {".png", ".jpg", ".jpeg", ".gif", ".zip", ".jar", ".aab", ".apk"}:
            continue
        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        require(
            secret_pattern.search(text) is None,
            f"secret-shaped material found: {path.relative_to(ROOT)}",
        )
        scanned += 1

    print(
        json.dumps(
            {"status": "PASS", "pythonFiles": len(python_files), "secretScanFiles": scanned}
        )
    )
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(json.dumps({"status": "FAIL", "error": str(exc)}))
        raise SystemExit(1)
