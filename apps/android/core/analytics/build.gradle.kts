// Shared with the iPhone app (DECISION-085).
plugins {
    id("serva.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // The sender is a coroutine collector; the rules it follows are pure and tested
            // without one.
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
