import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    id("architectury-plugin") version "3.4.164"
    id("dev.architectury.loom") version "1.7.416" apply false
    id("com.gradleup.shadow") version "8.3.6" apply false
    id("com.diffplug.spotless") version "7.0.3" apply false
    id("org.ajoberstar.grgit") version "5.3.0"
}

architectury {
    minecraft = project.property("minecraft.version").toString()
}

val baseVersion = project.property("mod.version").toString()
version = buildString {
    append(baseVersion)
    if (System.getenv("PRE_RELEASE") == "true") append("-pre")
    append('+')
    val runNumber = System.getenv("GITHUB_RUN_NUMBER")
    if (runNumber != null) {
        append(runNumber)
    } else {
        val repository = extensions.findByType<org.ajoberstar.grgit.Grgit>()
        val head = repository?.head()
        if (head != null) {
            append(head.id.substring(0, 8))
            if (!repository.status().isClean) append("-dirty")
        } else {
            append("unknown")
        }
    }
}

allprojects {
    group = "dev.galacticraft"
    version = rootProject.version
}

subprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "com.diffplug.spotless")

    extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        lineEndings = com.diffplug.spotless.LineEnding.UNIX
        java {
            licenseHeader(processLicenseHeader(rootProject.file("LICENSE")))
            leadingTabsToSpaces()
            removeUnusedImports()
            trimTrailingWhitespace()
        }
    }

    tasks.withType<Jar>().configureEach {
        from(rootProject.file("LICENSE")) {
            rename { "${it}_${rootProject.property("mod.id")}" }
        }
        manifest {
            attributes(
                "Specification-Title" to rootProject.property("mod.id"),
                "Specification-Vendor" to "Team Galacticraft",
                "Specification-Version" to baseVersion,
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to "Team Galacticraft",
                "Implementation-Timestamp" to LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                "Maven-Artifact" to "${project.group}:${rootProject.property("mod.name")}-${project.name}:${project.version}",
                "Built-On-Java" to "${System.getProperty("java.vm.version")} (${System.getProperty("java.vm.vendor")})"
            )
        }
    }

    afterEvaluate {
        extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("mavenJava") {
                    groupId = project.group.toString()
                    artifactId = "${rootProject.property("mod.name")}-${project.name}"
                    version = project.version.toString()
                    if (project.name == "common") {
                        from(components["java"])
                    } else {
                        artifact(tasks.named("remapJar"))
                        artifact(tasks.named("sourcesJar"))
                        artifact(tasks.named("javadocJar"))
                    }
                    pom {
                        organization {
                            name.set("Team Galacticraft")
                            url.set("https://github.com/TeamGalacticraft")
                        }
                        scm {
                            url.set("https://github.com/TeamGalacticraft/MachineLib")
                            connection.set("scm:git:git://github.com/TeamGalacticraft/MachineLib.git")
                            developerConnection.set("scm:git:git@github.com:TeamGalacticraft/MachineLib.git")
                        }
                        issueManagement {
                            system.set("github")
                            url.set("https://github.com/TeamGalacticraft/MachineLib/issues")
                        }
                        licenses {
                            license {
                                name.set("MIT")
                                url.set("https://github.com/TeamGalacticraft/MachineLib/blob/minecraft/1.21/LICENSE")
                            }
                        }
                    }
                }
            }
            repositories {
                val repositoryUrl = System.getenv("NEXUS_REPOSITORY_URL")
                if (repositoryUrl != null) {
                    maven(repositoryUrl) {
                        credentials {
                            username = System.getenv("NEXUS_USER")
                            password = System.getenv("NEXUS_PASSWORD")
                        }
                    }
                }
            }
        }
    }
}

fun processLicenseHeader(license: File): String {
    val text = license.readText()
    return "/*\n * " + text.substring(text.indexOf("Copyright"))
        .replace("\n", "\n * ")
        .replace("* \n", "*\n")
        .trim() + "/\n\n"
}
