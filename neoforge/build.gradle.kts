import multiversion.VersionConfig
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.language.jvm.tasks.ProcessResources
import utils.MinecraftVersion

plugins {
    id("multiloader-loader")
    id("net.neoforged.moddev")
}

@Suppress("UNCHECKED_CAST")
val versionConfig = rootProject.extra["versionConfig"] as VersionConfig

repositories {
    maven {
        url = uri("https://cursemaven.com")
        content {
            includeGroup("curse.maven")
        }
    }
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
    maven {
        name = "CaffeineMC"
        url = uri("https://maven.caffeinemc.net/releases")
        mavenContent { releasesOnly() }
        content {
            includeGroup("net.caffeinemc")
        }
    }

    maven {
        name = "CaffeineMC"
        url = uri("https://maven.caffeinemc.net/snapshots")
        mavenContent { snapshotsOnly() }
        content {
            includeGroup("net.caffeinemc")
        }
    }
}

val libraries = configurations.create("libraries")

val sourceSets = extensions.getByType(SourceSetContainer::class.java)
sourceSets.getByName("main").resources.srcDir("src/generated/resources")
sourceSets.configureEach {
    runtimeClasspath += libraries
}

neoForge {
    version = versionConfig.neoforge.loaderVersion
    val parchmentVersion = versionConfig.common.parchmentVersion
    if (parchmentVersion != null) {
        val parchmentParts = parchmentVersion.split(":")
        parchment {
            minecraftVersion = parchmentParts[0]
            mappingsVersion = parchmentParts[1]
        }
    }
    runs {
        configureEach {
            jvmArguments.add("-XX:+CreateMinidumpOnCrash")
            systemProperty("neoforge.enabledGameTestNamespaces", rootProject.property("mod_id").toString())
            ideName = "NeoForge ${name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} (${project.path})"

            if (MinecraftVersion.of(versionConfig.common.minecraftVersion) < MinecraftVersion.of("1.21.9")) {
                additionalRuntimeClasspathConfiguration.dependencies.add(dependencies.create("io.github.spair:imgui-java-app:1.87.5"))
                additionalRuntimeClasspathConfiguration.dependencies.add(dependencies.create("io.github.spair:imgui-java-binding:1.87.5"))
                additionalRuntimeClasspathConfiguration.dependencies.add(dependencies.create("io.github.spair:imgui-java-lwjgl3:1.87.5"))
                additionalRuntimeClasspathConfiguration.dependencies.add(dependencies.create("org.lwjgl:lwjgl-vulkan:${versionConfig.common.lwjglVersion}"))
                additionalRuntimeClasspathConfiguration.dependencies.add(dependencies.create("org.lwjgl:lwjgl-vma:${versionConfig.common.lwjglVersion}"))
                additionalRuntimeClasspathConfiguration.dependencies.add(dependencies.create("org.lwjgl:lwjgl-vma::natives-windows"))
                additionalRuntimeClasspathConfiguration.dependencies.add(dependencies.create("org.lwjgl:lwjgl-vma::natives-linux"))

            }
        }
        create("client") {
            client()
            gameDirectory = rootProject.file("runs/neoforge")
        }
    }
    mods {
        create(rootProject.property("mod_id").toString()) {
            sourceSet(sourceSets.getByName("main"))
        }
    }
}

dependencies {
    val imguiApp = implementation("io.github.spair:imgui-java-app:1.87.5")
    if (imguiApp != null) "libraries"(imguiApp)

    val imguiBinding = implementation("io.github.spair:imgui-java-binding:1.87.5")
    if (imguiBinding != null) "libraries"(imguiBinding)

    val imguiLwjgl = implementation("io.github.spair:imgui-java-lwjgl3:1.87.5")
    if (imguiLwjgl != null) "libraries"(imguiLwjgl)

    implementation("org.lwjgl:lwjgl-vulkan:${versionConfig.common.lwjglVersion}")?.let { jarJar(it);libraries(it) }
    implementation("org.lwjgl:lwjgl-vma:${versionConfig.common.lwjglVersion}")?.let { jarJar(it);libraries(it) }
    implementation("org.lwjgl:lwjgl-vma::natives-windows")?.let { jarJar(it);libraries(it) }
    implementation("org.lwjgl:lwjgl-vma::natives-linux")?.let { jarJar(it);libraries(it) }

    if (versionConfig.common.architecturyApiVersion != null) {
        implementation("dev.architectury:architectury-neoforge:${versionConfig.common.architecturyApiVersion}")
    }
    implementation("net.fabricmc.fabric-api:fabric-api-base:0.4.64+9ec45cd8e8")

    for (lib in versionConfig.neoforge.dependencies.modrinth) {
        var depName = "maven.modrinth:${lib.name}:${lib.version}-neoforge,${lib.minecraftVersion ?: versionConfig.common.minecraftVersion}"
        if (lib.name == "sodium" && MinecraftVersion.of(versionConfig.common.minecraftVersion) > MinecraftVersion.of("1.21.10")) {
            depName = "net.caffeinemc:sodium-neoforge-mod:${lib.version}"
            if (lib.compileOnly) {
                compileOnly(depName)
            } else {
                implementation(depName)
            }

            depName = "net.caffeinemc:sodium-neoforge:${lib.version}"
            if (lib.compileOnly) {
                compileOnly(depName)
            } else {
                implementation(depName)
            }
            continue
        }
        if (lib.compileOnly) {
            if (lib.useJarJar) {
                compileOnly(utils.JarJar.extractJars(project, depName))
            } else {
                compileOnly(depName)
            }
        } else {
            if (lib.useJarJar) {
                implementation(utils.JarJar.extractJars(project, depName))
            } else {
                implementation(depName)
            }
        }
    }

    for (lib in versionConfig.neoforge.dependencies.local) {
        if (lib.isMod) {
            if (lib.compileOnly) {
                if (lib.useJarJar) {
                    compileOnly(utils.JarJar.extractJars(project, "../libs/${lib.name}.jar"))
                } else {
                    compileOnly(files("../libs/${lib.name}.jar"))
                }
            } else {
                if (lib.useJarJar) {
                    implementation(utils.JarJar.extractJars(project, "../libs/${lib.name}.jar"))
                } else {
                    implementation(files("../libs/${lib.name}.jar"))
                }
            }
        } else {
            if (lib.compileOnly) {
                compileOnly(files("../libs/${lib.name}.jar"))
            } else {
                implementation(files("../libs/${lib.name}.jar"))
            }
        }
    }
}

tasks.named<ProcessResources>("processResources") {
    val neoModVersion = project.version.toString()
    val neoVersionRange = versionConfig.common.neoforgeVersionRange.toString()

    inputs.property("version", neoModVersion)
    inputs.property("versionRange", neoVersionRange)

    filesMatching("META-INF/neoforge.mods.toml") {
        expand(
            mapOf(
                "version" to neoModVersion,
                "versionRange" to neoVersionRange
            )
        )
    }

    if (MinecraftVersion.of(versionConfig.common.minecraftVersion) < MinecraftVersion.of("1.21.8")) {
        exclude("META-INF/services/net.neoforged.neoforgespi.earlywindow.*")
    }
    if (gradle.extensions.extraProperties.properties["isUseDebugLib"] as? Boolean == true){
        exclude("**/libSuperResolution*+*+release.*")
    } else {
        exclude("**/libSuperResolution*+*+debug.*")
    }
}
