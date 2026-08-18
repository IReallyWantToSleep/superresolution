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
import net.neoforged.nfrtgradle.CreateMinecraftArtifacts
import utils.CurseForgeUploader
import utils.ModrinthUploader
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

private val consoleReader = BufferedReader(InputStreamReader(System.`in`))

plugins {
    id("net.neoforged.moddev") version "2.0.141" apply false
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
        // On Linux, NeoForm's decompile step spawns Vineflower with only -Xmx4G, which
        // OOMs on modern Minecraft. Route the tool JVM through a wrapper that raises the
        // heap. MDG sets `toolsJavaExecutable` from its tools toolchain in the task's
        // registration action, so we override it with a `named(...).configure` action,
        // which runs after the registration action and thus wins the last-write.
        if (System.getProperty("os.name").contains("linux", ignoreCase = true)) {
            val toolWrapper = rootProject.file("script/java_tool_wrapper.sh")
            plugins.withId("net.neoforged.moddev") {
                afterEvaluate {
                    // Configure after registration (create$8 already ran during MDG's
                    // afterEvaluate) so this set wins the last-write over MDG's toolchain.
                    tasks.withType(CreateMinecraftArtifacts::class.java).forEach {
                        it.toolsJavaExecutable.set(toolWrapper.absolutePath)
                    }
                }
            }
        }

        tasks.withType(JavaCompile::class.java).configureEach {
            inputs.file(rootProject.layout.projectDirectory.file("build.properties"))
                .withPropertyName("manifoldDefines")
                .withPathSensitivity(PathSensitivity.RELATIVE)
            options.release.set((rootProject.extra["versionConfig"] as multiversion.VersionConfig).common.javaVersion)
            options.compilerArgs.add("-Xplugin:Manifold")
            options.encoding = "UTF-8"
            options.isFork = true
            // One forked javac per concurrent compile task, so this multiplies by
            // org.gradle.workers.max. Keep the product well under physical RAM.
            options.forkOptions.memoryMaximumSize = "2g"
        }

        dependencies {
            configurations.configureEach {
                resolutionStrategy {
                    force("org.lwjgl:lwjgl:${(rootProject.extra["versionConfig"] as multiversion.VersionConfig).common.lwjglVersion}")
                    force("org.lwjgl:lwjgl-glfw:${(rootProject.extra["versionConfig"] as multiversion.VersionConfig).common.lwjglVersion}")
                    force("org.lwjgl:lwjgl-opengl:${(rootProject.extra["versionConfig"] as multiversion.VersionConfig).common.lwjglVersion}")
                    force("org.lwjgl:lwjgl-vulkan:${(rootProject.extra["versionConfig"] as multiversion.VersionConfig).common.lwjglVersion}")
                    force("org.lwjgl:lwjgl-openal:${(rootProject.extra["versionConfig"] as multiversion.VersionConfig).common.lwjglVersion}")
                    force("org.lwjgl:lwjgl-stb:${(rootProject.extra["versionConfig"] as multiversion.VersionConfig).common.lwjglStbVersion}")
                    force("org.lwjgl:lwjgl-jemalloc:${(rootProject.extra["versionConfig"] as multiversion.VersionConfig).common.lwjglVersion}")
                    force("org.lwjgl:lwjgl-tinyfd:${(rootProject.extra["versionConfig"] as multiversion.VersionConfig).common.lwjglVersion}")
                    force("org.lwjgl:lwjgl-freetype:${(rootProject.extra["versionConfig"] as multiversion.VersionConfig).common.lwjglVersion}")
                }
            }
        }

        afterEvaluate {
            dependencies {
                annotationProcessor("systems.manifold:manifold-preprocessor:${rootProject.property("manifold_version")}")
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
            description = "Build version $versionName"
            buildName = "superresolution_$suffix"
            dir = rootDir
            setTasks(listOf("clean", "build"))
            startParameter.projectProperties["minecraft_version_config"] = versionName
            startParameter.excludedTaskNames.add(":native:build")
            startParameter.isContinueOnFailure = true
            startParameter.consoleOutput = ConsoleOutput.Plain
        }

        tasks.register(collectTaskName) {
            group = "build"
            description = "Collect build artifacts for version $versionName"
            dependsOn(nestedBuildTaskName)
            doLast {
                srOutputDir.mkdirs()
                platforms.forEach { platform ->
                    val libsDir = file("$rootDir/$platform/build/libs")
                    if (!libsDir.exists()) {
                        println("Warning: build directory does not exist - $libsDir")
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
    description = "Clear the build_jars output directory"
    delete(srOutputDir)
}

tasks.register<GradleBuild>("publishApiToShnexus") {
    group = "publishing"
    description = "Publish the Super Resolution API to shnexus with the Minecraft 1.20.1 (Java 17) configuration"
    buildName = "superresolution_api_1_20_1"
    dir = rootDir
    setTasks(listOf(":common:publishApiPublicationToShnexusRepository"))
    startParameter.projectProperties["minecraft_version_config"] = "1.20.1"
    startParameter.projectProperties["is_dev"] = "true"
    listOf("shnexusUsername", "shnexusPassword").forEach { property ->
        providers.gradleProperty(property).orNull?.let { value ->
            startParameter.projectProperties[property] = value
        }
    }
    startParameter.consoleOutput = ConsoleOutput.Plain

    doFirst {
        val missing = listOf("shnexusUsername", "shnexusPassword")
            .filterNot { providers.gradleProperty(it).isPresent }
        if (missing.isNotEmpty()) {
            throw GradleException("Missing remote publishing credentials: ${missing.joinToString()}")
        }
    }
}

tasks.register("buildOneVersion") {
    group = "build"
    description = "Build a specified version and collect artifacts; use -Psr.version=<configName>"
    dependsOn("cleanBuildJars")

    val requestedVersion = project.findProperty("sr.version")?.toString()
    if (!requestedVersion.isNullOrBlank() && collectTaskByVersion.containsKey(requestedVersion)) {
        dependsOn(collectTaskByVersion.getValue(requestedVersion))
    }

    doFirst {
        val versionName = project.findProperty("sr.version")?.toString()
        if (versionName.isNullOrBlank()) {
            throw GradleException("Specify a version with -Psr.version=<configName>, for example -Psr.version=1.20.6")
        }
        if (!collectTaskByVersion.containsKey(versionName)) {
            throw GradleException("No buildable version found: $versionName")
        }
    }
}

tasks.register("buildAllVersions") {
    group = "build"
    description = "Build every version in configs/*.json and collect artifacts in build_jars"
    dependsOn("cleanBuildJars")
    dependsOn(collectTaskByVersion.values)

    doFirst {
        gradle.startParameter.isContinueOnFailure = true
        if (!srConfigsDir.exists()) {
            throw GradleException("The configs directory does not exist: $srConfigsDir")
        }
        if (collectTaskByVersion.isEmpty()) {
            throw GradleException("No buildable versions found (all may have skip_build=true)")
        }
    }

    doLast {
        println("\nBuild complete. Output directory: $srOutputDir")
    }
}

tasks.register("buildAll") {
    group = "build"
    description = "Build native code, then run buildAllVersions"
    dependsOn(nativeBuildTaskPath)
    dependsOn("buildAllVersions")
}

tasks.named("buildAllVersions") {
    mustRunAfter(nativeBuildTaskPath)
}

tasks.register("uploadToModrinth") {
    doLast {
        val (currentVersion, latestChangelog) = findLatestChangelog()
        val autoConfirm = project.findProperty("modrinth.autoConfirm")
            ?.toString()
            ?.toBoolean() ?: false

        println("\n=== Latest release notes ($currentVersion) ===\n")
        println(latestChangelog)
        println("\n========================")

        if (!autoConfirm) {
            var confirm = getConsoleInput("Use these release notes? (Y/N): ").trim().lowercase()
            if (!confirm.startsWith("y")) {
                println("Upload cancelled")
                return@doLast
            }

            confirm = getConsoleInput("Continue? (Y/N): ").trim().lowercase()
            if (!confirm.startsWith("y")) {
                println("Upload cancelled")
                return@doLast
            }
        }

        ModrinthUploader.init()
        val jarsDir = file("$projectDir/build_jars")
        println("Files to upload:")
        jarsDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("super") && file.name.endsWith(".jar")) {
                println(file.absolutePath)
            }
        }

        jarsDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("super") && file.name.endsWith(".jar")) {
                var notSucceed = true
                while (notSucceed) {
                    try {
                        ModrinthUploader.uploadFile(file, latestChangelog)
                        notSucceed = false
                    } catch (e: Exception) {
                        e.printStackTrace()
                        val retry = getConsoleInput("Upload failed. Retry? (Y/N): ").trim().lowercase()
                        if (!retry.startsWith("y")) {
                            notSucceed = false
                        }
                    }
                }
            }
        }
    }
}

tasks.register("uploadToCurseForge") {
    group = "publishing"
    description = "Validate and upload mod artifacts in build_jars to CurseForge"

    doLast {
        val dryRun = project.findProperty("curseforge.dryRun")
            ?.toString()
            ?.toBoolean() ?: false
        val autoConfirm = project.findProperty("curseforge.autoConfirm")
            ?.toString()
            ?.toBoolean() ?: false
        val jarsDir = project.findProperty("curseforge.jarsDir")
            ?.toString()
            ?.let(::file)
            ?: file("$rootDir/build_jars")

        val uploadPlan = CurseForgeUploader.createUploadPlan(
            jarsDir,
            file("$rootDir/configs"),
            file("$rootDir/changelogs")
        )

        println("\n=== CurseForge upload plan ===")
        println("Project ID: ${CurseForgeUploader.defaultProjectId()}")
        println("Mod version: ${uploadPlan.modVersion}")
        println("Release notes: ${uploadPlan.changelogFile.absolutePath}")
        println("File count: ${uploadPlan.artifacts.size}")
        uploadPlan.artifacts.forEachIndexed { index, artifact ->
            println(
                "${index + 1}. ${artifact.file.name} | "
                        + "${artifact.loaderName} | "
                        + "${artifact.gameVersions.joinToString(", ")} | "
                        + "Client | "
                        + artifact.releaseType
            )
        }
        println("==========================\n")

        if (dryRun) {
            println("dryRun=true; preflight completed without connecting to CurseForge.")
            return@doLast
        }

        val apiToken = System.getenv("CURSEFORGE_API_TOKEN")
        if (apiToken.isNullOrBlank()) {
            throw GradleException("Missing CURSEFORGE_API_TOKEN environment variable")
        }

        if (!autoConfirm) {
            val confirm = getConsoleInput("Upload the listed files to CurseForge? (Y/N): ")
                .trim()
                .lowercase()
            if (!confirm.startsWith("y")) {
                println("Upload cancelled")
                return@doLast
            }
        }

        uploadPlan.artifacts.forEachIndexed { index, artifact ->
            println("[${index + 1}/${uploadPlan.artifacts.size}] Uploading ${artifact.file.name}")
            val fileId = CurseForgeUploader.uploadFile(artifact, apiToken)
            println("Upload successful. CurseForge file ID: $fileId")
        }
        println("All ${uploadPlan.artifacts.size} files uploaded.")
    }
}

val preReleasePriority = listOf("alpha", "beta", "pre")

fun compareSemver(a: String, b: String): Int {
    fun parseCore(v: String): Triple<Int, Int, Int> {
        val parts = v.substringBefore('-').split('.').map { it.toInt() }
        return Triple(parts[0], parts[1], parts[2])
    }

    fun parsePreRelease(v: String): List<String> {
        val idx = v.indexOf('-')
        if (idx == -1) return emptyList()
        return v.substring(idx + 1).split('.')
    }

    fun compareIdentifier(x: String, y: String): Int {
        val xIsNum = x.all { it.isDigit() }
        val yIsNum = y.all { it.isDigit() }
        if (xIsNum && yIsNum) return x.toInt().compareTo(y.toInt())
        val xIdx = preReleasePriority.indexOf(x)
        val yIdx = preReleasePriority.indexOf(y)
        return when {
            xIdx >= 0 && yIdx >= 0 -> xIdx - yIdx
            xIdx >= 0 -> -1
            yIdx >= 0 -> 1
            else -> x.compareTo(y)
        }
    }

    val (aMajor, aMinor, aPatch) = parseCore(a)
    val (bMajor, bMinor, bPatch) = parseCore(b)
    if (aMajor != bMajor) return aMajor - bMajor
    if (aMinor != bMinor) return aMinor - bMinor
    if (aPatch != bPatch) return aPatch - bPatch

    val aPre = parsePreRelease(a)
    val bPre = parsePreRelease(b)
    if (aPre.isEmpty() && bPre.isNotEmpty()) return 1
    if (aPre.isNotEmpty() && bPre.isEmpty()) return -1
    if (aPre.isEmpty() && bPre.isEmpty()) return 0

    val minLen = minOf(aPre.size, bPre.size)
    for (i in 0 until minLen) {
        val cmp = compareIdentifier(aPre[i], bPre[i])
        if (cmp != 0) return cmp
    }
    return aPre.size - bPre.size
}

fun findLatestChangelog(): Pair<String?, String> {
    val changelogsDir = rootProject.file("changelogs")
    if (!changelogsDir.exists() || !changelogsDir.isDirectory) {
        throw GradleException("The changelogs/ directory does not exist")
    }

    val pattern = Regex("^(\\d+\\.\\d+\\.\\d+(-[a-zA-Z]+(\\.[\\d]+)?)*)\\.md$")

    val versions = changelogsDir.listFiles()
        ?.mapNotNull { file ->
            val match = pattern.matchEntire(file.name)
            if (match != null) match.groupValues[1] to file else null
        }
        ?.sortedWith { (v1, _), (v2, _) -> -compareSemver(v1, v2) }
        ?: throw GradleException("No valid changelog file found in changelogs/")

    if (versions.isEmpty()) {
        throw GradleException("No valid changelog file found in changelogs/")
    }

    val (latestVersion, latestFile) = versions.first()
    return latestVersion to latestFile.readText().trimEnd()
}

fun getConsoleInput(prompt: String): String {
    try {
        print(prompt)
        println()
        return consoleReader.readLine() ?: ""
    } catch (e: IOException) {
        throw GradleException("Unable to read user input", e)
    }
}
