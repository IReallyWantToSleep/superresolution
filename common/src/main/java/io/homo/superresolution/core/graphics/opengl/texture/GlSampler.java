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
