// Shared with the iPhone app (DECISION-089): the province feature's repositories, use cases and
// rules in common code; its screens, view models and strings in androidMain.
plugins {
    id("serva.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:network"))
            implementation(project(":core:database"))
            implementation(project(":core:datastore"))
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
            implementation(project(":core:testing"))
        }
    }
}
