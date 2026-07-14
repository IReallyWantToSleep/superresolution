package utils

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.regex.Pattern

class CurseForgeUploader {
    static final String DEFAULT_API_BASE_URL = "https://minecraft.curseforge.com"
    static final String DEFAULT_PROJECT_ID = "1461023"

    private static final Pattern MOD_VERSION_PATTERN = Pattern.compile(
            "^(.+)-(\\d+\\.\\d+\\.\\d+(?:-[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?)\$"
    )
    private static final Pattern ARTIFACT_PREFIX_PATTERN = Pattern.compile(
            "^super_resolution-(fabric|forge|neoforge)-(.+)\$"
    )
    private static final Pattern EXACT_GAME_VERSION_PATTERN = Pattern.compile(
            "^\\d+(?:\\.\\d+){1,2}\$"
    )

    static class VersionConfig {
        String artifactMinecraftVersion
        List<String> gameVersions
        Set<String> loaders
        File sourceFile
    }

    static class ArtifactInfo {
        File file
        String loader
        String loaderName
        String artifactMinecraftVersion
        String modVersion
        List<String> labels
        List<String> gameVersions
        String releaseType
        Map<String, Object> metadata
    }

    static class UploadPlan {
        List<ArtifactInfo> artifacts
        String modVersion
        File changelogFile
        String changelog
    }

    static String defaultProjectId() {
        return DEFAULT_PROJECT_ID
    }

    static UploadPlan createUploadPlan(
            File jarsDir,
            File configsDir,
            File changelogsDir
    ) {
        if (!jarsDir.isDirectory()) {
            throw new IllegalArgumentException("构建产物目录不存在: ${jarsDir.absolutePath}")
        }

        List<File> jarFiles = jarsDir.listFiles()
                ?.findAll { File file ->
                    file.isFile()
                            && file.name.startsWith("super_resolution-")
                            && file.name.endsWith(".jar")
                }
                ?.sort { File left, File right -> left.name <=> right.name }
                ?: []
        if (jarFiles.isEmpty()) {
            throw new IllegalArgumentException(
                    "没有在 ${jarsDir.absolutePath} 中找到 super_resolution-*.jar"
            )
        }

        Map<String, VersionConfig> versionConfigs = loadVersionConfigs(configsDir)
        List<ArtifactInfo> artifacts = jarFiles.collect { File file ->
            ArtifactInfo artifact = parseArtifactFileName(file)
            VersionConfig versionConfig = versionConfigs[artifact.artifactMinecraftVersion]
            if (versionConfig == null) {
                throw new IllegalArgumentException(
                        "找不到 JAR ${file.name} 对应的版本配置，"
                                + "需要 common.mod_artifact_minecraft_ver="
                                + artifact.artifactMinecraftVersion
                )
            }
            if (!versionConfig.loaders.contains(artifact.loader)) {
                throw new IllegalArgumentException(
                        "JAR ${file.name} 的加载器 ${artifact.loader} "
                                + "未包含在 ${versionConfig.sourceFile.name} 的 common.platforms 中"
                )
            }

            artifact.gameVersions = versionConfig.gameVersions
            artifact.releaseType = determineReleaseType(artifact.modVersion, artifact.labels)
            artifact
        }

        Set<String> modVersions = artifacts.collect { ArtifactInfo artifact ->
            artifact.modVersion
        } as Set<String>
        if (modVersions.size() != 1) {
            throw new IllegalArgumentException(
                    "待上传 JAR 包含多个模组版本: ${modVersions.sort().join(', ')}"
            )
        }

        String modVersion = modVersions.first()
        File changelogFile = new File(changelogsDir, "${modVersion}.md")
        String changelog = readAndValidateChangelog(changelogFile, modVersion)

        artifacts.each { ArtifactInfo artifact ->
            artifact.metadata = createMetadata(artifact, changelog)
        }

        return new UploadPlan(
                artifacts: artifacts,
                modVersion: modVersion,
                changelogFile: changelogFile,
                changelog: changelog
        )
    }

    static ArtifactInfo parseArtifactFileName(File file) {
        String fileName = file.name
        if (!fileName.endsWith(".jar")) {
            throw new IllegalArgumentException("不是 JAR 文件: ${fileName}")
        }

        String withoutExtension = fileName.substring(0, fileName.length() - ".jar".length())
        String baseName
        List<String> labels
        int labelsSeparator = withoutExtension.indexOf("+")
        if (labelsSeparator >= 0) {
            baseName = withoutExtension.substring(0, labelsSeparator)
            labels = withoutExtension.substring(labelsSeparator + 1)
                    .split("\\.")
                    .findAll { String label -> !label.isBlank() }
                    .toList()
        } else {
            baseName = withoutExtension
            labels = []
        }

        def versionMatcher = MOD_VERSION_PATTERN.matcher(baseName)
        if (!versionMatcher.matches()) {
            throw new IllegalArgumentException("无法从文件名解析模组版本: ${fileName}")
        }

        String artifactPrefix = versionMatcher.group(1)
        String modVersion = versionMatcher.group(2)
        def prefixMatcher = ARTIFACT_PREFIX_PATTERN.matcher(artifactPrefix)
        if (!prefixMatcher.matches()) {
            throw new IllegalArgumentException(
                    "文件名格式无效，应为 super_resolution-<loader>-<mc-version>-<mod-version>: "
                            + fileName
            )
        }

        String loader = prefixMatcher.group(1)
        return new ArtifactInfo(
                file: file,
                loader: loader,
                loaderName: loaderDisplayName(loader),
                artifactMinecraftVersion: prefixMatcher.group(2),
                modVersion: modVersion,
                labels: Collections.unmodifiableList(new ArrayList<>(labels))
        )
    }

