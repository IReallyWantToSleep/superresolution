package utils

import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.JavaExec
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private data class NsightActivity(
    val taskName: String,
    val executable: String,
    val activity: String? = null,
    val startAfterHotkey: Boolean = false
)

fun Project.registerNsightRunTasks(
    runClientTaskName: String = "runClient",
    taskGroup: String = "nsight",
    preparationTaskNames: List<String> = emptyList(),
    workingDirectory: File = projectDir
) {
    val runClient = tasks.named(runClientTaskName, JavaExec::class.java)

    val ngfx = providers.gradleProperty("nsight.ngfx")
        .orElse(providers.environmentVariable("NGFX"))
        .orElse(providers.provider { findNsightBinary(project, "ngfx.exe", "ngfx") })
    val ngfxCapture = providers.gradleProperty("nsight.ngfxCapture")
        .orElse(providers.environmentVariable("NGFX_CAPTURE"))
        .orElse(providers.provider { findNsightBinary(project, "ngfx-capture.exe", "ngfx-capture") })
    val platform = providers.gradleProperty("nsight.platform")
        .orElse("windows")

    val activities = listOf(
        NsightActivity(
            taskName = "runClientWithNsightFrameDebugger",
            executable = "ngfx",
            activity = "OpenGL Frame Debugger"
        ),
        NsightActivity(
            taskName = "runClientWithNsightGpuTrace",
            executable = "ngfx",
            activity = "GPU Trace Profiler",
            startAfterHotkey = true
        ),
        NsightActivity(
            taskName = "runClientWithNsightGraphicsCapture",
            executable = "ngfx-capture"
        )
    )

    activities.forEach { activity ->
        tasks.register(activity.taskName, Exec::class.java) {
            group = taskGroup
            description = "Starts the client through NVIDIA Nsight Graphics"
            preparationTaskNames.forEach(::dependsOn)

            doFirst {
                val task = runClient.get()
                val javaExecutable = task.javaLauncher.get().executablePath.asFile.absolutePath
                val classpath = javaClasspath(task)
                val environment = task.environment.map { (key, value) -> "$key=$value" }
                val nsightPath = File(
                    if (activity.executable == "ngfx") ngfx.get() else ngfxCapture.get()
                ).absoluteFile
                val nsightWorkingDirectory = nsightPath.parentFile ?: workingDirectory

                val javaArguments = javaLaunchArguments(
                    task = task,
                    classpath = classpath,
                    classpathJar = createClasspathJar(
                        project,
                        classpath,
                        estimateCommandLineOverhead(javaExecutable, nsightPath, environment, activity)
                    )
                )
                val command = if (activity.executable == "ngfx") {
                    mutableListOf(
                        ngfx.get(),
                        "--activity",
                        activity.activity!!,
                        "--platform",
                        platform.get(),
                        "--launch-detached",
                        "--exe",
                        javaExecutable,
                        "--dir",
                        workingDirectory.absolutePath,
                        "--args",
                        javaArguments.joinToString(" ") { quoteForNsight(it) }
                    ).apply {
                        if (environment.isNotEmpty()) {
                            add("--env")
                            add(environment.joinToString(";"))
                        }
                        if (activity.startAfterHotkey) {
                            add("--start-after-hotkey")
                        }
                    }
                } else {
                    mutableListOf(
                        ngfxCapture.get(),
                        "--exe",
                        javaExecutable,
                        "--working-dir",
                        workingDirectory.absolutePath,
                    ).apply {
                        if (environment.isNotEmpty()) {
                            add("--env")
                            addAll(environment)
                        }
                        add("--args")
                    }.apply {
                        addAll(javaArguments)
                    }
                }

                commandLine(command)
                workingDir(nsightWorkingDirectory)
            }
        }
    }
}

private fun findNsightBinary(project: Project, executableName: String, pathFallback: String): String {
    if (!System.getProperty("os.name").lowercase(Locale.ROOT).contains("windows")) {
        return pathFallback
    }

    val root = project.providers.gradleProperty("nsight.installRoot")
        .orElse("C:\\Program Files\\NVIDIA Corporation")
        .get()
        .let(::File)
    if (!root.isDirectory) return pathFallback

    val matches = root.walkTopDown()
        .filter { it.isFile && it.name.equals(executableName, ignoreCase = true) }
        .sortedByDescending { it.absolutePath.lowercase(Locale.ROOT) }
        .toList()
    return matches.firstOrNull()?.absolutePath ?: pathFallback
}

