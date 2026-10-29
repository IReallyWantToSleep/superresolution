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

public class TextureViewDescription {
    private final ITexture parent;
    private final int baseMipLevel;
    private final int mipLevelCount;

    private TextureViewDescription(ITexture parent, int baseMipLevel, int mipLevelCount) {
        this.parent = parent;
        this.baseMipLevel = baseMipLevel;
        this.mipLevelCount = mipLevelCount;
    }

    public static Builder create(ITexture parent) {
        return new Builder(parent);
    }

    public ITexture getParent() {
        return parent;
    }

    public int getBaseMipLevel() {
        return baseMipLevel;
    }

    public int getMipLevelCount() {
        return mipLevelCount;
    }

    public int getWidth() {
        return Math.max(1, parent.getWidth() >> baseMipLevel);
    }

    public int getHeight() {
        return Math.max(1, parent.getHeight() >> baseMipLevel);
    }

    @Override
    public String toString() {
        return "TextureViewDescription{" +
                "parent=" + parent.string() +
                ", baseMipLevel=" + baseMipLevel +
                ", mipLevelCount=" + mipLevelCount +
                ", width=" + getWidth() +
                ", height=" + getHeight() +
                '}';
    }

    public static class Builder {
        private final ITexture parent;
        private int baseMipLevel = 0;
        private int mipLevelCount = 1;

        private Builder(ITexture parent) {
            this.parent = Objects.requireNonNull(parent, "Parent texture cannot be null");
        }

        public Builder baseMipLevel(int baseMipLevel) {
            if (baseMipLevel < 0) {
                throw new IllegalArgumentException("baseMipLevel must be >= 0");
            }
            this.baseMipLevel = baseMipLevel;
            return this;
        }

        public Builder mipLevelCount(int mipLevelCount) {
            if (mipLevelCount < 1) {
                throw new IllegalArgumentException("mipLevelCount must be >= 1");
            }
            this.mipLevelCount = mipLevelCount;
            return this;
        }

        public TextureViewDescription build() {
            return new TextureViewDescription(parent, baseMipLevel, mipLevelCount);
        }
    }
}

