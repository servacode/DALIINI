// Shared with the iPhone app (DECISION-089): the settings feature's repositories, use cases and
// rules in common code; its screens, view models and strings in androidMain.
plugins {
    id("serva.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:network"))
            // The province's offline map pack, and the preference that remembers a refusal.
            implementation(project(":core:maps"))
            implementation(project(":core:datastore"))
            // The emergency numbers kept for when there is no connection.
            implementation(project(":core:database"))
            implementation(project(":core:inject"))
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(project(":core:designsystem"))
            implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.compose.ui)
            implementation(libs.androidx.material3)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(libs.kotlinx.coroutines.test)
            implementation(project(":core:testing"))
        }
    }
}