    static String determineReleaseType(String modVersion, List<String> labels) {
        Set<String> preReleaseLabels = [
                "alpha",
                "beta",
                "pre",
                "rc",
                "dev",
                "snapshot",
                "nightly"
        ] as Set<String>
        boolean isDevelopmentBuild = labels.any { String label ->
            preReleaseLabels.contains(label.toLowerCase(Locale.ROOT))
        }
        return modVersion.contains("-") || isDevelopmentBuild ? "beta" : "release"
    }

    static Map<String, Object> createMetadata(ArtifactInfo artifact, String changelog) {
        List<String> gameVersionNames = new ArrayList<>(artifact.gameVersions)
        gameVersionNames.add(artifact.loaderName)

        Map<String, Object> metadata = new LinkedHashMap<>()
        metadata["changelog"] = changelog
        metadata["changelogType"] = "markdown"
        metadata["gameVersionNames"] = gameVersionNames
        metadata["releaseType"] = artifact.releaseType
        return metadata
    }

    static long uploadFile(
            ArtifactInfo artifact,
            String apiToken,
            String apiBaseUrl = DEFAULT_API_BASE_URL,
            String projectId = DEFAULT_PROJECT_ID
    ) {
        if (apiToken == null || apiToken.isBlank()) {
            throw new IllegalArgumentException("缺少环境变量 CURSEFORGE_API_TOKEN")
        }
        if (artifact.metadata == null) {
            throw new IllegalArgumentException("上传前必须先为 ${artifact.file.name} 生成 metadata")
        }

        String boundary = "----SuperResolutionCurseForge${UUID.randomUUID()}"
        String metadataJson = JsonOutput.toJson(artifact.metadata)
        String safeFileName = artifact.file.name
                .replace("\r", "")
                .replace("\n", "")
                .replace("\"", "%22")

        byte[] metadataPart = (
                "--${boundary}\r\n"
                        + "Content-Disposition: form-data; name=\"metadata\"\r\n"
                        + "Content-Type: application/json; charset=UTF-8\r\n"
                        + "\r\n"
                        + metadataJson
                        + "\r\n"
                        + "--${boundary}\r\n"
                        + "Content-Disposition: form-data; name=\"file\"; "
                        + "filename=\"${safeFileName}\"\r\n"
                        + "Content-Type: application/java-archive\r\n"
                        + "\r\n"
        ).getBytes(StandardCharsets.UTF_8)
        byte[] closingPart = "\r\n--${boundary}--\r\n".getBytes(StandardCharsets.UTF_8)

        HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.concat(
                HttpRequest.BodyPublishers.ofByteArray(metadataPart),
                HttpRequest.BodyPublishers.ofFile(artifact.file.toPath()),
                HttpRequest.BodyPublishers.ofByteArray(closingPart)
        )

        String normalizedBaseUrl = apiBaseUrl.replaceAll("/+\$", "")
        URI uploadUri = URI.create(
                "${normalizedBaseUrl}/api/projects/${projectId}/upload-file"
        )
        HttpRequest request = HttpRequest.newBuilder(uploadUri)
                .timeout(Duration.ofMinutes(30))
                .header("Accept", "application/json")
                .header("Content-Type", "multipart/form-data; boundary=${boundary}")
                .header("X-Api-Token", apiToken)
                .POST(body)
                .build()

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build()
        HttpResponse<String> response
        try {
            response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            )
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt()
            throw new IOException("上传 ${artifact.file.name} 时被中断", interrupted)
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(
                    "上传 ${artifact.file.name} 失败: HTTP ${response.statusCode()}\n"
                            + response.body()
            )
        }

