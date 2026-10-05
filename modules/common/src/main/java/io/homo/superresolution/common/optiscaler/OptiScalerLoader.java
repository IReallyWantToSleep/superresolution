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

package io.homo.superresolution.common.optiscaler;

import io.homo.superresolution.api.platform.OperatingSystemType;
import io.homo.superresolution.api.platform.Platform;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public final class OptiScalerLoader {
    private static boolean loadAttempted;

    private OptiScalerLoader() {
    }

    public static synchronized void loadConfiguredDll() {
        if (loadAttempted) {
            return;
        }
        loadAttempted = true;

        if (!SuperResolutionConfig.isEnableOptiScaler()) {
            return;
        }
        if (Platform.currentPlatform.getOS().type != OperatingSystemType.WINDOWS) {
            SuperResolution.LOGGER.warn("OptiScaler DLL loading is only supported on Windows");
            return;
        }

        String configuredPath = SuperResolutionConfig.getOptiScalerDllPath();
        if (configuredPath == null || configuredPath.isBlank()) {
            SuperResolution.LOGGER.warn("OptiScaler DLL loading is enabled, but no DLL file is selected");
            return;
        }

        try {
            Path dllPath = Path.of(configuredPath).toAbsolutePath().normalize();
            if (!Files.isRegularFile(dllPath)) {
                SuperResolution.LOGGER.warn("Selected OptiScaler DLL file does not exist: {}", dllPath);
                return;
            }

            System.load(dllPath.toString());
            SuperResolution.LOGGER.info("Loaded OptiScaler DLL: {}", dllPath);
        } catch (InvalidPathException | SecurityException exception) {
            SuperResolution.LOGGER.error("Invalid OptiScaler DLL path: {}", configuredPath, exception);
        } catch (UnsatisfiedLinkError error) {
            SuperResolution.LOGGER.error("Failed to load OptiScaler DLL: {}", configuredPath, error);
        }
    }
}
