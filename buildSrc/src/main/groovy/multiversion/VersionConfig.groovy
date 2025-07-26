package multiversion

import groovy.json.JsonSlurper

class VersionConfig {
    CommonConfig common
    FabricPlatformConfig fabric
    ForgePlatformConfig forge
    NeoForgePlatformConfig neoforge

    VersionConfig(Map json) {
        this.common = new CommonConfig(json.common as Map)
        if (json.fabric != null) this.fabric = new FabricPlatformConfig(json.fabric as Map)
        if (json.forge != null) this.forge = new ForgePlatformConfig(json.forge as Map)
        if (json.neoforge != null) this.neoforge = new NeoForgePlatformConfig(json.neoforge as Map)
    }

    static VersionConfig loadFromFile(File file) {
        def json = new JsonSlurper().parse(file)
        return new VersionConfig(json)
    }
}

