// A shared module whose Android side Hilt builds (DECISION-087): the Android implementations and
// their Hilt modules live in androidMain, processed by Hilt's compiler through KSP.
plugins {
    id("serva.kmp.library")
    id("com.google.devtools.ksp")
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    android {
        // Hilt's processor writes Java, which a multiplatform Android target compiles only when
        // asked to.
        withJava()
    }
    sourceSets {
        androidMain.dependencies {
            implementation(catalog.findLibrary("hilt-android").get())
        }
    }
}

dependencies {
    add("kspAndroid", catalog.findLibrary("hilt-compiler").get())
}
