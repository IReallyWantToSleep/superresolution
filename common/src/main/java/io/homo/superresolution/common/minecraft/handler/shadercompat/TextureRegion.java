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

package io.homo.superresolution.common.minecraft.handler.shadercompat;

import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.List;

public class TextureRegion {
    private final int x;
    private final int y;
    private final int w;
    private final int h;

    public TextureRegion(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public static TextureRegion fromList(List<Integer> list) {
        if (list == null || list.size() < 4) return new TextureRegion(0, 0, -1, -1);
        return new TextureRegion(list.get(0), list.get(1), list.get(2), list.get(3));
    }

    public int[] resolve(Vector2i renderSize, Vector2i screenSize) {
        return new int[]{
                resolveValue(this.x, renderSize.x, screenSize.x),
                resolveValue(this.y, renderSize.y, screenSize.y),
                resolveValue(this.w, renderSize.x, screenSize.x),
                resolveValue(this.h, renderSize.y, screenSize.y)
        };
    }

    private int resolveValue(int value, int renderSize, int screenSize) {
        if (value == -1) return renderSize;
        if (value == -2) return screenSize;
        return value;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getW() {
        return w;
    }

    public int getH() {
        return h;
    }
}