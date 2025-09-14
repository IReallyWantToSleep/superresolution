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

package io.homo.superresolution.core.graphics.opengl.texture;

import static io.homo.superresolution.core.graphics.opengl.Gl.DSA;
import static io.homo.superresolution.core.graphics.opengl.GlConst.*;

public class GlSampler {
    public final int id;

    protected GlSampler(SamplerType type) {
        id = DSA.createSampler();
        switch (type) {
            case LinearClamp -> {
                DSA.samplerParameteri(id, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                DSA.samplerParameteri(id, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                DSA.samplerParameteri(id, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
                DSA.samplerParameteri(id, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_NEAREST);
                DSA.samplerParameteri(id, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            }
            case LinearRepeat -> {
                DSA.samplerParameteri(id, GL_TEXTURE_WRAP_S, GL_REPEAT);
                DSA.samplerParameteri(id, GL_TEXTURE_WRAP_T, GL_REPEAT);
                DSA.samplerParameteri(id, GL_TEXTURE_WRAP_R, GL_REPEAT);
                DSA.samplerParameteri(id, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
                DSA.samplerParameteri(id, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            }
            case NearestClamp -> {
                DSA.samplerParameteri(id, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                DSA.samplerParameteri(id, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                DSA.samplerParameteri(id, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
                DSA.samplerParameteri(id, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
                DSA.samplerParameteri(id, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            }
        }
    }

    public static GlSampler create(SamplerType type) {
        return new GlSampler(type);
    }

    @Override
    public String toString() {
        return "GlSampler{" +
                "id=" + id +
                '}';
    }

    public enum SamplerType {
        NearestClamp, LinearRepeat, LinearClamp
    }
}