/**
 * Returns the classpath the `runClient` task would hand to the JVM.
 */
private fun javaClasspath(task: JavaExec): String {
    val classpath = task.classpath.asPath
    if (classpath.isNotBlank()) return classpath

    val provider = task.javaClass.methods.firstOrNull {
        it.name == "getClasspathProvider" && it.parameterCount == 0
    }?.invoke(task)
    return provider?.let {
        it.javaClass.methods.firstOrNull { method ->
            method.name == "getAsPath" && method.parameterCount == 0
        }?.invoke(it)?.toString()
    }.orEmpty()
}

/**
 * Windows rejects a process whose command line exceeds 32767 characters with
 * "CreateProcess error=206, 文件名或扩展名太长". Java itself works around that limit, but
 * these tasks start a separate executable and pass the whole client command line through
 * `--args`, so a long development classpath (about 27k characters for this project)
 * overflows it.
 *
 * A classpath that would overflow is replaced by a generated pathing JAR whose manifest
 * carries it instead. The JVM re-expands the manifest `Class-Path` while loading, so only
 * the short JAR path reaches the command line. Classpaths that already fit stay inline.
 *
 * A classpath whose entries contain whitespace keeps the inline form: a Java argument
 * file is split on whitespace, which would corrupt such an entry, and a development
 * classpath long enough to overflow the command line cannot be carried any other way.
 */
private fun createClasspathJar(project: Project, classpath: String, overhead: Int): File? {
    if (classpath.isBlank()) return null
    if (overhead + classpath.length + CLASS_PATH_QUOTING_ALLOWANCE <= WINDOWS_COMMAND_LINE_LIMIT) {
        return null
    }
    if (classpath.any(Char::isWhitespace)) return null

    val entries = classpath.split(File.pathSeparatorChar)
        .filter { it.isNotBlank() }
        .map { File(it).absoluteFile }
    if (entries.isEmpty()) return null

    val jarFile = File(project.layout.buildDirectory.get().asFile, "nsight/client-classpath.jar")
    jarFile.parentFile.mkdirs()
    ManifestJar.write(jarFile, manifestClassPath(entries))
    return jarFile
}

/**
 * Length the command line reaches without the JVM argument list and the classpath, which
 * are the only parts that a pathing JAR changes. Measured from resolved values because
 * the Nsight installation path alone is roughly 150 characters.
 */
private fun estimateCommandLineOverhead(
    javaExecutable: String,
    nsightPath: File,
    environment: List<String>,
    activity: NsightActivity
): Int {
    val arguments = buildList {
        add(nsightPath.absolutePath)
        add("--activity")
        add(activity.activity.orEmpty())
        add("--platform")
        add("--launch-detached")
        add("--exe")
        add(javaExecutable)
        add("--dir")
        add("--working-dir")
        add("--env")
        addAll(environment)
        if (activity.startAfterHotkey) add("--start-after-hotkey")
    }
    // The quoting helper can only lengthen an argument, so count each one plus its
    // separator and a quote on either side.
    return arguments.sumOf { it.length + 4 }
}

/**
 * JVM arguments for launching the client, with the classpath carried by [classpathJar]
 * when one was generated.
 *
 * A pathing JAR is expanded while loading, so its entries never reach the
 * `java.class.path` system property. Fabric Loader locates the Minecraft game by scanning
 * that property, so the real classpath is repeated as an explicit property override;
 * without it the client aborts with "couldn't locate the game". The override is as long
 * as the classpath, so it travels in a Java argument file placed next to the JAR rather
 * than on the command line.
 *
 * Loom materializes its own JVM argument file while the `runClient` task executes, so a
 * standalone run of an Nsight task would otherwise pass a `@file` reference to a file
 * that does not exist yet. Those references are dropped: the classpath is supplied here,
 * and `allJvmArgs` already carries the options that file held.
 */
