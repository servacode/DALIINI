plugins {
    id("serva.android.library")
    id("serva.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

// The generated P10 client is compiled as part of this module, the same way
// `:core:designsystem` compiles the generated design tokens: no copy, no hand-written DTOs.
// Only `api/` in this module may reference it.
extensions.configure<com.android.build.api.dsl.LibraryExtension> {
    sourceSets.getByName("main").kotlin.srcDir(
        rootProject.file("../../packages/api-kotlin/generated/src/main/kotlin")
    )
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:auth"))
    implementation(project(":core:observability"))
    implementation(project(":core:maps"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.retrofit.converter.scalars)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
