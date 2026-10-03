from __future__ import annotations

import re
import sys
from pathlib import Path

TEST_SOURCE_SET = re.compile(r"/src/(test|androidHostTest|commonTest)/")
ROOT = Path(__file__).resolve().parents[1]


def mentions(text: str, host: str) -> bool:
    """Whether source text names a host anywhere. A search of our own files for a forbidden
    address, not a check of a URL, so it is spelled as a search rather than a substring test."""
    return re.search(re.escape(host), text) is not None
PUBLIC = ("home", "province", "search", "facility", "map", "account", "ratings")


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def feature_text(name: str) -> str:
    # Every source set but the tests: a feature is split between commonMain and androidMain
    # (DECISION-089).
    base = ROOT / "feature" / name / "src"
    return "\n".join(
        p.read_text(encoding="utf-8")
        for p in base.rglob("*.kt")
        if "build" not in p.parts and not TEST_SOURCE_SET.search(p.as_posix())
    )


def check_public_features() -> None:
    for feature in PUBLIC:
        source = feature_text(feature)
        require("FeatureMarker" not in source, f"placeholder marker remains: {feature}")
        require("@HiltViewModel" in source, f"ViewModel missing: {feature}")
        require("Screen(" in source, f"Composable screen missing: {feature}")
        require("UseCase" in source, f"UseCase missing: {feature}")
        require("Repository" in source, f"Repository missing: {feature}")


def check_cache_and_privacy() -> None:
    # Features read the cache through the PublicCache interface; PublicCacheDataSource is the
    # Room implementation behind it, which no feature names. What matters is that a public list
    # is served from the cache before the network, and that personal data never enters it.
    # `directory` was a second list screen nobody could reach: Home already narrows by
    # category, and the route that opened it was never navigated to. Removed with the
    # module, so it is no longer among the features this gate expects to find.
    for feature in ("home", "facility"):
        source = feature_text(feature)
        require("PublicCache" in source, f"cache-first path missing: {feature}")
        require("cacheFirst(" in source, f"cache-first read missing: {feature}")
    account = feature_text("account")
    ratings = feature_text("ratings")
    require("PublicCache" not in account, "account must not persist profile in the public cache")
    require("PublicCache" not in ratings, "ratings must not persist personal data in the public cache")


def check_location_and_map() -> None:
    home = feature_text("home")
    # Home asks through the one shared list of foreground permissions rather than naming them,
    # so there is a single place that decides what this app may ever request.
    require(
        "FOREGROUND_LOCATION_PERMISSIONS" in home,
        "home must request location through the shared foreground permission list",
    )
    permissions = read(
        "core/location/src/commonMain/kotlin/com/servacode/directory/core/location/LocationProvider.kt"
    )
    require("ACCESS_COARSE_LOCATION" in permissions, "coarse location request missing")
    require("ACCESS_FINE_LOCATION" in permissions, "fine location request missing")
    require("ACCESS_BACKGROUND_LOCATION" not in permissions, "background location forbidden")
    require("ACCESS_BACKGROUND_LOCATION" not in home, "background location forbidden")
    mapping = feature_text("map")
    require("AndroidView" in mapping and "MapView" in mapping, "MapLibre native view missing")
    require("WebView" not in mapping, "WebView map is forbidden")
    require("mapFacilities(" in mapping, "viewport map API path missing")
    require(not mentions(mapping, "demotiles.maplibre.org"), "demo tiles must never ship")
    app = read("app/build.gradle.kts")
    require("DIRECTORY_MAP_STYLE_URL" in app, "map style must be environment configurable")
    require("maps.<ROOT_DOMAIN>" in app, "map style placeholder must use ROOT_DOMAIN")


def check_generated_client_boundary() -> None:
    boundary = read(
        "core/network/src/commonMain/kotlin/com/servacode/directory/core/network/PublicApiBoundary.kt"
    )
    require(
        "generated P10 Kotlin" in boundary,
        "P10 generated-client ownership not documented",
    )
    # Before P10 this boundary had a fail-closed placeholder because no generated client
    # existed. One exists now, so what must hold is that the boundary is a domain interface and
    # the generated adapter is what implements it.
    adapter = read(
        "core/network/src/androidMain/kotlin/com/servacode/directory/core/network/api/GeneratedPublicApi.kt"
    )
    require("interface PublicApiBoundary" in boundary, "public boundary interface missing")
    require(
        "PublicApiBoundary" in adapter and "com.servacode.directory.api" in adapter,
        "public boundary must be implemented over the generated client",
    )
    # The pre-P10 unbound client is gone: there is a generated one, and the app fails through
    # the error envelope rather than through a placeholder exception.
    require("AppException" in boundary, "public boundary must surface failures as AppException")
    lowered = boundary.lower()
    require("fake" not in lowered and "mock" not in lowered, "fake production data marker found")
    require("Dto" not in boundary, "hand-authored transport DTO detected")


def check_routes() -> None:
    app = read("app/src/main/kotlin/com/servacode/directory/DirectoryApp.kt")
    for route in (
        "Home",
        "ProvincePicker",
        "Search",
        "FacilityDetailRoute",
        "Map",
        "Account",
        "MyRatings",
    ):
        require(f"DirectoryRoute.{route}" in app, f"public route not wired: {route}")


def check_ratings() -> None:
    validator = read(
        "feature/ratings/src/commonMain/kotlin/com/servacode/directory/feature/ratings/RatingValidator.kt"
    )
    require("stars in 1..5" in validator, "rating range invariant missing")
    require(
        (ROOT / "feature/ratings/src/androidHostTest/kotlin/com/servacode/directory/feature/ratings/RatingValidatorTest.kt").exists(),
        "rating validator test missing",
    )


def check_architecture() -> None:
    for feature in PUBLIC:
        source = feature_text(feature)
        viewmodels = [p for p in (ROOT / "feature" / feature).rglob("*ViewModel.kt")]
        require(viewmodels, f"ViewModel source missing: {feature}")
        vm_text = "\n".join(p.read_text(encoding="utf-8") for p in viewmodels)
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
    # Gradle's own build output is not this project's source; a fresh checkout has none of it.
    sources = [path for path in sources if "build" not in path.parts]
    provider_policy = ROOT / (
        "core/maps/src/commonMain/kotlin/com/servacode/directory/core/maps/NavigationModels.kt"
    )
    for path in sources:
        text = path.read_text(encoding="utf-8")
        require("ACCESS_BACKGROUND_LOCATION" not in text, f"background location found: {path}")
        if path != provider_policy and not TEST_SOURCE_SET.search(path.as_posix()):
            require(not mentions(text, "demotiles.maplibre.org"), f"demo tiles found: {path}")
        if path.name != "DirectoryTokens.kt":
            require(not re.search(r"#[0-9A-Fa-f]{6,8}", text), f"hardcoded UI color: {path}")
        for line_no, line in enumerate(text.splitlines(), 1):
            # A vector path is one geometric value; wrapping it would only make it unreadable.
            if "android:pathData" in line:
                continue
            # A shared word is one value too: Compose resources keep a line break and its
            # indentation as written, where Android's own resources folded them into a space
            # (DECISION-094), so a long sentence stays on one line.
            if "/composeResources/values/" in path.as_posix() and "<string " in line:
                continue
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
