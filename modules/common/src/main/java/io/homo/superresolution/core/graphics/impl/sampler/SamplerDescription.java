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

package io.homo.superresolution.core.graphics.impl.sampler;

import io.homo.superresolution.core.graphics.impl.pipeline.state.CompareOp;
import io.homo.superresolution.core.graphics.impl.texture.TextureFilterMode;
import io.homo.superresolution.core.graphics.impl.texture.TextureWrapMode;

import java.util.Objects;
import java.util.Optional;

import static org.lwjgl.opengl.GL41.*;

public class SamplerDescription {
    private TextureFilterMode minFilter = TextureFilterMode.Nearest;
    private TextureFilterMode magFilter = TextureFilterMode.Nearest;
    private SamplerMipmapMode mipmapMode = SamplerMipmapMode.None;
    private TextureWrapMode wrapMode = TextureWrapMode.ClampToEdge;
    private float lodBias = 0.0f;
    private float maxAnisotropy = 1.0f;
    private SamplerBorderColor borderColor = SamplerBorderColor.TransparentBlack;
    private CompareOp compareOp = null;

    private SamplerDescription() {
    }

    public static Builder create() {
        return new Builder();
    }

    public TextureFilterMode getMinFilter() {
        return minFilter;
    }

    public TextureFilterMode getMagFilter() {
        return magFilter;
    }

    public SamplerMipmapMode getMipmapMode() {
        return mipmapMode;
    }

    public TextureWrapMode getWrapMode() {
        return wrapMode;
    }

    public float getLodBias() {
        return lodBias;
    }

    public float getMaxAnisotropy() {
        return maxAnisotropy;
    }

    public SamplerBorderColor getBorderColor() {
        return borderColor;
    }

    public Optional<CompareOp> getCompareOp() {
        return Optional.ofNullable(compareOp);
    }

    public int glMinFilter() {
        if (mipmapMode == SamplerMipmapMode.None) {
            return minFilter.gl();
        }
        return switch (minFilter) {
            case Nearest -> switch (mipmapMode) {
                case Nearest -> GL_NEAREST_MIPMAP_NEAREST;
                case Linear -> GL_NEAREST_MIPMAP_LINEAR;
                default -> minFilter.gl();
            };
            case Linear -> switch (mipmapMode) {
                case Nearest -> GL_LINEAR_MIPMAP_NEAREST;
                case Linear -> GL_LINEAR_MIPMAP_LINEAR;
                default -> minFilter.gl();
            };
        };
    }

    public int glMagFilter() {
        return magFilter.gl();
    }

    @Override
    public String toString() {
        return "SamplerDescription{" +
                "minFilter=" + minFilter +
                ", magFilter=" + magFilter +
                ", mipmapMode=" + mipmapMode +
                ", wrapMode=" + wrapMode +
                ", lodBias=" + lodBias +
                ", maxAnisotropy=" + maxAnisotropy +
                ", borderColor=" + borderColor +
                ", compareOp=" + compareOp +
                '}';
    }

    public static class Builder {
        private final SamplerDescription description;

        public Builder() {
            this.description = new SamplerDescription();
        }

        public Builder minFilter(TextureFilterMode minFilter) {
            description.minFilter = Objects.requireNonNull(minFilter);
            return this;
        }

        public Builder magFilter(TextureFilterMode magFilter) {
            description.magFilter = Objects.requireNonNull(magFilter);
            return this;
        }

        public Builder filterMode(TextureFilterMode filterMode) {
            Objects.requireNonNull(filterMode);
            description.minFilter = filterMode;
            description.magFilter = filterMode;
            return this;
        }

        public Builder mipmapMode(SamplerMipmapMode mipmapMode) {
            description.mipmapMode = Objects.requireNonNull(mipmapMode);
            return this;
        }

        public Builder wrapMode(TextureWrapMode wrapMode) {
            description.wrapMode = Objects.requireNonNull(wrapMode);
            return this;
        }

        public Builder lodBias(float lodBias) {
            description.lodBias = lodBias;
            return this;
        }

        public Builder maxAnisotropy(float maxAnisotropy) {
            if (maxAnisotropy < 1.0f) {
                throw new IllegalArgumentException("maxAnisotropy must be >= 1.0");
            }
            description.maxAnisotropy = maxAnisotropy;
            return this;
        }

        public Builder borderColor(SamplerBorderColor borderColor) {
            description.borderColor = Objects.requireNonNull(borderColor);
            return this;
        }

        public Builder compareOp(CompareOp compareOp) {
            description.compareOp = compareOp;
            return this;
        }

        public SamplerDescription build() {
            return description;
        }
    }
}

