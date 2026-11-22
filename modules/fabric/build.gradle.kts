import net.fabricmc.loom.api.LoomGradleExtensionAPI
import utils.SrLoader
import utils.configureSrLoaderDependencies
import utils.loomPlugin
import utils.srVersionConfig
import org.gradle.language.jvm.tasks.ProcessResources
import utils.registerNsightRunTasks

plugins {
    id("multiloader-loader")
}

val versionConfig = srVersionConfig
apply(plugin = versionConfig.loomPlugin)
val loom = extensions.getByType<LoomGradleExtensionAPI>()

configureSrLoaderDependencies(SrLoader.FABRIC, packageSr = true)

dependencies {
    add("minecraft", "com.mojang:minecraft:${versionConfig.common.minecraftVersion}")
    if (!versionConfig.unobfuscated) {
        add("mappings", loom.layered {
            officialMojangMappings()
            versionConfig.common.parchmentVersion?.let { parchment("org.parchmentmc.data:parchment-$it@zip") }
        })
    }
}

loom.apply {
    mixin {
        if (!versionConfig.unobfuscated) {
            useLegacyMixinAp = true
            defaultRefmapName.set("${rootProject.property("mod_id")}.refmap.json")
        }
    }
    runs {
        named("client") {
            jvmArguments.add("-XX:+CreateMinidumpOnCrash")

            jvmArguments.add("--enable-preview")
            jvmArguments.add("--enable-native-access=ALL-UNNAMED")

            jvmArguments.add("-Dmixin.debug.export=true")

            // We force set the lwjgl version to versionConfig.common.lwjglVersion
            // Sodium will check the lwjgl version,ensure it equals the lwjgl version of corresponding Minecraft version
            // We must disable the check,otherwise sodium will crash the game.
            jvmArguments.add("-Dsodium.checks.issue2561=false")

            // Only effective in 1.21.5+
            programArguments.add("--graphicsBackend=opengl")
            programArguments.add("--renderDebugLabels=true")

            runDirectory.set(rootProject.file("runs/fabric"))
        }
    }
}

registerNsightRunTasks(
    taskGroup = "fabric",
    preparationTaskNames = listOf("configureClientLaunch"),
    workingDirectory = rootProject.file("runs/fabric")
)

val fabricModVersion = project.version.toString()
val fabricJavaVersion = versionConfig.common.javaVersion.toString()
val fabricVersionRange = if (versionConfig.common.fabricVersionRange.size == 1) {
    "\"${versionConfig.common.fabricVersionRange[0]}\""
} else {
    versionConfig.common.fabricVersionRange.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
}

// The velocity extension is versioned because Iris and Minecraft change its rendering APIs.
// The common jar only contains the selected version's mixin config, so keep this reference
// conditional — Fabric loader fails hard on a mixin config that is absent from the jar.
val irisVelocityExtMixinEntry = if (
    versionConfig.common.minecraftVersion.startsWith("26.1")
        || versionConfig.common.minecraftVersion == "26.2"
        || versionConfig.common.minecraftVersion == "26.3"
) {
    ",\n        \"super_resolution.iris_velocity_ext.mixins.json\""
} else {
    ""
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", fabricModVersion)
    inputs.property("javaVersion", fabricJavaVersion)
    inputs.property("versionRange", fabricVersionRange)
    inputs.property("irisVelocityExtMixin", irisVelocityExtMixinEntry)
    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "version" to fabricModVersion,
                "javaVersion" to fabricJavaVersion,
                "versionRange" to fabricVersionRange
            )
        )
        filter { line: String ->
            line.replace("\"{versionRange}\"", fabricVersionRange)
                .replace("{irisVelocityExtMixin}", irisVelocityExtMixinEntry)
        }
    }
    if (gradle.extensions.extraProperties.properties["isUseDebugLib"] as? Boolean == true){
        exclude("**/libSuperResolution*+*+release.*")
    } else {
        exclude("**/libSuperResolution*+*+debug.*")
    }
}

tasks.matching { it.name == "remapSourcesJar" }.configureEach {
    enabled = false
}
