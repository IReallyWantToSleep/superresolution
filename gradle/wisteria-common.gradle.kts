import java.util.Properties
import org.gradle.language.jvm.tasks.ProcessResources
import utils.srVersionConfig

plugins {
    `java-library`
}

val config = srVersionConfig
val metadata = Properties().apply {
    val text = providers.fileContents(rootProject.layout.projectDirectory.file("modules/wisteria/gradle.properties"))
    load(text.asText.get().reader())
}
extra["wisteriaMetadata"] = metadata

group = metadata.getProperty("group")
version = "${metadata.getProperty("mod_version")}+${config.common.minecraftVersion}"
java.toolchain.languageVersion.set(JavaLanguageVersion.of(config.common.javaVersion))

// Match the loader-independent Minecraft compile environment used by SR common.
if (config.common.minecraftVersion == "1.20.1") {
    apply(plugin = "net.neoforged.moddev.legacyforge")
    extensions.getByType<net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeExtension>().apply {
        setMcpVersion(config.common.minecraftVersion)
        config.common.parchmentVersion?.split(":", limit = 2)?.let { (mc, mappings) ->
            parchment.minecraftVersion.set(mc)
            parchment.mappingsVersion.set(mappings)
        }
    }
} else {
    apply(plugin = "net.neoforged.moddev")
    extensions.getByType<net.neoforged.moddevgradle.dsl.NeoForgeExtension>().apply {
        @Suppress("UNCHECKED_CAST")
        val neoFormVersion = config.common.neoFormVersion
            ?: (rootProject.extra["getCurrentNeoFormVersion"] as () -> String)()
        config.common.parchmentVersion?.split(":", limit = 2)?.let { (mc, mappings) ->
            parchment.minecraftVersion.set(mc)
            parchment.mappingsVersion.set(mappings)
        }
        setNeoFormVersion(neoFormVersion)
    }
}

dependencies {
    compileOnly(project(path = ":common", configuration = "srCommonClasses"))
    compileOnly("org.lwjgl:lwjgl-vulkan:${config.common.lwjglVersion}") { isTransitive = false }
    compileOnly("net.neoforged:bus:8.0.5")
}

val streamlineLibraryNames = listOf(
    "NvLowLatencyVk.dll", "nvngx_dlssg.dll", "sl.common.dll", "sl.dlss_g.dll",
    "sl.interposer.dll", "sl.pcl.dll", "sl.reflex.dll",
)
val streamlineLibraries = providers.gradleProperty("streamline_bin_dir").orElse("K:/sl/bin/x64").map { path ->
    val dir = rootProject.file(path)
    streamlineLibraryNames.map(dir::resolve)
}
val streamlineOutput = layout.buildDirectory.dir("generated/streamline")
val syncStreamlineLibraries = tasks.register("syncStreamlineLibraries") {
    description = "Copies the Streamline runtime for Wisteria."
    inputs.files(streamlineLibraries).withPathSensitivity(PathSensitivity.NAME_ONLY)
    outputs.dir(streamlineOutput)
    doLast {
        val sources = streamlineLibraries.get()
        val missing = sources.filterNot { it.isFile }
        if (missing.isNotEmpty()) {
            throw GradleException("Set -Pstreamline_bin_dir to a complete Streamline SDK bin/x64 directory. Missing: ${missing.joinToString()}")
        }
        val dir = streamlineOutput.get().asFile
        dir.mkdirs()
        sources.forEach { it.copyTo(dir.resolve(it.name), overwrite = true) }
        dir.resolve("index.txt").writeText(sources.joinToString("\n", postfix = "\n") { "${it.name} ${it.length()}" })
    }
}

tasks.named<ProcessResources>("processResources") {
    from(syncStreamlineLibraries) { into("streamline") }
}
