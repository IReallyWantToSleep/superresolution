package superresolution

class Dependencies {
    List<Dependence> modrinth
    List<Dependence> local

    Dependencies(Map libs) {
        this.modrinth = libs.modrinth.collect { new Dependence(it as Map) }
        this.local = libs.local.collect { new Dependence(it as Map) }
    }
}