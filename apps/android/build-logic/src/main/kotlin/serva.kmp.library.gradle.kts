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
        withHostTestBuilder {}
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
