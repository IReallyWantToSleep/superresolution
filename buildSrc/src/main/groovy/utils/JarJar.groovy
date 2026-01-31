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

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact

import java.security.MessageDigest
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.zip.ZipEntry

class JarJar {
    static def extractJars(Project project, String dependency) {
        Configuration tempConfig = project.configurations.detachedConfiguration(
                project.dependencies.create(dependency)
        )

        def extractedJars = []

        tempConfig.resolvedConfiguration.resolvedArtifacts.each { ResolvedArtifact artifact ->
            def jarFile = artifact.file

            if (jarFile.exists() && jarFile.name.endsWith('.jar')) {
                extractedJars.addAll(extractNestedJars(project, jarFile))
                extractedJars.add(jarFile)
            }

        }

        if (extractedJars.isEmpty()) {
            return project.files(tempConfig.files)
        }

        return project.files(extractedJars)
    }

    private static List<File> extractNestedJars(Project project, File jarFile) {
        def extractedJars = []
        def fileMD5 = jarFile.withInputStream { input ->
            def digest = MessageDigest.getInstance("MD5")
            byte[] buffer = new byte[8192]
            int read
            while ((read = input.read(buffer)) > 0) {
                digest.update(buffer, 0, read)
            }
            return digest.digest().encodeHex().toString()
        }
        def extractDir = new File(project.buildDir, "jarjar-extracted/${jarFile.name}-${fileMD5}")

        try {
            new JarFile(jarFile).withCloseable { jar ->
                Enumeration<JarEntry> entries = jar.entries()
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement()
                    String entryName = entry.name

                    if ((entryName.startsWith("META-INF/jarjar/") || entryName.startsWith("META-INF/jars/"))
                            && entryName.endsWith('.jar') && !entry.isDirectory()) {

                        def targetFile = new File(extractDir, new File(entryName).name)
                        targetFile.parentFile.mkdirs()

                        targetFile.withOutputStream { output ->
                            jar.getInputStream(entry).withStream { input ->
                                output << input
                            }
                        }

                        extractedJars.add(targetFile)
                        project.logger.info("Extracted nested jar: ${entryName} -> ${targetFile}")
                    }
                }
            }
        } catch (Exception e) {
            project.logger.warn("Failed to extract nested jars from ${jarFile.name}: ${e.message}")
            throw new RuntimeException(e)
        }

        return extractedJars
    }
}
