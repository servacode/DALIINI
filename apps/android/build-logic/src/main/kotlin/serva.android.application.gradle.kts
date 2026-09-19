plugins {
    id("com.android.application")
}

extensions.configure<com.android.build.api.dsl.ApplicationExtension> {
    namespace = "com.servacode.directory"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.servacode.directory"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // The generated API client uses java.time, which minSdk 24 predates (API 26).
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        buildConfig = true
    }
}

// Looked up on the project. Inside `dependencies { }`, `extensions` is the dependency
// handler's own, which holds no version catalog.
val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    add("coreLibraryDesugaring", catalog.findLibrary("desugar-jdk-libs").get())
}
