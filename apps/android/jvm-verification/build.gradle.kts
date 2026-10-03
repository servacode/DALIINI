import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val android = rootDir.resolve("..")
val generatedClient = rootDir.resolve("../../../packages/api-kotlin/generated/src/main/kotlin")

// Files that need the Android framework, an AAR, Room, DataStore or Compose. Everything else
// in these directories is compiled here exactly as the app compiles it.
val androidOnly = listOf(
    // Room and DataStore come from Google Maven, which this harness does without. Their code is
    // common since DECISION-092 and tested on Android and on the iPhone simulator instead; this
    // list keeps the harness exactly where it was when that code sat in androidMain.
    "**/core/database/DirectoryDatabase.kt",
    "**/core/database/CacheDao.kt",
    "**/core/database/CacheEntities.kt",
    "**/core/database/LocalStoresRoom.kt",
    "**/core/database/PublicCacheDataSource.kt",
    "**/core/datastore/DirectoryDataStore.kt",
    "**/core/datastore/PreferencesRepository.kt",
    "**/core/datastore/StoredAnonymousId.kt",
    "**/core/network/AndroidNetworkMonitor.kt",
    "**/core/network/UploadReader.kt",
    "**/core/network/NetworkBindings.kt",
    // Builds its Retrofit service from the generated client, which is an AAR here.
    "**/core/network/api/GeneratedAnalyticsTransport.kt",
    "**/core/network/MapProviderAdapters.kt",
    "**/core/network/MapProviderNetworkModule.kt",
    "**/core/maps/MapLibreController.kt",
    "**/core/maps/OfflineMapPacks.kt",
    "**/core/maps/NavigationLayers.kt",
    "**/core/maps/MapViewLifecycle.kt",
    // A `…Copy` reads the module's own strings.xml, which is Android and not Kotlin.
    "**/*Copy.kt",
    "**/*Screen.kt",
    "**/*Screens.kt",
    "**/*ViewModel.kt",
    "**/*ViewModels.kt",
    // Where Hilt meets a shared screen (DECISION-095), in a feature's androidMain.
    "**/feature/*/*Route.kt",
    "**/feature/*/*Routes.kt",
    "**/feature/bootstrap/BootstrapModule.kt",
    // Reads the built-in emergency lines' names from the settings module's strings.xml.
    "**/feature/settings/EmergencyLabelsModule.kt",
    "**/feature/navigation/NavigationMap.kt",
    "**/feature/navigation/NavigationWords.kt",
    // A table of R ids for the voice pack: Android resources, not Kotlin.
    "**/feature/navigation/NavigationClipResources.kt",
    "**/feature/navigation/AndroidNavigationVoice.kt",
    "**/feature/navigation/NavigationVoice.kt",
    "**/feature/onboarding/OnboardingMapPicker.kt",
)

// Feature modules whose repositories and use cases are platform-free.
val features = listOf(
    "account", "auth", "bootstrap", "duty", "facility", "home", "map", "navigation",
    "onboarding", "owner", "province", "ratings", "search", "settings",
)

// Kotlin Multiplatform modules shared with the iPhone app (DECISIONS 085 to 087): their common
// code and its tests are compiled here too, so the JVM harness keeps seeing the whole app. Their
// androidMain, where the Keystore and Hilt's modules live, is not.
val sharedCores = listOf(
    "model", "observability", "analytics", "auth", "database", "datastore", "location", "network",
    "maps",
)

// Shared modules whose Android side is largely plain JVM code worth compiling here too, less
// the files in `androidOnly`: the network's transport is Retrofit and OkHttp on the generated
// client (DECISION-088), and the map's rules sit beside MapLibre (DECISION-089).
val sharedCoresWithJvmAndroidSide = listOf("network", "maps")

