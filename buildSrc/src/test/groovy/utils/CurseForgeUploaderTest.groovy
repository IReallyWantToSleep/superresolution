package utils

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.Test

import java.nio.charset.StandardCharsets
import java.nio.file.Files

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertThrows
import static org.junit.Assert.assertTrue

class CurseForgeUploaderTest {
    @Test
    void createsPlanForCurrentArtifactLayouts() {
        File root = Files.createTempDirectory("curseforge-plan").toFile()
        File jarsDir = new File(root, "build_jars")
        File configsDir = new File(root, "configs")
        File changelogsDir = new File(root, "changelogs")
        jarsDir.mkdirs()
        configsDir.mkdirs()
        changelogsDir.mkdirs()

        writeConfig(
                configsDir,
                "1.20.1.json",
                "1.20.1",
                ["~1.20.1"],
                ["forge"]
        )
        writeConfig(
                configsDir,
                "1.21.1.json",
                "1.21..1.21.1",
                ["~1.21.1", "~1.21"],
                ["fabric", "neoforge"]
        )
        writeConfig(
                configsDir,
                "26.1.x.json",
                "26.1",
                ["~26.1.2", "~26.1.1", "~26.1"],
                ["fabric", "neoforge"]
        )
        writeConfig(
                configsDir,
                "26.2.json",
                "26.2",
                ["~26.2"],
                ["fabric", "neoforge"]
        )

        new File(
                jarsDir,
                "super_resolution-forge-1.20.1-0.8.3-alpha.5+opengl.jar"
        ).bytes = [1] as byte[]
        new File(
                jarsDir,
                "super_resolution-fabric-1.21..1.21.1-0.8.3-alpha.5+opengl.jar"
        ).bytes = [2] as byte[]
        new File(
                jarsDir,
                "super_resolution-neoforge-26.1-0.8.3-alpha.5+opengl.jar"
        ).bytes = [3] as byte[]
        new File(
                jarsDir,
                "super_resolution-fabric-26.2-0.8.3-alpha.5+opengl.jar"
        ).bytes = [4] as byte[]
        new File(changelogsDir, "0.8.3-alpha.5.md").setText(
                "# 0.8.3-alpha.5\n\nChanges",
                StandardCharsets.UTF_8.name()
        )

        CurseForgeUploader.UploadPlan plan = CurseForgeUploader.createUploadPlan(
                jarsDir,
                configsDir,
                changelogsDir
        )

        assertEquals("0.8.3-alpha.5", plan.modVersion)
        assertEquals(4, plan.artifacts.size())

        def byMinecraftVersion = plan.artifacts.collectEntries {
            [(it.artifactMinecraftVersion): it]
        }
        assertEquals(["1.20.1"], byMinecraftVersion["1.20.1"].gameVersions)
        assertEquals(["1.21", "1.21.1"], byMinecraftVersion["1.21..1.21.1"].gameVersions)
        assertEquals(["26.1", "26.1.1", "26.1.2"], byMinecraftVersion["26.1"].gameVersions)
        assertEquals(["26.2"], byMinecraftVersion["26.2"].gameVersions)
        assertTrue(plan.artifacts.every { it.releaseType == "beta" })

        plan.artifacts.each { artifact ->
            assertEquals("markdown", artifact.metadata["changelogType"])
            assertFalse(artifact.metadata.containsKey("relations"))
            assertTrue(
                    artifact.metadata["gameVersionNames"].contains(artifact.loaderName)
            )
        }
    }

    @Test
    void mapsStableAndDevelopmentVersions() {
        assertEquals(
                "release",
                CurseForgeUploader.determineReleaseType("1.0.0", ["opengl"])
        )
        assertEquals(
                "beta",
                CurseForgeUploader.determineReleaseType("1.0.0-alpha.1", ["opengl"])
        )
        assertEquals(
                "beta",
                CurseForgeUploader.determineReleaseType("1.0.0", ["dev", "opengl"])
        )
        assertEquals(
                "beta",
                CurseForgeUploader.determineReleaseType("1.0.0", ["beta", "opengl"])
        )
    }

    @Test
    void rejectsMissingOrMismatchedChangelog() {
        File root = Files.createTempDirectory("curseforge-changelog").toFile()
        File jarsDir = new File(root, "build_jars")
        File configsDir = new File(root, "configs")
        File changelogsDir = new File(root, "changelogs")
        jarsDir.mkdirs()
        configsDir.mkdirs()
        changelogsDir.mkdirs()
        writeConfig(configsDir, "26.2.json", "26.2", ["~26.2"], ["fabric"])
        new File(
                jarsDir,
                "super_resolution-fabric-26.2-1.0.0+opengl.jar"
        ).bytes = [1] as byte[]

        assertThrows(IllegalArgumentException) {
            CurseForgeUploader.createUploadPlan(jarsDir, configsDir, changelogsDir)
        }

        new File(changelogsDir, "1.0.0.md").text = "# 1.0.1\n\nWrong"
        IllegalArgumentException error = assertThrows(IllegalArgumentException) {
            CurseForgeUploader.createUploadPlan(jarsDir, configsDir, changelogsDir)
        }
        assertTrue(error.message.contains("期望 1.0.0"))
    }

