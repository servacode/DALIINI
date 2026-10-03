#!/usr/bin/env python3
from __future__ import annotations

import ast
import base64
import json
import os
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BACKEND = ROOT / "apps/backend"
ANDROID = ROOT / "apps/android"

# Directories that hold no source of this project's own: a dependency tree, a build output, a
# tool's cache. None of them is committed — `node_modules/` and `build/` are ignored — so what
# they contain says nothing about what this repository ships, and reading them is what made the
# scan below fail on Next.js rather than on a secret.
NOT_OUR_SOURCE = frozenset(
    {
        ".git",
        ".gradle",
        ".next",
        ".pytest_cache",
        ".venv",
        "__pycache__",
        "build",
        "dist",
        "node_modules",
    }
)


def require(ok: bool, message: str) -> None:
    if not ok:
        raise AssertionError(message)


def verify_pattern(pattern: re.Pattern[str]) -> None:
    """The gate proves itself before it judges anything else.

    Both ways this check can be wrong are silent. It was case-insensitive, so an AWS key id
    matched ordinary base64 and the gate failed on a dependency's compiled WebAssembly; a pattern
    that matched nothing at all would instead report PASS forever. Neither is visible in the
    result, so the samples below run on every invocation rather than in a test nobody runs.

    `AKIA` and `sk_live_` are written in two pieces on purpose: spelled whole, this file would be
    the material its own scan is looking for.
    """
    body = base64.b64encode(os.urandom(256)).decode()
    header = "-----BEGIN PRIVATE KEY-----"
    must_catch = (
        f"{header}\n{body}\n-----END PRIVATE KEY-----",
        '{"private_key":"' + header + "\\n" + body + '"}',
        "AKIA" + "IOSFODNN7EXAMPLE",
        "sk_live_" + "4eC39HqLyjWDarjtT1zdp7dc",
    )
    # A key named and refused, the base64 that broke the gate, and the wrong case.
    must_not_catch = (
        header + "\\n" + "xx",
        "AkIANwMgDOACC0EAIQAC AKIAoQChgILIAQgAUEBa AKiABQQFqIQFBzwAhAwy",
        "akia" + "iosfodnn7example",
    )
    for sample in must_catch:
        require(
            pattern.search(sample) is not None,
            "the secret pattern no longer catches a key it must catch",
        )
    for sample in must_not_catch:
        require(
            pattern.search(sample) is None,
            "the secret pattern matches material that is not a key",
        )


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

    # The evidence stream lives with the rest of the review queue (views_reviews, phase 2.9).
    evidence_views = read("apps/backend/admin_console/views_reviews.py")
    require(
        "Cache-Control" in evidence_views and "no-store" in evidence_views,
        "evidence no-store missing",
    )
    require("verification_evidence.viewed" in evidence_views, "evidence access audit missing")

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
            "apps/android/core/auth/src/androidMain/kotlin/com/servacode/directory/core/auth/"
            "AndroidKeyStoreRefreshTokenVault.kt"
        )
        require("AES/GCM" in auth, "Android refresh vault must use AES/GCM")

    # Case is part of each of these three: an AWS access-key id and a PEM header are uppercase,
    # a Stripe live key is lowercase. Read case-insensitively, `AKIA[0-9A-Z]{16}` also matches
    # ordinary base64 — `AkIANwMgDOACC0EAIQAC`, out of a compiled WebAssembly blob — so the
    # pattern is case-sensitive, which is both the truth about the formats and far fewer lies.
    #
    # A private key is caught by its material, not by its name. The header alone is something a
    # test says out loud on purpose: `notifications/tests/test_fcm_transport.py` passes a
    # deliberately malformed `-----BEGIN PRIVATE KEY-----\nxx` to prove the refusal never echoes
    # what it was given. Any real key carries hundreds of base64 characters after the header —
    # the shortest form in use, an EC P-256 key, already carries over two hundred — so the body
    # is what the pattern requires. A separator written as a literal backslash-n, the way a key
    # pasted into JSON arrives, still counts towards it.
    secret_pattern = re.compile(
        r"AKIA[0-9A-Z]{16}"
        r"|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----[A-Za-z0-9+/=\s\\]{100,}"
        r"|sk_live_[A-Za-z0-9]+"
    )
    verify_pattern(secret_pattern)
    scanned = 0
    for folder, subfolders, filenames in os.walk(ROOT):
        # Pruned here rather than skipped further down: the dependency trees hold tens of
        # thousands of files each, and merely walking into them was most of this gate's cost.
        subfolders[:] = [name for name in subfolders if name not in NOT_OUR_SOURCE]
        for filename in filenames:
            path = Path(folder) / filename
            if not path.is_file():
                continue
            if path.stat().st_size > 2_000_000:
                continue
            if path.suffix.lower() in {
                ".png",
                ".jpg",
                ".jpeg",
                ".gif",
                ".zip",
                ".jar",
                ".aab",
                ".apk",
            }:
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
