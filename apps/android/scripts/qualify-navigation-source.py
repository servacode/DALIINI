from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def read(relative: str) -> str:
    return (ROOT / relative).read_text()


def nav_text() -> str:
    base = ROOT / "feature/navigation/src"
    return "\n".join(path.read_text() for path in base.rglob("*.kt"))


def check_provider_configuration() -> None:
    app = read("app/build.gradle.kts")
    models = read(
        "core/maps/src/main/kotlin/com/servacode/directory/core/maps/NavigationModels.kt"
    )
    adapters = read(
        "core/network/src/main/kotlin/com/servacode/directory/core/network/MapProviderAdapters.kt"
    )
    for key in (
        "DIRECTORY_ROUTING_BASE_URL",
        "DIRECTORY_GEOCODING_BASE_URL",
        "DIRECTORY_GEOCODING_USER_AGENT",
    ):
        require(key in app, f"missing configurable provider input: {key}")
    require("router.project-osrm.org" in models, "public OSRM demo host policy missing")
    require("requireConfiguredHttps" in models, "HTTPS provider policy missing")
    require("OsrmRoutingProvider" in adapters, "OSRM adapter missing")
    require("NominatimGeocodingProvider" in adapters, "Nominatim adapter missing")
    require('addQueryParameter("accept-language", "ar")' in adapters, "Arabic geocoding missing")
    require('header("User-Agent"' in adapters, "geocoding user-agent policy missing")


def check_navigation_engine() -> None:
    engine = read(
        "feature/navigation/src/main/kotlin/com/servacode/directory/feature/navigation/NavigationEngine.kt"
    )
    models = read(
        "core/maps/src/main/kotlin/com/servacode/directory/core/maps/NavigationModels.kt"
    )
    for state in ("Idle", "Routing", "Navigating", "Rerouting", "Arrived", "Error"):
        require(state in engine, f"navigation state missing: {state}")
    for field in (
        "offRouteMeters",
        "arrivalMeters",
        "maneuverAdvanceMeters",
        "rerouteCooldownMillis",
        "remainingDistanceMeters",
        "remainingDurationSeconds",
        "offRouteDistanceMeters",
    ):
        require(field in engine, f"navigation invariant missing: {field}")
    require("distanceToPolylineMeters" in models, "off-route geometry calculation missing")
    require("latitude in -90.0..90.0" in models, "latitude validation missing")
    require("longitude in -180.0..180.0" in models, "longitude validation missing")


def check_voice_and_map() -> None:
    source = nav_text()
    require("ArabicManeuverPhraseBuilder" in source, "Arabic maneuver builder missing")
    require("TextToSpeech" in source and 'Locale("ar")' in source, "Arabic native TTS missing")
    require("AndroidView" in source and "MapView" in source, "MapLibre native navigation view missing")
    require("WebView" not in source, "WebView navigation is forbidden")
    require("showRoute" in source, "route rendering missing")
    require("showNavigationLocation" in source, "navigation location rendering missing")
    require("mutableStateOf<MapLibreMap?>" in source, "MapLibre async state is not Compose-observable")


def check_location_policy() -> None:
    manifest = read("app/src/main/AndroidManifest.xml")
    location = read(
        "core/location/src/main/kotlin/com/servacode/directory/core/location/AndroidLocationProvider.kt"
    )
    require("ACCESS_BACKGROUND_LOCATION" not in manifest, "background location permission is forbidden")
    require("requestLocationUpdates" in location, "foreground navigation updates missing")
    require("callbackFlow" in location and "awaitClose" in location, "location flow cleanup missing")


def check_product_integration() -> None:
    app = read("app/src/main/kotlin/com/servacode/directory/DirectoryApp.kt")
    facility = read(
        "feature/facility/src/main/kotlin/com/servacode/directory/feature/facility/FacilityScreen.kt"
    )
    require("BuiltInNavigationScreen" in app, "built-in navigation destination not wired")
    require("DirectoryRoute.BuiltInNavigation" in app, "navigation route not wired")
    require("onDirections" in facility and 'Text("الاتجاهات")' in facility, "facility directions action missing")


def check_tests() -> None:
    expected = (
        "core/maps/src/test/kotlin/com/servacode/directory/core/maps/NavigationModelsTest.kt",
        "feature/navigation/src/test/kotlin/com/servacode/directory/feature/navigation/NavigationEngineTest.kt",
        "feature/navigation/src/test/kotlin/com/servacode/directory/feature/navigation/ArabicManeuverPhraseBuilderTest.kt",
    )
    for relative in expected:
        require((ROOT / relative).exists(), f"navigation test missing: {relative}")


def check_hygiene() -> None:
    sources = [
        *ROOT.joinpath("core/maps").rglob("*.kt"),
        *ROOT.joinpath("core/network").rglob("*.kt"),
        *ROOT.joinpath("core/location").rglob("*.kt"),
        *ROOT.joinpath("feature/navigation").rglob("*.kt"),
        ROOT / "app/build.gradle.kts",
    ]
    policy = ROOT / "core/maps/src/main/kotlin/com/servacode/directory/core/maps/NavigationModels.kt"
    runtime_sources = [
        path for path in sources
        if path != policy and "/src/test/" not in path.as_posix()
    ]
    combined = "\n".join(path.read_text() for path in runtime_sources)
    require("demotiles.maplibre.org" not in combined, "MapLibre demo tiles runtime reference found")
    require("router.project-osrm.org" not in combined, "public OSRM demo endpoint hardcoded")
    require("ACCESS_BACKGROUND_LOCATION" not in combined, "background location reference found")
    for path in sources:
        for line_no, line in enumerate(path.read_text().splitlines(), 1):
            require(len(line) <= 120, f"line >120: {path.relative_to(ROOT)}:{line_no}")
        if path.name != "DirectoryTokens.kt":
            require(not re.search(r"#[0-9A-Fa-f]{6,8}", path.read_text()), f"hardcoded color: {path}")


def main() -> int:
    checks = (
        check_provider_configuration,
        check_navigation_engine,
        check_voice_and_map,
        check_location_policy,
        check_product_integration,
        check_tests,
        check_hygiene,
    )
    for check in checks:
        check()
        print(f"PASS {check.__name__}")
    print("PASS P17 maps/navigation source qualification")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(f"FAIL {exc}", file=sys.stderr)
        raise SystemExit(1)
