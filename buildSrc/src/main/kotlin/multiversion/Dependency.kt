package multiversion

class Dependency(lib: Map<*, *>) {
    val name: String = lib["name"]?.toString().orEmpty()
    val version: String = lib["version"]?.toString().orEmpty()
    val projectId: String = lib["project_id"]?.toString().orEmpty()
    val fileId: String = lib["file_id"]?.toString().orEmpty()
    val minecraftVersion: String? = lib["minecraft_version"]?.toString()
    val isMod: Boolean = (lib["isMod"] as? Boolean) ?: true
    val compileOnly: Boolean = (lib["compileOnly"] as? Boolean) ?: false
    val useJarJar: Boolean = (lib["use_jarjar"] as? Boolean) ?: false

    fun curseMavenNotation(): String {
        require(name.isNotBlank()) { "CurseForge dependency is missing name" }
        require(projectId.isNotBlank()) { "CurseForge dependency '$name' is missing project_id" }
        require(fileId.isNotBlank()) { "CurseForge dependency '$name' is missing file_id" }
        return "curse.maven:$name-$projectId:$fileId"
    }
}
