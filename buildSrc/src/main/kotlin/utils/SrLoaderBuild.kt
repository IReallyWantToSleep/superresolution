package utils

import multiversion.VersionConfig
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*

enum class SrLoader(val id: String) {
    FABRIC("fabric"),
    NEOFORGE("neoforge"),
    FORGE("forge"),
}

val Project.srVersionConfig: VersionConfig
    get() = rootProject.extra["versionConfig"] as VersionConfig

val VersionConfig.loomPlugin: String
    get() = if (unobfuscated) "net.fabricmc.fabric-loom" else "net.fabricmc.fabric-loom-remap"

val srForgeMixinConfigs = listOf(
    "super_resolution.mixins.json",
    "super_resolution.hack.mixins.json",
    "super_resolution-forge.mixins.json",
    "super_resolution-forge-compat.mixins.json",
    "super_resolution.shadercompat.mixins.json",
    "super_resolution_irisapi.mixins.json",
)

private fun VersionConfig.imguiLibraries(): List<String> {
    val version = if (MinecraftVersion.of(common.minecraftVersion) >= MinecraftVersion.of("26.1")) "1.92.0" else "1.90.0"
    return listOf("app", "binding", "lwjgl3").map { "io.github.spair:imgui-java-$it:$version" }
}

private const val EVENT_BUS = "net.neoforged:bus:8.0.5"

/** Libraries ModDev runs need on the legacy classpath in addition to the mod classpath. */
fun VersionConfig.srLegacyRuntimeLibraries(loader: SrLoader): List<String> {
    if (loader == SrLoader.NEOFORGE && MinecraftVersion.of(common.minecraftVersion) >= MinecraftVersion.of("1.21.9")) {
        return emptyList()
    }
    val lwjgl = common.lwjglVersion
    val libraries = imguiLibraries() + listOf(
        "org.lwjgl:lwjgl-vulkan:$lwjgl",
        "org.lwjgl:lwjgl-vma:$lwjgl",
        "org.lwjgl:lwjgl-vma::natives-windows",
        "org.lwjgl:lwjgl-vma::natives-linux",
    )
    return if (loader == SrLoader.FORGE) libraries + EVENT_BUS else libraries
}

/** Declare dependencies in the consumer, so Loom and LegacyForge can remap external mods themselves. */
fun Project.configureSrLoaderDependencies(loader: SrLoader, packageSr: Boolean) {
    val config = srVersionConfig
    val mc = MinecraftVersion.of(config.common.minecraftVersion)
    val packageDevLibraries = packageSr && gradle.extensions.extraProperties["isDev"] == true
    val nested = if (loader == SrLoader.FABRIC) "include" else "jarJar"
    val remapsMods = loader != SrLoader.NEOFORGE && configurations.findByName("modImplementation") != null
    val platform = when (loader) {
        SrLoader.FABRIC -> config.fabric
        SrLoader.NEOFORGE -> config.neoforge
        SrLoader.FORGE -> config.forge
    }

    configureSrLoaderRepositories(loader)

    val libraries = if (loader == SrLoader.NEOFORGE) configurations.create("libraries") {
        isCanBeConsumed = false
    } else null
    if (libraries != null) {
        extensions.getByType<SourceSetContainer>().configureEach {
            runtimeClasspath += libraries
        }
    }

    fun DependencyHandler.addLibrary(notation: Any, bundle: Boolean = false, neoLibrary: Boolean = false) {
        val dependency = add("implementation", notation) ?: return
        if (bundle && packageSr) add(nested, dependency)
        if (neoLibrary) add("libraries", dependency)
    }

    fun DependencyHandler.addMod(notation: Any, compileOnly: Boolean = false) {
        val scope = when {
            compileOnly && remapsMods -> "modCompileOnly"
            compileOnly -> "compileOnly"
            remapsMods -> "modImplementation"
            else -> "implementation"
        }
        add(scope, notation)
    }

    dependencies {
        if (loader == SrLoader.FABRIC) {
            addMod("net.fabricmc:fabric-loader:${config.fabric.loaderVersion}")
            addMod("net.fabricmc.fabric-api:fabric-api:${config.fabric.apiVersion}")
            config.common.clothConfigVersion?.let { addMod("me.shedaniel.cloth:cloth-config-fabric:$it") }
            config.fabric.modmenuVersion?.let { addMod("com.terraformersmc:modmenu:$it") }
            addLibrary("net.lenni0451:Reflect:1.3.4")
            addLibrary("org.antlr:antlr4-runtime:4.13.1")
            addLibrary("net.jodah:typetools:0.6.3", bundle = true)
            val glslTransformer = if (mc < MinecraftVersion.of("1.21.8")) "2.0.1" else "3.0.0-pre3"
            addLibrary("io.github.douira:glsl-transformer:$glslTransformer")
            addLibrary("com.electronwill.night-config:toml:3.8.3", bundle = true)
            addLibrary("com.electronwill.night-config:core:3.8.3", bundle = true)
        }
        if (loader != SrLoader.FORGE) {
            config.common.architecturyApiVersion?.let { addMod("dev.architectury:architectury-${loader.id}:$it") }
        }
        addLibrary("org.anarres:jcpp:1.4.14")
        val isNeoForge = loader == SrLoader.NEOFORGE
        config.imguiLibraries().forEach { addLibrary(it, bundle = packageDevLibraries, neoLibrary = isNeoForge) }
        if (!isNeoForge) addLibrary(EVENT_BUS, bundle = true)

        if (mc < MinecraftVersion.of("26.2")) {
            val lwjgl = config.common.lwjglVersion
            addLibrary("org.lwjgl:lwjgl-vulkan:$lwjgl", bundle = true, neoLibrary = isNeoForge)
            if (loader == SrLoader.FABRIC) {
                addLibrary("org.lwjgl:lwjgl-vma:$lwjgl", bundle = true)
                addLibrary("org.lwjgl:lwjgl-vma::natives-windows", bundle = true)
                addLibrary("org.lwjgl:lwjgl-vma::natives-linux", bundle = true)
            } else {
                val vma = mergeVmaNatives(lwjgl)
                addLibrary(files(vma.flatMap { it.archiveFile }), bundle = true, neoLibrary = isNeoForge)
            }
        }
        // Sodium exposes Fabric's Event type on both Forge-based loaders for compilation.
        if (loader == SrLoader.FORGE) add("compileOnly", "net.fabricmc.fabric-api:fabric-api-base:0.4.39+80f8cf51bb")
        if (isNeoForge) add("compileOnly", "net.fabricmc.fabric-api:fabric-api-base:0.4.64+9ec45cd8e8")

        for (lib in platform.dependencies.modrinth) {
            val fromCaffeine = loader != SrLoader.FORGE &&
                (lib.name == "sodium.maven" || (lib.name == "sodium" && mc > MinecraftVersion.of("1.21.10")))
            if (fromCaffeine) {
                if (isNeoForge) addMod("net.caffeinemc:sodium-neoforge-mod:${lib.version}", lib.compileOnly)
                addMod("net.caffeinemc:sodium-${loader.id}:${lib.version}", lib.compileOnly)
            } else {
                val notation = "maven.modrinth:${lib.name}:${lib.version}-${loader.id},${lib.minecraftVersion ?: config.common.minecraftVersion}"
                addMod(if (lib.useJarJar && loader != SrLoader.FORGE) JarJar.extractJars(project, notation) else notation, lib.compileOnly)
            }
        }
        for (lib in platform.dependencies.curseforge) {
            val notation = lib.curseMavenNotation()
            addMod(if (lib.useJarJar && loader != SrLoader.FORGE) JarJar.extractJars(project, notation) else notation, lib.compileOnly)
        }
        for (lib in platform.dependencies.local) {
            val path = rootProject.file("libs/${lib.name}.jar")
            val notation = when {
                loader == SrLoader.FORGE -> mapOf("name" to lib.name, "ext" to "jar")
                lib.isMod && lib.useJarJar -> JarJar.extractJars(project, path)
                else -> files(path)
            }
            if (lib.isMod) addMod(notation, lib.compileOnly)
            else add(if (lib.compileOnly) "compileOnly" else "implementation", notation)
        }
    }
}

