package io.homo.superresolution.core.graphics.glslang;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.core.utils.FileReadHelper;
import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileIncluder {
    private static final String LOCAL_INCLUDE_BASE_PATH = "/shader/";
    private static final String SYSTEM_INCLUDE_BASE_PATH = "/shader/include/";

    public static String cppIncludeLocal(
            String headerName,
            String includerName,
            int inclusionDepth
    ) {
        String resolvedPath = resolveRelativePath(headerName, includerName);
        String fullPath = LOCAL_INCLUDE_BASE_PATH + resolvedPath;
        try {
            String includeSource = "";
            Path commonProjectPath = Path.of(Minecraft.getInstance().gameDirectory.getAbsolutePath())
                    .getParent()
                    .getParent()
                    .getParent()
                    .resolve("common")
                    .resolve("src")
                    .resolve("main")
                    .resolve("resources");
            Path includePath = Path.of(commonProjectPath.toAbsolutePath().toString(), fullPath);
            if (includePath.toFile().exists()) {
                try {
                    includeSource = Files.readString(includePath);
                    SuperResolution.LOGGER.info("加载ShaderInclude {}", includePath);

                } catch (Throwable e) {
                    includeSource = String.join("\n", FileReadHelper.readText(fullPath));
                }
            } else {
                includeSource = String.join("\n", FileReadHelper.readText(fullPath));
            }
            return includeSource;
        } catch (Exception e) {
            return "// ERROR: include failed: " + fullPath;
        }
    }

    public static String cppIncludeSystem(
            String headerName,
            String includerName,
            int inclusionDepth
    ) {
        String fullPath = SYSTEM_INCLUDE_BASE_PATH + headerName;
        try {
            String includeSource = "";
            Path commonProjectPath = Path.of(Minecraft.getInstance().gameDirectory.getAbsolutePath())
                    .getParent()
                    .getParent()
                    .getParent()
                    .resolve("common")
                    .resolve("src")
                    .resolve("main")
                    .resolve("resources");
            Path includePath = Path.of(commonProjectPath.toAbsolutePath().toString(), fullPath);
            if (includePath.toFile().exists()) {
                try {
                    includeSource = Files.readString(includePath);
                    SuperResolution.LOGGER.info("加载ShaderInclude {}", includePath);

                } catch (Throwable e) {
                    includeSource = String.join("\n", FileReadHelper.readText(fullPath));
                }
            } else {
                includeSource = String.join("\n", FileReadHelper.readText(fullPath));
            }
            return includeSource;
        } catch (Exception e) {
            return "// ERROR: include failed: " + fullPath;
        }
    }

    private static String resolveRelativePath(String headerName, String includerName) {
        if (headerName.startsWith("/") || headerName.contains("/")) {
            return headerName;
        }
        if (includerName == null || includerName.isEmpty()) {
            return headerName;
        }
        Path base = Paths.get(includerName).getParent();
        if (base == null) base = Paths.get("");
        Path resolved = base.resolve(headerName).normalize();
        String rel = resolved.toString().replace("\\", "/");
        return rel;
    }
}