plugins {
    id("serva.android.application")
    id("serva.android.compose")
    id("serva.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

val apiBaseUrl = providers.gradleProperty("DIRECTORY_API_BASE_URL")
    .orElse("https://api.<ROOT_DOMAIN>/")

android {
    defaultConfig {
        buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl.get()}\"")
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:auth"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:location"))
    implementation(project(":core:maps"))
    implementation(project(":core:analytics"))
    implementation(project(":core:observability"))
    implementation(project(":feature:bootstrap"))
    implementation(project(":feature:home"))
    implementation(project(":feature:province"))
    implementation(project(":feature:search"))
    implementation(project(":feature:directory"))
    implementation(project(":feature:facility"))
    implementation(project(":feature:map"))
    implementation(project(":feature:navigation"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:account"))
    implementation(project(":feature:ratings"))
    implementation(project(":feature:owner"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:duty"))
    implementation(project(":feature:settings"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
