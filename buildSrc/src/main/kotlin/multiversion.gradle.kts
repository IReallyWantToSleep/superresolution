import groovy.json.JsonSlurper
import multiversion.VersionConfig
import org.gradle.api.GradleException
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths

plugins {
    `java-library`
    `maven-publish`
}

fun fetchAllReleaseVersions(): List<String> {
    return try {
        val connection = URL("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json").openConnection().apply {
            connectTimeout = 500
            readTimeout = 500
        }

        @Suppress("UNCHECKED_CAST")
        val manifest = JsonSlurper().parse(connection.getInputStream()) as Map<String, Any?>

        println("Minecraft版本信息获取成功")

        val versions = (manifest["versions"] as? List<*>)
            .orEmpty()
            .mapNotNull { it as? Map<*, *> }
            .filter { it["type"] == "release" }
            .mapNotNull { it["id"]?.toString() }

        println("最新的Minecraft ${versions.firstOrNull().orEmpty()}")
        versions
    } catch (e: Exception) {
        e.printStackTrace()
        println("Minecraft版本信息获取失败，使用默认版本列表")
        getDefaultReleaseVersions()
    }
}

fun getDefaultReleaseVersions(): List<String> {
    return listOf(
        "26.2","26.1.2", "26.1.1", "26.1", "1.21.11",
        "1.21.10", "1.21.9",
        "1.21.8", "1.21.7", "1.21.6", "1.21.5", "1.21.4",
        "1.21.3", "1.21.2", "1.21.1", "1.21",
        "1.20.6", "1.20.5", "1.20.4", "1.20.3",
        "1.20.2", "1.20.1", "1.20", "1.19.4",
        "1.19.3", "1.19.2", "1.19.1", "1.19",
        "1.18.2", "1.18.1", "1.18", "1.17.1",
        "1.17", "1.16.5", "1.16.4", "1.16.3",
        "1.16.2", "1.16.1", "1.16", "1.15.2",
        "1.15.1", "1.15", "1.14.4", "1.14.3",
        "1.14.2", "1.14.1", "1.14", "1.13.2",
        "1.13.1", "1.13", "1.12.2", "1.12.1",
        "1.12", "1.11.2", "1.11.1", "1.11",
        "1.10.2", "1.10.1", "1.10", "1.9.4",
        "1.9.3", "1.9.2", "1.9.1", "1.9",
        "1.8.9", "1.8.8", "1.8.7", "1.8.6",
        "1.8.5", "1.8.4", "1.8.3", "1.8.2",
        "1.8.1", "1.8", "1.7.10", "1.7.9",
        "1.7.8", "1.7.7", "1.7.6", "1.7.5",
        "1.7.4", "1.7.3", "1.7.2", "1.6.4",
        "1.6.2", "1.6.1", "1.5.2", "1.5.1",
        "1.4.7", "1.4.5", "1.4.6", "1.4.4",
        "1.4.2", "1.3.2", "1.3.1", "1.2.5",
        "1.2.4", "1.2.3", "1.2.2", "1.2.1",
        "1.1", "1.0"
    )
}

fun toVersionCode(verStr: String): Int {
    val parts = verStr.split(".")
    val a = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val b = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val c = parts.getOrNull(2)?.toIntOrNull() ?: 0
    return (a * 10000000) + (b * 1000) + c
}

fun getVersionConfigPath(): Path {
    val configName = providers.gradleProperty("minecraft_version_config").orNull
        ?: project.findProperty("minecraft_version_config")?.toString()
        ?: throw GradleException("缺少属性 minecraft_version_config")

    return Paths.get(rootDir.toString(), "configs", "$configName.json")
}

val versionConfigVar = try {
    VersionConfig.loadFromFile(getVersionConfigPath().toFile())
} catch (e: Exception) {
    println("无法解析版本配置")
    e.printStackTrace()
    throw GradleException("无法解析版本配置 ${getVersionConfigPath()}")
}

extra["allMinecraftVersions"] = if (providers.gradleProperty("sr.refreshVersions").orNull?.toBoolean() == true) {
    fetchAllReleaseVersions()
} else {
    getDefaultReleaseVersions()
}

@Suppress("UNCHECKED_CAST")
fun writeDefines() {
    val allMinecraftVersions = extra["allMinecraftVersions"] as List<String>
    val sb = StringBuilder()

    allMinecraftVersions.forEach { ver ->
        val verCode = toVersionCode(ver)
        sb.append("MC_").append(ver.replace(".", "_")).append("=").append(verCode).append("\n")
    }

    sb.append("MC_VER=").append(toVersionCode(versionConfigVar.common.minecraftVersion)).append("\n")

    val gradleExtra = gradle.extensions.extraProperties
    if ((gradleExtra.properties["isDev"] as? Boolean) == true) sb.append("IS_DEV=1\n")else sb.append("IS_DEV=0\n")
    if ((gradleExtra.properties["isVulkan"] as? Boolean) == true) sb.append("IS_VULKAN=1\n")else sb.append("IS_VULKAN=0\n")
    if ((gradleExtra.properties["isEnableAutoDownload"] as? Boolean) == true) sb.append("ENABLE_AUTO_DOWNLOAD=1\n") else sb.append("ENABLE_AUTO_DOWNLOAD=0\n")
    if ((gradleExtra.properties["isUseDebugLib"] as? Boolean) == true) sb.append("USE_DEBUG_LIB=1\n") else sb.append("USE_DEBUG_LIB=0\n")

    val outputFile = File(projectDir, "build.properties")
    val newText = sb.toString()

    if (!outputFile.exists() || outputFile.readText() != newText) {
        outputFile.writeText(newText)
        println("成功写入定义文件")
    }
}

fun getCurrentVersionConfig(): VersionConfig = versionConfigVar

extra["getVersionConfigPath"] = ::getVersionConfigPath
extra["writeDefines"] = ::writeDefines
extra["getCurrentVersionConfig"] = ::getCurrentVersionConfig
extra["versionConfig"] = versionConfigVar

writeDefines()
