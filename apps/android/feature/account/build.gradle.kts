// Shared with the iPhone app (DECISIONS 089 and 095): the account feature's repositories, use
// cases, screens, view models and words in common code; Hilt's view models in androidMain.
plugins {
    id("serva.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:datastore"))
            implementation(project(":core:database"))
            implementation(project(":core:network"))
            implementation(project(":core:auth"))
            implementation(project(":core:inject"))
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(project(":core:testing"))
        }
    }
}
