package superresolution

class Dependence {
    String name
    String version
    boolean isMod

    Dependence(Map lib) {
        this.name = lib.name as String
        this.version = lib.version ? lib.version as String : ""
        this.isMod = lib.isMod ? lib.isMod as Boolean : true
    }
}