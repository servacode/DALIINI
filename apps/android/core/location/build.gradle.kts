plugins {
    id("serva.android.library")
    id("serva.android.hilt")
}

dependencies {
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core)
    testImplementation(libs.junit)
}
