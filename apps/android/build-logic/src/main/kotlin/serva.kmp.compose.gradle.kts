// A shared module with Compose UI (DECISION-094): Compose Multiplatform on Android and the
// iPhone. On Android its Compose is androidx Compose itself, at the app's versions; on the
// iPhone it is JetBrains'. Strings, fonts and images are Compose Multiplatform resources, read
// the same way on both; a module that also keeps Android resources (for a widget, a notification
// or XML) turns them on itself.
plugins {
    id("serva.kmp.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(catalog.findLibrary("compose-multiplatform-runtime").get())
            implementation(catalog.findLibrary("compose-multiplatform-foundation").get())
            implementation(catalog.findLibrary("compose-multiplatform-ui").get())
            implementation(catalog.findLibrary("compose-multiplatform-material3").get())
            implementation(catalog.findLibrary("compose-multiplatform-resources").get())
        }
    }
}
