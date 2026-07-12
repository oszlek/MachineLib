plugins {
    java
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("com.gradleup.shadow")
}

base {
    archivesName.set("${rootProject.property("mod.name")}-${project.name}")
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

loom {
    silentMojangMappingsLicense()
}

architectury {
    platformSetupLoomIde()
    neoForge {}
}

val common: Configuration by configurations.creating
val shadowCommon: Configuration by configurations.creating

configurations["compileClasspath"].extendsFrom(common)
configurations["runtimeClasspath"].extendsFrom(common)
configurations["developmentNeoForge"].extendsFrom(common)

repositories {
    maven("https://maven.neoforged.net/releases/")
    maven("https://maven.terraformersmc.com/releases") {
        content {
            includeGroup("com.terraformersmc")
            includeGroup("dev.emi")
        }
    }
    maven("https://maven.shedaniel.me") {
        content {
            includeGroup("me.shedaniel")
            includeGroup("me.shedaniel.cloth")
            includeGroup("dev.architectury")
        }
    }
    maven("https://maven.blamejared.com/") {
        content {
            includeGroup("mezz.jei")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${rootProject.property("minecraft.version")}")
    mappings(loom.officialMojangMappings())
    "neoForge"("net.neoforged:neoforge:${rootProject.property("neoforge.version")}")
    modApi("dev.architectury:architectury-neoforge:${rootProject.property("architectury.version")}")

    // Optional integrations (compile-only; provided at runtime by the user)
    modCompileOnly("me.shedaniel.cloth:cloth-config-neoforge:${rootProject.property("cloth.config.version")}")
    modCompileOnly("me.shedaniel:RoughlyEnoughItems-api-neoforge:${rootProject.property("rei.version")}")
    modCompileOnly("mezz.jei:jei-${rootProject.property("minecraft.version")}-neoforge-api:${rootProject.property("jei.version")}")
    modCompileOnly("dev.emi:emi-neoforge:${rootProject.property("emi.version")}:api")

    "common"(project(path = ":common", configuration = "namedElements")) { isTransitive = false }
    "shadowCommon"(project(path = ":common", configuration = "transformProductionNeoForge")) { isTransitive = false }
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("mod_id", rootProject.property("mod.id"))
    inputs.property("mod_name", rootProject.property("mod.name"))

    filesMatching("META-INF/neoforge.mods.toml") {
        expand(
            "version" to project.version,
            "mod_id" to rootProject.property("mod.id"),
            "mod_name" to rootProject.property("mod.name")
        )
    }
}

tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    inputFile.set(tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar").flatMap { it.archiveFile })
    dependsOn("shadowJar")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    exclude("fabric.mod.json")
    exclude("architectury.common.json")
    configurations = listOf(shadowCommon)
    archiveClassifier.set("dev-shadow")
}
