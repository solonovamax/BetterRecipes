import ca.solostudios.nyx.plugin.minecraft.loom.FabricModJson
import ca.solostudios.nyx.util.fabric
import ca.solostudios.nyx.util.soloStudios
import ca.solostudios.nyx.util.soloStudiosSnapshots

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.axion.release)
    alias(libs.plugins.nyx)
}

nyx {
    compile {
        distributeLicense = true
        buildDependsOnJar = true
        jvmTarget = 21
        reproducibleBuilds = true
    }

    info {
        name = "Better Recipes Testmod"
        group = "gay.solonovamax"
        module = "better-recipes-testmod"
        version = rootProject.scmVersion.version

        repository.fromGithub("solonovamax", "BetterRecipes")
        license.useMIT()
    }

    minecraft {
        accessWidener("betterrecipes")

        mixin {
            hotswap = true
            verbose = false
            export = true

            mixinRefmapName("betterrecipes")
        }

        runs {
            val gametest by registering {
                server()
                properties(mapOf("fabric-api.gametest" to "true"))
            }

            val clientGametest by registering {
                client()
                properties(mapOf("fabric.client.gametest" to "true"))
            }
        }

        fabricModJson {
            mixin("mixins/betterrecipes/mixins.client.json", FabricModJson.Environment.CLIENT)
            mixin("mixins/betterrecipes/mixins.server.json", FabricModJson.Environment.UNIVERSAL)
            entrypoints {
                client {
                    entrypoint("gay.solonovamax.betterrecipes.BetterRecipesTestmod", "kotlin")
                }
                main {
                    entrypoint("gay.solonovamax.betterrecipes.BetterRecipesTestmod", "kotlin")
                }
                entry("fabric-client-gametest") {
                    entrypoint("gay.solonovamax.betterrecipes.BetterRecipesTestmod", "kotlin")
                }
            }
        }
    }
}

repositories {
    soloStudios()
    soloStudiosSnapshots()
    fabric()
    mavenCentral()
}

dependencies {
    minecraft(libs.minecraft)

    @Suppress("UnstableApiUsage")
    mappings(loom.layered {
        mappings(variantOf(libs.yarn.mappings) { classifier("v2") })
    })

    implementation(project(path = ":", configuration = "namedElements"))

    modImplementation(libs.fabric.loader)

    modImplementation(libs.fabric.api)
    modImplementation(libs.fabric.language.kotlin)

    modImplementation(libs.bundles.silk)

    annotationProcessor(libs.sponge.mixin)
    implementation(libs.sponge.mixin)

    annotationProcessor(libs.mixinextras)
    implementation(libs.mixinextras)

    implementation(libs.slf4k)
}
