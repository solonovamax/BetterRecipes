rootProject.name = "BetterRecipes"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    
        maven("https://maven.quiltmc.org/repository/release") {
            name = "Quilt"
        }
        maven("https://maven.fabricmc.net") {
            name = "FabricMC"
        }
    }
}
