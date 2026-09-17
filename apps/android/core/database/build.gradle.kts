plugins {
    id("serva.android.library")
    id("serva.android.hilt")
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.kotlinx.serialization.json)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
}


ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
