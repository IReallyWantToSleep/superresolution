/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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

package io.homo.superresolution.core.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class DirectoryEnsurer {
    private final Path path;

    public DirectoryEnsurer(Path path) {
        this.path = path;
    }

    public static DirectoryEnsurer wrapper(Path directory) {
        return new DirectoryEnsurer(directory);
    }

    public static DirectoryEnsurer wrapper(File directory) {
        return wrapper(directory.toPath());
    }

    public Path getPath() {
        ensureDirectory();
        return path;
    }

    public File getFile() {
        ensureDirectory();
        return path.toFile();
    }

    private void ensureDirectory() {
        File file = path.toFile();
        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw new RuntimeException();
            }
        }
    }
}
