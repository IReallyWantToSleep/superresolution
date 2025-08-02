package utils

import masecla.modrinth4j.client.agent.UserAgent
import masecla.modrinth4j.endpoints.version.CreateVersion
import masecla.modrinth4j.main.ModrinthAPI
import masecla.modrinth4j.model.version.ProjectVersion

class ModrinthUploader {
    private static ModrinthAPI api

    private static final List<String> ALL_VERSIONS = [
            '1.0', '1.1', '1.2.1', '1.2.2', '1.2.3', '1.2.4', '1.2.5',
            '1.3.1', '1.3.2', '1.4.2', '1.4.4', '1.4.5', '1.4.6', '1.4.7',
            '1.5.1', '1.5.2', '1.6.1', '1.6.2', '1.6.4', '1.7.2', '1.7.3',
            '1.7.4', '1.7.5', '1.7.6', '1.7.7', '1.7.8', '1.7.9', '1.7.10',
            '1.8', '1.8.1', '1.8.2', '1.8.3', '1.8.4', '1.8.5', '1.8.6', '1.8.7', '1.8.8', '1.8.9',
            '1.9', '1.9.1', '1.9.2', '1.9.3', '1.9.4', '1.10', '1.10.1', '1.10.2',
            '1.11', '1.11.1', '1.11.2', '1.12', '1.12.1', '1.12.2', '1.13', '1.13.1', '1.13.2',
            '1.14', '1.14.1', '1.14.2', '1.14.3', '1.14.4', '1.15', '1.15.1', '1.15.2',
            '1.16', '1.16.1', '1.16.2', '1.16.3', '1.16.4', '1.16.5', '1.17', '1.17.1',
            '1.18', '1.18.1', '1.18.2', '1.19', '1.19.1', '1.19.2', '1.19.3', '1.19.4',
            '1.20', '1.20.1', '1.20.2', '1.20.3', '1.20.4', '1.20.5', '1.20.6',
            '1.21', '1.21.1', '1.21.2', '1.21.3', '1.21.4', '1.21.5', '1.21.6', '1.21.7', '1.21.8'
    ]

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

    static List<String> parseVersionRange(String versionRange) {
        if (versionRange.contains("..")) {
            def parts = versionRange.split("\\Q..\\E")
            if (parts.length != 2) throw new IllegalArgumentException("版本范围格式错误: " + versionRange)
            def from = parts[0]
            def to = parts[1]
            int idxFrom = ALL_VERSIONS.indexOf(from)
            int idxTo = ALL_VERSIONS.indexOf(to)
            if (idxFrom == -1 || idxTo == -1) throw new IllegalArgumentException("未知版本: " + versionRange)
            if (idxFrom > idxTo) throw new IllegalArgumentException("版本范围顺序错误: " + versionRange)
            return ALL_VERSIONS.subList(idxFrom, idxTo + 1)
        } else {
            if (!ALL_VERSIONS.contains(versionRange)) throw new IllegalArgumentException("未知版本: " + versionRange)
            return [versionRange]
        }
    }

    static int versionToId(String version) {
        int idx = ALL_VERSIONS.indexOf(version)
        if (idx == -1) throw new IllegalArgumentException("未知版本: " + version)
        return idx + 1
    }

    static List<String> parseFileVersions(String fileName) {
        String cleaned = fileName.replace(".jar", "")
        String[] parts = cleaned.split("-")
        if (parts.length < 4) {
            throw new IllegalArgumentException("文件名格式无效: " + fileName)
        }
        String mcVersionPart = parts[2]
        return parseVersionRange(mcVersionPart)
    }

    static void uploadFile(
            File file,
            String changelog
    ) {
        String fileName = file.name
        String cleaned = fileName.replace(".jar", "")
        String[] parts = cleaned.split("-")
        if (parts.length < 4) {
            throw new IllegalArgumentException("文件名格式无效: " + fileName)
        }
        String loader = parts[1]
        String mcVersionPart = parts[2]
        StringBuilder _modVersion = new StringBuilder()
        for (int i = 3; i < parts.length; i++) {
            if (i > 3) _modVersion.append("-")
            _modVersion.append(parts[i])
        }
        String modVersion = _modVersion.toString()
        ProjectVersion.VersionType versionType = parseVersionType(modVersion)
        String loaderName = "Fabric"
        if (loader.trim().equalsIgnoreCase("neoforge")) loaderName = "NeoForge"
        if (loader.trim().equalsIgnoreCase("forge")) loaderName = "Forge"

        List<String> gameVersions = parseVersionRange(mcVersionPart)
        List<Integer> versionIds = gameVersions.collect { versionToId(it) }

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
        String versionRangeDisplay
        if (gameVersions.size() > 1) {
            versionRangeDisplay = gameVersions.first() + '~' + gameVersions.last()
        } else {
            versionRangeDisplay = gameVersions.first()
        }

        CreateVersion.CreateVersionRequest createVersionRequest = CreateVersion.CreateVersionRequest.builder()
                .projectId("Hf3Qz2H3")
                .files(file)
                .dependencies(dependencies)
                .featured(false)
                .gameVersions(gameVersions)
                .loaders([loader])
                .versionType(versionType)
                .versionNumber("${gameVersions.first()}-${modVersion}-${loader}")
                .name("Super Resolution $modVersion for $loaderName $versionRangeDisplay")
                .changelog(changelog.replaceAll("\r\n", "\n"))
                .build()
        System.out.println("Super Resolution $modVersion for $loaderName $gameVersions")
        System.out.println("File:${file.absolutePath}")
        System.out.println("Changelog:${changelog}")
        System.out.println("版本唯一ID: $versionIds")
        System.out.println("版本: $gameVersions")
        api.versions().createProjectVersion(createVersionRequest).get()
        System.out.println("File:${file.absolutePath} done")
    }
}