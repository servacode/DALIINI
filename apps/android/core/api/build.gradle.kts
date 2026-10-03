// The API client generated for Kotlin Multiplatform (DECISION-090): Ktor and kotlinx-datetime,
// compiled from `packages/api-kotlin-multiplatform` for Android and iOS. It is the transport
// the iPhone app's boundaries are built on; the Android app still uses the JVM client in
// `:core:network`.
plugins {
    id("serva.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain {
            // No copy and no hand-written DTOs: the generated sources are compiled where they are.
            kotlin.srcDir(rootProject.file("../../packages/api-kotlin-multiplatform/generated/src/commonMain/kotlin"))
            dependencies {
                api(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                api(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
