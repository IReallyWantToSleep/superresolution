import multiversion.BasePlatformConfig
import multiversion.Dependency
import multiversion.VersionConfig
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.tasks.Jar

plugins {
    `java-library`
}

@Suppress("UNCHECKED_CAST")
val versionConfig = rootProject.extra["versionConfig"] as VersionConfig
@Suppress("UNCHECKED_CAST")
val getCurrentNeoFormVersion = rootProject.extra["getCurrentNeoFormVersion"] as () -> String
val imguiVersion = if (versionConfig.common.minecraftVersion >= "26.1") "1.92.0" else "1.90.0"

val isNewVersion = versionConfig.common.minecraftVersion > "1.20.1"
if (isNewVersion) {
    apply(plugin = "net.neoforged.moddev")
} else {
    apply(plugin = "net.neoforged.moddev.legacyforge")
}


if (isNewVersion) {
    extensions.configure<Any>("neoForge") {
        withGroovyBuilder {
            setProperty("neoFormVersion", versionConfig.common.neoFormVersion ?: getCurrentNeoFormVersion())
            val parchmentVersion = versionConfig.common.parchmentVersion
            if (parchmentVersion != null) {
                "parchment" {
                    val parts = parchmentVersion.split(":")
                    setProperty("minecraftVersion", parts[0])
                    setProperty("mappingsVersion", parts[1])
                }
            }
        }
    }
} else {
    extensions.configure<Any>("legacyForge") {
        withGroovyBuilder {
            setProperty("mcpVersion", versionConfig.common.minecraftVersion)
            val parchmentVersion = versionConfig.common.parchmentVersion
            if (parchmentVersion != null) {
                "parchment" {
                    val parts = parchmentVersion.split(":")
                    setProperty("minecraftVersion", parts[0])
                    setProperty("mappingsVersion", parts[1])
                }
            }
        }
    }
}

