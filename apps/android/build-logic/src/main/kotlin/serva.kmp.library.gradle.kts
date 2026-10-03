// A layer the Android and iPhone apps share (DECISION-085): common Kotlin, compiled for
// Android and for iOS (devices and the Apple-silicon simulator). Platform code, when a layer
// needs any, goes in androidMain / iosMain; commonMain imports nothing from java.*, android.*
// or javax.inject.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    // Android lint for a multiplatform module comes from its own plugin, as `lint`.
    id("com.android.lint")
}

kotlin {
    android {
        namespace = "com.servacode.directory" + project.path.replace(":", ".")
        // The same levels as the Android libraries (serva.android.library, DECISION-040).
        compileSdk = 37
        minSdk = 24
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
        // As in serva.android.library: java.time, which the generated API client and
        // kotlinx-datetime use on Android, predates minSdk 24 (API 26).
        enableCoreLibraryDesugaring = true
        withHostTestBuilder {}.configure {
            // A module whose host tests read its own Android resources by id opts in, in its own
            // gradle.properties; AGP allows the host tests to be set up only once, here.
            isIncludeAndroidResources = findProperty("serva.hostTestAndroidResources") == "true"
        }
    }
    iosArm64()
    iosSimulatorArm64()
    // The shared database opens the system's SQLite (DECISION-092). A module's tests reach it
    // through the shared fakes once its view models are common (DECISION-095), so every test
    // binary links it, as the app does.
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.withType<org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable>().configureEach {
            linkerOpts("-lsqlite3")
        }
    }

    compilerOptions {
        // Expect/actual classes are Beta; the shared modules use them for the injection
        // annotations (DECISION-087) and accept that.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

// Looked up on the project. Inside `dependencies { }`, `extensions` is the dependency
// handler's own, which holds no version catalog.
val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    add("coreLibraryDesugaring", catalog.findLibrary("desugar-jdk-libs").get())
}
