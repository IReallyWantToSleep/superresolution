import net.neoforged.moddevgradle.legacyforge.dsl.MixinExtension
import org.gradle.jvm.tasks.Jar
import utils.SrLoader
import utils.configureSrLoaderDependencies
import utils.srForgeMixinConfigs
import utils.srLegacyRuntimeLibraries
import utils.srVersionConfig
import utils.registerNsightRunTasks

plugins {
    id("multiloader-loader")
    id("net.neoforged.moddev.legacyforge")
}

val versionConfig = srVersionConfig

base {
    archivesName.set("super_resolution-forge-${versionConfig.common.modArtifactMinecraftVer}")
}

legacyForge {
    version = "${versionConfig.common.minecraftVersion}-${versionConfig.forge.loaderVersion}"
    runs {
        configureEach {
            versionConfig.srLegacyRuntimeLibraries(SrLoader.FORGE).forEach {
                additionalRuntimeClasspathConfiguration.dependencies.add(dependencies.create(it))
            }
        }
        create("client") {
            client()
            gameDirectory = rootProject.file("runs/forge")
            jvmArguments.add("-Dmixin.debug.export=true")
        }
    }
    mods {
        create(rootProject.property("mod_id").toString()) {
            sourceSet(extensions.getByType(SourceSetContainer::class.java).getByName("main"))
        }
    }
    val parchmentVersion = versionConfig.common.parchmentVersion
    if (parchmentVersion != null) {
        val parchmentParts = parchmentVersion.split(":")
        parchment {
            minecraftVersion = parchmentParts[0]
            mappingsVersion = parchmentParts[1]
        }
    }
}
val sourceSets = extensions.getByType(SourceSetContainer::class.java)
registerNsightRunTasks(
    taskGroup = "forge",
    preparationTaskNames = listOf("prepareClientRun"),
    workingDirectory = rootProject.file("runs/forge")
)
sourceSets.getByName("main").resources.srcDir("src/generated/resources")

val mixinExtension = extensions.getByType<MixinExtension>()
val refmapName = "super_resolution.refmap.json"
val refmap = mixinExtension.add(sourceSets.getByName("main"), refmapName)
srForgeMixinConfigs.forEach(mixinExtension::config)

configureSrLoaderDependencies(SrLoader.FORGE, packageSr = true)

dependencies {
    compileOnly("org.jetbrains:annotations:25.0.0")

    // Mixin AP 必须在 annotationProcessor 路径上，否则 -AoutTsrgFile/-AoutRefMapFile 无人消费，
    // reobfJar 需要的 build/mixin/*.mappings.tsrg 不会生成。
    // mixin 主 jar 的传递依赖是 provided 作用域，AP 运行所需的 gson/guava/asm 需显式带上。
    annotationProcessor("org.spongepowered:mixin:0.8.5")
    annotationProcessor("com.google.code.gson:gson:2.10")
    annotationProcessor("com.google.guava:guava:31.1-jre")
    annotationProcessor("org.ow2.asm:asm-tree:9.7")
}

sourceSets.forEach {
    val dir = layout.buildDirectory.dir("sourcesSets/${it.name}")
    it.output.setResourcesDir(dir.get().asFile)
    it.java.destinationDirectory.set(dir)
}

tasks.named<ProcessResources>("processResources") {
    // MDG adds this to jar automatically. Directory-based runs also need the refmap.
    dependsOn(tasks.named("compileJava"))
    from(refmap)
    val forgeModVersion = project.version.toString()
    val forgeVersionRange = versionConfig.common.forgeVersionRange.toString()

    inputs.property("version", forgeModVersion)
    inputs.property("versionRange", forgeVersionRange)

    filesMatching("META-INF/mods.toml") {
        expand(mapOf("version" to forgeModVersion))
        filter { line: String ->
            line.replace("\"{versionRange}\"", "\"$forgeVersionRange\"")
        }
    }
    if (gradle.extensions.extraProperties.properties["isUseDebugLib"] as? Boolean == true) {
        exclude("**/libSuperResolution*+*+release.*")
    } else {
        exclude("**/libSuperResolution*+*+debug.*")
    }
}

tasks.named<Jar>("jar") {
    filesMatching(refmapName) { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
    manifest.attributes(
        mapOf(
            "MixinConfigs" to srForgeMixinConfigs.joinToString(separator = ",")
        )
    )
}
