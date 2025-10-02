/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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
                    SuperResolution.LOGGER.debug("加载ShaderInclude {}", includePath);

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
                    SuperResolution.LOGGER.debug("加载ShaderInclude {}", includePath);

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