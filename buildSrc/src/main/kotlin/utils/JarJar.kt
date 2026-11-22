/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package utils

import java.io.File
import java.security.MessageDigest
import java.util.HexFormat
import java.util.jar.JarFile
import org.gradle.api.Project
import org.gradle.api.file.FileCollection

object JarJar {
    fun extractJars(project: Project, notation: Any): FileCollection {
        val files = if (notation is File) {
            require(notation.isFile) { "Nested-jar dependency does not exist: $notation" }
            setOf(notation)
        } else {
            project.configurations.detachedConfiguration(project.dependencies.create(notation)).files
        }
        val jars = files.filter { it.isFile && it.extension == "jar" }
        return if (jars.isEmpty()) project.files(files)
        else project.files(jars.flatMap { extractNestedJars(project, it) + it })
    }

    private fun extractNestedJars(project: Project, jar: File): List<File> {
        val digest = MessageDigest.getInstance("MD5")
        jar.forEachBlock { buffer, length -> digest.update(buffer, 0, length) }
        val hash = HexFormat.of().formatHex(digest.digest())
        val dir = project.layout.buildDirectory.dir("jarjar-extracted/${jar.name}-$hash").get().asFile
        return JarFile(jar).use { archive ->
            archive.entries().asSequence().filter { entry ->
                !entry.isDirectory && entry.name.endsWith(".jar") &&
                    (entry.name.startsWith("META-INF/jarjar/") || entry.name.startsWith("META-INF/jars/"))
            }.map { entry ->
                val target = dir.resolve(File(entry.name).name)
                target.parentFile.mkdirs()
                archive.getInputStream(entry).use { input -> target.outputStream().use { input.copyTo(it) } }
                project.logger.info("Extracted nested jar: ${entry.name} -> $target")
                target
            }.toList()
        }
    }
}
