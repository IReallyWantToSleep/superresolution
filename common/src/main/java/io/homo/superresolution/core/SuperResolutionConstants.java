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

package io.homo.superresolution.core;

import io.homo.superresolution.api.platform.Platform;
import io.homo.superresolution.core.utils.DirectoryEnsurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SuperResolutionConstants {
    public static DirectoryEnsurer DATA_DIR = DirectoryEnsurer.wrapper(Path.of(
            Platform.currentPlatform.getGameFolder().toString(),
            "config",
            "super_resolution"
    ));
    public static Path CONFIG_FILE = Path.of(
            DATA_DIR.getPath().toAbsolutePath().toString(),
            "config.toml"
    );
    public static DirectoryEnsurer NATIVE_LIBRARIES_DIR = DirectoryEnsurer.wrapper(Path.of(
            DATA_DIR.getPath().toAbsolutePath().toString(),
            "libraries"
    ));
    public static DirectoryEnsurer ERROR_DIR = DirectoryEnsurer.wrapper(Path.of(
            DATA_DIR.getPath().toAbsolutePath().toString(),
            "error_logs"
    ));
    public static DirectoryEnsurer DEBUG_DIR = DirectoryEnsurer.wrapper(Path.of(
            DATA_DIR.getPath().toAbsolutePath().toString(),
            "debug"
    ));
    public static DirectoryEnsurer SHADER_CACHE_DIR = DirectoryEnsurer.wrapper(Path.of(
            DATA_DIR.getPath().toAbsolutePath().toString(),
            "shader_caches"
    ));
}
