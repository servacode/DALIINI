// Shared with the iPhone app (DECISION-088). commonMain holds what every repository speaks: the
// boundaries and their inputs, realtime models, maintenance state, push registration, sign-out
// and the place-name resolver. androidMain holds Android's transport, Retrofit and OkHttp on
// the generated JVM client, with the map providers and the Hilt modules.
plugins {
    id("serva.kmp.hilt")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:auth"))
            implementation(project(":core:inject"))
            implementation(project(":core:observability"))
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.atomicfu)
        }
        androidMain {
            // The generated P10 client is compiled as part of this module, the same way
            // `:core:designsystem` compiles the generated design tokens: no copy, no hand-written
            // DTOs. Only `api/` in this module may reference it.
            kotlin.srcDir(rootProject.file("../../packages/api-kotlin/generated/src/main/kotlin"))
            dependencies {
                // Only for the `AnalyticsTransport` contract the generated sender implements.
                implementation(project(":core:analytics"))
                implementation(project(":core:maps"))
                implementation(libs.retrofit.core)
                implementation(libs.retrofit.converter.kotlinx.serialization)
                implementation(libs.retrofit.converter.scalars)
                implementation(libs.okhttp.core)
                implementation(libs.okhttp.logging)
                implementation(libs.kotlinx.coroutines.android)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.okhttp.mockwebserver)
        }
    }
}
