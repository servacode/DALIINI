// Shared with the iPhone app (DECISION-086).
plugins {
    id("serva.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            // Dates on Damascus clocks are part of this module's API.
            api(libs.kotlinx.datetime)
        }
    }
}
