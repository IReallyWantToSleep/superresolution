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
import org.gradle.api.logging.configuration.ConsoleOutput
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.GradleBuild
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withGroovyBuilder
import utils.ModrinthUploader
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

plugins {
    id("net.neoforged.moddev") version "2.0.141" apply false
    id("systems.manifold.manifold-gradle-plugin") version "0.0.2-alpha"
    id("multiversion")
    id("multiversion-neoform")
}

allprojects {
    group = rootProject.group

    val gradleExtra = gradle.extensions.extraProperties
    val isDev = (gradleExtra.properties["isDev"] as? Boolean) == true
    val isVulkan = (gradleExtra.properties["isVulkan"] as? Boolean) == true

    if (isDev) {
        val gitCommitHash = providers.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
        }.standardOutput.asText.get().trim()
        version = "${rootProject.property("mod_version")}+dev.${gitCommitHash}.${if (isVulkan) "vulkan" else "opengl"}"
    } else {
        version = "${rootProject.property("mod_version")}+${if (isVulkan) "vulkan" else "opengl"}"
    }

    repositories {
        mavenCentral()
        maven(url = "https://maven.neoforged.net/releases")
        maven(url = "https://maven.aliyun.com/repository/central")
        maven(url = "https://maven.aliyun.com/repository/gradle-plugin")
        maven(url = "https://maven.architectury.dev/")
        maven(url = "https://maven.nucleoid.xyz/")
        maven(url = "https://maven.shedaniel.me/")
        maven(url = "https://maven.neoforged.net/releases")
        maven(url = "https://libraries.minecraft.net")
        maven(url = "https://maven.fabricmc.net/")
        maven(url = "https://maven.parchmentmc.org/")
        maven(url = "https://maven.blamejared.com")
    }

    if (project.name != "native") {
        apply(plugin = "systems.manifold.manifold-gradle-plugin")

        extensions.findByName("manifold")?.withGroovyBuilder {
            setProperty("manifoldVersion", rootProject.property("manifold_version"))
        }

        tasks.withType(JavaCompile::class.java).configureEach {
            options.release.set((rootProject.extra["versionConfig"] as multiversion.VersionConfig).common.javaVersion)
            options.compilerArgs.add("-Xplugin:Manifold")
            options.encoding = "UTF-8"
        }

        dependencies {
            annotationProcessor("systems.manifold:manifold-preprocessor:${rootProject.property("manifold_version")}")
            configurations.configureEach {
                resolutionStrategy {
                    force("org.lwjgl:lwjgl:${(rootProject.extra["versionConfig"] as multiversion.VersionConfig).common.lwjglVersion}")
                    force("org.lwjgl:lwjgl-glfw:${(rootProject.extra["versionConfig"] as multiversion.VersionConfig).common.lwjglVersion}")
                    force("org.lwjgl:lwjgl-opengl:${(rootProject.extra["versionConfig"] as multiversion.VersionConfig).common.lwjglVersion}")
                    force("org.lwjgl:lwjgl-vulkan:${(rootProject.extra["versionConfig"] as multiversion.VersionConfig).common.lwjglVersion}")
                    force("org.lwjgl:lwjgl-openal:${(rootProject.extra["versionConfig"] as multiversion.VersionConfig).common.lwjglVersion}")
                    force("org.lwjgl:lwjgl-stb:${(rootProject.extra["versionConfig"] as multiversion.VersionConfig).common.lwjglVersion}")
                    force("org.lwjgl:lwjgl-jemalloc:${(rootProject.extra["versionConfig"] as multiversion.VersionConfig).common.lwjglVersion}")
                    force("org.lwjgl:lwjgl-tinyfd:${(rootProject.extra["versionConfig"] as multiversion.VersionConfig).common.lwjglVersion}")
                    force("org.lwjgl:lwjgl-freetype:${(rootProject.extra["versionConfig"] as multiversion.VersionConfig).common.lwjglVersion}")
                }
            }
        }

        afterEvaluate {
            if (System.getProperty("os.name").lowercase().contains("windows")) {
                return@afterEvaluate
            }
            val javaWrapper = rootProject.file("script/java_tool_wrapper.sh")
            if (!javaWrapper.exists()) {
                return@afterEvaluate
            }
            tasks.matching { it.javaClass.name.contains("CreateMinecraftArtifacts") }.configureEach {
                if (hasProperty("javaExecutable")) {
                    withGroovyBuilder {
                        setProperty("javaExecutable", javaWrapper.absolutePath)
                    }
                }
                if (hasProperty("toolsJavaExecutable")) {
                    withGroovyBuilder {
                        setProperty("toolsJavaExecutable", javaWrapper.absolutePath)
                    }
                }
            }
        }
    }
}

