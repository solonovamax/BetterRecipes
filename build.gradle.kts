import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `maven-publish`
    kotlin("jvm") version "1.6.21"
    id("fabric-loom") version "0.12-SNAPSHOT"
    id("org.jetbrains.dokka") version "1.6.20"
}

group = "gay.solonovamax"
version = "1.1.1"

repositories {
    mavenCentral()
    maven {
        name = "FabricMC"
        url = uri("https://maven.fabricmc.net")
    }
    maven("https://oss.sonatype.org/content/repositories/snapshots/") {
        mavenContent { snapshotsOnly() }
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    val minecraftVersion by properties
    val yarnMappings by properties
    val loaderVersion by properties
    val fabricVersion by properties
    val fabricKotlinVersion by properties
    
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$yarnMappings:v2")
    
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
    
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")
    
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
