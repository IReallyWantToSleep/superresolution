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

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven(url = "https://maven.neoforged.net")
        maven(url = "https://maven.fabricmc.net/")
        maven(url = "https://maven.shedaniel.me/")
        maven(url = "https://libraries.minecraft.net")
        maven(url = "https://maven.parchmentmc.org/")
    }
}

plugins {
    // 1.0.0 is the first release compatible with Gradle 9: every earlier version
    // references JvmVendorSpec.IBM_SEMERU, which Gradle 9 removed. The failure only
    // surfaces when a toolchain actually has to be provisioned, so it stays hidden
    // as long as the requested JDK is already installed or cached in ~/.gradle/jdks.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "superresolution"

val requestedTasks = gradle.startParameter.taskNames.orEmpty()
val nativeOnlyMode = requestedTasks.isNotEmpty() && requestedTasks.all { taskName ->
    val normalized = taskName.lowercase()
    normalized.startsWith(":native:") || normalized == ":native" || normalized == "native" || normalized.startsWith("native:")
}

include("native")
project(":native").projectDir = File(rootDir, "modules/native")
if (!nativeOnlyMode) {
    include("common")
    project(":common").projectDir = File(rootDir, "modules/common")
}

val minecraftVersionConfig = providers.gradleProperty("minecraft_version_config").orNull
    ?: throw GradleException("Missing minecraft_version_config property")

val versionConfigSrc = JsonSlurper().parse(
    File(rootDir, "configs/${minecraftVersionConfig}.json")
) as Map<*, *>

gradle.extensions.extraProperties["versionConfigSrc"] = versionConfigSrc

val commonConfig = versionConfigSrc["common"] as? Map<*, *>
    ?: throw GradleException("Version configuration is missing the common node")

val minecraftVersion = commonConfig["minecraft_version"]?.toString()
    ?: throw GradleException("Version configuration is missing common.minecraft_version")

gradle.extensions.extraProperties["minecraft_version"] = minecraftVersion

gradle.extensions.extraProperties["isVulkan"] = providers.gradleProperty("is_vulkan").orNull?.toBoolean() ?: false
gradle.extensions.extraProperties["isDev"] = providers.gradleProperty("is_dev").orNull?.toBoolean() ?: false
gradle.extensions.extraProperties["isEnableAutoDownload"] = providers.gradleProperty("enable_auto_download").orNull?.toBoolean() ?: false
gradle.extensions.extraProperties["isUseDebugLib"] = providers.gradleProperty("use_debug_lib").orNull?.toBoolean() ?: false

if (!nativeOnlyMode) {
    val isVulkan = gradle.extensions.extraProperties["isVulkan"] as Boolean
    println("Graphics API: " + if (isVulkan) "Vulkan" else "OpenGL")
    println("Current Minecraft version: $minecraftVersion")

    val platforms = (commonConfig["platforms"] as? List<*>)
        .orEmpty().mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
    for (loader in platforms) {
        println("Enabled loader: $loader")
        include(loader)
        project(":$loader").projectDir = file("modules/$loader")
    }

    val wisteriaDir = file("modules/wisteria")
    if (wisteriaDir.resolve("gradle.properties").isFile && wisteriaDir.resolve("common/src").isDirectory) {
        for (module in listOf("common") + platforms) {
            val name = "wisteria-$module"
            include(name)
            project(":$name").apply {
                projectDir = wisteriaDir.resolve(module)
                // SR owns the embedded build. The gitlink can point to an upstream commit as-is.
                val script = if (module == "common") "common" else "loader"
                buildFileName = "../../../gradle/wisteria-$script.gradle.kts"
            }
        }
    } else if (requestedTasks.any { it.removePrefix(":").startsWith("wisteria-") }) {
        throw GradleException("Initialize Wisteria with `git submodule update --init -- modules/wisteria`.")
    }
}
