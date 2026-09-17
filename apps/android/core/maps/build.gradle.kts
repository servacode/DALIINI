plugins {
    id("serva.android.library")
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.maplibre.android)
    testImplementation(libs.junit)
}
