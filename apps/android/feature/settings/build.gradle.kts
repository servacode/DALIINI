// Shared with the iPhone app (DECISIONS 089 and 095): the settings feature's repositories, use
// cases, screens, view models and words in common code; Hilt's view models in androidMain.
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
            implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(libs.kotlinx.coroutines.test)
            implementation(project(":core:testing"))
        }
    }
}
