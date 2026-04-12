package multiversion

class Dependency(lib: Map<*, *>) {
    val name: String = lib["name"]?.toString().orEmpty()
    val version: String = lib["version"]?.toString().orEmpty()
    val minecraftVersion: String? = lib["minecraft_version"]?.toString()
    val isMod: Boolean = (lib["isMod"] as? Boolean) ?: true
    val compileOnly: Boolean = (lib["compileOnly"] as? Boolean) ?: false
    val useJarJar: Boolean = (lib["use_jarjar"] as? Boolean) ?: false
}
