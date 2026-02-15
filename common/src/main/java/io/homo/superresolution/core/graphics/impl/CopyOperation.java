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

package io.homo.superresolution.core.graphics.impl;

import io.homo.superresolution.core.graphics.impl.texture.ITexture;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CopyOperation {

    private final List<ChannelMapping> mappings = new ArrayList<>();
    private ITexture srcTexture;
    private ITexture dstTexture;

    private CopyOperation() {
    }

    public static CopyOperation create() {
        return new CopyOperation();
    }

    public CopyOperation src(ITexture texture) {
        this.srcTexture = Objects.requireNonNull(texture);
        return this;
    }

    public CopyOperation dst(ITexture texture) {
        this.dstTexture = Objects.requireNonNull(texture);
        return this;
    }

    public CopyOperation fromTo(TextureChancel src, TextureChancel dst) {
        mappings.add(new ChannelMapping(src, dst));
        return this;
    }

    public ITexture getSrcTexture() {
        return srcTexture;
    }

    public ITexture getDstTexture() {
        return dstTexture;
    }

    public List<ChannelMapping> getMappings() {
        return List.copyOf(mappings);
    }

    public enum TextureChancel {
        R,
        G,
        B,
        A
    }

    public static class ChannelMapping {
        public final TextureChancel src;
        public final TextureChancel dst;

        public ChannelMapping(TextureChancel src, TextureChancel dst) {
            this.src = Objects.requireNonNull(src);
            this.dst = Objects.requireNonNull(dst);
        }
    }

}
