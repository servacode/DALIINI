// The transport the iPhone app talks to the backend through (DECISION-091): the shared
// boundaries of `:core:network`, implemented with Ktor on the multiplatform client of
// `:core:api`. The Android app does not use it yet; it still has Retrofit and OkHttp.
plugins {
    id("serva.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:network"))
            implementation(project(":core:auth"))
            implementation(project(":core:api"))
            // Only for the `AnalyticsTransport` contract the sender implements.
            implementation(project(":core:analytics"))
            implementation(project(":core:observability"))
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
