plugins {
    id("serva.android.library")
    id("serva.android.hilt")
}

dependencies {
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // The sender is a coroutine collector; the rules it follows are pure and tested without one.
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
