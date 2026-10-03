pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DirectoryAndroid"
includeBuild("build-logic")

include(":app")
include(":core:model")
include(":core:network")
include(":core:database")
include(":core:datastore")
include(":core:auth")
include(":core:designsystem")
include(":core:location")
include(":core:maps")
include(":core:analytics")
include(":core:observability")
include(":core:inject")
include(":core:api")
include(":core:testing")
include(":feature:bootstrap")
include(":feature:home")
include(":feature:province")
include(":feature:search")
include(":feature:facility")
include(":feature:map")
include(":feature:navigation")
include(":feature:auth")
include(":feature:account")
include(":feature:ratings")
include(":feature:owner")
include(":feature:onboarding")
include(":feature:duty")
include(":feature:settings")
