import java.util.Properties
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.neoforged.moddevgradle.dsl.ModDevExtension
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeExtension
import net.neoforged.moddevgradle.legacyforge.dsl.MixinExtension
import org.gradle.language.jvm.tasks.ProcessResources
import utils.SrLoader
import utils.configureSrLoaderDependencies
import utils.loomPlugin
import utils.srForgeMixinConfigs
import utils.srLegacyRuntimeLibraries
import utils.srVersionConfig

plugins {
    `java-library`
}

val config = srVersionConfig
val loader = SrLoader.entries.single { name == "wisteria-${it.id}" }
val wisteriaCommon = evaluationDependsOn(":wisteria-common")
val srLoader = evaluationDependsOn(":${loader.id}")
val wisteriaMain = wisteriaCommon.extensions.getByType<SourceSetContainer>().named("main")
val srMain = srLoader.extensions.getByType<SourceSetContainer>().named("main")
val main = sourceSets.named("main")
val clientDirectory = rootProject.layout.projectDirectory.dir("runs/${loader.id}")
val metadata = wisteriaCommon.extra["wisteriaMetadata"] as Properties
val modId = metadata.getProperty("mod_id")
val srModId = rootProject.property("mod_id").toString()

group = wisteriaCommon.group
version = wisteriaCommon.version
base.archivesName.set("$modId-${loader.id}-${config.common.modArtifactMinecraftVer}")
java.toolchain.languageVersion.set(JavaLanguageVersion.of(config.common.javaVersion))

fun configureModDev(extension: ModDevExtension) = with(extension) {
    mods {
        register(modId) {
            sourceSet(main.get())
            sourceSet(wisteriaMain.get())
        }
        register(srModId) { sourceSet(srMain.get()) }
    }
    runs {
        register("client") {
            client()
            gameDirectory.set(clientDirectory)
            jvmArguments.add("-Dmixin.debug.export=true")
            if (loader == SrLoader.NEOFORGE) {
                jvmArguments.add("-Dsodium.checks.issue2561=false")
                jvmArguments.add("-XX:+CreateMinidumpOnCrash")
            }
            config.srLegacyRuntimeLibraries(loader).forEach {
                additionalRuntimeClasspathConfiguration.dependencies.add(project.dependencies.create(it))
            }
        }
    }
    config.common.parchmentVersion?.split(":", limit = 2)?.let { (mc, mappings) ->
        parchment.minecraftVersion.set(mc)
        parchment.mappingsVersion.set(mappings)
    }
}

when (loader) {
    SrLoader.FABRIC -> {
        apply(plugin = config.loomPlugin)
        // cross-project classpath groups need Loom's published source set paths
        wisteriaCommon.pluginManager.apply("net.fabricmc.fabric-loom-companion")
        val loom = extensions.getByType<LoomGradleExtensionAPI>()
        dependencies {
            add("minecraft", "com.mojang:minecraft:${config.common.minecraftVersion}")
            if (!config.unobfuscated) {
                add("mappings", loom.layered {
                    officialMojangMappings()
                    config.common.parchmentVersion?.let { parchment("org.parchmentmc.data:parchment-$it@zip") }
                })
            }
        }
        loom.mods {
            register(modId) {
                sourceSet("main")
                sourceSet("main", wisteriaCommon.path)
            }
            register(srModId) { sourceSet("main", srLoader.path) }
        }
        loom.runs {
            named("client") {
                runDirectory.set(clientDirectory)
                jvmArguments.addAll(
                    "-XX:+CreateMinidumpOnCrash", "--enable-preview", "--enable-native-access=ALL-UNNAMED",
                    "-Dmixin.debug.export=true", "-Dsodium.checks.issue2561=false",
                )
                programArguments.addAll("--graphicsBackend=opengl", "--renderDebugLabels=true")
            }
        }
    }
    SrLoader.NEOFORGE -> {
        apply(plugin = "net.neoforged.moddev")
        val neoForge = extensions.getByType<NeoForgeExtension>()
        configureModDev(neoForge)
        neoForge.setVersion(config.neoforge.loaderVersion)
    }
    SrLoader.FORGE -> {
        apply(plugin = "net.neoforged.moddev.legacyforge")
        val forge = extensions.getByType<LegacyForgeExtension>()
        configureModDev(forge)
        forge.setVersion("${config.common.minecraftVersion}-${config.forge.loaderVersion}")
        val mixin = extensions.getByType<MixinExtension>()
        srForgeMixinConfigs.forEach(mixin::config)
    }
}

configureSrLoaderDependencies(loader, packageSr = false)

dependencies {
    implementation(wisteriaMain.get().output)
    runtimeOnly(project(path = srLoader.path, configuration = "srDevOutput"))
}

tasks.named<Jar>("jar") {
    from(wisteriaMain.map { it.output })
}

val expansions = mapOf(
    "mod_id" to modId,
    "mod_name" to metadata.getProperty("mod_name"),
    "mod_version" to version.toString(),
    "mod_license" to metadata.getProperty("mod_license"),
    "mod_authors" to metadata.getProperty("mod_authors"),
    "sr_mod_id" to srModId,
    "sr_min_version" to metadata.getProperty("sr_min_version"),
    "java_version" to config.common.javaVersion,
    "fabric_loader_version" to config.fabric.loaderVersion,
    "fabric_mc_range" to config.common.fabricVersionRange.joinToString { "\"$it\"" },
    "neoforge_mc_range" to config.common.neoforgeVersionRange.orEmpty(),
    "forge_loader_range" to "[${config.forge.loaderVersion.substringBefore('.')},)",
    "forge_mc_range" to config.common.forgeVersionRange.orEmpty(),
)
tasks.named<ProcessResources>("processResources") {
    inputs.properties(expansions)
    filesMatching(listOf("fabric.mod.json", "META-INF/mods.toml", "META-INF/neoforge.mods.toml")) {
        expand(expansions)
    }
}
