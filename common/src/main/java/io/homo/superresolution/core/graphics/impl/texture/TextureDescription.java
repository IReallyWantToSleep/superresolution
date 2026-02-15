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

import java.util.Objects;

public class TextureDescription {
    private int width;
    private int height;
    private TextureFormat format;
    private TextureType type;
    private TextureUsages usages = TextureUsages.create();
    private TextureFilterMode filterMode = TextureFilterMode.Nearest;
    private TextureWrapMode wrapMode = TextureWrapMode.ClampToEdge;
    private TextureMipmapSettings mipmapSettings = TextureMipmapSettings.disabled();

    private String label;


    private TextureDescription() {
    }

    public static TextureDescription.Builder create() {
        return new Builder();
    }

    public String getLabel() {
        return label;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public TextureFormat getFormat() {
        return format;
    }

    public TextureType getType() {
        return type;
    }

    public TextureUsages getUsages() {
        return usages;
    }

    public TextureFilterMode getFilterMode() {
        return filterMode;
    }

    public TextureWrapMode getWrapMode() {
        return wrapMode;
    }

    public TextureMipmapSettings getMipmapSettings() {
        return mipmapSettings;
    }

    @Override
    public String toString() {
        return "TextureDescription{" +
                "width=" + width +
                ", height=" + height +
                ", format=" + format +
                ", type=" + type +
                ", usages=" + usages +
                ", filterMode=" + filterMode +
                ", wrapMode=" + wrapMode +
                ", mipmap=" + mipmapSettings +
                '}';
    }

    public static class Builder {
        private final TextureDescription description;

        public Builder() {
            this.description = new TextureDescription();
        }

        public Builder width(int width) {
            if (width <= 0) {
                throw new IllegalArgumentException("Width must be positive");
            }
            description.width = width;
            return this;
        }

        public Builder height(int height) {
            if (height <= 0) {
                throw new IllegalArgumentException("Height must be positive");
            }
            description.height = height;
            return this;
        }

        public Builder size(int width, int height) {
            return width(width).height(height);
        }

        public Builder format(TextureFormat format) {
            description.format = Objects.requireNonNull(format, "TextureFormat cannot be null");
            return this;
        }

        public Builder type(TextureType type) {
            description.type = Objects.requireNonNull(type, "TextureType cannot be null");
            return this;
        }

        public Builder usages(TextureUsages usages) {
            if (usages == null || usages.isEmpty()) {
                throw new IllegalArgumentException("At least one usage must be specified");
            }
            description.usages = usages.copy();
            return this;
        }

        public Builder filterMode(TextureFilterMode filterMode) {
            description.filterMode = Objects.requireNonNull(filterMode, "FilterMode cannot be null");
            return this;
        }

        public Builder wrapMode(TextureWrapMode wrapMode) {
            description.wrapMode = Objects.requireNonNull(wrapMode, "WrapMode cannot be null");
            return this;
        }

        public Builder mipmapSettings(TextureMipmapSettings mipmapSettings) {
            description.mipmapSettings = mipmapSettings;
            return this;
        }

        public Builder mipmapsDisabled() {
            description.mipmapSettings = TextureMipmapSettings.disabled();
            return this;
        }

        public Builder mipmapsAuto() {
            description.mipmapSettings = TextureMipmapSettings.auto();
            return this;
        }


        public Builder label(String label) {
            description.label = label;
            return this;
        }

        public Builder mipmapsManual(int levels) {
            description.mipmapSettings = TextureMipmapSettings.manual(levels);
            return this;
        }

        public TextureDescription build() {
            if (description.usages.getUsages().contains(TextureUsage.AttachmentDepth) &&
                    !description.format.name().toUpperCase().startsWith("DEPTH")) {
                throw new IllegalStateException("Depth attachment requires a depth texture format");
            }
            return description;
        }
    }
}