sourceSets {
    main {
        kotlin.srcDir(generatedClient)
        for (core in sharedCores) {
            kotlin.srcDir(android.resolve("core/$core/src/commonMain/kotlin"))
        }
        for (core in sharedCoresWithJvmAndroidSide) {
            kotlin.srcDir(android.resolve("core/$core/src/androidMain/kotlin"))
        }
        // The features are multiplatform too (DECISION-089); their Android side is compiled here,
        // less `androidOnly`, exactly as before the split.
        for (feature in features) {
            kotlin.srcDir(android.resolve("feature/$feature/src/commonMain/kotlin"))
            kotlin.srcDir(android.resolve("feature/$feature/src/androidMain/kotlin"))
        }
        kotlin.exclude(androidOnly)
    }
    test {
        // The tests' fakes, common since DECISION-089, and the JUnit rule beside them.
        kotlin.srcDir(android.resolve("core/testing/src/commonMain/kotlin"))
        kotlin.srcDir(android.resolve("core/testing/src/androidMain/kotlin"))
        for (core in sharedCores) {
            kotlin.srcDir(android.resolve("core/$core/src/commonTest/kotlin"))
        }
        for (core in sharedCoresWithJvmAndroidSide) {
            kotlin.srcDir(android.resolve("core/$core/src/androidHostTest/kotlin"))
        }
        for (feature in features) {
            kotlin.srcDir(android.resolve("feature/$feature/src/androidHostTest/kotlin"))
        }
        // ViewModels are not compiled here (androidOnly), so neither are their tests. The same
        // goes for anything that reaches into the design system: it is a Compose library and
        // this harness has no Android framework. Those tests run in the Android unit suite.
        kotlin.exclude("**/*ViewModelTest.kt", "**/OwnerStatusToneTest.kt")
        // Reads its module's Compose resources by path, from the module's own directory.
        kotlin.exclude("**/RosterWeekdaysTest.kt")
        // The preferences' tests drive DataStore itself (see `androidOnly`).
        kotlin.exclude(
            "**/core/datastore/MemoryDataStore.kt",
            "**/core/datastore/PreferencesRepositoryTest.kt",
            "**/core/datastore/StoredAnonymousIdTest.kt",
        )
    }
}

// The connected suite drives the generated client against a running backend. It is its own
// source set and task so that `test` never needs a server.
val connectedTest: SourceSet = sourceSets.create("connectedTest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}
configurations[connectedTest.implementationConfigurationName]
    .extendsFrom(configurations.testImplementation.get())
configurations[connectedTest.runtimeOnlyConfigurationName]
    .extendsFrom(configurations.testRuntimeOnly.get())

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.atomicfu)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.retrofit.converter.scalars)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.javax.inject)
    // Hilt module declarations compile against the annotations; nothing here runs Hilt.
    compileOnly("com.google.dagger:dagger:${libs.versions.hilt.get()}")
    compileOnly("com.google.dagger:hilt-core:${libs.versions.hilt.get()}")

    testImplementation(libs.junit)
    // The shared modules' tests are written against kotlin.test, which runs on JUnit here.
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}

tasks.test {
    useJUnit()
}

tasks.register<Test>("connectedCheck") {
    description = "Runs the generated client against the backend at DIRECTORY_API_BASE_URL."
    group = "verification"
    testClassesDirs = connectedTest.output.classesDirs
    classpath = connectedTest.runtimeClasspath
    useJUnit()
    // Every run talks to a live server; a cached result would prove nothing.
    outputs.upToDateWhen { false }
    environment("DIRECTORY_API_BASE_URL", System.getenv("DIRECTORY_API_BASE_URL") ?: "")
    // The backend container, for the test-only OTP helper; see ConnectedStack.setOtp.
    environment("E2E_API_CONTAINER", System.getenv("E2E_API_CONTAINER") ?: "e2e-api")
    // The Android -> Admin -> Android hand-off runs in phases; see HandoffConnectedTest.
    environment("HANDOFF_PHASE", System.getenv("HANDOFF_PHASE") ?: "")
    testLogging {
        events("passed", "failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