    @Test
    void rejectsMixedModVersionsAndMissingConfig() {
        File root = Files.createTempDirectory("curseforge-validation").toFile()
        File jarsDir = new File(root, "build_jars")
        File configsDir = new File(root, "configs")
        File changelogsDir = new File(root, "changelogs")
        jarsDir.mkdirs()
        configsDir.mkdirs()
        changelogsDir.mkdirs()
        writeConfig(configsDir, "26.1.x.json", "26.1", ["~26.1"], ["fabric"])

        new File(
                jarsDir,
                "super_resolution-fabric-26.1-1.0.0+opengl.jar"
        ).bytes = [1] as byte[]
        new File(
                jarsDir,
                "super_resolution-fabric-26.1-1.0.1+opengl.jar"
        ).bytes = [2] as byte[]

        IllegalArgumentException mixedError = assertThrows(IllegalArgumentException) {
            CurseForgeUploader.createUploadPlan(jarsDir, configsDir, changelogsDir)
        }
        assertTrue(mixedError.message.contains("多个模组版本"))

        new File(
                jarsDir,
                "super_resolution-fabric-99.1-1.0.0+opengl.jar"
        ).bytes = [3] as byte[]
        IllegalArgumentException configError = assertThrows(IllegalArgumentException) {
            CurseForgeUploader.createUploadPlan(jarsDir, configsDir, changelogsDir)
        }
        assertTrue(configError.message.contains("找不到"))
    }

    @Test
    void uploadsMultipartRequestAndParsesFileId() {
        byte[] receivedBody = null
        String receivedToken = null
        String receivedContentType = null
        String receivedPath = null

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { HttpExchange exchange ->
            receivedPath = exchange.requestURI.path
            receivedToken = exchange.requestHeaders.getFirst("X-Api-Token")
            receivedContentType = exchange.requestHeaders.getFirst("Content-Type")
            receivedBody = exchange.requestBody.readAllBytes()

            byte[] response = '{"id":987654}'.getBytes(StandardCharsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, response.length)
            exchange.responseBody.withCloseable { it.write(response) }
        }
        server.start()

        try {
            File jar = Files.createTempFile("curseforge-upload", ".jar").toFile()
            jar.bytes = "jar-payload".getBytes(StandardCharsets.UTF_8)
            CurseForgeUploader.ArtifactInfo artifact = new CurseForgeUploader.ArtifactInfo(
                    file: jar,
                    metadata: [
                            changelog       : "# 1.0.0",
                            changelogType   : "markdown",
                            gameVersionNames: ["26.2", "Fabric"],
                            releaseType     : "release"
                    ]
            )

            long id = CurseForgeUploader.uploadFile(
                    artifact,
                    "test-token",
                    "http://127.0.0.1:${server.address.port}",
                    "1461023"
            )

            assertEquals(987654L, id)
            assertEquals("/api/projects/1461023/upload-file", receivedPath)
            assertEquals("test-token", receivedToken)
            assertTrue(receivedContentType.startsWith("multipart/form-data; boundary="))

            String bodyText = new String(receivedBody, StandardCharsets.ISO_8859_1)
            assertTrue(bodyText.contains('name="metadata"'))
            assertTrue(bodyText.contains('"changelogType":"markdown"'))
            assertTrue(bodyText.contains('name="file"'))
            assertTrue(bodyText.contains("jar-payload"))
            assertFalse(bodyText.contains('"relations"'))
        } finally {
            server.stop(0)
        }
    }

    @Test
    void rejectsMissingTokenBeforeNetworkRequest() {
        File jar = Files.createTempFile("curseforge-token", ".jar").toFile()
        CurseForgeUploader.ArtifactInfo artifact = new CurseForgeUploader.ArtifactInfo(
                file: jar,
                metadata: [:]
        )

        IllegalArgumentException error = assertThrows(IllegalArgumentException) {
            CurseForgeUploader.uploadFile(
                    artifact,
                    " ",
                    "http://127.0.0.1:1",
                    "1461023"
            )
        }
        assertTrue(error.message.contains("CURSEFORGE_API_TOKEN"))
    }

    private static void writeConfig(
            File configsDir,
            String fileName,
            String artifactMinecraftVersion,
            List<String> gameVersions,
            List<String> platforms
    ) {
        String versionsJson = gameVersions.collect { "\"${it}\"" }.join(",")
        String platformsJson = platforms.collect { "\"${it}\"" }.join(",")
        new File(configsDir, fileName).text = """
            {
              "common": {
                "mod_artifact_minecraft_ver": "${artifactMinecraftVersion}",
                "platforms": [${platformsJson}],
                "fabric": {
                  "minecraft_version_range": [${versionsJson}]
                }
              }
            }
        """.stripIndent()
    }
}
