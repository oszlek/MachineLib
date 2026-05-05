plugins {
    id("architectury-plugin") version "3.4.164"
    id("dev.architectury.loom") version "1.7.416" apply false
    id("com.gradleup.shadow") version "8.3.6" apply false
}

architectury {
    minecraft = project.property("minecraft.version").toString()
}

allprojects {
    group = "dev.galacticraft"
    version = project.property("mod.version").toString()
}

// NOTE (Phase 0 scaffolding): the full version-suffix computation (grgit/CI run number),
// spotless license-header enforcement, maven-publish, and jar manifest attributes from the
// pre-port single-module build are intentionally deferred to a Phase 0 cleanup task once
// both loaders build. They are not required to produce or launch the mod.
