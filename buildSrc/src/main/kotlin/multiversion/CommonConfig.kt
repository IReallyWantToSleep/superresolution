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
    val hasLwjglVma: Boolean = compareVersionParts(lwjglVersion, "3.3.4") >= 0
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

    private fun compareVersionParts(left: String, right: String): Int {
        val leftParts = left.splitToSequence(Regex("[^0-9]+"))
            .filter { it.isNotEmpty() }
            .map { it.toIntOrNull() ?: 0 }
            .toList()
        val rightParts = right.splitToSequence(Regex("[^0-9]+"))
            .filter { it.isNotEmpty() }
            .map { it.toIntOrNull() ?: 0 }
            .toList()
        val max = maxOf(leftParts.size, rightParts.size)
        for (i in 0 until max) {
            val l = leftParts.getOrElse(i) { 0 }
            val r = rightParts.getOrElse(i) { 0 }
            if (l != r) {
                return l.compareTo(r)
            }
        }
        return 0
    }
}
