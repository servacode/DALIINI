// Shared with the iPhone app (DECISIONS 089 and 095): the home feature's repositories, use
// cases, screen, view models and words in common code; Hilt's view models and the permission
// dialog in androidMain, Core Location's in iosMain.
plugins {
    id("serva.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:network"))
            implementation(project(":core:analytics"))
            implementation(project(":core:database"))
            implementation(project(":core:datastore"))
            implementation(project(":core:location"))
            implementation(project(":core:inject"))
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(project(":core:testing"))
        }
    }
}
