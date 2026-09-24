plugins {
    id("serva.android.application")
    id("serva.android.compose")
    id("serva.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

val apiBaseUrl = providers.gradleProperty("DIRECTORY_API_BASE_URL")
    .orElse(providers.environmentVariable("DIRECTORY_API_BASE_URL"))
    .orElse("https://api.<ROOT_DOMAIN>/")
// The map style, per environment. Local builds use OpenFreeMap's Liberty style, free and
// without a key, for development only (INT-087). Staging and production have their own settings
// and stay unset until a provider is decided; the map refuses to render from the placeholder.
val mapStyleUrl = providers.gradleProperty("DIRECTORY_MAP_STYLE_URL")
    .orElse(providers.environmentVariable("DIRECTORY_MAP_STYLE_URL"))
    .orElse("https://maps.<ROOT_DOMAIN>/style.json")
val stagingMapStyleUrl = providers.gradleProperty("DIRECTORY_STAGING_MAP_STYLE_URL")
    .orElse(providers.environmentVariable("DIRECTORY_STAGING_MAP_STYLE_URL"))
    .orElse("https://maps.<ROOT_DOMAIN>/style.json")
// The province's own map, served from this machine: RahalGo's cartography over vector tiles
// built from the same OpenStreetMap extract, in the local media store beside the photographs.
// Reachable from a phone through `adb reverse tcp:9000`.
val localMapStyleUrl = providers.gradleProperty("DIRECTORY_LOCAL_MAP_STYLE_URL")
    .orElse(providers.environmentVariable("DIRECTORY_LOCAL_MAP_STYLE_URL"))
    .orElse("http://localhost:9000/directory-public/map/style.json")
val routingBaseUrl = providers.gradleProperty("DIRECTORY_ROUTING_BASE_URL")
    .orElse(providers.environmentVariable("DIRECTORY_ROUTING_BASE_URL"))
    .orElse("https://<ROUTING_PROVIDER_HOST>/")
val geocodingBaseUrl = providers.gradleProperty("DIRECTORY_GEOCODING_BASE_URL")
    .orElse(providers.environmentVariable("DIRECTORY_GEOCODING_BASE_URL"))
    .orElse("https://<GEOCODING_PROVIDER_HOST>/")
val geocodingUserAgent = providers.gradleProperty("DIRECTORY_GEOCODING_USER_AGENT")
    .orElse(providers.environmentVariable("DIRECTORY_GEOCODING_USER_AGENT"))
    .orElse("DirectoryPlatformAndroid/1")
val realtimeWebSocketUrl = providers.gradleProperty("DIRECTORY_REALTIME_WS_URL")
    .orElse(providers.environmentVariable("DIRECTORY_REALTIME_WS_URL"))
    .orElse("wss://api.<ROOT_DOMAIN>/ws/v1/directory/")

// Staging has its own addresses, never production's.
val stagingApiBaseUrl = providers.gradleProperty("DIRECTORY_STAGING_API_BASE_URL")
    .orElse(providers.environmentVariable("DIRECTORY_STAGING_API_BASE_URL"))
    .orElse("https://api.staging.<ROOT_DOMAIN>/")
val stagingRealtimeWebSocketUrl = providers.gradleProperty("DIRECTORY_STAGING_REALTIME_WS_URL")
    .orElse(providers.environmentVariable("DIRECTORY_STAGING_REALTIME_WS_URL"))
    .orElse("wss://api.staging.<ROOT_DOMAIN>/ws/v1/directory/")

// A development backend. The default is the Android emulator's alias for the host machine's
// loopback, not any one developer's machine; a phone on a local network overrides it.
val localApiBaseUrl = providers.gradleProperty("DIRECTORY_LOCAL_API_BASE_URL")
    .orElse(providers.environmentVariable("DIRECTORY_LOCAL_API_BASE_URL"))
    .orElse("http://10.0.2.2:8000/")
val localRealtimeWebSocketUrl = providers.gradleProperty("DIRECTORY_LOCAL_REALTIME_WS_URL")
    .orElse(providers.environmentVariable("DIRECTORY_LOCAL_REALTIME_WS_URL"))
    .orElse("ws://10.0.2.2:8000/ws/v1/directory/")

// Firebase client configuration for push. Never committed: it comes from Gradle properties or
// the environment, and when it is absent the app runs with push off.
fun firebase(name: String) = providers.gradleProperty(name)
    .orElse(providers.environmentVariable(name))
    .orElse("")
val firebaseProjectId = firebase("DIRECTORY_FIREBASE_PROJECT_ID")
val firebaseApplicationId = firebase("DIRECTORY_FIREBASE_APPLICATION_ID")
val firebaseApiKey = firebase("DIRECTORY_FIREBASE_API_KEY")
val firebaseSenderId = firebase("DIRECTORY_FIREBASE_SENDER_ID")

// A stable signature for debug builds, so a device keeps its app and data from one build to the
// next (INT-091, DECISION-041). The key is for debug builds only, never the upload or Play key,
// and never in Git: CI decodes it from a GitHub Actions secret, and an authorised machine points
// at its private copy. Without it AGP signs with that machine's own debug key, which cannot
// update an app installed from a build signed with this one.
fun debugSigning(name: String) = providers.gradleProperty(name).orElse(providers.environmentVariable(name))
val debugKeystorePath = debugSigning("DIRECTORY_DEBUG_KEYSTORE_PATH")
val debugKeystorePassword = debugSigning("DIRECTORY_DEBUG_KEYSTORE_PASSWORD")
val debugKeyAlias = debugSigning("DIRECTORY_DEBUG_KEY_ALIAS")
val debugKeyPassword = debugSigning("DIRECTORY_DEBUG_KEY_PASSWORD")

val uploadKeystorePath = providers.environmentVariable("ANDROID_UPLOAD_KEYSTORE_PATH")
val uploadKeyAlias = providers.environmentVariable("ANDROID_UPLOAD_KEY_ALIAS")
val uploadStorePassword = providers.environmentVariable("ANDROID_UPLOAD_STORE_PASSWORD")
val uploadKeyPassword = providers.environmentVariable("ANDROID_UPLOAD_KEY_PASSWORD")


android {
    signingConfigs {
        create("stableDebug") {
            if (debugKeystorePath.isPresent) {
                storeFile = file(debugKeystorePath.get())
                storePassword = debugKeystorePassword.orNull
                keyAlias = debugKeyAlias.orNull
                keyPassword = debugKeyPassword.orNull
            }
        }
        create("release") {
            if (uploadKeystorePath.isPresent) {
                storeFile = file(uploadKeystorePath.get())
                storePassword = uploadStorePassword.orNull
                keyAlias = uploadKeyAlias.orNull
                keyPassword = uploadKeyPassword.orNull
            }
        }
    }

    defaultConfig {
        buildConfigField("String", "ROUTING_BASE_URL", "\"${routingBaseUrl.get()}\"")
        buildConfigField("String", "GEOCODING_BASE_URL", "\"${geocodingBaseUrl.get()}\"")
        buildConfigField("String", "GEOCODING_USER_AGENT", "\"${geocodingUserAgent.get()}\"")
        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"${firebaseProjectId.get()}\"")
        buildConfigField("String", "FIREBASE_APPLICATION_ID", "\"${firebaseApplicationId.get()}\"")
        buildConfigField("String", "FIREBASE_API_KEY", "\"${firebaseApiKey.get()}\"")
        buildConfigField("String", "FIREBASE_SENDER_ID", "\"${firebaseSenderId.get()}\"")
    }

    // Where the app finds its backend. Only `local` may use cleartext, and only towards the
    // hosts its network security config names; the app refuses to start a request to an
    // address that still carries a placeholder.
    flavorDimensions += "environment"
    productFlavors {
        create("local") {
            dimension = "environment"
            applicationIdSuffix = ".local"
            versionNameSuffix = "-local"
            buildConfigField("String", "API_BASE_URL", "\"${localApiBaseUrl.get()}\"")
            buildConfigField("String", "MAP_STYLE_URL", "\"${localMapStyleUrl.get()}\"")
            buildConfigField("String", "REALTIME_WS_URL", "\"${localRealtimeWebSocketUrl.get()}\"")
            buildConfigField("boolean", "ALLOW_CLEARTEXT", "true")
        }
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            buildConfigField("String", "API_BASE_URL", "\"${stagingApiBaseUrl.get()}\"")
            buildConfigField("String", "MAP_STYLE_URL", "\"${stagingMapStyleUrl.get()}\"")
            buildConfigField("String", "REALTIME_WS_URL", "\"${stagingRealtimeWebSocketUrl.get()}\"")
            buildConfigField("boolean", "ALLOW_CLEARTEXT", "false")
        }
        create("production") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl.get()}\"")
            buildConfigField("String", "MAP_STYLE_URL", "\"${mapStyleUrl.get()}\"")
            buildConfigField("String", "REALTIME_WS_URL", "\"${realtimeWebSocketUrl.get()}\"")
            buildConfigField("boolean", "ALLOW_CLEARTEXT", "false")
        }
    }

    buildTypes {
        getByName("debug") {
            if (debugKeystorePath.isPresent) {
                signingConfig = signingConfigs.getByName("stableDebug")
            }
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:auth"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:location"))
    implementation(project(":core:maps"))
    implementation(project(":core:analytics"))
    implementation(project(":core:observability"))
    implementation(project(":feature:bootstrap"))
    implementation(project(":feature:home"))
    implementation(project(":feature:province"))
    implementation(project(":feature:search"))
    implementation(project(":feature:directory"))
    implementation(project(":feature:facility"))
    implementation(project(":feature:map"))
    implementation(project(":feature:navigation"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:account"))
    implementation(project(":feature:ratings"))
    implementation(project(":feature:owner"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:duty"))
    implementation(project(":feature:settings"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.core)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.material3)
    implementation(libs.maplibre.android)
    implementation(libs.firebase.messaging)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}


val validatePlayRelease by tasks.registering {
    group = "verification"
    description = "Fails closed when Play release configuration is incomplete or unsafe."
    doLast {
        val endpoints = mapOf(
            "DIRECTORY_API_BASE_URL" to apiBaseUrl.get(),
            "DIRECTORY_MAP_STYLE_URL" to mapStyleUrl.get(),
            "DIRECTORY_ROUTING_BASE_URL" to routingBaseUrl.get(),
            "DIRECTORY_GEOCODING_BASE_URL" to geocodingBaseUrl.get(),
            "DIRECTORY_REALTIME_WS_URL" to realtimeWebSocketUrl.get(),
        )
        endpoints.forEach { (name, value) ->
            require(!value.contains("<") && !value.contains(">")) {
                "$name still contains a placeholder"
            }
            val secure = value.startsWith("https://") || value.startsWith("wss://")
            require(secure) { "$name must use HTTPS/WSS" }
            require(listOf("localhost", "127.0.0.1", "10.0.2.2").none { it in value }) {
                "$name must not use a local endpoint"
            }
        }
        val requiredSigning = mapOf(
            "ANDROID_UPLOAD_KEYSTORE_PATH" to uploadKeystorePath.orNull,
            "ANDROID_UPLOAD_KEY_ALIAS" to uploadKeyAlias.orNull,
            "ANDROID_UPLOAD_STORE_PASSWORD" to uploadStorePassword.orNull,
            "ANDROID_UPLOAD_KEY_PASSWORD" to uploadKeyPassword.orNull,
        )
        requiredSigning.forEach { (name, value) ->
            require(!value.isNullOrBlank()) { "$name is required for a Play release" }
        }
        require(file(uploadKeystorePath.get()).isFile) {
            "ANDROID_UPLOAD_KEYSTORE_PATH must point to an existing file outside the repository"
        }
    }
}

// Only the production flavor is uploaded to Play.
tasks.matching { it.name == "bundleProductionRelease" || it.name == "assembleProductionRelease" }.configureEach {
    dependsOn(validatePlayRelease)
}
