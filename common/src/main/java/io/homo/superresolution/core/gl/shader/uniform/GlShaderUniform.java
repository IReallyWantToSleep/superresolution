package io.homo.superresolution.core.gl.shader.uniform;

import io.homo.superresolution.core.gl.buffer.GlUniformBuffer;
import io.homo.superresolution.core.impl.texture.ITexture;
import org.joml.Matrix4f;

import static io.homo.superresolution.core.gl.Gl.glActiveTexture;
import static io.homo.superresolution.core.gl.Gl.glBindTexture;
import static io.homo.superresolution.core.gl.Gl.glUniform1i;
import static io.homo.superresolution.core.gl.GlConst.GL_TEXTURE0;
import static io.homo.superresolution.core.gl.GlConst.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL20.*;

public abstract class GlShaderUniform<T> {
    protected final int uniformLocation;
    protected final GlShaderUniforms shaderUniforms;

    protected GlShaderUniform(int uniformLocation, GlShaderUniforms shaderUniforms) {
        this.uniformLocation = uniformLocation;
        this.shaderUniforms = shaderUniforms;
    }

    public abstract GlShaderUniforms value(T value);

    protected void set(int value) {
        if (uniformLocation < 0) return;
        glUniform1i(uniformLocation, value);
    }

    protected void set(float value) {
        if (uniformLocation < 0) return;
        glUniform1f(uniformLocation, value);
    }

    protected void set(float x, float y) {
        if (uniformLocation < 0) return;
        glUniform2f(uniformLocation, x, y);
    }

    protected void set(float x, float y, float z) {
        if (uniformLocation < 0) return;
        glUniform3f(uniformLocation, x, y, z);
    }

    protected void set(float x, float y, float z, float w) {
        if (uniformLocation < 0) return;
        glUniform4f(uniformLocation, x, y, z, w);
    }

    protected void set(Matrix4f matrix) {
        if (uniformLocation < 0) return;
        glUniformMatrix4fv(uniformLocation, false, matrix.get(new float[16]));
    }

    public static class Vec2 extends GlShaderUniform<io.homo.superresolution.core.impl.Vec2> {
        public Vec2(int location, GlShaderUniforms shaderUniforms) {
            super(location, shaderUniforms);
        }

        @Override
        public GlShaderUniforms value(io.homo.superresolution.core.impl.Vec2 value) {
            set(value.x(), value.y());
            return shaderUniforms;
        }

        public GlShaderUniforms value(float x, float y) {
            set(x, y);
            return shaderUniforms;
        }
    }

    public static class Vec3 extends GlShaderUniform<io.homo.superresolution.core.impl.Vec3> {
        public Vec3(int location, GlShaderUniforms shaderUniforms) {
            super(location, shaderUniforms);
        }

        @Override
        public GlShaderUniforms value(io.homo.superresolution.core.impl.Vec3 value) {
            set(value.x(), value.y(), value.z());
            return shaderUniforms;

        }

        public GlShaderUniforms value(float x, float y, float z) {
            set(x, y, z);
            return shaderUniforms;

        }
    }

    public static class Vec4 extends GlShaderUniform<io.homo.superresolution.core.impl.Vec4> {
        public Vec4(int location, GlShaderUniforms shaderUniforms) {
            super(location, shaderUniforms);
        }

        @Override
        public GlShaderUniforms value(io.homo.superresolution.core.impl.Vec4 value) {
            set(value.x(), value.y(), value.z(), value.w());
            return shaderUniforms;

        }

        public GlShaderUniforms value(float x, float y, float z, float w) {
            set(x, y, z, w);
            return shaderUniforms;

        }
    }

    public static class Float extends GlShaderUniform<java.lang.Float> {
        public Float(int location, GlShaderUniforms shaderUniforms) {
            super(location, shaderUniforms);
        }

        @Override
        public GlShaderUniforms value(java.lang.Float value) {
            set(value);
            return shaderUniforms;

        }
    }

    public static class Int extends GlShaderUniform<Integer> {
        public Int(int location, GlShaderUniforms shaderUniforms) {
            super(location, shaderUniforms);
        }

        @Override
        public GlShaderUniforms value(Integer value) {
            set(value);
            return shaderUniforms;

        }
    }

    public static class Bool extends GlShaderUniform<Boolean> {
        public Bool(int location, GlShaderUniforms shaderUniforms) {
            super(location, shaderUniforms);
        }

        @Override
        public GlShaderUniforms value(Boolean value) {
            set(value ? 1 : 0);
            return shaderUniforms;

        }
    }

    public static class Matrix4 extends GlShaderUniform<Matrix4f> {
        public Matrix4(int location, GlShaderUniforms shaderUniforms) {
            super(location, shaderUniforms);
        }

        @Override
        public GlShaderUniforms value(Matrix4f value) {
            set(value);
            return shaderUniforms;

        }
    }

    public static class Texture extends GlShaderUniform<ITexture> {
        public Texture(int location, GlShaderUniforms shaderUniforms) {
            super(location, shaderUniforms);
        }

        @Override
        public GlShaderUniforms value(ITexture value) {
            return value(value, 0);
        }

        public GlShaderUniforms value(int textureId) {
            return value(textureId, 0);
        }

        public GlShaderUniforms value(int textureId, int texture) {
            glActiveTexture(GL_TEXTURE0 + texture);
            glBindTexture(GL_TEXTURE_2D, textureId);
            glUniform1i(uniformLocation, texture);
            return shaderUniforms;
        }

        public GlShaderUniforms value(ITexture textureId, int texture) {
            return value(textureId.getTextureId(), texture);
        }
    }

    public static class Struct extends GlShaderUniform<GlUniformBuffer<?>> {
        private final int bindingPoint;

        public Struct(int location, int bindingPoint, GlShaderUniforms shaderUniforms) {
            super(location, shaderUniforms);
            this.bindingPoint = bindingPoint;
        }

        @Override
        public GlShaderUniforms value(GlUniformBuffer<?> value) {
            if (uniformLocation < 0) return shaderUniforms;
            value.bind(bindingPoint);
            return shaderUniforms;

        }
    }
}