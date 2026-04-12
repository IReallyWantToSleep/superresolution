package multiversion

import groovy.json.JsonSlurper
import java.io.File

class VersionConfig(json: Map<*, *>) {
    val common: CommonConfig = CommonConfig(json["common"] as? Map<*, *> ?: emptyMap<String, Any>())

    lateinit var fabric: FabricPlatformConfig
    lateinit var forge: ForgePlatformConfig
    lateinit var neoforge: NeoForgePlatformConfig

    init {
        if (json["fabric"] != null && common.enableFabric) {
            fabric = FabricPlatformConfig(json["fabric"] as? Map<*, *> ?: emptyMap<String, Any>())
        }

        if (json["forge"] != null && common.enableForge) {
            forge = ForgePlatformConfig(json["forge"] as? Map<*, *> ?: emptyMap<String, Any>())
        }

        if (json["neoforge"] != null && common.enableNeoForge) {
            neoforge = NeoForgePlatformConfig(json["neoforge"] as? Map<*, *> ?: emptyMap<String, Any>())
        }
    }

    companion object {
        fun loadFromFile(file: File): VersionConfig {
            @Suppress("UNCHECKED_CAST")
            val json = JsonSlurper().parse(file) as Map<*, *>
            return VersionConfig(json)
        }
    }
}
