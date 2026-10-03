plugins {
    `kotlin-dsl`
}

group = "com.servacode.directory.buildlogic"

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.android.tools.build:gradle:${libs.versions.agp.get()}")
    // Kotlin Multiplatform, for the layers the iPhone app shares (DECISION-085).
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    implementation(
        "org.jetbrains.kotlin:compose-compiler-gradle-plugin:${libs.versions.kotlin.get()}"
    )
    implementation("com.google.dagger:hilt-android-gradle-plugin:${libs.versions.hilt.get()}")
    implementation("com.google.devtools.ksp:symbol-processing-gradle-plugin:${libs.versions.ksp.get()}")
    // Compose Multiplatform's resources (strings, fonts, images) for the shared design system
    // (DECISION-094).
    implementation(libs.compose.multiplatform.gradle.plugin)
}
