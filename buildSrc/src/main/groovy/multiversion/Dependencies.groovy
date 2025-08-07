package multiversion

class Dependencies {
    List<Dependency> modrinth
    List<Dependency> local

    Dependencies(Map libs) {
        this.modrinth = libs.modrinth.collect { new Dependency(it as Map) }
        this.local = libs.local.collect { new Dependency(it as Map) }
    }
}