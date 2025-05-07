package io.homo.superresolution.core.glslang;

import io.homo.superresolution.core.utils.FileReadHelper;

public class FileIncluder {
    private static final String LOCAL_INCLUDE_BASE_PATH = "/shader/include/";

    public static String cppIncludeLocal(
            String headerName,
            String includerName,
            int inclusionDepth
    ) {
        String resolvedPath = resolveRelativePath(headerName, includerName);
        String fullPath = LOCAL_INCLUDE_BASE_PATH + resolvedPath;
        try {
            return String.join("\n", FileReadHelper.readText(fullPath));

        } catch (Exception e) {
            System.err.println("Local include failed: " + fullPath + " | " + e.getMessage());
            return "";
        }
    }

    public static String cppIncludeSystem(
            String headerName,
            String includerName,
            int inclusionDepth
    ) {
        String fullPath = LOCAL_INCLUDE_BASE_PATH + headerName;
        try {
            return String.join("\n", FileReadHelper.readText(fullPath));
        } catch (Exception e) {
            System.err.println("System include failed: " + fullPath + " | " + e.getMessage());
            return "";
        }
    }

    private static String resolveRelativePath(String targetPath, String baseFilePath) {
        if (baseFilePath == null || baseFilePath.isEmpty()) {
            return targetPath;
        }
        String baseDir = baseFilePath.replaceFirst("/[^/]+$", "");
        if (baseDir.equals(baseFilePath)) {
            baseDir = "";
        }
        String combined = (baseDir + "/" + targetPath)
                .replaceAll("/+", "/")
                .replaceAll("/\\./", "/");
        while (combined.contains("../")) {
            combined = combined.replaceFirst("[^/]+/\\.\\./", "");
        }
        return combined;
    }

}