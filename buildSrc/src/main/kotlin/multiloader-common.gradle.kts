import multiversion.VersionConfig

plugins {
    `java-library`
    `maven-publish`
}

@Suppress("UNCHECKED_CAST")
val getCurrentVersionConfig = rootProject.extra["getCurrentVersionConfig"] as () -> VersionConfig
val currentVersionConfig = getCurrentVersionConfig()

base {
    archivesName.set("${property("mod_id")}-${project.name}-${currentVersionConfig.common.modArtifactMinecraftVer}")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(currentVersionConfig.common.javaVersion))
    withSourcesJar()
    withJavadocJar()
}

repositories {
    exclusiveContent {
        forRepositories(
            maven {
                name = "ParchmentMC"
                url = uri("https://maven.parchmentmc.org/")
            },
            maven {
                name = "NeoForge"
                url = uri("https://maven.neoforged.net/releases")
            }
        )
        filter {
            includeGroup("org.parchmentmc.data")
        }
    }
}
