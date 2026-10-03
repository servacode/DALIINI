// Shared with the iPhone app. The preferences' shape and contract (DECISION-087), and since
// DECISION-092 the DataStore code that keeps them and the anonymous id, are common; each platform
// only says where the file lives: Android from its Context, the iPhone in Application Support.
plugins {
    id("serva.kmp.hilt")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:inject"))
            // Only for the `AnonymousId` contract the stored id implements.
            implementation(project(":core:analytics"))
            api(libs.androidx.datastore.preferences.core)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.kotlinx.coroutines.android)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
