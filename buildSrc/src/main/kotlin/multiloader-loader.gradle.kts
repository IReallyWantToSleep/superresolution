import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    id("multiloader-common")
}

val commonJava by configurations.creating {
    isCanBeResolved = true
}

val commonResources by configurations.creating {
    isCanBeResolved = true
}

val useDebugLib = gradle.extensions.extraProperties.properties["isUseDebugLib"] as? Boolean == true
val requiredStreamlineLibraries = listOf(
    "NvLowLatencyVk.dll",
    "sl.common.dll",
    "sl.dlss_g.dll",
    "sl.interposer.dll",
    "sl.pcl.dll",
    "sl.reflex.dll",
    "nvngx_dlssg.dll"
)
val selectedStreamlineResourceDir = rootProject.layout.projectDirectory.dir(
    "common/src/main/resources/lib/${if (useDebugLib) "sl.dev" else "sl.rel"}"
)
val streamlineSyncTasks = listOf(
    ":common:syncStreamlineDebugLibs",
    ":common:syncStreamlineReleaseLibs"
)

dependencies {
    compileOnly(project(":common")) {
        exclude(group = "me.shedaniel.cloth", module = "*")
        exclude(group = "dev.architectury", module = "*")
    }

    "commonJava"(project(mapOf("path" to ":common", "configuration" to "commonJava")))
    "commonResources"(project(mapOf("path" to ":common", "configuration" to "commonResources")))
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(commonJava)
    source(commonJava)
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(commonResources)
    dependsOn(streamlineSyncTasks)
    from(commonResources)
    exclude(
        "lib/sl.dev/**",
        "lib/sl.rel/**",
        "lib/sl.*.dll",
        "lib/NvLowLatencyVk.dll"
    )
    from(selectedStreamlineResourceDir) {
        include(requiredStreamlineLibraries)
        into("lib")
    }
}

tasks.named<Javadoc>("javadoc") {
    onlyIf { false }
}

tasks.named<Jar>("sourcesJar") {
    dependsOn(commonJava)
    from(commonJava)
    dependsOn(commonResources)
    dependsOn(streamlineSyncTasks)
    from(commonResources)
    exclude(
        "lib/sl.dev/**",
        "lib/sl.rel/**",
        "lib/sl.*.dll",
        "lib/NvLowLatencyVk.dll"
    )
    from(selectedStreamlineResourceDir) {
        include(requiredStreamlineLibraries)
        into("lib")
    }
}
