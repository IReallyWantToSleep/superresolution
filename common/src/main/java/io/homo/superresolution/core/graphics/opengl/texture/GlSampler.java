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

import io.homo.superresolution.core.graphics.impl.GpuObject;
import io.homo.superresolution.core.impl.Destroyable;

import static io.homo.superresolution.core.graphics.opengl.Gl.DSA;
import static io.homo.superresolution.core.graphics.opengl.GlConst.*;

public class GlSampler implements GpuObject, Destroyable {
    private int handle;

    protected GlSampler(SamplerType type) {
        handle = DSA.createSampler();
        switch (type) {
            case LinearClamp -> {
                DSA.samplerParameteri(handle, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                DSA.samplerParameteri(handle, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                DSA.samplerParameteri(handle, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
                DSA.samplerParameteri(handle, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_NEAREST);
                DSA.samplerParameteri(handle, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            }
            case LinearRepeat -> {
                DSA.samplerParameteri(handle, GL_TEXTURE_WRAP_S, GL_REPEAT);
                DSA.samplerParameteri(handle, GL_TEXTURE_WRAP_T, GL_REPEAT);
                DSA.samplerParameteri(handle, GL_TEXTURE_WRAP_R, GL_REPEAT);
                DSA.samplerParameteri(handle, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
                DSA.samplerParameteri(handle, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            }
            case NearestClamp -> {
                DSA.samplerParameteri(handle, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                DSA.samplerParameteri(handle, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                DSA.samplerParameteri(handle, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
                DSA.samplerParameteri(handle, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
                DSA.samplerParameteri(handle, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            }
        }
    }

    public static GlSampler create(SamplerType type) {
        return new GlSampler(type);
    }

    @Override
    public String toString() {
        return "GlSampler{" +
                "handle=" + handle +
                '}';
    }

    @Override
    public long handle() {
        return handle;
    }

    @Override
    public void destroy() {
        if (handle > 0) DSA.deleteSampler(handle);
        handle = 0;
    }

    public enum SamplerType {
        NearestClamp, LinearRepeat, LinearClamp
    }
}
