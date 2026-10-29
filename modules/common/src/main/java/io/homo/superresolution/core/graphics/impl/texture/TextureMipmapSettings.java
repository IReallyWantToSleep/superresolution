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

package io.homo.superresolution.core.graphics.impl.texture;

public class TextureMipmapSettings {
    private final boolean enabled;
    private final int levels;
    private final boolean autoGenerate;
    private float bias;

    private TextureMipmapSettings(boolean enabled, int levels, boolean autoGenerate, float bias) {
        this.enabled = enabled;
        this.levels = levels;
        this.autoGenerate = autoGenerate;
        this.bias = bias;
    }

    private TextureMipmapSettings(boolean enabled, int levels, boolean autoGenerate) {
        this(enabled, levels, autoGenerate, 0);
    }

    public static TextureMipmapSettings disabled() {
        return new TextureMipmapSettings(false, 1, false);
    }

    public static TextureMipmapSettings auto() {
        return new TextureMipmapSettings(true, -1, true);
    }

    public static TextureMipmapSettings manual(int levels) {
        if (levels < 1) {
            throw new IllegalArgumentException("Mipmap levels must be at least 1");
        }
        return new TextureMipmapSettings(true, levels, false);
    }

    public float getBias() {
        return bias;
    }

    public TextureMipmapSettings bias(float bias) {
        this.bias = bias;
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getLevels() {
        return enabled ? levels : 1;
    }

    public boolean isAutoGenerate() {
        return autoGenerate;
    }

    public int resolveLevels(int width, int height) {
        if (!enabled) {
            return 1;
        }
        if (levels > 0) {
            return levels;
        }

        int maxDim = Math.max(width, height);
        return (int) (Math.log(maxDim) / Math.log(2)) + 1;
    }

    @Override
    public String toString() {
        if (!enabled) {
            return "MipmapDisabled";
        }
        if (levels < 0) {
            return "MipmapAuto";
        }
        return "MipmapLevels=" + levels +
                ", Generate=" + (autoGenerate ? "Auto" : "Manual");
    }
}
