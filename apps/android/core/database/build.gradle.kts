// Shared with the iPhone app. The cache-first rule and the stores' contracts (DECISION-087), and
// since DECISION-092 the Room database, its DAOs and the stores built on them, are common. Each
// platform only opens the database: Android with its framework SQLite and the 1→2 migration, in
// androidMain with its Hilt module; the iPhone with the system SQLite, in iosMain.
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("serva.kmp.hilt")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:inject"))
            implementation(project(":core:model"))
            api(libs.androidx.room.runtime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.androidx.room.ktx)
        }
        iosMain.dependencies {
            // Room brings 2.6, whose driver refers to `sqlite3_load_extension`, which iOS's SQLite
            // does not have: nothing links. 2.7 fixed it (b/434324365).
            implementation(libs.androidx.sqlite.framework.ios)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
    // Room's driver for the iPhone opens the system's SQLite, which every binary that carries
    // it links: the app's framework and the tests.
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.all { linkerOpts("-lsqlite3") }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    // Kotlin, which is what Room writes for a multiplatform target anyway; Android gets the same.
    arg("room.generateKotlin", "true")
}
