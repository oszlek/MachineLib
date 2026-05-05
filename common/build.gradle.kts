plugins {
    java
    id("dev.architectury.loom")
    id("architectury-plugin")
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
    common(rootProject.property("enabled_platforms").toString().split(","))
}

dependencies {
    minecraft("com.mojang:minecraft:${rootProject.property("minecraft.version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${rootProject.property("loader.version")}")
    modApi("dev.architectury:architectury:${rootProject.property("architectury.version")}")
}
