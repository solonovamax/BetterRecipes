rootProject.name = "BetterRecipes"

pluginManagement {
    repositories {
        maven("https://maven.solo-studios.ca/releases")
        maven("https://maven.solo-studios.ca/snapshots")
        mavenCentral()
        gradlePluginPortal()
    }
}

include(":testmod")