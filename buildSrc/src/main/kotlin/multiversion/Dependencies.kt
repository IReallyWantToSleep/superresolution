package multiversion

class Dependencies(libs: Map<*, *>) {
    val modrinth: List<Dependency> = (libs["modrinth"] as? List<*>)
        ?.mapNotNull { it as? Map<*, *> }
        ?.map { Dependency(it) }
        ?: emptyList()

    val local: List<Dependency> = (libs["local"] as? List<*>)
        ?.mapNotNull { it as? Map<*, *> }
        ?.map { Dependency(it) }
        ?: emptyList()
}
