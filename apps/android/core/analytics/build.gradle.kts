plugins {
    id("serva.android.library")
    id("serva.android.hilt")
}

dependencies {
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
