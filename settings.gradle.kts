pluginManagement {
    repositories {
        google()          // НИЈЕ потребан content‑filter – он уме да сакрије AGP
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {

    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "LottoCombinationFinder"
include(":app")
