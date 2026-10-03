plugins {
    id("com.android.application")
}

// Whoever builds it numbers it (DECISION-079). CI passes DIRECTORY_VERSION_CODE for every build
// it makes, so two builds never share a number, and a Play release refuses to start without one
// (validatePlayRelease). The name is chosen by people, in gradle.properties, and reviewed like
// any change. A machine that sets neither builds 1 and the checked-in name.
val versionCodeSetting = providers.gradleProperty("DIRECTORY_VERSION_CODE")
    .orElse(providers.environmentVariable("DIRECTORY_VERSION_CODE"))
val versionNameSetting = providers.gradleProperty("DIRECTORY_VERSION_NAME")
    .orElse(providers.environmentVariable("DIRECTORY_VERSION_NAME"))

extensions.configure<com.android.build.api.dsl.ApplicationExtension> {
    namespace = "com.servacode.directory"
    // 37: the pinned AndroidX, Compose and Coil releases require it (DECISION-040).
    // targetSdk stays 36; compiling against newer APIs does not opt into their behaviour.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.servacode.directory"
        minSdk = 24
        targetSdk = 36
        versionCode = versionCodeSetting.orNull?.toIntOrNull() ?: 1
        versionName = versionNameSetting.orNull ?: "0.1.0"
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
