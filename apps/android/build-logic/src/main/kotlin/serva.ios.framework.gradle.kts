// The iPhone app's Kotlin side (DECISION-093): iOS only, built as the one static framework the
// Xcode project in apps/ios links (DaliiniKit), with Compose Multiplatform for its screens.
// Static, because a static framework is linked into the app and needs no embedding or signing
// of its own.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    // Compose resources: the framework's build copies every module's strings, fonts and images
    // into the app's bundle, where the shared design system reads them (DECISION-094).
    id("org.jetbrains.compose")
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "DaliiniKit"
            isStatic = true
        }
    }

    compilerOptions {
        // As in serva.kmp.library: the shared modules' expect/actual classes (DECISION-087).
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        iosTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
