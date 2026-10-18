pluginManagement {
    // Shared build settings (convention plugins) for every module.
    includeBuild("build-logic")
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

rootProject.name = "SplitEasy"
include(":app")
include(":core:domain")
include(":core:database")
include(":core:data")
include(":core:designsystem")
include(":core:testing")
include(":core:sync")
include(":feature:auth")
include(":feature:home")
include(":feature:group")
include(":feature:expense")
include(":feature:settle")
include(":feature:profile")