val srConfigsDir = file("$rootDir/configs")
val srOutputDir = file("$rootDir/build_jars")

fun loadVersionConfig(configFile: File): Map<*, *> {
    return JsonSlurper().parse(configFile) as Map<*, *>
}

fun normalizeTaskSuffix(versionName: String): String {
    return versionName.replace(Regex("[^A-Za-z0-9_]"), "_")
}

fun normalizePlatforms(config: Map<*, *>): List<String> {
    val common = config["common"] as? Map<*, *> ?: return emptyList()
    val platforms = common["platforms"] as? List<*> ?: emptyList<Any?>()
    return platforms.mapNotNull { it?.toString()?.trim() }.filter { it.isNotBlank() }
}

val collectTaskByVersion = linkedMapOf<String, String>()
val orderedNestedBuildTasks = mutableListOf<String>()
val orderedCollectTasks = mutableListOf<String>()

if (srConfigsDir.exists()) {
    val configFiles = srConfigsDir.listFiles()
        ?.filter { it.name.endsWith(".json") }
        ?.sortedBy { it.name }
        ?: emptyList()

    configFiles.forEach { configFile ->
        val versionName = configFile.name.substringBeforeLast('.')
        val config = loadVersionConfig(configFile)
        val skipBuild = (config["skip_build"] as? Boolean) ?: false
        if (skipBuild) {
            return@forEach
        }

        val platforms = normalizePlatforms(config)
        val suffix = normalizeTaskSuffix(versionName)
        val nestedBuildTaskName = "buildVersion_$suffix"
        val collectTaskName = "collectVersion_$suffix"

        tasks.register<GradleBuild>(nestedBuildTaskName) {
            group = "build"
            description = "构建版本 $versionName"
            buildName = "superresolution_$suffix"
            dir = rootDir
            setTasks(listOf("clean", "build"))
            startParameter.projectProperties["minecraft_version_config"] = versionName
            startParameter.excludedTaskNames.add(":native:build")
            startParameter.consoleOutput = ConsoleOutput.Plain
        }

        tasks.register(collectTaskName) {
            group = "build"
            description = "收集版本 $versionName 构建产物"
            dependsOn(nestedBuildTaskName)
            doLast {
                srOutputDir.mkdirs()
                platforms.forEach { platform ->
                    val libsDir = file("$rootDir/$platform/build/libs")
                    if (!libsDir.exists()) {
                        println("警告: 构建目录不存在 - $libsDir")
                        return@forEach
                    }
                    copy {
                        from(libsDir)
                        include("*.jar")
                        exclude("*dev-shadow.jar", "*sources.jar", "*javadoc.jar")
                        into(srOutputDir)
                    }
                }
            }
        }

        collectTaskByVersion[versionName] = collectTaskName
        orderedNestedBuildTasks += nestedBuildTaskName
        orderedCollectTasks += collectTaskName
    }
}

for (i in 1 until orderedNestedBuildTasks.size) {
    val currentTask = orderedNestedBuildTasks[i]
    val previousTask = orderedNestedBuildTasks[i - 1]
    tasks.named(currentTask) {
        mustRunAfter(previousTask)
    }
}

for (i in 1 until orderedCollectTasks.size) {
    val currentTask = orderedCollectTasks[i]
    val previousTask = orderedCollectTasks[i - 1]
    tasks.named(currentTask) {
        mustRunAfter(previousTask)
    }
}

val nativeBuildTaskPath = ":native:buildNative"
orderedNestedBuildTasks.forEach { taskName ->
    tasks.named(taskName) {
        mustRunAfter(nativeBuildTaskPath)
    }
}

tasks.register<Delete>("cleanBuildJars") {
    group = "build"
    description = "清理 build_jars 输出目录"
    delete(srOutputDir)
}

