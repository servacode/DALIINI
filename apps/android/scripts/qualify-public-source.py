from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PUBLIC = ("home", "province", "search", "directory", "facility", "map", "account", "ratings")


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def read(path: str) -> str:
    return (ROOT / path).read_text()


def feature_text(name: str) -> str:
    base = ROOT / "feature" / name / "src" / "main"
    return "\n".join(p.read_text() for p in base.rglob("*.kt"))


def check_public_features() -> None:
    for feature in PUBLIC:
        source = feature_text(feature)
        require("FeatureMarker" not in source, f"placeholder marker remains: {feature}")
        require("@HiltViewModel" in source, f"ViewModel missing: {feature}")
        require("Screen(" in source, f"Composable screen missing: {feature}")
        require("UseCase" in source, f"UseCase missing: {feature}")
        require("Repository" in source, f"Repository missing: {feature}")


def check_cache_and_privacy() -> None:
    for feature in ("home", "directory", "facility"):
        source = feature_text(feature)
        require("PublicCacheDataSource" in source, f"cache-first path missing: {feature}")
    account = feature_text("account")
    ratings = feature_text("ratings")
    require("PublicCacheDataSource" not in account, "account must not persist profile in public Room cache")
    require("PublicCacheDataSource" not in ratings, "ratings must not persist personal data in public Room cache")


def check_location_and_map() -> None:
    home = feature_text("home")
    require("ACCESS_COARSE_LOCATION" in home, "coarse location request missing")
    require("ACCESS_FINE_LOCATION" in home, "fine location request missing")
    require("ACCESS_BACKGROUND_LOCATION" not in home, "background location forbidden")
    mapping = feature_text("map")
    require("AndroidView" in mapping and "MapView" in mapping, "MapLibre native view missing")
    require("WebView" not in mapping, "WebView map is forbidden")
    require("mapFacilities(" in mapping, "viewport map API path missing")
    require("demotiles.maplibre.org" not in mapping, "demo tiles must never ship")
    app = read("app/build.gradle.kts")
    require("DIRECTORY_MAP_STYLE_URL" in app, "map style must be environment configurable")
    require("maps.<ROOT_DOMAIN>" in app, "map style placeholder must use ROOT_DOMAIN")


def check_generated_client_boundary() -> None:
    boundary = read(
        "core/network/src/main/kotlin/com/servacode/directory/core/network/PublicApiBoundary.kt"
    )
    require("generated P10 Kotlin API client" in boundary, "P10 generated-client ownership not documented")
    require("UnboundGeneratedPublicApi" in boundary, "fail-closed generated client placeholder missing")
    require("GeneratedClientRequiredException" in boundary, "unbound client must fail closed")
    lowered = boundary.lower()
    require("fake" not in lowered and "mock" not in lowered, "fake production data marker found")
    require("Dto" not in boundary, "hand-authored transport DTO detected")


def check_routes() -> None:
    app = read("app/src/main/kotlin/com/servacode/directory/DirectoryApp.kt")
    for route in (
        "Home",
        "ProvincePicker",
        "Search",
        "Directory",
        "FacilityDetailRoute",
        "Map",
        "Account",
        "MyRatings",
    ):
        require(f"DirectoryRoute.{route}" in app, f"public route not wired: {route}")


def check_ratings() -> None:
    validator = read(
        "feature/ratings/src/main/kotlin/com/servacode/directory/feature/ratings/RatingValidator.kt"
    )
    require("stars in 1..5" in validator, "rating range invariant missing")
    require(
        (ROOT / "feature/ratings/src/test/kotlin/com/servacode/directory/feature/ratings/RatingValidatorTest.kt").exists(),
        "rating validator test missing",
    )


def check_architecture() -> None:
    for feature in PUBLIC:
        source = feature_text(feature)
        viewmodels = [p for p in (ROOT / "feature" / feature).rglob("*ViewModel.kt")]
        require(viewmodels, f"ViewModel source missing: {feature}")
        vm_text = "\n".join(p.read_text() for p in viewmodels)
        require("UseCase" in vm_text, f"ViewModel must depend on UseCase: {feature}")
        require("PublicApiBoundary" not in vm_text, f"ViewModel bypasses UseCase/Repository: {feature}")
        if feature not in ("home",):
            require("UseCase" in source, f"application UseCase absent: {feature}")


def check_hygiene() -> None:
    sources = [
        *ROOT.rglob("*.kt"),
        *ROOT.rglob("*.kts"),
        *ROOT.rglob("*.xml"),
    ]
    for path in sources:
        text = path.read_text()
        require("ACCESS_BACKGROUND_LOCATION" not in text, f"background location found: {path}")
        require("demotiles.maplibre.org" not in text, f"demo tiles found: {path}")
        if path.name != "DirectoryTokens.kt":
            require(not re.search(r"#[0-9A-Fa-f]{6,8}", text), f"hardcoded UI color: {path}")
        for line_no, line in enumerate(text.splitlines(), 1):
            require(len(line) <= 120, f"line >120: {path.relative_to(ROOT)}:{line_no}")


def main() -> int:
    checks = (
        check_public_features,
        check_cache_and_privacy,
        check_location_and_map,
        check_generated_client_boundary,
        check_routes,
        check_ratings,
        check_architecture,
        check_hygiene,
    )
    for check in checks:
        check()
        print(f"PASS {check.__name__}")
    print("PASS P15 Android public source qualification")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(f"FAIL {exc}", file=sys.stderr)
        raise SystemExit(1)
