plugins {
    id("serva.android.library")
    id("serva.android.hilt")
}

dependencies {
    // Only for the `AnonymousId` contract the stored id implements.
    implementation(project(":core:analytics"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
}
