package multiversion

class Dependence {
    String name
    String version
    boolean isMod
    boolean compileOnly

    Dependence(Map lib) {
        this.name = lib.name as String
        this.version = lib.version ? lib.version as String : ""
        this.isMod = lib.isMod != null ? lib.isMod as Boolean : true
        this.compileOnly = lib.compileOnly != null ? lib.compileOnly as Boolean : false

    }
}