        Object responseJson
        try {
            responseJson = new JsonSlurper().parseText(response.body())
        } catch (Exception parseError) {
            throw new IOException(
                    "CurseForge 返回了无法解析的响应: ${response.body()}",
                    parseError
            )
        }
        Object fileId = responseJson instanceof Map ? responseJson["id"] : null
        if (!(fileId instanceof Number)) {
            throw new IOException("CurseForge 响应中缺少文件 ID: ${response.body()}")
        }
        return ((Number) fileId).longValue()
    }

    private static Map<String, VersionConfig> loadVersionConfigs(File configsDir) {
        if (!configsDir.isDirectory()) {
            throw new IllegalArgumentException("版本配置目录不存在: ${configsDir.absolutePath}")
        }

        List<File> configFiles = configsDir.listFiles()
                ?.findAll { File file -> file.isFile() && file.name.endsWith(".json") }
                ?.sort { File left, File right -> left.name <=> right.name }
                ?: []
        if (configFiles.isEmpty()) {
            throw new IllegalArgumentException("版本配置目录中没有 JSON 文件: ${configsDir.absolutePath}")
        }

        Map<String, VersionConfig> configs = new LinkedHashMap<>()
        JsonSlurper jsonSlurper = new JsonSlurper()
        configFiles.each { File configFile ->
            Object parsed = jsonSlurper.parse(configFile)
            if (!(parsed instanceof Map)) {
                throw new IllegalArgumentException("版本配置不是 JSON 对象: ${configFile.name}")
            }

            Map common = parsed["common"] as Map
            String artifactMinecraftVersion = common?.get("mod_artifact_minecraft_ver")
                    ?.toString()
                    ?.trim()
            if (artifactMinecraftVersion == null || artifactMinecraftVersion.isBlank()) {
                return
            }

            Map fabric = common["fabric"] as Map
            Object rangeValue = fabric?.get("minecraft_version_range")
            if (!(rangeValue instanceof List) || ((List) rangeValue).isEmpty()) {
                throw new IllegalArgumentException(
                        "${configFile.name} 缺少 common.fabric.minecraft_version_range"
                )
            }

            List<String> gameVersions = ((List) rangeValue).collect { Object selector ->
                normalizeExactGameVersion(selector?.toString(), configFile)
            }.unique()
            gameVersions.sort { String left, String right ->
                compareGameVersions(left, right)
            }

            Object platformsValue = common["platforms"]
            if (!(platformsValue instanceof List) || ((List) platformsValue).isEmpty()) {
                throw new IllegalArgumentException(
                        "${configFile.name} 缺少 common.platforms"
                )
            }
            Set<String> loaders = ((List) platformsValue)
                    .collect { Object loader -> loader?.toString()?.trim()?.toLowerCase(Locale.ROOT) }
                    .findAll { String loader -> loader != null && !loader.isBlank() }
                    .toSet()

            VersionConfig newConfig = new VersionConfig(
                    artifactMinecraftVersion: artifactMinecraftVersion,
                    gameVersions: Collections.unmodifiableList(gameVersions),
                    loaders: Collections.unmodifiableSet(loaders),
                    sourceFile: configFile
            )
            VersionConfig existing = configs[artifactMinecraftVersion]
            if (existing != null) {
                throw new IllegalArgumentException(
                        "common.mod_artifact_minecraft_ver=${artifactMinecraftVersion} "
                                + "同时出现在 ${existing.sourceFile.name} 和 ${configFile.name}"
                )
            }
            configs[artifactMinecraftVersion] = newConfig
        }
        return configs
    }

    private static String normalizeExactGameVersion(String selector, File configFile) {
        if (selector == null) {
            throw new IllegalArgumentException(
                    "${configFile.name} 包含空的 Minecraft 版本选择器"
            )
        }

        String normalized = selector.trim()
        if (normalized.startsWith("~") || normalized.startsWith("=")) {
            normalized = normalized.substring(1).trim()
        }
        if (!EXACT_GAME_VERSION_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "${configFile.name} 包含无法映射到 CurseForge 的版本选择器: ${selector}"
            )
        }
        return normalized
    }

    private static int compareGameVersions(String left, String right) {
        List<Integer> leftParts = left.split("\\.").collect { String part -> part.toInteger() }
        List<Integer> rightParts = right.split("\\.").collect { String part -> part.toInteger() }
        int length = Math.max(leftParts.size(), rightParts.size())
        for (int i = 0; i < length; i++) {
            int leftPart = i < leftParts.size() ? leftParts[i] : -1
            int rightPart = i < rightParts.size() ? rightParts[i] : -1
            int comparison = leftPart <=> rightPart
            if (comparison != 0) {
                return comparison
            }
        }
        return 0
    }

    private static String readAndValidateChangelog(File changelogFile, String modVersion) {
        if (!changelogFile.isFile()) {
            throw new IllegalArgumentException(
                    "缺少与模组版本匹配的更新日志: ${changelogFile.absolutePath}"
            )
        }

        String changelog = changelogFile.getText(StandardCharsets.UTF_8.name())
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim()
        def headingMatcher = Pattern.compile("(?m)^#\\s+(.+?)\\s*\$").matcher(changelog)
        if (!headingMatcher.find()) {
            throw new IllegalArgumentException(
                    "更新日志 ${changelogFile.name} 缺少一级标题"
            )
        }
        String headingVersion = headingMatcher.group(1).trim()
        if (headingVersion != modVersion) {
            throw new IllegalArgumentException(
                    "更新日志 ${changelogFile.name} 的一级标题版本为 "
                            + "${headingVersion}，期望 ${modVersion}"
            )
        }
        return changelog
    }

    private static String loaderDisplayName(String loader) {
        switch (loader) {
            case "fabric":
                return "Fabric"
            case "forge":
                return "Forge"
            case "neoforge":
                return "NeoForge"
            default:
                throw new IllegalArgumentException("不支持的加载器: ${loader}")
        }
    }
}
