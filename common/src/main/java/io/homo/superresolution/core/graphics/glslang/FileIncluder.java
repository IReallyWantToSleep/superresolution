/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
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
import io.homo.superresolution.api.platform.Platform;
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

        String source = loadSource(fullPath);
        if (source != null) {
            return source;
        }

        SuperResolution.LOGGER.debug("加载ShaderInclude失败 {}", fullPath);
        return """
                #error "include failed %s"
                """.formatted(fullPath);
    }

    public static String cppIncludeSystem(
            String headerName,
            String includerName,
            int inclusionDepth
    ) {
        String fullPath = SYSTEM_INCLUDE_BASE_PATH + headerName;

        String source = loadSource(fullPath);
        if (source != null) {
            return source;
        }

        return """
                #error "include failed %s"
                """.formatted(fullPath);
    }

    private static String loadSource(String path) {
        String source = null;

        if (Platform.currentPlatform.isDevelopmentEnvironment()) {
            try {
                Path gameDir = Minecraft.getInstance().gameDirectory.toPath().toAbsolutePath();
                Path commonResources = gameDir.getParent().getParent()
                        .resolve("common/src/main/resources");

                String relativePath = path.startsWith("/") ? path.substring(1) : path;
                Path includePath = commonResources.resolve(relativePath).toAbsolutePath();

                if (Files.exists(includePath)) {
                    source = Files.readString(includePath);
                    SuperResolution.LOGGER.debug("加载ShaderInclude (Dev): {}", includePath);
                }
            } catch (Throwable e) {
                SuperResolution.LOGGER.warn("开发环境ShaderInclude热加载失败: {}", e.getMessage());
                e.printStackTrace();
            }
        }

        if (source == null) {
            try {
                source = String.join("\n", FileReadHelper.readText(path));
            } catch (Exception e) {
            }
        }

        return source;
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