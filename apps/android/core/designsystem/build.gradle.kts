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
    // The same tokens as colour resources, for XML that cannot read Kotlin (brand_mark.xml).
    sourceSets.getByName("main").res.srcDir(
        rootProject.file("../../packages/design-tokens/generated/android")
    )
    // The brand faces, IBM Plex Sans Arabic and Alexandria, from the package that owns them (SIL
    // OFL, packages/design-tokens/fonts/OFL-*.txt): the site, the console and the app set the
    // same files, so no copy lives here to fall behind.
    sourceSets.getByName("main").res.srcDir(
        rootProject.file("../../packages/design-tokens/fonts/android")
    )
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.androidx.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // The advertisement slider stops advancing when the app is not resumed.
    implementation(libs.androidx.lifecycle.runtime.compose)
    // One place decides how a screen gives way to another (DirectoryMotion).
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
