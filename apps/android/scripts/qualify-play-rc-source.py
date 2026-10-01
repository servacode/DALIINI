#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
ANDROID = ROOT / "apps/android"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def text(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


def main() -> int:
    convention = text(
        "apps/android/build-logic/src/main/kotlin/serva.android.application.gradle.kts"
    )
    app_gradle = text("apps/android/app/build.gradle.kts")
    manifest = text("apps/android/app/src/main/AndroidManifest.xml")
    boundary = text(
        "apps/android/core/network/src/main/kotlin/com/servacode/directory/core/network/"
        "PublicApiBoundary.kt"
    )
    account_screen = text(
        "apps/android/feature/account/src/main/kotlin/com/servacode/directory/feature/"
        "account/AccountScreen.kt"
    )
    account_words = text("apps/android/feature/account/src/main/res/values/strings.xml")

    require("targetSdk = 36" in convention, "Play RC must target API 36")
    require(
        'applicationId = "com.servacode.directory"' in convention,
        "applicationId drift",
    )
    require("versionCode =" in convention and "versionName =" in convention, "version missing")
    require("validatePlayRelease" in app_gradle, "release validation task missing")
    for name in (
        "ANDROID_UPLOAD_KEYSTORE_PATH",
        "ANDROID_UPLOAD_KEY_ALIAS",
        "ANDROID_UPLOAD_STORE_PASSWORD",
        "ANDROID_UPLOAD_KEY_PASSWORD",
    ):
        require(name in app_gradle, f"signing environment contract missing: {name}")
    require("ACCESS_BACKGROUND_LOCATION" not in manifest, "background location is forbidden")
    require("READ_SMS" not in manifest and "READ_CONTACTS" not in manifest, "unexpected permission")
    require("requestAccountDeletion" in boundary, "generated API boundary lacks account deletion")
    # The screen shows the deletion and its confirmation; the words are in the module's own
    # strings.xml, which is where a second language replaces them.
    require(
        "AccountCopy.DELETE" in account_screen and "AccountCopy.DELETE_CONFIRM" in account_screen,
        "in-app deletion UI missing",
    )
    require(
        "حذف الحساب" in account_words and "تأكيد الحذف" in account_words,
        "in-app deletion wording missing",
    )

    required_files = (
        "apps/android/play/policy-baseline-2026-09-17.md",
        "apps/android/play/data-safety-inventory.json",
        "apps/android/play/app-content-checklist.md",
        "apps/android/play/closed-testing-runbook.md",
        "apps/android/play/store-listing/ar.json",
        "apps/android/play/evidence/release-evidence-template.json",
        "apps/android/play/release.env.example",
    )
    for relative in required_files:
        require((ROOT / relative).is_file(), f"missing Play RC artifact: {relative}")

    inventory = json.loads(text("apps/android/play/data-safety-inventory.json"))
    require(inventory["security"]["accountDeletionAvailable"] is True, "deletion disclosure missing")
    require(
        "<ROOT_DOMAIN>" in inventory["security"]["externalDeletionUrl"],
        "domain placeholder policy drift",
    )

    listing = json.loads(text("apps/android/play/store-listing/ar.json"))
    require(listing["locale"] == "ar", "Arabic listing baseline missing")
    require(listing["assets"]["phoneScreenshots"] == [], "fabricated screenshots must not be committed")

    for path in ANDROID.rglob("*"):
        if not path.is_file():
            continue
        lower = path.name.lower()
        require(
            not lower.endswith((".jks", ".keystore", ".p12", ".pem")),
            f"signing material in repo: {path}",
        )

    print(json.dumps({"status": "PASS", "checks": 7}))
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(json.dumps({"status": "FAIL", "error": str(exc)}))
        raise SystemExit(1)
