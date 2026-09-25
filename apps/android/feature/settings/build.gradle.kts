plugins {
    id("serva.android.library")
    id("serva.android.compose")
    id("serva.android.hilt")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:network"))
    // The province's offline map pack, and the preference that remembers a refusal.
    implementation(project(":core:maps"))
    implementation(project(":core:datastore"))
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
}