repositories {
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

fun findIris(config: BasePlatformConfig?): Pair<Dependency, Boolean>? {
    if (config == null) return null

    config.dependencies?.modrinth?.forEach { dep ->
        if (dep.name.trim() == "iris" || dep.name.trim() == "oculus") {
            return dep to false
        }
    }

    config.dependencies?.local?.forEach { dep ->
        if (dep.name.contains("iris") || dep.name.contains("oculus")) {
            return dep to true
        }
    }

    return null
}
fun findFirstConfiguration(vararg names: String): String {
    return names.firstOrNull { name -> configurations.findByName(name) != null } ?: names.last()
}
fun DependencyHandler.modCompileOnlyCompat(notation: Any) =
    add(findFirstConfiguration("modCompileOnly", "compileOnly"), notation)


dependencies {
    compileOnly("org.anarres:jcpp:1.4.14")
    compileOnly("org.spongepowered:mixin:0.8.7")
    compileOnly("io.github.spair:imgui-java-app:$imguiVersion")
    compileOnly("io.github.spair:imgui-java-binding:$imguiVersion")
    compileOnly("io.github.spair:imgui-java-lwjgl3:$imguiVersion")
    compileOnly("org.lwjgl:lwjgl-vulkan:${versionConfig.common.lwjglVersion}")
    compileOnly("org.lwjgl:lwjgl-vma:${versionConfig.common.lwjglVersion}")

    compileOnly("com.electronwill.night-config:toml:3.6.0")
    compileOnly("com.electronwill.night-config:core:3.6.0")

    compileOnly("net.neoforged:bus:8.0.5")

    if (versionConfig.common.minecraftVersion <= "1.21.1") {
        compileOnly("org.ow2.asm:asm:9.7.1")
        compileOnly("org.ow2.asm:asm-tree:9.7.1")
    } else {
        compileOnly("org.ow2.asm:asm:9.6")
        compileOnly("org.ow2.asm:asm-tree:9.6")
    }

    var irisDependency: Pair<Dependency, Boolean>? = null
    var irisPlatform: String? = null

    if (versionConfig.common.enableNeoForge && irisDependency == null) {
        irisDependency = findIris(versionConfig.neoforge)
        irisPlatform = "neoforge"
    }

    if (versionConfig.common.enableForge && irisDependency == null) {
        irisDependency = findIris(versionConfig.forge)
        irisPlatform = "forge"
    }

    if (versionConfig.common.enableFabric && irisDependency == null) {
        irisDependency = findIris(versionConfig.fabric)
        irisPlatform = "fabric"
    }

    if (irisDependency != null) {
        val dep = irisDependency.first
        if (irisDependency.second) {
            compileOnly(mapOf("name" to dep.name, "ext" to "jar"))
        } else {
            if (irisPlatform == "neoforge") {
                compileOnly(
                    "maven.modrinth:${dep.name}:${dep.version}-neo,${dep.minecraftVersion ?: versionConfig.common.minecraftVersion}"
                )
            } else {
                modCompileOnlyCompat(
                    "maven.modrinth:${dep.name}:${dep.version}-${irisPlatform},${dep.minecraftVersion ?: versionConfig.common.minecraftVersion}"
                )
            }
        }
    }
}

configurations {
    create("commonJava") {
        isCanBeResolved = false
        isCanBeConsumed = true
    }
    create("commonResources") {
        isCanBeResolved = false
        isCanBeConsumed = true
    }
}

val sourceSets = extensions.getByType(SourceSetContainer::class.java)
val javaToolchains = extensions.getByType(JavaToolchainService::class.java)
val mainSourceSet = sourceSets.getByName("main")
val irisapiSourceSet = sourceSets.maybeCreate("irisapi")
val sharedSourceSet = sourceSets.maybeCreate("shared")
val hackSourceSet = sourceSets.maybeCreate("hack")
val shaderCompatSourceSet = sourceSets.maybeCreate("shadercompat")

tasks.register<JavaCompile>("genJNIHeader") {
    val outputDir = file("../native/cpp/SRNativeMain/include")

    source = fileTree("../common/src/main/java") {
        include(
            "**/core/SuperResolutionNative.java",
            "**/core/streamline/StreamlineNative.java",
            "**/core/ngx/NgxNative.java",
            "**/thirdparty/nanovg/*.java"
        )
    }

    classpath = mainSourceSet.compileClasspath + mainSourceSet.output
    destinationDirectory.set(file("$buildDir/jni-temp"))
    javaCompiler.set(javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(versionConfig.common.javaVersion))
    })
    options.headerOutputDirectory.set(outputDir)
    options.annotationProcessorPath = configurations.getByName("annotationProcessor")
    options.compilerArgs.addAll(listOf("-encoding", "UTF-8", "-proc:full"))

    doFirst {
        outputDir.mkdirs()
    }

    doLast {
        println("JNI 头文件已生成到: ${outputDir.absolutePath}")
        delete("$buildDir/jni-temp")
    }
}

sharedSourceSet.annotationProcessorPath += mainSourceSet.annotationProcessorPath
sharedSourceSet.compileClasspath += mainSourceSet.compileClasspath
sharedSourceSet.runtimeClasspath += mainSourceSet.runtimeClasspath

irisapiSourceSet.annotationProcessorPath += mainSourceSet.annotationProcessorPath
irisapiSourceSet.compileClasspath += mainSourceSet.compileClasspath
irisapiSourceSet.runtimeClasspath += mainSourceSet.runtimeClasspath
irisapiSourceSet.compileClasspath += sharedSourceSet.output
irisapiSourceSet.runtimeClasspath += sharedSourceSet.output

mainSourceSet.compileClasspath += irisapiSourceSet.output
mainSourceSet.runtimeClasspath += irisapiSourceSet.output
mainSourceSet.compileClasspath += sharedSourceSet.output
mainSourceSet.runtimeClasspath += sharedSourceSet.output

