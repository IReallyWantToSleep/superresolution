package multiversion

class CommonConfig {
    int javaVersion
    String minecraftVersion
    String parchmentVersion
    String neoFormVersion
    List<String> platforms
    String lwjglVersion
    String architecturyApiVersion
    String clothConfigVersion
    String modArtifactMinecraftVer
    String forgeVersionRange
    String neoforgeVersionRange

    List<String> fabricVersionRange

    CommonConfig(Map config) {
        this.javaVersion = config.java_version as Integer
        this.minecraftVersion = config.minecraft_version as String
        this.parchmentVersion = config.parchment_version as String
        if (config.neoform_version != null) this.neoFormVersion = config.neoform_version as String
        this.platforms = config.platforms as List<String>
        this.lwjglVersion = config.lwjgl_version as String
        this.architecturyApiVersion = config.architectury_api_version as String
        this.clothConfigVersion = config.cloth_config_version as String
        this.modArtifactMinecraftVer = config.mod_artifact_minecraft_ver as String
        if (config.forge != null) this.forgeVersionRange = config.forge.minecraft_version_range as String
        if (config.fabric != null) this.fabricVersionRange = config.fabric.minecraft_version_range as List<String>
        if (config.neoforge != null) this.neoforgeVersionRange = config.neoforge.minecraft_version_range as String
    }
}