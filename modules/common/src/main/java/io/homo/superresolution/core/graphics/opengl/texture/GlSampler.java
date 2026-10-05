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

package io.homo.superresolution.core.graphics.opengl.texture;

import io.homo.superresolution.core.graphics.impl.sampler.ISampler;
import io.homo.superresolution.core.graphics.impl.sampler.SamplerDescription;
import io.homo.superresolution.core.graphics.impl.sampler.SamplerMipmapMode;
import io.homo.superresolution.core.graphics.impl.texture.TextureFilterMode;
import io.homo.superresolution.core.graphics.impl.texture.TextureWrapMode;

import static io.homo.superresolution.core.graphics.opengl.Gl.DSA;
import static org.lwjgl.opengl.GL41.*;

public class GlSampler implements ISampler {
    private final SamplerDescription description;
    private int handle;

    public GlSampler(SamplerDescription description) {
        this.description = description;
        handle = DSA.createSampler();

        int glWrap = description.getWrapMode().gl();
        DSA.samplerParameteri(handle, GL_TEXTURE_WRAP_S, glWrap);
        DSA.samplerParameteri(handle, GL_TEXTURE_WRAP_T, glWrap);
        DSA.samplerParameteri(handle, GL_TEXTURE_WRAP_R, glWrap);
        DSA.samplerParameteri(handle, GL_TEXTURE_MIN_FILTER, description.glMinFilter());
        DSA.samplerParameteri(handle, GL_TEXTURE_MAG_FILTER, description.glMagFilter());
        DSA.samplerParameterf(handle, GL_TEXTURE_LOD_BIAS, description.getLodBias());

        if (description.getMaxAnisotropy() > 1.0f) {
            DSA.samplerParameterf(handle, org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, description.getMaxAnisotropy());
        }

        if (description.getWrapMode() == TextureWrapMode.ClampToBorder) {
            float[] rgba = description.getBorderColor().glRGBA();
            DSA.samplerParameterfv(handle, GL_TEXTURE_BORDER_COLOR, rgba);
        }

        description.getCompareOp().ifPresent(op -> {
            DSA.samplerParameteri(handle, GL_TEXTURE_COMPARE_MODE, org.lwjgl.opengl.GL30.GL_COMPARE_REF_TO_TEXTURE);
            DSA.samplerParameteri(handle, GL_TEXTURE_COMPARE_FUNC, op.gl());
        });
    }

    @Deprecated
    protected GlSampler(SamplerType type) {
        this(type.toDescription());
    }

    @Deprecated
    public static GlSampler create(SamplerType type) {
        return new GlSampler(type);
    }

    @Override
    public SamplerDescription getSamplerDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "GlSampler{" +
                "handle=" + handle +
                ", description=" + description +
                '}';
    }

    @Override
    public long handle() {
        return handle;
    }

    @Override
    public void destroy() {
        if (handle > 0) {
            DSA.deleteSampler(handle);
        }
        handle = 0;
    }

    @Deprecated
    public enum SamplerType {
        NearestClamp,
        LinearRepeat,
        LinearClamp;

        public SamplerDescription toDescription() {
            return switch (this) {
                case NearestClamp -> SamplerDescription.create()
                        .filterMode(TextureFilterMode.Nearest)
                        .mipmapMode(SamplerMipmapMode.Nearest)
                        .wrapMode(TextureWrapMode.ClampToEdge)
                        .build();
                case LinearRepeat -> SamplerDescription.create()
                        .filterMode(TextureFilterMode.Linear)
                        .mipmapMode(SamplerMipmapMode.Linear)
                        .wrapMode(TextureWrapMode.Repeat)
                        .build();
                case LinearClamp -> SamplerDescription.create()
                        .filterMode(TextureFilterMode.Linear)
                        .mipmapMode(SamplerMipmapMode.Nearest)
                        .wrapMode(TextureWrapMode.ClampToEdge)
                        .build();
            };
        }
    }
}
