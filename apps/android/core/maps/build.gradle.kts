plugins {
    id("serva.android.library")
    id("serva.android.compose")
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.maplibre.android)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.lifecycle.runtime.compose)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
