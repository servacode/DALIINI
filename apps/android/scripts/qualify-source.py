from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]

EXPECTED_MODULES = {
    ":app",
    ":core:model",
    ":core:network",
    ":core:database",
    ":core:datastore",
    ":core:auth",
    ":core:designsystem",
    ":core:location",
    ":core:maps",
    ":core:analytics",
    ":core:observability",
    ":core:testing",
    ":feature:bootstrap",
    ":feature:home",
    ":feature:province",
    ":feature:search",
    ":feature:directory",
    ":feature:facility",
    ":feature:map",
    ":feature:navigation",
    ":feature:auth",
    ":feature:account",
    ":feature:ratings",
    ":feature:owner",
    ":feature:onboarding",
    ":feature:duty",
    ":feature:settings",
}


ARABIC_LETTER = re.compile("[\u0600-\u06ff\u0750-\u077f\ufb50-\ufeff]")
ARABIC_CHAR_LITERAL = re.compile("'[\u0600-\u06ff\u0750-\u077f\ufb50-\ufeff]'")


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def text(relative: str) -> str:
    return (ROOT / relative).read_text()


def module_path(module: str) -> Path:
    return ROOT / module.removeprefix(":").replace(":", "/")


def check_modules() -> None:
    settings = text("settings.gradle.kts")
    found = set(re.findall(r'include\("(:[^"\n]+)"\)', settings))
    require(found == EXPECTED_MODULES, f"module graph mismatch: {sorted(found ^ EXPECTED_MODULES)}")
    for module in EXPECTED_MODULES:
        require((module_path(module) / "build.gradle.kts").exists(), f"missing build file: {module}")


def check_sdk_policy() -> None:
    app = text("build-logic/src/main/kotlin/serva.android.application.gradle.kts")
    library = text("build-logic/src/main/kotlin/serva.android.library.gradle.kts")
    for source in (app, library):
        # 36 or higher; 37 since the pinned libraries required it (DECISION-040).
        require("compileSdk = 37" in source, "compileSdk must be 37")
        require("minSdk = 24" in source, "minSdk must be 24")
    require("targetSdk = 36" in app, "targetSdk must be 36")
    require('applicationId = "com.servacode.directory"' in app, "applicationId mismatch")


def check_security() -> None:
    manifest = text("app/src/main/AndroidManifest.xml")
    require("ACCESS_BACKGROUND_LOCATION" not in manifest, "background location is forbidden in Core V3")
    require('android:usesCleartextTraffic="false"' in manifest, "cleartext traffic must be disabled")
    vault = text(
        "core/auth/src/main/kotlin/com/servacode/directory/core/auth/AndroidKeyStoreRefreshTokenVault.kt"
    )
    require('KEYSTORE = "AndroidKeyStore"' in vault, "Android Keystore is required")
    require('TRANSFORMATION = "AES/GCM/NoPadding"' in vault, "AES/GCM is required")
    lowered = vault.lower().replace(" ", "")
    require(
        ".putstring(\"refresh_token\"" not in lowered,
        "raw refresh token preference storage is forbidden",
    )
    coordinator = text("core/auth/src/main/kotlin/com/servacode/directory/core/auth/SessionCoordinator.kt")
    require("Mutex()" in coordinator and "withLock" in coordinator, "refresh mutex missing")
    access = text("core/auth/src/main/kotlin/com/servacode/directory/core/auth/AccessTokenStore.kt")
    require("AtomicReference" in access, "access token must be memory-backed")


def check_architecture() -> None:
    boundary = text(
        "core/network/src/main/kotlin/com/servacode/directory/core/network/GeneratedApiClientBoundary.kt"
    )
    require("Transport DTOs must not be duplicated" in boundary, "generated client boundary missing")
    routes = text("core/model/src/main/kotlin/com/servacode/directory/core/model/DirectoryRoute.kt")
    for route in (
        "Welcome",
        "LocationPermission",
        "Home",
        "ProvincePicker",
        "Search",
        "Directory",
        "FacilityDetailRoute",
        "Map",
        "BuiltInNavigation",
        "Login",
        "Register",
        "Recovery",
        "Account",
        "MyRatings",
        "MyFacilities",
        "Onboarding",
        "ManageFacility",
        "Duty",
        "Settings",
    ):
        require(route in routes, f"missing route: {route}")
    theme = text(
        "core/designsystem/src/main/kotlin/"
        "com/servacode/directory/core/designsystem/DirectoryTheme.kt"
    )
    require("LayoutDirection.Rtl" in theme, "RTL must be first-class")
    design_gradle = text("core/designsystem/build.gradle.kts")
    require(
        "packages/design-tokens/generated" in design_gradle,
        "canonical design tokens not wired",
    )
    require(
        "Composable" in text(
            "feature/bootstrap/src/main/kotlin/"
            "com/servacode/directory/feature/bootstrap/BootstrapScreen.kt"
        ),
        "bootstrap composable missing",
    )
    require(
        "ViewModel" in text(
            "feature/bootstrap/src/main/kotlin/"
            "com/servacode/directory/feature/bootstrap/BootstrapViewModel.kt"
        ),
        "bootstrap ViewModel missing",
    )
    require(
        "UseCase" in text(
            "feature/bootstrap/src/main/kotlin/"
            "com/servacode/directory/feature/bootstrap/BootstrapUseCase.kt"
        ),
        "bootstrap use case missing",
    )
    require(
        "Repository" in text(
            "feature/bootstrap/src/main/kotlin/"
            "com/servacode/directory/feature/bootstrap/BootstrapRepository.kt"
        ),
        "bootstrap repository missing",
    )
    # The splash is shown while bootstrap runs, so bootstrap reads the device only: a network
    # call there would make the app's start wait on the network.
    bootstrap_sources = "\n".join(
        path.read_text(encoding="utf-8")
        for path in (ROOT / "feature/bootstrap/src/main").rglob("*.kt")
    )
    require(
        ":core:network" not in text("feature/bootstrap/build.gradle.kts")
        and "core.network" not in bootstrap_sources,
        "bootstrap and the splash must not depend on the network",
    )


