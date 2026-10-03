// Shared with the iPhone app (DECISION-087): the session and its token stores in common code;
// the Keystore vault and the Hilt bindings in androidMain.
plugins {
    id("serva.kmp.hilt")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:inject"))
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
