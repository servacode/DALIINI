from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OWNER_FEATURES = ("owner", "onboarding", "duty")


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def read(relative: str) -> str:
    return (ROOT / relative).read_text()


def feature_text(name: str) -> str:
    base = ROOT / "feature" / name / "src"
    return "\n".join(path.read_text() for path in base.rglob("*.kt"))


def check_feature_architecture() -> None:
    for feature in OWNER_FEATURES:
        source = feature_text(feature)
        require("FeatureMarker" not in source, f"placeholder marker remains: {feature}")
        require("@HiltViewModel" in source, f"ViewModel missing: {feature}")
        require("Screen(" in source, f"Composable screen missing: {feature}")
        require("UseCase" in source, f"UseCase missing: {feature}")
        require("Repository" in source, f"Repository missing: {feature}")
        require("PublicCacheDataSource" not in source, f"owner data must not use public Room cache: {feature}")


def check_onboarding() -> None:
    vm = read(
        "feature/onboarding/src/main/kotlin/com/servacode/directory/feature/onboarding/"
        "OnboardingViewModel.kt"
    )
    screen = read(
        "feature/onboarding/src/main/kotlin/com/servacode/directory/feature/onboarding/"
        "OnboardingScreen.kt"
    )
    steps = (
        "PROVINCE_CATEGORY",
        "BASIC_INFO",
        "MAP_POINT",
        "HOURS",
        "PUBLIC_IMAGES",
        "SPECIALIZED_FIELDS",
        "VERIFICATION_EVIDENCE",
        "REVIEW",
        "SUBMIT",
        "STATUS",
    )
    for step in steps:
        require(step in vm and step in screen, f"onboarding step missing: {step}")
    require("delay(650)" in vm, "draft autosave debounce missing")
    require("PickVisualMedia" in screen, "Android Photo Picker missing")
    require("READ_MEDIA_IMAGES" not in screen, "broad media permission is forbidden")
    require("READ_EXTERNAL_STORAGE" not in screen, "legacy storage permission is forbidden")
    require("ACCESS_BACKGROUND_LOCATION" not in screen, "background location is forbidden")
    require("ACCESS_COARSE_LOCATION" in screen and "ACCESS_FINE_LOCATION" in screen, "foreground location permissions missing")
    require("OnboardingMapPicker" in screen, "native map point picker missing")
    require("mutableStateListOf<HourDraft>" in screen, "per-day hours editor missing")
    require("verificationRequirements" in screen, "verification requirement-driven upload missing")


def check_generated_boundary() -> None:
    boundary = read(
        "core/network/src/main/kotlin/com/servacode/directory/core/network/OwnerApiBoundary.kt"
    )
    require("UnboundGeneratedOwnerApi" in boundary, "fail-closed generated owner API missing")
    require("GeneratedClientRequiredException" in boundary, "owner API must fail closed before P10")
    require("Dto" not in boundary, "hand-authored owner transport DTO detected")
    require("storageKey" not in boundary, "storage keys must not enter Android owner contract")


def check_manage_and_duty() -> None:
    owner = feature_text("owner")
    duty = feature_text("duty")
    require("createTemporaryClosure" in owner, "temporary closure creation missing")
    require("deleteTemporaryClosure" in owner, "temporary closure deletion missing")
    require("upsertMember" in owner and "deleteMember" in owner, "manager controls missing")
    require("FOUR_HOURS" not in duty and "4 ساعات" not in duty, "invented fixed duty duration remains")
    require("startNow(endsAt" in duty, "start-now must use owner-selected end time")
    require("DutyValidator.isValid" in duty, "duty validation missing")
    require(
        (ROOT / "feature/duty/src/test/kotlin/com/servacode/directory/feature/duty/DutyValidatorTest.kt").exists(),
        "duty validator test missing",
    )


def check_routes_and_map() -> None:
    app = read("app/src/main/kotlin/com/servacode/directory/DirectoryApp.kt")
    for route in ("MyFacilities", "Onboarding", "ManageFacility", "Duty"):
        require(f"DirectoryRoute.{route}" in app, f"owner route not wired: {route}")
    picker = read(
        "feature/onboarding/src/main/kotlin/com/servacode/directory/feature/onboarding/"
        "OnboardingMapPicker.kt"
    )
    require("MapView" in picker and "MapLibreController" in picker, "MapLibre native picker missing")
    require("WebView" not in picker, "WebView map is forbidden")


def check_hygiene() -> None:
    sources = [
        *(ROOT / "feature/owner").rglob("*.kt"),
        *(ROOT / "feature/onboarding").rglob("*.kt"),
        *(ROOT / "feature/duty").rglob("*.kt"),
        *(ROOT / "core/network").rglob("*.kt"),
    ]
    for path in sources:
        text = path.read_text()
        require("storage_key" not in text, f"raw storage key reference found: {path}")
        require("ACCESS_BACKGROUND_LOCATION" not in text, f"background location found: {path}")
        if path.name != "DirectoryTokens.kt":
            require(not re.search(r"#[0-9A-Fa-f]{6,8}", text), f"hardcoded UI color: {path}")
        for line_no, line in enumerate(text.splitlines(), 1):
            require(len(line) <= 120, f"line >120: {path.relative_to(ROOT)}:{line_no}")


def main() -> int:
    checks = (
        check_feature_architecture,
        check_onboarding,
        check_generated_boundary,
        check_manage_and_duty,
        check_routes_and_map,
        check_hygiene,
    )
    for check in checks:
        check()
        print(f"PASS {check.__name__}")
    print("PASS P16 Android owner source qualification")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(f"FAIL {exc}", file=sys.stderr)
        raise SystemExit(1)
