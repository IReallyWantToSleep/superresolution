import utils.publishSrDirectories
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
    from(commonResources)
}

tasks.named<Javadoc>("javadoc") {
    onlyIf { false }
}

tasks.named<Jar>("sourcesJar") {
    dependsOn(commonJava)
    from(commonJava)
    dependsOn(commonResources)
    from(commonResources)
}

// The loader already links all SR common sources and resources into its output.
// Consumers running from directories must use this output as the only copy of the SR mod.
afterEvaluate {
    publishSrDirectories("srDevOutput", listOf(sourceSets.main.get()), resources = true)
}
