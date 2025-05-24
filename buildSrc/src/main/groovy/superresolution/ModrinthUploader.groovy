package superresolution

import masecla.modrinth4j.client.HttpClient
import masecla.modrinth4j.client.agent.UserAgent
import masecla.modrinth4j.client.instances.UnlimitedHttpClient
import masecla.modrinth4j.endpoints.version.CreateVersion
import masecla.modrinth4j.main.ModrinthAPI
import masecla.modrinth4j.model.version.ProjectVersion

import java.util.concurrent.CompletableFuture

class ModrinthUploader {
    private static ModrinthAPI api

    static void init() {
        if (api == null) {
            String apiKey = System.getenv("MODRINTH_TOKEN")
            api = ModrinthAPI.rateLimited(
                    UserAgent.builder()
                            .build(),
                    apiKey
            )
        }
    }

    private static ProjectVersion.VersionType parseVersionType(String modVersion) {
        String lowerVersion = modVersion.toLowerCase()
        if (lowerVersion.contains("alpha")) {
            return ProjectVersion.VersionType.ALPHA
        } else if (lowerVersion.contains("beta")) {
            return ProjectVersion.VersionType.BETA
        }
        return ProjectVersion.VersionType.RELEASE
    }

    static void uploadFile(
            File file,
            String changelog
    ) {
        String fileName = file.name
        String loader
        String mcVersion
        String modVersion

        String cleaned = fileName.replace(".jar", "")
        String[] parts = cleaned.split("-")
        if (parts.length < 4) {
            throw new IllegalArgumentException("Invalid version string format: " + fileName)
        }
        loader = parts[1]
        mcVersion = parts[2]
        StringBuilder _modVersion = new StringBuilder()
        for (int i = 3; i < parts.length; i++) {
            if (i > 3) _modVersion.append("-")
            _modVersion.append(parts[i])
        }
        modVersion = _modVersion.toString()
        ProjectVersion.VersionType versionType = parseVersionType(modVersion)
        String loaderName = "Fabric"
        if (loader.trim().equals("neoforge")) loaderName = "NeoForge"
        if (loader.trim().equals("forge")) loaderName = "Forge"
        List<ProjectVersion.ProjectDependency> dependencies = [
                new ProjectVersion.ProjectDependency(
                        null,
                        "9s6osm5g",
                        null,
                        ProjectVersion.ProjectDependencyType.REQUIRED
                ),
                new ProjectVersion.ProjectDependency(
                        null,
                        "lhGA9TYQ",
                        null,
                        ProjectVersion.ProjectDependencyType.REQUIRED
                )
        ]

        CreateVersion.CreateVersionRequest createVersionRequest = CreateVersion.CreateVersionRequest.builder()
                .projectId("Hf3Qz2H3")
                .files(file)
                .dependencies(dependencies)
                .featured(false)
                .gameVersions(mcVersion == "1.21.1" ? ["1.21.1", "1.21"] : [mcVersion])
                .loaders([loader])
                .versionType(versionType)
                .versionNumber("$mcVersion-$modVersion-$loader")
                .name("Super Resolution $modVersion for $loaderName $mcVersion")
                .changelog(changelog.replaceAll("\r\n", "\n"))
                .build()
        System.out.println((String) "Super Resolution $modVersion for $loaderName $mcVersion")
        System.out.println((String) "File:${file.absolutePath}")
        System.out.println((String) "Changelog:${changelog}")
        api.versions().createProjectVersion(createVersionRequest).get()
        System.out.println((String) "File:${file.absolutePath} done")
    }

    static void upload() {

    }
}
