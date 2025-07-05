package multiversion

class BasePlatformConfig {
    String loaderVersion
    Dependencies dependencies

    BasePlatformConfig(Map config) {
        this.loaderVersion = config.loader_version as String
        this.dependencies = new Dependencies(config.dependencies as Map)
    }
}