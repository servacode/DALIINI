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
    }

    buildFeatures {
        buildConfig = true
    }
}
