/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import java.io.File

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven(url = "https://maven.neoforged.net")
        maven(url = "https://maven.fabricmc.net/")
        maven(url = "https://maven.aliyun.com/repository/central")
        maven(url = "https://maven.aliyun.com/repository/gradle-plugin")
        maven(url = "https://maven.shedaniel.me/")
        maven(url = "https://libraries.minecraft.net")
        maven(url = "https://maven.parchmentmc.org/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "superresolution"

val requestedTasks = gradle.startParameter.taskNames.orEmpty()
val nativeOnlyMode = requestedTasks.isNotEmpty() && requestedTasks.all { taskName ->
    val normalized = taskName.lowercase()
    normalized.startsWith(":native:") || normalized == ":native" || normalized == "native" || normalized.startsWith("native:")
}

include("native")
if (!nativeOnlyMode) {
    include("common")
}

val minecraftVersionConfig = providers.gradleProperty("minecraft_version_config").orNull
    ?: throw GradleException("缺少属性 minecraft_version_config")

val versionConfigSrc = JsonSlurper().parse(
    File("$rootDir/configs/$minecraftVersionConfig.json")
) as Map<*, *>

val isUnobfuscated = versionConfigSrc["unobfuscated"] as? Boolean ?: false

gradle.extensions.extraProperties["versionConfigSrc"] = versionConfigSrc

val commonConfig = versionConfigSrc["common"] as? Map<*, *>
    ?: throw GradleException("版本配置缺少 common 节点")

val minecraftVersion = commonConfig["minecraft_version"]?.toString()
    ?: throw GradleException("版本配置缺少 common.minecraft_version")

gradle.extensions.extraProperties["minecraft_version"] = minecraftVersion

gradle.extensions.extraProperties["isVulkan"] = providers.gradleProperty("is_vulkan").orNull?.toBoolean() ?: false
gradle.extensions.extraProperties["isDev"] = providers.gradleProperty("is_dev").orNull?.toBoolean() ?: false
gradle.extensions.extraProperties["isEnableAutoDownload"] = providers.gradleProperty("enable_auto_download").orNull?.toBoolean() ?: false
gradle.extensions.extraProperties["isUseDebugLib"] = providers.gradleProperty("use_debug_lib").orNull?.toBoolean() ?: false

if (!nativeOnlyMode) {
    val isVulkan = gradle.extensions.extraProperties["isVulkan"] as Boolean
    println("❇️ 图形API " + if (isVulkan) "Vulkan" else "OpenGL")
    println("❇️ 当前Minecraft版本 $minecraftVersion")

    val platforms = commonConfig["platforms"] as? List<*> ?: emptyList<Any?>()
    for (loader in platforms) {
        val loaderName = loader?.toString()?.trim().orEmpty()
        if (loaderName.isBlank()) continue
        println("❇️ 已启用加载器 $loaderName")
        include(loaderName)
        if (loaderName == "fabric") {
            val fabricBuildFile = File(rootDir, "fabric/build.gradle.kts")
            val targetPluginId = if (isUnobfuscated) {
                "net.fabricmc.fabric-loom"
            } else {
                "net.fabricmc.fabric-loom-remap"
            }

            val originalScript = fabricBuildFile.readText()
            val loomPluginPattern = Regex(
                """(id\s*\(\s*")net\.fabricmc\.fabric-loom[^"]*("\s*\)(?:\s*version\s*"[^"]*")?)"""
            )
            val updatedScript = originalScript.replace(loomPluginPattern) { match ->
                "${match.groupValues[1]}$targetPluginId${match.groupValues[2]}"
            }

            if (updatedScript != originalScript) {
                fabricBuildFile.writeText(updatedScript)
            }
        }
    }
}