import ca.solostudios.nyx.plugin.minecraft.NyxMinotaurExtension.VersionType
import ca.solostudios.nyx.plugin.minecraft.loom.FabricModJson.Environment
import ca.solostudios.nyx.util.fabric
import ca.solostudios.nyx.util.soloStudios
import net.fabricmc.loom.task.RunGameTask
import org.gradle.jvm.tasks.Jar

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.axion.release)
    alias(libs.plugins.minotaur)
    alias(libs.plugins.nyx)
}

nyx {
    compile {
        // javadocJar = true
        sourcesJar = true

        allWarnings = true
        // warningsAsErrors = true
        distributeLicense = true
        buildDependsOnJar = true
        jvmTarget = 17
        reproducibleBuilds = true

        kotlin {
            compilerArgs.add("-Xcontext-receivers")
            optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
        }
    }

    info {
        name = "Better Recipes"
        group = "gay.solonovamax"
        module = "better-recipes"
        version = scmVersion.version
        description = """
            Adds a bunch of useful recipes to minecraft
        """.trimIndent()

        developer {
            id = "solonovamax"
            name = "solonovamax"
            email = "solonovamax@12oclockpoint.com"
            url = "https://solonovamax.gay"
        }

        repository.fromGithub("solonovamax", "BetterRecipes")
        license.useMIT()
    }

    minecraft {
        configureDataGeneration {
            createSourceSet = true
            // strictValidation = true
            modId = "better-recipes"
        }

        // accessWidener("beaconoverhaul")

        additionalJvmProperties.putAll(
            mapOf(
                "fabric-tag-conventions-v2.missingTagTranslationWarning" to "FAIL",
                "fabric-tag-conventions-v1.legacyTagWarning" to "FAIL"
            )
        )

        mixin {
            hotswap = true
            verbose = false
            export = true

            // mixinRefmapName("beaconoverhaul")
        }

        fabricModJson {
            author("solonovamax", mapOf("homepage" to "https://solonovamax.gay"))
            contact {
                homepage = "https://github.com/solonovamax/BetterRecipes/"
                environment = Environment.UNIVERSAL
            }
            depends("minecraft", ">=1.21")

            entrypoints {
                entry("fabric-datagen") {
                    entrypoint("gay.solonovamax.betterrecipes.datagen.BetterResourcesDataGenerator", "kotlin")
                }
            }
        }

        minotaur {
            versionType = if (isSnapshot) VersionType.ALPHA else VersionType.BETA
            projectId = "better-recipes"
            detectLoaders = true
            gameVersions = listOf("1.21")
            dependencies {
                // required("fabric-api")
                // required("fabric-language-kotlin")

                // optional("modmenu")

                // optional("emi")
                // optional("rei")
                // optional("jei")
            }
        }
    }
}

repositories {
    soloStudios()
    fabric()
    mavenCentral()
}

dependencies {
    minecraft(libs.minecraft)

    @Suppress("UnstableApiUsage")
    mappings(loom.layered {
        mappings(variantOf(libs.yarn.mappings) { classifier("v2") })
    })

    modImplementation(libs.fabric.loader)

    modImplementation(libs.fabric.api)
    modImplementation(libs.fabric.language.kotlin)

    // annotationProcessor(libs.sponge.mixin)
    // implementation(libs.sponge.mixin)
    //
    // annotationProcessor(libs.mixinextras)
    // implementation(libs.mixinextras)

    // only used for datagen
    implementation(libs.slf4k)
}

tasks {
    val runDatagen by named<RunGameTask>("runDatagen")

    withType<RunGameTask>().matching { it != runDatagen }.configureEach {
        dependsOn(runDatagen)
    }

    withType<Jar>().configureEach {
        dependsOn(runDatagen)
    }
}

// modrinth {
//     syncBodyFrom.set(rootProject.file("README.md").toRelativeString(rootDir))
// }

val Project.isSnapshot: Boolean
    get() = version.toString().endsWith("-SNAPSHOT")
