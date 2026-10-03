// DaliiniKit, the iPhone app's Kotlin (DECISION-093): the shared layers wired by hand, the
// iPhone's platform parts, and the shell's screens. apps/ios builds it from Xcode.
plugins {
    id("serva.ios.framework")
}

kotlin {
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.withType<org.jetbrains.kotlin.gradle.plugin.mpp.Framework>().configureEach {
            // The vault's class is what the app's own tests drive, from Swift.
            export(project(":core:auth"))
        }
        // The shared database opens the system's SQLite (DECISION-092). The static framework
        // leaves linking to the app (OTHER_LDFLAGS in apps/ios/project.yml); the tests link here.
        binaries.withType<org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable>().configureEach {
            linkerOpts("-lsqlite3")
        }
    }
    sourceSets {
        iosMain {
            dependencies {
                api(project(":core:auth"))
                implementation(project(":core:model"))
                implementation(project(":core:database"))
                implementation(project(":core:datastore"))
                implementation(project(":core:designsystem"))
                implementation(project(":core:location"))
                implementation(project(":core:network"))
                implementation(project(":core:transport"))
                implementation(project(":feature:home"))
                implementation(project(":feature:province"))
                implementation(project(":feature:search"))
                implementation(project(":feature:facility"))
                implementation(project(":core:analytics"))
                implementation(libs.ktor.client.core)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.compose.multiplatform.runtime)
                implementation(libs.compose.multiplatform.foundation)
                implementation(libs.compose.multiplatform.ui)
                implementation(libs.compose.multiplatform.material3)
                implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            }
        }
        iosTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}
