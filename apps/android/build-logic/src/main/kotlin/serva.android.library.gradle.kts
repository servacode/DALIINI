plugins {
    id("com.android.library")
}

extensions.configure<com.android.build.api.dsl.LibraryExtension> {
    namespace = "com.servacode.directory" + project.path.replace(":", ".")
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // The generated API client uses java.time, which minSdk 24 predates (API 26).
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    add(
        "coreLibraryDesugaring",
        extensions.getByType<VersionCatalogsExtension>().named("libs").findLibrary("desugar-jdk-libs").get(),
    )
}
