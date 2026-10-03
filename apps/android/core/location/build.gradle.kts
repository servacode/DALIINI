// Shared with the iPhone app (DECISION-087): the provider's contract in common code, the Android
// provider and its Hilt binding in androidMain.
plugins {
    id("serva.kmp.hilt")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.core)
        }
    }
}
