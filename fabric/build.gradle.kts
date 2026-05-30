import multiversion.VersionConfig
import org.gradle.language.jvm.tasks.ProcessResources
import utils.MinecraftVersion

plugins {
    id("multiloader-loader")
    id("net.fabricmc.fabric-loom") version "1.16.1"
}

@Suppress("UNCHECKED_CAST")
val versionConfig = rootProject.extra["versionConfig"] as VersionConfig

fun findFirstConfiguration(vararg names: String): String {
    return names.firstOrNull { name -> configurations.findByName(name) != null } ?: names.last()
}

fun DependencyHandler.mappingsCompat(notation: Any) =
    add(findFirstConfiguration("mappings"), notation)

fun DependencyHandler.modImplementationCompat(notation: Any) =
    add(findFirstConfiguration("modImplementation", "implementation"), notation)

fun DependencyHandler.modCompileOnlyCompat(notation: Any) =
    add(findFirstConfiguration("modCompileOnly", "compileOnly"), notation)

repositories {
    maven {
        url = uri("https://cursemaven.com")
        content {
            includeGroup("curse.maven")
        }
    }
    maven {
        name = "Terraformers"
        url = uri("https://maven.terraformersmc.com/")
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

dependencies {
    minecraft("com.mojang:minecraft:${versionConfig.common.minecraftVersion}")
    if (MinecraftVersion.of(versionConfig.common.minecraftVersion) < MinecraftVersion.of("26.1")) {
        mappingsCompat(
            loom.layered {
                officialMojangMappings()
                if (versionConfig.common.parchmentVersion != null) {
                    parchment("org.parchmentmc.data:parchment-${versionConfig.common.parchmentVersion}@zip")
                }
            }
        )
    }

    modImplementationCompat("net.fabricmc:fabric-loader:${versionConfig.fabric.loaderVersion}")
    modImplementationCompat("net.fabricmc.fabric-api:fabric-api:${versionConfig.fabric.apiVersion}")

    if (versionConfig.common.architecturyApiVersion != null) {
        modImplementationCompat("dev.architectury:architectury-fabric:${versionConfig.common.architecturyApiVersion}")
    }
    if (versionConfig.common.clothConfigVersion != null) {
        modImplementationCompat("me.shedaniel.cloth:cloth-config-fabric:${versionConfig.common.clothConfigVersion}")
    }
    if (versionConfig.fabric.modmenuVersion != null) {
        modImplementationCompat("com.terraformersmc:modmenu:${versionConfig.fabric.modmenuVersion}")
    }

    implementation("net.lenni0451:Reflect:1.3.4")
    implementation("io.github.spair:imgui-java-app:1.87.5")
    implementation("io.github.spair:imgui-java-binding:1.87.5")
    implementation("io.github.spair:imgui-java-lwjgl3:1.87.5")
    implementation("org.anarres:jcpp:1.4.14")
    implementation("org.antlr:antlr4-runtime:4.13.1")

    val busDep = implementation("net.neoforged:bus:8.0.5")
    if (busDep != null) include(busDep)

    val typetoolsDep = implementation("net.jodah:typetools:0.6.3")
    if (typetoolsDep != null) include(typetoolsDep)

    if (MinecraftVersion.of(versionConfig.common.minecraftVersion) < MinecraftVersion.of("1.21.8")) {
        implementation("io.github.douira:glsl-transformer:2.0.1")
    } else {
        implementation("io.github.douira:glsl-transformer:3.0.0-pre3")
    }

    implementation("org.lwjgl:lwjgl-vulkan:${versionConfig.common.lwjglVersion}")?.let { include(it) }
    if (versionConfig.common.hasLwjglVma) {
        implementation("org.lwjgl:lwjgl-vma:${versionConfig.common.lwjglVersion}")?.let { include(it) }
        implementation("org.lwjgl:lwjgl-vma::natives-windows")?.let { include(it) }
        implementation("org.lwjgl:lwjgl-vma::natives-linux")?.let { include(it) }
    }

    val nightToml = implementation("com.electronwill.night-config:toml:3.8.0")
    if (nightToml != null) include(nightToml)
    val nightCore = implementation("com.electronwill.night-config:core:3.8.0")
    if (nightCore != null) include(nightCore)

    for (lib in versionConfig.fabric.dependencies.modrinth) {
        var depName = "maven.modrinth:${lib.name}:${lib.version}-fabric,${lib.minecraftVersion ?: versionConfig.common.minecraftVersion}"
        if (lib.name == "sodium" && MinecraftVersion.of(versionConfig.common.minecraftVersion) > MinecraftVersion.of("1.21.10")) {
            depName = "net.caffeinemc:sodium-fabric:${lib.version}"
            if (lib.compileOnly) {
                modCompileOnlyCompat(depName)
            } else {
                modImplementationCompat(depName)
            }
            continue
        }

        if (lib.compileOnly) {
            if (lib.useJarJar) {
                modCompileOnlyCompat(utils.JarJar.extractJars(project, depName))
            } else {
                modCompileOnlyCompat(depName)
            }
        } else {
            if (lib.useJarJar) {
                modImplementationCompat(utils.JarJar.extractJars(project, depName))
            } else {
                modImplementationCompat(depName)
            }
        }
    }

    for (lib in versionConfig.fabric.dependencies.local) {
        if (lib.isMod) {
            if (lib.compileOnly) {
                if (lib.useJarJar) {
                    modCompileOnlyCompat(utils.JarJar.extractJars(project, "../libs/${lib.name}.jar"))
                } else {
                    modCompileOnlyCompat(files("../libs/${lib.name}.jar"))
                }
            } else {
                if (lib.useJarJar) {
                    modImplementationCompat(utils.JarJar.extractJars(project, "../libs/${lib.name}.jar"))
                } else {
                    modImplementationCompat(files("../libs/${lib.name}.jar"))
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

loom {
    mixin {
        if(MinecraftVersion.of(versionConfig.common.minecraftVersion) < MinecraftVersion.of("26.1")) {
            useLegacyMixinAp = true
            defaultRefmapName.set("${rootProject.property("mod_id")}.refmap.json")
        }
    }
    runs {
        named("client") {
            vmArg("-XX:+CreateMinidumpOnCrash")
            vmArg("--enable-preview")
            vmArg("--enable-native-access=ALL-UNNAMED")
            vmArg("-Dmixin.debug.export=true")
            ideConfigGenerated(true)
            runDir("../runs/fabric")
        }
    }
}

val fabricModVersion = project.version.toString()
val fabricJavaVersion = versionConfig.common.javaVersion.toString()
val fabricVersionRange = if (versionConfig.common.fabricVersionRange.size == 1) {
    "\"${versionConfig.common.fabricVersionRange[0]}\""
} else {
    versionConfig.common.fabricVersionRange.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", fabricModVersion)
    inputs.property("javaVersion", fabricJavaVersion)
    inputs.property("versionRange", fabricVersionRange)
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
