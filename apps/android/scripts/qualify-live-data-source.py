from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def read(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


def check_realtime_contract() -> None:
    models = read("core/network/src/commonMain/kotlin/com/servacode/directory/core/network/RealtimeModels.kt")
    stream = read("core/network/src/commonMain/kotlin/com/servacode/directory/core/network/RealtimeStream.kt") + read(
        "core/network/src/androidMain/kotlin/com/servacode/directory/core/network/OkHttpRealtimeStream.kt"
    )
    for event in (
        "public.province.configuration_changed",
        "public.facility.changed",
        "public.facility.availability_changed",
        "public.duty.changed",
        "user.application.changed",
        "user.facility.changed",
        "admin.review_queue.changed",
        "admin.system.changed",
    ):
        require(event in models, f"realtime catalog event missing: {event}")
    require("FacilityDetail" not in models and "OwnerFacility" not in models, "event envelope leaks domain objects")
    require("subscribeProvince" in stream, "province subscription missing")
    require("authenticate" in stream and "subscribeUser" in stream, "post-connect user auth/subscription missing")
    require("accessToken" in stream, "post-connect auth payload missing")
    require("?token=" not in stream and "Authorization" not in read("app/build.gradle.kts"), "query token path found")


def check_reconnect_and_lifecycle() -> None:
    policy = read("core/network/src/commonMain/kotlin/com/servacode/directory/core/network/ReconnectPolicy.kt")
    coordinator = read("app/src/main/kotlin/com/servacode/directory/RealtimeCoordinator.kt")
    monitor = read("core/network/src/androidMain/kotlin/com/servacode/directory/core/network/AndroidNetworkMonitor.kt")
    application = read("app/src/main/kotlin/com/servacode/directory/DirectoryApplication.kt")
    require("baseMillis: Long = 1_000L" in policy, "1s reconnect base missing")
    require("maxMillis: Long = 30_000L" in policy, "30s reconnect cap missing")
    require("jitterFraction" in policy, "reconnect jitter missing")
    require("stableMillis >= 15_000L" in coordinator, "stable-connection reset missing")
    require("collectLatest" in coordinator, "province/network resubscription cancellation missing")
    require("NetworkCapabilities.NET_CAPABILITY_VALIDATED" in monitor, "validated network awareness missing")
    require("ProcessLifecycleOwner" in application, "foreground lifecycle binding missing")


def check_rest_truth_and_offline() -> None:
    for feature, filename in (
        ("home", "HomeViewModel.kt"),
        ("facility", "FacilityViewModel.kt"),
    ):
        source = read(
            f"feature/{feature}/src/androidMain/kotlin/com/servacode/directory/feature/{feature}/{filename}"
        )
        require("RealtimeInvalidationBus" in source, f"realtime invalidation missing: {feature}")
        require("refresh" in source, f"REST refetch path missing: {feature}")
    owner = read("feature/owner/src/androidMain/kotlin/com/servacode/directory/feature/owner/OwnerViewModel.kt")
    # The user-scope rule is stated once, in the shared predicate, rather than repeated in each
    # ViewModel that needs it.
    require("refreshesOwnerState" in owner, "owner user-scope invalidation missing")
    predicate = read(
        "core/network/src/commonMain/kotlin/com/servacode/directory/core/network/RealtimeInvalidation.kt"
    )
    require(
        'ScopeType.USER' in predicate or '"user"' in predicate,
        "owner invalidation predicate must be scoped to the user",
    )
    screens = "\n".join(
        read(path)
        for path in (
            "feature/home/src/androidMain/kotlin/com/servacode/directory/feature/home/HomeScreen.kt",
            "feature/facility/src/androidMain/kotlin/com/servacode/directory/feature/facility/FacilityScreen.kt",
        )
    )
    # Every screen shows the same offline notice from the design system rather than writing its
    # own sentence, so the warning is asserted where it is now written.
    require("DirectoryOfflineNotice" in screens, "offline notice missing from the public screens")
    notice = read(
        "core/designsystem/src/commonMain/kotlin/com/servacode/directory/core/designsystem/States.kt"
    )
    require(
        "Res.string.ds_offline" in notice,
        "the offline notice does not read its sentence from resources",
    )
    # The sentence itself is in the design system's strings.xml, so that a second language is
    # a second file rather than a search through the components. Compose resources since
    # DECISION-094, read the same way on Android and on the iPhone.
    words = read("core/designsystem/src/commonMain/composeResources/values/strings.xml")
    require(
        "قد لا تكون محدثة" in words,
        "offline time-sensitive freshness warning missing",
    )


def check_push_boundary() -> None:
    manifest = read("app/src/main/AndroidManifest.xml")
    push = read(
        "core/network/src/commonMain/kotlin/com/servacode/directory/core/network/PushRegistrationCoordinator.kt"
    )
    boundary = read(
        "core/network/src/commonMain/kotlin/com/servacode/directory/core/network/PushRegistrationBoundary.kt"
    )
    require("POST_NOTIFICATIONS" in manifest, "notification permission declaration missing")
    require("registerAndroidToken" in push and "deactivateAndroidToken" in push, "push token lifecycle missing")
    # A push carries identifiers only: its own id and type, and the ids and day a tap needs to
    # open the right screen. Never words: the app reads those from the inbox over REST.
    allowed = re.search(r"val ALLOWED = setOf\(([^)]*)\)", push)
    keys = set(re.findall(r'"([A-Za-z]+)"', allowed.group(1))) if allowed else set()
    identifiers = {"notificationId", "type", "date", "gapDate", "provinceId", "facilityId"}
    require({"notificationId", "type"} <= keys <= identifiers, "identifier-only push payload policy missing")
    # P10 shipped: the boundary is implemented over the generated client, and a failure travels
    # as the app's own error rather than as a placeholder for a client that does not exist.
    adapter = read(
        "core/network/src/androidMain/kotlin/com/servacode/directory/core/network/api/"
        "GeneratedPushRegistration.kt"
    )
    require("interface PushRegistrationBoundary" in boundary, "push boundary interface missing")
    require(
        "PushRegistrationBoundary" in adapter,
        "push boundary must be implemented over the generated client",
    )
    require("apiKey" not in push and "serverKey" not in push, "push provider secret marker found")


def check_tests_and_hygiene() -> None:
    for relative in (
        "core/network/src/commonTest/kotlin/com/servacode/directory/core/network/ReconnectPolicyTest.kt",
        "core/network/src/commonTest/kotlin/com/servacode/directory/core/network/RealtimeConfigTest.kt",
        "core/network/src/commonTest/kotlin/com/servacode/directory/core/network/RealtimeEventDeduplicatorTest.kt",
        "core/network/src/commonTest/kotlin/com/servacode/directory/core/network/PushMessageDataTest.kt",
    ):
        require((ROOT / relative).exists(), f"P18 test missing: {relative}")
    combined = "\n".join(path.read_text(encoding="utf-8") for path in ROOT.rglob("*.kt") if "build" not in path.parts)
    require("ACCESS_BACKGROUND_LOCATION" not in combined, "background location reintroduced")
    require("firebase_server_key" not in combined.lower(), "Firebase server secret marker found")


def main() -> int:
    checks = (
        check_realtime_contract,
        check_reconnect_and_lifecycle,
        check_rest_truth_and_offline,
        check_push_boundary,
        check_tests_and_hygiene,
    )
    for check in checks:
        check()
        print(f"PASS {check.__name__}")
    print("PASS P18 mobile live-data source qualification")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(f"FAIL {exc}", file=sys.stderr)
        raise SystemExit(1)
