// The fakes the tests share (DECISION-089): common, so a shared module's tests can use them on
// every platform; the JUnit rule that swaps the main dispatcher stays in androidMain.
plugins {
    id("serva.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.test)
            // The fakes implement these modules' interfaces, so tests see their types.
            api(project(":core:model"))
            api(project(":core:database"))
            api(project(":core:datastore"))
            api(project(":core:location"))
            api(project(":core:network"))
        }
        androidMain.dependencies {
            api(libs.junit)
        }
    }
}
