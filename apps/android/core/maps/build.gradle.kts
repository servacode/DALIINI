plugins {
    id("serva.android.library")
    id("serva.android.compose")
    id("serva.android.hilt")
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.androidx.core)
    implementation(libs.maplibre.android)
    // The offline pack is a singleton the app injects, and it reports progress as a flow.
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.lifecycle.runtime.compose)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
