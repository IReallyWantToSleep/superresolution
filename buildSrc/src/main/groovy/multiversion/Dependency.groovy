package multiversion

class Dependency {
    String name
    String version
    String minecraftVersion
    boolean isMod
    boolean compileOnly

    Dependency(Map lib) {
        this.name = lib.name as String
        this.version = lib.version ? lib.version as String : ""
        this.isMod = lib.isMod != null ? lib.isMod as Boolean : true
        this.compileOnly = lib.compileOnly != null ? lib.compileOnly as Boolean : false
        this.minecraftVersion = lib.minecraft_version != null ? lib.minecraft_version as String : null
    }
}