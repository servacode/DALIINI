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
    implementation(
        "org.jetbrains.kotlin:compose-compiler-gradle-plugin:${libs.versions.kotlin.get()}"
    )
    implementation("com.google.dagger:hilt-android-gradle-plugin:${libs.versions.hilt.get()}")
    implementation("com.google.devtools.ksp:symbol-processing-gradle-plugin:${libs.versions.ksp.get()}")
}