# Build outputs are not source: after a real build they hold compiled, binary XML (INT-014).
GENERATED = {"build", ".gradle", ".kotlin"}


def source_paths(pattern: str) -> list[Path]:
    return [
        path for path in ROOT.rglob(pattern)
        if not GENERATED.intersection(path.relative_to(ROOT).parts)
    ]


def check_design_system() -> None:
    """One app, one set of shapes.

    Four screens had grown their own private `Section`, six had reached for `Surface` to draw
    the same soft green field or the same outlined card, and the design system had two
    components for the line above a group. A shape drawn again in every file drifts in every
    file, and is then corrected one element at a time. A screen that needs a shape the design
    system does not have adds it there, where the next screen inherits it.
    """
    features = [path for path in source_paths("*.kt") if "feature" in path.relative_to(ROOT).parts]
    own_containers = [
        f"{path.relative_to(ROOT)}"
        for path in features
        if "import androidx.compose.material3.Surface" in path.read_text(encoding="utf-8")
    ]
    require(not own_containers, f"features drawing their own containers: {own_containers}")
    own_sections = [
        f"{path.relative_to(ROOT)}"
        for path in features
        if "private fun Section(" in path.read_text(encoding="utf-8")
    ]
    require(not own_sections, f"features with a section of their own: {own_sections}")


def check_words() -> None:
    """A screen's words live in its resources, not in its Kotlin.

    Every module reads what it says from its own `res/values/strings.xml`, which is what makes a
    second language a second file: Android picks the file that matches the phone's setting and no
    Kotlin changes. This gate is what keeps it that way — 315 sentences had grown across
    seventeen modules before they were moved, and one literal added back is how that starts again.

    What is allowed through:

    * comments, which explain the words and are not shown to anyone;
    * the app's own name, a proper noun that must match the launcher's label (`DirectoryBrand`);
    * single-character literals, which are the rules about writing rather than writing: the voice
      decides whether a street name can be read aloud by looking at its characters.
    """
    offenders: list[str] = []
    for path in source_paths("*.kt"):
        parts = path.relative_to(ROOT).parts
        # Tests name Arabic places and read Arabic answers back: that is the data under test,
        # not what the app says. The harness's `connectedTest` sends real names to a real server.
        if any("test" in part.lower() for part in parts):
            continue
        if path.name == "DirectoryBrand.kt":
            continue
        for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            stripped = line.strip()
            if stripped.startswith(("*", "//", "/*")):
                continue
            without_chars = ARABIC_CHAR_LITERAL.sub("", line)
            if ARABIC_LETTER.search(without_chars):
                offenders.append(f"{path.relative_to(ROOT)}:{number}")
    require(not offenders, f"words in Kotlin rather than in resources: {offenders}")


def check_hygiene() -> None:
    source_files = [*source_paths("*.kt"), *source_paths("*.kts"), *source_paths("*.xml")]
    combined = "\n".join(path.read_text(encoding="utf-8") for path in source_files)
    require("React Native" not in combined, "React Native reference found in Android source")
    require("Flutter" not in combined, "Flutter reference found in Android source")
    # P21 may add an env-only release signing configuration. The foundation gate
    # forbids embedded signing material rather than forbidding the configuration itself.
    app_gradle = text("app/build.gradle.kts")
    if "signingConfig" in app_gradle:
        for env_name in (
            "ANDROID_UPLOAD_KEYSTORE_PATH",
            "ANDROID_UPLOAD_KEY_ALIAS",
            "ANDROID_UPLOAD_STORE_PASSWORD",
            "ANDROID_UPLOAD_KEY_PASSWORD",
        ):
            require(env_name in app_gradle, f"env-only signing contract missing: {env_name}")
        require('storePassword = "' not in app_gradle, "hardcoded store password forbidden")
        require('keyPassword = "' not in app_gradle, "hardcoded key password forbidden")
    forbidden = ("AIza", "BEGIN PRIVATE KEY", "DATABASE_URL=", "api_secret")
    for marker in forbidden:
        require(marker not in combined, f"possible secret marker found: {marker}")
    hardcoded_hex = []
    for path in source_files:
        if path.name == "DirectoryTokens.kt":
            continue
        for line_no, line in enumerate(path.read_text().splitlines(), 1):
            if re.search(r'#[0-9A-Fa-f]{6,8}', line):
                hardcoded_hex.append(f"{path.relative_to(ROOT)}:{line_no}")
    require(not hardcoded_hex, f"hardcoded Android UI colors: {hardcoded_hex}")


def main() -> int:
    checks = [
        check_modules,
        check_sdk_policy,
        check_security,
        check_architecture,
        check_design_system,
        check_words,
        check_hygiene,
    ]
    for check in checks:
        check()
        print(f"PASS {check.__name__}")
    print(f"PASS Android source qualification ({len(EXPECTED_MODULES)} modules)")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(f"FAIL {exc}", file=sys.stderr)
        raise SystemExit(1)
