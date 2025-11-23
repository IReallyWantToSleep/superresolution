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

package io.homo.superresolution.core.gui.core.backends.nanovg;

import io.homo.superresolution.core.gui.core.backends.interfaces.IFont;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;


public class NanoVGFont implements IFont {
    public int id = -1;
    public String name;
    public String path;

    public NanoVGFont(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public void load() {
        try {
            Path tempFile = Files.createTempFile("grapheneui-font-" + UUID.randomUUID(), ".ttf");
            InputStream fontStream = getClass().getResourceAsStream(path);
            if (fontStream == null) {
                throw new RuntimeException("字体路径不存在: " + path);
            }
            Files.copy(fontStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            fontStream.close();
            id = NanoVG.context.contextPtr.createFont(name, tempFile.toString());
            tempFile.toFile().delete();
        } catch (Exception e) {
            throw new RuntimeException("字体加载失败: " + name, e);
        }
    }
}