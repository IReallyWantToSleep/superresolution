package multiversion

class CommonConfig(config: Map<*, *>) {
    val javaVersion: Int = (config["java_version"] as? Number)?.toInt() ?: 0
    val minecraftVersion: String = config["minecraft_version"]?.toString().orEmpty()
    val parchmentVersion: String? = config["parchment_version"]?.toString()
    val neoFormVersion: String? = config["neoform_version"]?.toString()

    val platforms: List<String> = (config["platforms"] as? List<*>)
        ?.mapNotNull { it?.toString() }
        ?: emptyList()

    val lwjglVersion: String = config["lwjgl_version"]?.toString().orEmpty()

    // LWJGL 3.3.4 移除了旧版 stb_image_resize API（stbir_resize_uint8），而旧版
    // Minecraft 的 NativeImage.resizeSubRectTo 链接的正是该 API，因此这类版本需要在
    // 配置里单独指定 lwjgl_stb_version（如 3.3.3）；缺省时跟随 lwjgl_version。
    val lwjglStbVersion: String = config["lwjgl_stb_version"]?.toString()?.takeIf { it.isNotBlank() } ?: lwjglVersion
    val architecturyApiVersion: String? = config["architectury_api_version"]?.toString()
    val clothConfigVersion: String? = config["cloth_config_version"]?.toString()
    val modArtifactMinecraftVer: String = config["mod_artifact_minecraft_ver"]?.toString().orEmpty()

    var forgeVersionRange: String? = null
    var neoforgeVersionRange: String? = null
    var fabricVersionRange: List<String> = emptyList()

    val enableFabric: Boolean = platforms.contains("fabric")
    val enableForge: Boolean = platforms.contains("forge")
    val enableNeoForge: Boolean = platforms.contains("neoforge")

    init {
        val forge = config["forge"] as? Map<*, *>
        if (forge != null && enableForge) {
            forgeVersionRange = forge["minecraft_version_range"]?.toString()
        }

        val fabric = config["fabric"] as? Map<*, *>
        if (fabric != null && enableFabric) {
            fabricVersionRange = (fabric["minecraft_version_range"] as? List<*>)
                ?.mapNotNull { it?.toString() }
                ?: emptyList()
        }

        val neoforge = config["neoforge"] as? Map<*, *>
        if (neoforge != null && enableNeoForge) {
            neoforgeVersionRange = neoforge["minecraft_version_range"]?.toString()
        }
    }
}