private fun Project.configureSrLoaderRepositories(loader: SrLoader) = repositories {
    maven("https://cursemaven.com") { content { includeGroup("curse.maven") } }
    if (loader == SrLoader.FABRIC) maven("https://maven.terraformersmc.com") { name = "Terraformers" }
    maven("https://api.modrinth.com/maven") {
        name = "Modrinth"
        content { includeGroup("maven.modrinth") }
    }
    maven("https://repo1.maven.org/maven2") { name = "yoga" }
    if (loader == SrLoader.FORGE) {
        flatDir { dirs(rootProject.file("libs")) }
    } else {
        maven("https://maven.caffeinemc.net/releases") {
            name = "CaffeineMC"
            mavenContent { releasesOnly() }
            content { includeGroup("net.caffeinemc") }
        }
        maven("https://maven.caffeinemc.net/snapshots") {
            mavenContent { snapshotsOnly() }
            content { includeGroup("net.caffeinemc") }
        }
    }
}

/** Keep the VMA native libraries next to the Java classes in Forge-family environments. */
private fun Project.mergeVmaNatives(version: String) = tasks.register<Jar>("mergeVmaNatives") {
    archiveFileName.set("lwjgl-vma-$version-merged.jar")
    destinationDirectory.set(layout.buildDirectory.dir("mergedVma"))
    val vma = "org.lwjgl:lwjgl-vma:$version"
    for (notation in listOf(vma, "$vma:natives-windows", "$vma:natives-linux")) {
        val dependency: Dependency = project.dependencies.create(notation)
        val resolved = configurations.detachedConfiguration(dependency).apply { isTransitive = false }
        from(providers.provider { resolved.files.map(project::zipTree) })
    }
    manifest.attributes("Automatic-Module-Name" to "org.lwjgl.vma")
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

/** Publish directories with their producer tasks instead of exposing a loader's jar variant. */
fun Project.publishSrDirectories(name: String, sourceSets: List<SourceSet>, resources: Boolean) {
    val output = configurations.consumable(name)
    sourceSets.forEach { sourceSet ->
        val dirs = if (resources) sourceSet.output.files else sourceSet.output.classesDirs.files
        val builtBy = if (resources) sourceSet.classesTaskName else sourceSet.compileJavaTaskName
        dirs.forEach { dir ->
            artifacts.add(output.name, dir) {
                type = "directory"
                builtBy(builtBy)
            }
        }
    }
}
