package io.homo.superresolution.core.gl.texture;

import io.homo.superresolution.core.gl.Gl;

import static io.homo.superresolution.core.gl.Gl.*;
import static io.homo.superresolution.core.gl.GlConst.*;

public class GlSampler {
    public final int id;

    protected GlSampler(SamplerType type) {
        id = glGenSamplers();
        switch (type) {
            case LinearClamp -> {
                glSamplerParameteri(id, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                glSamplerParameteri(id, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                glSamplerParameteri(id, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
                glSamplerParameteri(id, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_NEAREST);
                glSamplerParameteri(id, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            }
            case LinearRepeat -> {
                glSamplerParameteri(id, GL_TEXTURE_WRAP_S, GL_REPEAT);
                glSamplerParameteri(id, GL_TEXTURE_WRAP_T, GL_REPEAT);
                glSamplerParameteri(id, GL_TEXTURE_WRAP_R, GL_REPEAT);
                glSamplerParameteri(id, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
                glSamplerParameteri(id, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            }
            case NearestClamp -> {
                glSamplerParameteri(id, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                glSamplerParameteri(id, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                glSamplerParameteri(id, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
                glSamplerParameteri(id, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
                glSamplerParameteri(id, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
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
