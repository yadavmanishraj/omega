pluginManagement {
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
rootProject.name = "SaavnMusic"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
include(":app")
include(":core:model")
include(":core:common")
include(":core:designsystem")
include(":core:ui")
include(":core:network")
include(":core:database")
include(":core:datastore")
include(":core:data")
include(":core:playback")
include(":core:download")
include(":feature:home")
include(":feature:search")
include(":feature:library")
include(":feature:settings")
include(":feature:player")
include(":feature:detail")
