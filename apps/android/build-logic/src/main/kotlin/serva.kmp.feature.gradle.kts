// A feature shared with the iPhone app (DECISIONS 089 and 095): its repositories, use cases and
// rules in common code, and, as each feature moves, its screens, view models and words too, on
// Compose Multiplatform. What stays in androidMain is Android's own: the Hilt-built view models
// the app's navigation asks for, and the words a notification, a widget or a service reads.
plugins {
    id("serva.kmp.hilt")
    id("serva.kmp.compose")
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    android {
        // The Android-only words, under the module's own R; and the shared ones, which Compose
        // Multiplatform packs into the module's Android assets.
        androidResources {
            enable = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:designsystem"))
            implementation(catalog.findLibrary("jetbrains-lifecycle-viewmodel-compose").get())
            implementation(catalog.findLibrary("jetbrains-lifecycle-runtime-compose").get())
        }
        commonTest.dependencies {
            // The shared fakes, and `runMainTest` for a view model's tests on both platforms.
            implementation(project(":core:testing"))
        }
        iosTest.dependencies {
            // A shared screen drawn on the iPhone simulator.
            implementation(catalog.findLibrary("compose-multiplatform-ui-test").get())
        }
    }
}

compose.resources {
    // `Res` beside the module's own code (its namespace), so a screen names `Res.string.x`
    // without an import.
    packageOfResClass = "com.servacode.directory" + project.path.replace(":", ".")
}
