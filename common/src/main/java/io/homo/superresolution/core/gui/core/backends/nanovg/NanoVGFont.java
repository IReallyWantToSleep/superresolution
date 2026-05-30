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

package io.homo.superresolution.core.gui.core.backends.nanovg;

import io.homo.superresolution.core.gui.core.backends.interfaces.IFont;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;


public class NanoVGFont implements IFont {
    public int id = -1;
    public String name;
    public String path;

    public NanoVGFont(String name, String path) {
        this.name = name;
        this.path = path;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int nativeId() {
        return id;
    }

    public void load() {
        if (id != -1) {
            return;
        }

        try (InputStream fontStream = getClass().getResourceAsStream(path)) {

            if (fontStream == null) {
                throw new IllegalStateException("Font resource not found: " + path);
            }

            byte[] bytes = fontStream.readAllBytes();

            ByteBuffer fontBuffer = MemoryUtil.memAlloc(bytes.length);
            fontBuffer.put(bytes);
            fontBuffer.flip();
            id = NanoVGBackend.context.rawContext.createFontMem(name, fontBuffer, 0);

            if (id == -1) {
                MemoryUtil.memFree(fontBuffer);
                throw new RuntimeException("NanoVG createFontMem failed: " + name);
            }

            //TODO: 这里存在内存泄漏风险，后续需要在不需要字体时调用MemoryUtil.memFree(fontBuffer)释放内存
        } catch (Exception e) {
            throw new RuntimeException("字体加载失败: " + name, e);
        }
    }
}