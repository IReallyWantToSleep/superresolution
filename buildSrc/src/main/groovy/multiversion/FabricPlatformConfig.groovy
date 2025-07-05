package multiversion

class FabricPlatformConfig extends BasePlatformConfig {
    String apiVersion
    String modmenuVersion

    FabricPlatformConfig(Map config) {
        super(config)
        this.apiVersion = config.api_version as String
        this.modmenuVersion = config.modmenu_version as String
    }
}