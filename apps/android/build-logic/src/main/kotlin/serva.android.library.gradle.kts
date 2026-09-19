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

// Looked up on the project. Inside `dependencies { }`, `extensions` is the dependency
// handler's own, which holds no version catalog.
val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    add("coreLibraryDesugaring", catalog.findLibrary("desugar-jdk-libs").get())
}
