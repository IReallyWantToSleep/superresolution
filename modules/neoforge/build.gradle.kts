import utils.SrLoader
import utils.configureSrLoaderDependencies
import utils.srLegacyRuntimeLibraries
import utils.srVersionConfig
import utils.registerNsightRunTasks

plugins {
    id("multiloader-loader")
    id("net.neoforged.moddev")
}

val versionConfig = srVersionConfig
configureSrLoaderDependencies(SrLoader.NEOFORGE, packageSr = true)

val sourceSets = extensions.getByType(SourceSetContainer::class.java)
sourceSets.getByName("main").resources.srcDir("src/generated/resources")

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
            jvmArguments.add("-Dmixin.debug.export=true")

            // We force set the lwjgl version to versionConfig.common.lwjglVersion
            // Sodium will check the lwjgl version,ensure it equals the lwjgl version of corresponding Minecraft version
            // We must disable the check,otherwise sodium will crash the game.
            jvmArguments.add("-Dsodium.checks.issue2561=false")
            systemProperty("neoforge.enabledGameTestNamespaces", rootProject.property("mod_id").toString())
            ideName =
                "NeoForge ${name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} (${project.path})"

            versionConfig.srLegacyRuntimeLibraries(SrLoader.NEOFORGE).forEach {
                additionalRuntimeClasspathConfiguration.dependencies.add(dependencies.create(it))
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

registerNsightRunTasks(
    taskGroup = "mod development",
    preparationTaskNames = listOf("prepareClientRun"),
    workingDirectory = rootProject.file("runs/neoforge")
)

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

    if (gradle.extensions.extraProperties.properties["isUseDebugLib"] as? Boolean == true) {
        exclude("**/libSuperResolution*+*+release.*")
    } else {
        exclude("**/libSuperResolution*+*+debug.*")
    }
}
