plugins {
    id("serva.android.library")
    id("serva.android.compose")
}

android {
    sourceSets.getByName("main").java.srcDir(
        rootProject.file("../../packages/design-tokens/generated")
    )
}

dependencies {
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