tasks.register("buildOneVersion") {
    group = "build"
    description = "构建指定版本并收集产物，使用 -Psr.version=<configName>"
    dependsOn("cleanBuildJars")

    val requestedVersion = project.findProperty("sr.version")?.toString()
    if (!requestedVersion.isNullOrBlank() && collectTaskByVersion.containsKey(requestedVersion)) {
        dependsOn(collectTaskByVersion.getValue(requestedVersion))
    }

    doFirst {
        val versionName = project.findProperty("sr.version")?.toString()
        if (versionName.isNullOrBlank()) {
            throw GradleException("请通过 -Psr.version=<configName> 指定版本，例如 -Psr.version=1.20.6")
        }
        if (!collectTaskByVersion.containsKey(versionName)) {
            throw GradleException("未找到可构建版本: $versionName")
        }
    }
}

tasks.register("buildAllVersions") {
    group = "build"
    description = "遍历 configs/*.json 构建全部版本并收集产物到 build_jars"
    dependsOn("cleanBuildJars")
    dependsOn(collectTaskByVersion.values)

    doFirst {
        gradle.startParameter.isContinueOnFailure = true
        if (!srConfigsDir.exists()) {
            throw GradleException("configs 目录不存在: $srConfigsDir")
        }
        if (collectTaskByVersion.isEmpty()) {
            throw GradleException("未找到可构建版本（可能全部 skip_build=true）")
        }
    }

    doLast {
        println("\n构建完成，输出目录: $srOutputDir")
    }
}

tasks.register("buildAll") {
    group = "build"
    description = "先构建 native，再执行 buildAllVersions"
    dependsOn(nativeBuildTaskPath)
    dependsOn("buildAllVersions")
}

tasks.named("buildAllVersions") {
    mustRunAfter(nativeBuildTaskPath)
}

tasks.register("uploadToModrinth") {
    doLast {
        val changelogFile = rootProject.file("CHANGELOG.md")
        if (!changelogFile.exists()) {
            throw GradleException("CHANGELOG.md not found!")
        }

        val (currentVersion, latestChangelog) = parseChangelog(changelogFile)

        println("\n=== 最新版本更新日志 ($currentVersion) ===\n")
        latestChangelog.forEach { println(it) }
        println("\n========================")

        var confirm = getConsoleInput("是否使用此更新日志？(Y/N): ").trim().lowercase()
        if (!confirm.startsWith("y")) {
            println("上传已取消")
            return@doLast
        }

        ModrinthUploader.init()
        val jarsDir = file("$projectDir/build_jars")
        println("将要上传的文件：")
        jarsDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("super") && file.name.endsWith(".jar")) {
                println(file.absolutePath)
            }
        }

        confirm = getConsoleInput("是否继续？(Y/N): ").trim().lowercase()
        if (!confirm.startsWith("y")) {
            println("上传已取消")
            return@doLast
        }

        jarsDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("super") && file.name.endsWith(".jar")) {
                var notSucceed = true
                while (notSucceed) {
                    try {
                        ModrinthUploader.uploadFile(file, latestChangelog.joinToString("\n"))
                        notSucceed = false
                    } catch (e: Exception) {
                        e.printStackTrace()
                        confirm = getConsoleInput("上传失败，是否重试？(Y/N): ").trim().lowercase()
                        if (!confirm.startsWith("y")) {
                            notSucceed = false
                        }
                    }
                }
            }
        }
    }
}

fun parseChangelog(file: File): Pair<String?, MutableList<String>> {
    val versionPattern = Regex("^#\\s+(\\d+\\.\\d+\\.\\d+(-[a-zA-Z]+(\\.[\\d]+)?)*)\\s*$")
    var currentVersion: String? = null
    val changelog = mutableListOf<String>()
    var versionEnded = false
    var previousEmpty = false

    file.forEachLine { line ->
        if (versionEnded) return@forEachLine

        val matcher = versionPattern.matchEntire(line)
        if (matcher != null) {
            if (currentVersion == null) {
                currentVersion = matcher.groupValues[1]
                changelog += line
            } else {
                versionEnded = true
                return@forEachLine
            }
        } else if (currentVersion != null) {
            if (line.trim().isEmpty()) {
                if (previousEmpty) {
                    versionEnded = true
                    return@forEachLine
                }
                previousEmpty = true
            } else {
                previousEmpty = false
            }
            changelog += line
        }
    }

    while (changelog.isNotEmpty() && changelog.last().trim().isEmpty()) {
        changelog.removeLast()
    }

    return currentVersion to changelog
}

fun getConsoleInput(prompt: String): String {
    try {
        print(prompt)
        val br = BufferedReader(InputStreamReader(System.`in`))
        println()
        return br.readLine()
    } catch (e: IOException) {
        throw GradleException("无法读取用户输入", e)
    }
}
