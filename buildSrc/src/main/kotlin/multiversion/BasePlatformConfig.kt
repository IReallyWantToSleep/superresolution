package multiversion

open class BasePlatformConfig(config: Map<*, *>) {
    val loaderVersion: String = config["loader_version"]?.toString().orEmpty()
    val dependencies: Dependencies = Dependencies(config["dependencies"] as? Map<*, *> ?: emptyMap<String, Any>())
}
