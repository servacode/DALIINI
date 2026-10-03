// Shared with the iPhone app (DECISION-087): the preferences' shape and contract in common code;
// the DataStore that keeps them on Android, and the stored anonymous id, in androidMain.
plugins {
    id("serva.kmp.hilt")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            // Only for the `AnonymousId` contract the stored id implements.
            implementation(project(":core:analytics"))
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.kotlinx.coroutines.android)
        }
    }
}
