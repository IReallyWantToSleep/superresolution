import multiversion.BasePlatformConfig
import multiversion.Dependency
import multiversion.VersionConfig
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar

@Suppress("UNCHECKED_CAST")
val versionConfig = rootProject.extra["versionConfig"] as VersionConfig
@Suppress("UNCHECKED_CAST")
val getCurrentNeoFormVersion = rootProject.extra["getCurrentNeoFormVersion"] as () -> String

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
    compileOnly("org.spongepowered:mixin:0.8.7")
    compileOnly("io.github.spair:imgui-java-app:1.87.5")
    compileOnly("io.github.spair:imgui-java-binding:1.87.5")
    compileOnly("io.github.spair:imgui-java-lwjgl3:1.87.5")
    compileOnly("org.lwjgl:lwjgl-vulkan:${versionConfig.common.lwjglVersion}")

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
val mainSourceSet = sourceSets.getByName("main")
val irisapiSourceSet = sourceSets.maybeCreate("irisapi")
val sharedSourceSet = sourceSets.maybeCreate("shared")

tasks.register<JavaCompile>("genJNIHeader") {
    val outputDir = file("../native/cpp/SRNativeMain/include")

    source = fileTree("../common/src/main/java") {
        include("**/core/SuperResolutionNative.java", "**/thirdparty/nanovg/*.java")
    }

    classpath = mainSourceSet.compileClasspath + mainSourceSet.output
    destinationDirectory.set(file("$buildDir/jni-temp"))
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

tasks.named<Jar>("jar") {
    from(irisapiSourceSet.output)
    from(sharedSourceSet.output)
}

artifacts {
    add("commonJava", mainSourceSet.java.sourceDirectories.singleFile)
    add("commonJava", irisapiSourceSet.java.sourceDirectories.singleFile)
    add("commonJava", sharedSourceSet.java.sourceDirectories.singleFile)

    add("commonResources", mainSourceSet.resources.sourceDirectories.singleFile)
    add("commonResources", irisapiSourceSet.resources.sourceDirectories.singleFile)
    add("commonResources", sharedSourceSet.resources.sourceDirectories.singleFile)
}
