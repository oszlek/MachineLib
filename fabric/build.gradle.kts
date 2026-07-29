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
    withJavadocJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

sourceSets {
    register("testmod") {
        resources.srcDir("src/testmod/generated")
        runtimeClasspath += sourceSets.main.get().runtimeClasspath
        compileClasspath += sourceSets.main.get().compileClasspath
    }
}

loom {
    silentMojangMappingsLicense()

    val testmod = sourceSets.getByName("testmod")
    mods {
        create("machinelib") {
            sourceSet(sourceSets.main.get())
        }
        create("machinelib_testmod") {
            sourceSet(testmod)
        }
    }
    createRemapConfigurations(testmod)

    runs {
        named("client") {
            source(testmod)
        }
        named("server") {
            source(testmod)
            vmArgs("-ea")
        }
        register("gametest") {
            name("GameTest Server")
            server()
            source(testmod)
            vmArgs("-ea")
            property("fabric-api.gametest")
            property("fabric-api.gametest.report-file", "${project.layout.buildDirectory.get()}/junit.xml")
        }
        register("datagen") {
            name("Data Generation")
            client()
            source(testmod)
            runDir("build/datagen")
            property("fabric-api.datagen")
            property("fabric-api.datagen.modid", "machinelib_testmod")
            property("fabric-api.datagen.output-dir", project.file("src/testmod/generated").toString())
            property("fabric-api.datagen.strict-validation", "false")
        }
    }
}

architectury {
    platformSetupLoomIde()
    fabric()
}

val common: Configuration by configurations.creating
val shadowCommon: Configuration by configurations.creating

configurations["compileClasspath"].extendsFrom(common)
configurations["runtimeClasspath"].extendsFrom(common)
configurations["developmentFabric"].extendsFrom(common)
configurations["testCompileClasspath"].extendsFrom(common)
configurations["testRuntimeClasspath"].extendsFrom(common)

repositories {
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
    maven("https://maven.bai.lol") {
        content {
            includeGroup("lol.bai")
            includeGroup("mcp.mobius.waila")
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
    modImplementation("net.fabricmc:fabric-loader:${rootProject.property("loader.version")}")
    modApi("dev.architectury:architectury-fabric:${rootProject.property("architectury.version")}")

    // Fabric API (transfer, lookup, networking, item, screen-handler, model, rendering, gametest, ...)
    modImplementation("net.fabricmc.fabric-api:fabric-api:${rootProject.property("fabric.version")}")

    // Cross-loader C2S/S2C packet helper
    modImplementation("lol.bai:badpackets:fabric-${rootProject.property("badpackets.version")}")

    // Unit tests (headless, via fabric-loader-junit)
    testImplementation("net.fabricmc:fabric-loader-junit:${rootProject.property("loader.version")}")

    // Energy (team-reborn) — bundled via jar-in-jar
    include(modApi("teamreborn:energy:${rootProject.property("energy.version")}") {
        isTransitive = false
    })

    // Optional integrations (compile-only; provided at runtime by the user)
    modCompileOnly("mcp.mobius.waila:wthit-api:fabric-${rootProject.property("wthit.version")}")
    modCompileOnly("me.shedaniel.cloth:cloth-config-fabric:${rootProject.property("cloth.config.version")}")
    modCompileOnly("com.terraformersmc:modmenu:${rootProject.property("modmenu.version")}")
    modCompileOnly("me.shedaniel:RoughlyEnoughItems-api-fabric:${rootProject.property("rei.version")}")
    modCompileOnly("mezz.jei:jei-${rootProject.property("minecraft.version")}-fabric-api:${rootProject.property("jei.version")}")
    modCompileOnly("dev.emi:emi-fabric:${rootProject.property("emi.version")}:api")

    "common"(project(path = ":common", configuration = "namedElements")) { isTransitive = false }
    "shadowCommon"(project(path = ":common", configuration = "transformProductionFabric")) { isTransitive = false }

    // Testmod compiles against the fabric main output (e.g. MachineModelGenerator) + fabric-api gametest/datagen.
    "testmodImplementation"(sourceSets.main.get().output)
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("mod_id", rootProject.property("mod.id"))
    inputs.property("mod_name", rootProject.property("mod.name"))

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "mod_id" to rootProject.property("mod.id"),
            "mod_name" to rootProject.property("mod.name")
        )
    }
}

tasks.test {
    useJUnitPlatform()
    enableAssertions = true
}

tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    inputFile.set(tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar").flatMap { it.archiveFile })
    dependsOn("shadowJar")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    exclude("architectury.common.json")
    configurations = listOf(shadowCommon)
    archiveClassifier.set("dev-shadow")
}

tasks.named<Jar>("sourcesJar") {
    from(project(":common").extensions.getByType<SourceSetContainer>()["main"].allSource)
}
