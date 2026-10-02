package multiversion

import groovy.json.JsonSlurper
import java.io.File

class VersionConfig(json: Map<*, *>) {
    val common: CommonConfig = CommonConfig(json["common"] as? Map<*, *> ?: emptyMap<String, Any>())

    var fabric: FabricPlatformConfig = if (json["fabric"] != null && common.enableFabric) {
        FabricPlatformConfig(json["fabric"] as? Map<*, *> ?: emptyMap<String, Any>())
    } else {
        FabricPlatformConfig(emptyMap<String, Any>())
    }
    var forge: ForgePlatformConfig = if (json["forge"] != null && common.enableForge) {
        ForgePlatformConfig(json["forge"] as? Map<*, *> ?: emptyMap<String, Any>())
    } else {
        ForgePlatformConfig(emptyMap<String, Any>())
    }
    var neoforge: NeoForgePlatformConfig = if (json["neoforge"] != null && common.enableNeoForge) {
        NeoForgePlatformConfig(json["neoforge"] as? Map<*, *> ?: emptyMap<String, Any>())
    } else {
        NeoForgePlatformConfig(emptyMap<String, Any>())
    }

    companion object {
        fun loadFromFile(file: File): VersionConfig {
            @Suppress("UNCHECKED_CAST")
            val json = JsonSlurper().parse(file) as Map<*, *>
            return VersionConfig(json)
        }
    }
}
