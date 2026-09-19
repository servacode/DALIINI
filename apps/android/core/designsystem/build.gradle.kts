plugins {
    id("serva.android.library")
    id("serva.android.compose")
}

// AGP 9 serves source sets through com.android.build.api.dsl only; the generated
// `android { }` accessor still resolves to the removed AGP 8 source-set type and
// fails with a ClassCastException, so configure the new DSL extension directly.
extensions.configure<com.android.build.api.dsl.LibraryExtension> {
    sourceSets.getByName("main").kotlin.srcDir(
        rootProject.file("../../packages/design-tokens/generated")
    )
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
