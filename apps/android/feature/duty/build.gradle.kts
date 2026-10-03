// Shared with the iPhone app (DECISIONS 089 and 095): the duty feature's repositories, use
// cases, screens, view models and words in common code; Hilt's view models in androidMain.
plugins {
    id("serva.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:network"))
            // The reader's province, for the public roster.
            implementation(project(":core:datastore"))
            implementation(project(":core:inject"))
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
            implementation(libs.androidx.navigation.compose)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(libs.kotlinx.coroutines.test)
            implementation(project(":core:testing"))
        }
    }
}
