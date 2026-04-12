package multiversion

class FabricPlatformConfig(config: Map<*, *>) : BasePlatformConfig(config) {
    val apiVersion: String? = config["api_version"]?.toString()
    val modmenuVersion: String? = config["modmenu_version"]?.toString()
}
