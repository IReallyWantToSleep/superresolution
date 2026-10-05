package utils

import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.JavaExec
import java.io.File
import java.util.Locale

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
                val javaArguments = javaLaunchArguments(task)
                val environment = task.environment.map { (key, value) -> "$key=$value" }
                val nsightPath = File(
                    if (activity.executable == "ngfx") ngfx.get() else ngfxCapture.get()
                ).absoluteFile
                val nsightWorkingDirectory = nsightPath.parentFile ?: workingDirectory

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

private fun javaLaunchArguments(task: JavaExec): List<String> {
    val mainClass = task.mainClass.orNull
        ?: error("The ${task.name} task has no main class")
    return buildList {
        addAll(task.allJvmArgs)
        val classpath = task.classpath.asPath
        if (classpath.isNotBlank()) {
            add("-cp")
            add(classpath)
        } else {
            val provider = task.javaClass.methods.firstOrNull {
                it.name == "getClasspathProvider" && it.parameterCount == 0
            }?.invoke(task)
            val providerClasspath = provider?.let {
                it.javaClass.methods.firstOrNull { method ->
                    method.name == "getAsPath" && method.parameterCount == 0
                }?.invoke(it)?.toString()
            }.orEmpty()
            if (providerClasspath.isNotBlank()) {
                add("-cp")
                add(providerClasspath)
            }
        }
        add(mainClass)
        addAll(task.args)
        task.argumentProviders.forEach { provider ->
            addAll(provider.asArguments())
        }
    }
}

private fun quoteForNsight(argument: String): String {
    if (argument.isEmpty()) return "\"\""
    if (argument.none { it.isWhitespace() || it == '"' }) return argument
    return "\"${argument.replace("\\", "\\\\").replace("\"", "\\\"")}\""
}
