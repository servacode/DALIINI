plugins {
    id("serva.android.library")
}

dependencies {
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    // The fakes implement these modules' interfaces, so tests see their types.
    api(project(":core:model"))
    api(project(":core:database"))
    api(project(":core:datastore"))
    api(project(":core:location"))
    api(project(":core:network"))
}
