import ca.solostudios.nyx.plugin.minecraft.NyxMinotaurExtension.VersionType
import ca.solostudios.nyx.plugin.minecraft.loom.FabricModJson.Environment
import ca.solostudios.nyx.util.fabric
import ca.solostudios.nyx.util.soloStudios
import ca.solostudios.nyx.util.soloStudiosSnapshots
import net.fabricmc.loom.task.RunGameTask
import org.gradle.jvm.tasks.Jar
import kotlin.reflect.KProperty

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

            mixinRefmapName("betterrecipes")
        }

        fabricModJson {
            author("solonovamax", mapOf("homepage" to "https://solonovamax.gay"))
            contact {
                homepage = "https://github.com/solonovamax/BetterRecipes/"
                environment = Environment.UNIVERSAL
            }
            val minecraftVersion by libs.versions.minecraft
            depends("minecraft", ">=$minecraftVersion")
            // val fabricApiVersion by libs.versions.fabric.api
            // depends("fabric-api", ">=$fabricApiVersion")
            // val fabricLanguageKotlinVersion by libs.versions.fabric.language.kotlin
            // depends("fabric-language-kotlin", ">=$fabricLanguageKotlinVersion")
            // for now, these are bundled
            // val silkVersion by libs.versions.silk
            // depends("silk-core", ">=$silkVersion")
            // depends("silk-game", ">=$silkVersion")
            // depends("silk-igui", ">=$silkVersion")
            // depends("silk-nbt", ">=$silkVersion")
            // depends("silk-network", ">=$silkVersion")
            // depends("silk-persistence", ">=$silkVersion")
            // depends("silk-fabric", ">=$silkVersion")
            // val mixinExtrasVersion by libs.versions.mixinextras
            // depends("mixinextras", ">=$mixinExtrasVersion")

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
                // for now, we use a bundled version of silk
                // embedded("silk")

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

tasks {
    val runDatagen by named<RunGameTask>("runDatagen")

    withType<RunGameTask>().matching { it != runDatagen }.configureEach {
        dependsOn(runDatagen)
    }

    withType<Jar>().configureEach {
        dependsOn(runDatagen)
    }
}

modrinth {
    val branchProvider = scmVersion.versionProvider().map { version -> version.position.branch }
    val baseUrlProvider = nyx.info.repository.projectPath.zip(branchProvider) { repoPath, branch -> "https://raw.githubusercontent.com/$repoPath/$branch" }
    val body = rootProject.file("README.md").readText().replace("!\\[\\]\\((.*)\\)".toRegex()) { match ->
        val (path) = match.destructured
        baseUrlProvider.map { baseUrl -> "![]($baseUrl/$path)" }.get()
    }
    syncBodyFrom.set(body)
}

val Project.isSnapshot: Boolean
    get() = version.toString().endsWith("-SNAPSHOT")

operator fun <T> Provider<T>.getValue(thisRef: Any?, property: KProperty<*>): T = get()
