// Shared with the iPhone app (DECISION-089): the camera, route, pack and geometry rules in
// common code; MapLibre, the offline packs and the composables that host the map in androidMain.
plugins {
    id("serva.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:inject"))
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core)
            implementation(libs.maplibre.android)
            // The offline pack is a singleton the app injects, and it reports progress as a flow.
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.compose.ui)
            implementation(libs.androidx.lifecycle.runtime.compose)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