hackSourceSet.annotationProcessorPath += mainSourceSet.annotationProcessorPath
hackSourceSet.compileClasspath += mainSourceSet.compileClasspath
hackSourceSet.compileClasspath += mainSourceSet.output
hackSourceSet.compileClasspath += sharedSourceSet.output
hackSourceSet.runtimeClasspath += mainSourceSet.runtimeClasspath
hackSourceSet.runtimeClasspath += mainSourceSet.output
hackSourceSet.runtimeClasspath += sharedSourceSet.output

shaderCompatSourceSet.annotationProcessorPath += mainSourceSet.annotationProcessorPath
shaderCompatSourceSet.compileClasspath += mainSourceSet.compileClasspath
shaderCompatSourceSet.compileClasspath += mainSourceSet.output
shaderCompatSourceSet.compileClasspath += sharedSourceSet.output
shaderCompatSourceSet.compileClasspath += irisapiSourceSet.output
shaderCompatSourceSet.runtimeClasspath += mainSourceSet.runtimeClasspath
shaderCompatSourceSet.runtimeClasspath += mainSourceSet.output
shaderCompatSourceSet.runtimeClasspath += sharedSourceSet.output
shaderCompatSourceSet.runtimeClasspath += irisapiSourceSet.output

tasks.named<Jar>("jar") {
    from(irisapiSourceSet.output)
    from(sharedSourceSet.output)
    from(hackSourceSet.output)
    from(shaderCompatSourceSet.output)
}

artifacts {
    listOf(mainSourceSet, irisapiSourceSet, sharedSourceSet, hackSourceSet, shaderCompatSourceSet).forEach { sourceSet ->
        sourceSet.java.sourceDirectories.files.forEach { dir ->
            add("commonJava", dir)
        }
        sourceSet.resources.sourceDirectories.files.forEach { dir ->
            add("commonResources", dir)
        }
    }
}

val useDebugLib = gradle.extensions.extraProperties.properties["isUseDebugLib"] as? Boolean == true
val streamlineBinDir = rootProject.file(
    providers.gradleProperty("streamline_bin_dir").orElse("K:/sl/bin/x64").get()
)
val streamlineResourceLibDir = layout.projectDirectory.dir("src/main/resources/lib")
val requiredStreamlineLibraries = listOf(
    "NvLowLatencyVk.dll",
    "sl.common.dll",
    "sl.dlss_g.dll",
    "sl.interposer.dll",
    "sl.pcl.dll",
    "sl.reflex.dll"
)

fun registerStreamlineSyncTask(
    taskName: String,
    sourceDir: File,
    targetDirName: String
) = tasks.register<Sync>(taskName) {
    group = "build"
    description = "Copy required Streamline libraries into $targetDirName"

    from(sourceDir) {
        include(requiredStreamlineLibraries)
    }
    into(streamlineResourceLibDir.dir(targetDirName))

    doFirst {
        val missingLibraries = requiredStreamlineLibraries.filterNot { sourceDir.resolve(it).isFile }
        if (missingLibraries.isNotEmpty()) {
            throw GradleException(
                "Streamline library source is incomplete: ${sourceDir.absolutePath}; missing: " +
                        missingLibraries.joinToString()
            )
        }
    }
}

val syncStreamlineReleaseLibs = registerStreamlineSyncTask(
    "syncStreamlineReleaseLibs",
    streamlineBinDir,
    "sl.rel"
)
val syncStreamlineDebugLibs = registerStreamlineSyncTask(
    "syncStreamlineDebugLibs",
    streamlineBinDir.resolve("development"),
    "sl.dev"
)

tasks.named<ProcessResources>("processResources") {
    dependsOn(syncStreamlineReleaseLibs, syncStreamlineDebugLibs)

    if (useDebugLib) {
        exclude("**/libSuperResolution*+*+release.*")
    } else {
        exclude("**/libSuperResolution*+*+debug.*")
    }

    exclude(
        "lib/sl.dev/**",
        "lib/sl.rel/**",
        "lib/sl.*.dll",
        "lib/NvLowLatencyVk.dll"
    )

    from(streamlineResourceLibDir.dir(if (useDebugLib) "sl.dev" else "sl.rel")) {
        include(requiredStreamlineLibraries)
        into("lib")
    }
}