private fun javaLaunchArguments(
    task: JavaExec,
    classpath: String,
    classpathJar: File?
): List<String> {
    val mainClass = task.mainClass.orNull
        ?: error("The ${task.name} task has no main class")
    return buildList {
        addAll(task.allJvmArgs.filterNot(::isJvmArgumentFile))
        if (classpathJar != null) {
            add("-cp")
            add(classpathJar.absolutePath)
            val propertyFile = writeJvmArgumentsFile(
                File(classpathJar.parentFile, "client-classpath-property.args"),
                "-Djava.class.path=$classpath"
            )
            if (propertyFile != null) add("@${propertyFile.absolutePath}")
        } else if (classpath.isNotBlank()) {
            add("-cp")
            add(classpath)
        }
        add(mainClass)
        addAll(task.args)
        task.argumentProviders.forEach { provider ->
            addAll(provider.asArguments())
        }
    }
}

/**
 * Writes a Java argument file holding a single JVM argument, or returns `null` when the
 * argument cannot survive the file's whitespace splitting.
 */
private fun writeJvmArgumentsFile(file: File, argument: String): File? {
    if (argument.any(Char::isWhitespace)) return null
    file.parentFile.mkdirs()
    file.writeText(argument, Charsets.UTF_8)
    return file
}

/** Matches the `@<path>` form of a Java launch argument file. */
private fun isJvmArgumentFile(argument: String): Boolean =
    argument.length > 1 && argument.startsWith("@") && !argument.startsWith("@@")

private fun quoteForNsight(argument: String): String {
    if (argument.isEmpty()) return "\"\""
    if (argument.none { it.isWhitespace() || it == '"' }) return argument
    return "\"${argument.replace("\\", "\\\\").replace("\"", "\\\"")}\""
}

/**
 * Renders `Class-Path` entries for [entries] as percent-encoded `file:` URLs.
 *
 * The JVM resolves `Class-Path` entries as URLs: a bare `C:/...` path and a raw space are
 * both dropped, which silently empties the classpath. Directories also need a trailing
 * slash, because an entry resolving to a directory without one is skipped.
 */
private fun manifestClassPath(entries: List<File>): String {
    return entries.joinToString(" ") { entry ->
        val suffix = if (entry.isDirectory) "/" else ""
        "file:/" + encodeAsUrl(entry.absolutePath) + suffix
    }
}

private fun encodeAsUrl(path: String): String = buildString(path.length) {
    path.forEach { character ->
        when (character) {
            '\\' -> append('/')
            ' ' -> append("%20")
            '%' -> append("%25")
            '#' -> append("%23")
            '?' -> append("%3F")
            else -> append(character)
        }
    }
}

private object ManifestJar {
    /**
     * Writes a JAR holding only a manifest. Manifest values must be wrapped at 72 bytes
     * with a leading space on continuation lines; a longer unwrapped `Class-Path` line is
     * split mid-path by the manifest parser and silently loses entries.
     */
    fun write(file: File, classPath: String) {
        val lines = wrapManifestLine("Class-Path: $classPath")
        val manifest = buildString {
            append("Manifest-Version: 1.0\r\n")
            lines.forEach { append(it).append("\r\n") }
            append("\r\n")
        }

        ZipOutputStream(file.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("META-INF/MANIFEST.MF"))
            zip.write(manifest.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()
        }
    }

    private fun wrapManifestLine(line: String): List<String> {
        // The manifest spec counts bytes, not characters.
        val bytes = line.toByteArray(StandardCharsets.UTF_8)
        val wrapped = mutableListOf<String>()
        var offset = 0
        while (offset < bytes.size) {
            // Continuation lines carry a leading space that also counts towards the limit.
            val limit = if (offset == 0) MANIFEST_LINE_BYTES else MANIFEST_LINE_BYTES - 1
            val length = minOf(limit, bytes.size - offset)
            val chunk = String(bytes, offset, length, StandardCharsets.UTF_8)
            wrapped += if (offset == 0) chunk else " $chunk"
            offset += length
        }
        return wrapped
    }
}

private const val MANIFEST_LINE_BYTES = 72

/** Headroom for the quoting Windows applies to an argument holding a path. */
private const val CLASS_PATH_QUOTING_ALLOWANCE = 4

/** Windows' limit for a process command line, in characters. */
private const val WINDOWS_COMMAND_LINE_LIMIT = 32767

