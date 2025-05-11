package io.homo.superresolution.core.glslang;

import io.homo.superresolution.core.utils.FileReadHelper;

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
        System.err.printf("[INCLUDE-LOCAL] header: %s, includer: %s, depth: %d, resolved: %s%n",
                headerName, includerName, inclusionDepth, fullPath);
        try {
            return String.join("\n", FileReadHelper.readText(fullPath));
        } catch (Exception e) {
            System.err.println("Local include failed: " + fullPath + " | " + e.getMessage());
            return "// ERROR: include failed: " + fullPath;
        }
    }

    public static String cppIncludeSystem(
            String headerName,
            String includerName,
            int inclusionDepth
    ) {
        String fullPath = SYSTEM_INCLUDE_BASE_PATH + headerName;
        System.err.printf("[INCLUDE-SYSTEM] header: %s, includer: %s, depth: %d, resolved: %s%n",
                headerName, includerName, inclusionDepth, fullPath);
        try {
            return String.join("\n", FileReadHelper.readText(fullPath));
        } catch (Exception e) {
            System.err.println("System include failed: " + fullPath + " | " + e.getMessage());
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