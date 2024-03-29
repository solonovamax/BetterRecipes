import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `maven-publish`
    kotlin("jvm") version "1.7.10"
    id("org.quiltmc.loom") version "0.12.40"
    id("org.jetbrains.dokka") version "1.7.10"
    id("com.modrinth.minotaur") version "2.4.4"
}

group = "gay.solonovamax"
version = "1.3.0"

repositories {
    mavenCentral()
    
    maven("https://maven.quiltmc.org/repository/release") {
        name = "Quilt"
    }
    maven("https://maven.fabricmc.net") {
        name = "FabricMC"
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    val minecraftVersion by properties
    val quiltMappingsBuild by properties
    val loaderVersion by properties
    val quiltedFabricApiVersion by properties
    val fabricKotlinVersion by properties
    
    minecraft("com.mojang:minecraft:$minecraftVersion")
    @Suppress("UnstableApiUsage")
    mappings(loom.layered {
        addLayer(quiltMappings.mappings("org.quiltmc:quilt-mappings:$minecraftVersion+build.$quiltMappingsBuild:v2"))
    })
    
    modImplementation("org.quiltmc:quilt-loader:$loaderVersion")
    
    // modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")
    modImplementation("org.quiltmc.quilted-fabric-api:quilted-fabric-api:$quiltedFabricApiVersion") {
        exclude(group = "org.quiltmc.quilted-fabric-api", module = "fabric-gametest-api-v1")
    }
    
    // modImplementation("net.fabricmc:fabric-language-kotlin:$fabricKotlinVersion")
    modImplementation("net.fabricmc:fabric-language-kotlin:$fabricKotlinVersion")
}

tasks {
    
    processResources {
        inputs.properties("version" to project.version)
        filesMatching("fabric.mod.json") {
            expand("version" to project.version)
        }
    }
    
    jar {
        from("LICENSE")
    }
    
    // ensure that the encoding is set to UTF-8, no matter what the system default is
    // this fixes some edge cases with special characters not displaying correctly
    // see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
    withType(JavaCompile::class) {
        options.encoding = "UTF-8"
    }
    
    withType(KotlinCompile::class) {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
    
    configureLaunch {
        doFirst {
            loom {
                runs {
                    configureEach {
                        property("fabric.development=true")
                        property("mixin.hotSwap")
                        val mixinJarFile = configurations.compileClasspath.get().files {
                            it.group == "net.fabricmc" && it.name == "sponge-mixin"
                        }.find { true } // only find one
                        vmArg("-javaagent:$mixinJarFile")
    
                        ideConfigGenerated(true)
                    }
                }
            }
        }
    }
}

modrinth {
    val modrinthToken: String by project
    token.value(modrinthToken)
    projectId.set("better-recipes")
    versionNumber.set(project.version.toString())
    versionType.set("release")
    uploadFile.set(tasks.remapJar.get())
    gameVersions.set(listOf("1.19", "1.19.1", "1.19.2"))
    loaders.set(listOf("fabric", "quilt"))
    
    dependencies {
        required.project("fabric-api")
        required.project("qsl")
    }
    
    syncBodyFrom.set(rootProject.file("README.md").toRelativeString(rootDir))
}
