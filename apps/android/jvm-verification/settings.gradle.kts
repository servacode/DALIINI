// A second, JVM-only build over the Android app's platform-free sources.
//
// It exists because the Android build needs Google Maven, which does not serve every
// network this project is developed on. Everything here resolves from Maven Central and the
// Gradle Plugin Portal, and every version comes from the app's own catalog, so what this
// build compiles and tests is what the app compiles. It does not replace the Android build
// and passing here is not BUILD_VERIFIED for the app. See README.md.
pluginManagement {
    val catalog = file("../gradle/libs.versions.toml").readText()
    val kotlin = Regex("""(?m)^kotlin = "([^"]+)"""").find(catalog)!!.groupValues[1]
    plugins {
        id("org.jetbrains.kotlin.jvm") version kotlin
        id("org.jetbrains.kotlin.plugin.serialization") version kotlin
    }
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "directory-android-jvm-verification"
