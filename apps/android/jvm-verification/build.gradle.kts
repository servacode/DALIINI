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
    "**/core/auth/AndroidKeyStoreRefreshTokenVault.kt",
    "**/core/auth/AuthBindings.kt",
    "**/core/network/NetworkMonitor.kt",
    "**/core/network/UploadReader.kt",
    "**/core/network/NetworkBindings.kt",
    "**/core/network/MapProviderAdapters.kt",
    "**/core/network/MapProviderNetworkModule.kt",
    "**/core/database/CacheDao.kt",
    "**/core/database/CacheEntities.kt",
    "**/core/database/DatabaseModule.kt",
    "**/core/database/DirectoryDatabase.kt",
    "**/core/database/PublicCacheDataSource.kt",
    "**/core/datastore/PreferencesRepository.kt",
    "**/core/location/AndroidLocationProvider.kt",
    "**/core/location/LocationModule.kt",
    "**/core/maps/MapLibreController.kt",
    "**/core/maps/NavigationLayers.kt",
    "**/core/maps/MapViewLifecycle.kt",
    // A `…Copy` reads the module's own strings.xml, which is Android and not Kotlin.
    "**/*Copy.kt",
    "**/*Screen.kt",
    "**/*Screens.kt",
    "**/*ViewModel.kt",
    "**/*ViewModels.kt",
    "**/feature/bootstrap/BootstrapModule.kt",
    "**/feature/navigation/NavigationMap.kt",
    "**/feature/navigation/AndroidNavigationVoice.kt",
    "**/feature/navigation/NavigationVoice.kt",
    "**/feature/onboarding/OnboardingMapPicker.kt",
)

// Feature modules whose repositories and use cases are platform-free.
val features = listOf(
    "account", "auth", "bootstrap", "directory", "duty", "facility", "home", "map", "navigation",
    "onboarding", "owner", "province", "ratings", "search",
)

sourceSets {
    main {
        kotlin.srcDir(generatedClient)
        for (core in listOf("model", "observability", "auth", "network", "database", "datastore", "location", "maps")) {
            kotlin.srcDir(android.resolve("core/$core/src/main/kotlin"))
        }
        for (feature in features) {
            kotlin.srcDir(android.resolve("feature/$feature/src/main/kotlin"))
        }
        kotlin.exclude(androidOnly)
    }
    test {
        kotlin.srcDir(android.resolve("core/testing/src/main/kotlin"))
        kotlin.srcDir(android.resolve("core/auth/src/test/kotlin"))
        kotlin.srcDir(android.resolve("core/network/src/test/kotlin"))
        kotlin.srcDir(android.resolve("core/database/src/test/kotlin"))
        kotlin.srcDir(android.resolve("core/model/src/test/kotlin"))
        kotlin.srcDir(android.resolve("core/maps/src/test/kotlin"))
        for (feature in features) {
            kotlin.srcDir(android.resolve("feature/$feature/src/test/kotlin"))
        }
        // ViewModels are not compiled here (androidOnly), so neither are their tests. The same
        // goes for anything that reaches into the design system: it is a Compose library and
        // this harness has no Android framework. Those tests run in the Android unit suite.
        kotlin.exclude("**/*ViewModelTest.kt", "**/OwnerStatusToneTest.kt")
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
