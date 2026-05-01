import multiversion.VersionConfig
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.jvm.tasks.Jar

plugins {
    id("multiloader-loader")
    id("net.neoforged.moddev.legacyforge")
}

@Suppress("UNCHECKED_CAST")
val versionConfig = rootProject.extra["versionConfig"] as VersionConfig

base {
    archivesName.set("super_resolution-forge-${versionConfig.common.modArtifactMinecraftVer}")
}

repositories {
    maven {
        url = uri("https://cursemaven.com")
        content {
            includeGroup("curse.maven")
        }
    }
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
        content {
            includeGroup("maven.modrinth")
        }
    }

    maven {
        name = "yoga"
        url = uri("https://repo1.maven.org/maven2")
        content {
            includeGroup("org.appliedenergistics.yoga")
        }
    }
    flatDir {
        dirs("../libs")
    }
}

legacyForge {
    version = "${versionConfig.common.minecraftVersion}-${versionConfig.forge.loaderVersion}"
    runs {
        configureEach {
            additionalRuntimeClasspathConfiguration.dependencies.add(dependencies.create("io.github.spair:imgui-java-app:1.87.5"))
            additionalRuntimeClasspathConfiguration.dependencies.add(dependencies.create("io.github.spair:imgui-java-binding:1.87.5"))
            additionalRuntimeClasspathConfiguration.dependencies.add(dependencies.create("io.github.spair:imgui-java-lwjgl3:1.87.5"))
            additionalRuntimeClasspathConfiguration.dependencies.add(dependencies.create("org.lwjgl:lwjgl-vulkan:${versionConfig.common.lwjglVersion}"))
            additionalRuntimeClasspathConfiguration.dependencies.add(dependencies.create("net.neoforged:bus:8.0.5"))
        }
        create("client") {
            client()
            gameDirectory = rootProject.file("runs/forge")
        }
    }
    mods {
        create(rootProject.property("mod_id").toString()) {
            sourceSet(extensions.getByType(SourceSetContainer::class.java).getByName("main"))
        }
    }
    val parchmentVersion = versionConfig.common.parchmentVersion
    if (parchmentVersion != null) {
        val parchmentParts = parchmentVersion.split(":")
        parchment {
            minecraftVersion = parchmentParts[0]
            mappingsVersion = parchmentParts[1]
        }
    }
}

extensions.configure<Any>("mixin") {
    val sourceSets = extensions.getByType(SourceSetContainer::class.java)
    withGroovyBuilder {
        "add"(sourceSets.getByName("main"), "super_resolution.refmap.json")
        "config"("super_resolution.mixins.json")
        "config"("super_resolution-forge.mixins.json")
        "config"("super_resolution-forge-compat.mixins.json")
        "config"("super_resolution.shadercompat.mixins.json")
        "config"("super_resolution_irisapi.mixins.json")
    }
}

val sourceSets = extensions.getByType(SourceSetContainer::class.java)
sourceSets.getByName("main").resources.srcDir("src/generated/resources")

dependencies {
    compileOnly("org.spongepowered:mixin:0.8.5")
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    compileOnly("org.jetbrains:annotations:25.0.0")
    implementation("io.github.spair:imgui-java-app:1.87.5")
    implementation("io.github.spair:imgui-java-binding:1.87.5")
    implementation("io.github.spair:imgui-java-lwjgl3:1.87.5")

    val lwjglVulkanDep = implementation("org.lwjgl:lwjgl-vulkan:${versionConfig.common.lwjglVersion}")
    if (lwjglVulkanDep != null) jarJar(lwjglVulkanDep)

    //modImplementation("dev.architectury:architectury-forge:${versionConfig.common.architecturyApiVersion}")
    implementation("net.fabricmc.fabric-api:fabric-api-base:0.4.39+80f8cf51bb")

    val busDep = implementation("net.neoforged:bus:8.0.5")
    if (busDep != null) jarJar(busDep)

    for (lib in versionConfig.forge.dependencies.modrinth) {
        val depName = "maven.modrinth:${lib.name}:${lib.version}-forge,${lib.minecraftVersion ?: versionConfig.common.minecraftVersion}"
        if (lib.compileOnly) {
            modCompileOnly(depName)
        } else {
            modImplementation(depName)
        }
    }

    for (lib in versionConfig.forge.dependencies.local) {
        if (lib.isMod) {
            if (lib.compileOnly) {
                modCompileOnly(mapOf("name" to lib.name, "ext" to "jar"))
            } else {
                modImplementation(mapOf("name" to lib.name, "ext" to "jar"))
            }
        } else {
            if (lib.compileOnly) {
                compileOnly(mapOf("name" to lib.name, "ext" to "jar"))
            } else {
                implementation(mapOf("name" to lib.name, "ext" to "jar"))
            }
        }
    }
}

sourceSets.forEach {
    val dir = layout.buildDirectory.dir("sourcesSets/${it.name}")
    it.output.setResourcesDir(dir.get().asFile)
    it.java.destinationDirectory.set(dir)
}

tasks.named<ProcessResources>("processResources") {
    val forgeModVersion = project.version.toString()
    val forgeVersionRange = versionConfig.common.forgeVersionRange.toString()

    inputs.property("version", forgeModVersion)
    inputs.property("versionRange", forgeVersionRange)

    filesMatching("META-INF/mods.toml") {
        expand(mapOf("version" to forgeModVersion))
        filter { line: String ->
            line.replace("\"{versionRange}\"", "\"$forgeVersionRange\"")
        }
    }
    if (gradle.extensions.extraProperties.properties["isUseDebugLib"] as? Boolean == true){
        exclude("**/libSuperResolution*+*+release.*")
    } else {
        exclude("**/libSuperResolution*+*+debug.*")
    }
}

tasks.named<Jar>("jar") {
    manifest.attributes(
        mapOf(
            "MixinConfigs" to "super_resolution.mixins.json,super_resolution-forge.mixins.json,super_resolution-forge-compat.mixins.json,super_resolution.shadercompat.mixins.json,super_resolution_irisapi.mixins.json"
        )
    )
}
