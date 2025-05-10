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

public abstract class ShaderUniform<T> {
    protected final int uniformLocation;
    protected final ShaderUniforms shaderUniforms;

    protected ShaderUniform(int uniformLocation, ShaderUniforms shaderUniforms) {
        this.uniformLocation = uniformLocation;
        this.shaderUniforms = shaderUniforms;
    }

    public abstract ShaderUniforms value(T value);

    protected void set(int value) {
        glUniform1i(uniformLocation, value);
    }

    protected void set(float value) {
        glUniform1f(uniformLocation, value);
    }

    protected void set(float x, float y) {
        glUniform2f(uniformLocation, x, y);
    }

    protected void set(float x, float y, float z) {
        glUniform3f(uniformLocation, x, y, z);
    }

    protected void set(float x, float y, float z, float w) {
        glUniform4f(uniformLocation, x, y, z, w);
    }

    protected void set(Matrix4f matrix) {
        glUniformMatrix4fv(uniformLocation, false, matrix.get(new float[16]));
    }

    public static class Vec2 extends ShaderUniform<io.homo.superresolution.core.impl.Vec2> {
        public Vec2(int location, ShaderUniforms shaderUniforms) {
            super(location, shaderUniforms);
        }

        @Override
        public ShaderUniforms value(io.homo.superresolution.core.impl.Vec2 value) {
            set(value.x(), value.y());
            return shaderUniforms;
        }

        public ShaderUniforms value(float x, float y) {
            set(x, y);
            return shaderUniforms;
        }
    }

    public static class Vec3 extends ShaderUniform<io.homo.superresolution.core.impl.Vec3> {
        public Vec3(int location, ShaderUniforms shaderUniforms) {
            super(location, shaderUniforms);
        }

        @Override
        public ShaderUniforms value(io.homo.superresolution.core.impl.Vec3 value) {
            set(value.x(), value.y(), value.z());
            return shaderUniforms;

        }

        public ShaderUniforms value(float x, float y, float z) {
            set(x, y, z);
            return shaderUniforms;

        }
    }

    public static class Vec4 extends ShaderUniform<io.homo.superresolution.core.impl.Vec4> {
        public Vec4(int location, ShaderUniforms shaderUniforms) {
            super(location, shaderUniforms);
        }

        @Override
        public ShaderUniforms value(io.homo.superresolution.core.impl.Vec4 value) {
            set(value.x(), value.y(), value.z(), value.w());
            return shaderUniforms;

        }

        public ShaderUniforms value(float x, float y, float z, float w) {
            set(x, y, z, w);
            return shaderUniforms;

        }
    }

    public static class Float extends ShaderUniform<java.lang.Float> {
        public Float(int location, ShaderUniforms shaderUniforms) {
            super(location, shaderUniforms);
        }

        @Override
        public ShaderUniforms value(java.lang.Float value) {
            set(value);
            return shaderUniforms;

        }
    }

    public static class Int extends ShaderUniform<Integer> {
        public Int(int location, ShaderUniforms shaderUniforms) {
            super(location, shaderUniforms);
        }

        @Override
        public ShaderUniforms value(Integer value) {
            set(value);
            return shaderUniforms;

        }
    }

    public static class Bool extends ShaderUniform<Boolean> {
        public Bool(int location, ShaderUniforms shaderUniforms) {
            super(location, shaderUniforms);
        }

        @Override
        public ShaderUniforms value(Boolean value) {
            set(value ? 1 : 0);
            return shaderUniforms;

        }
    }

    public static class Matrix4 extends ShaderUniform<Matrix4f> {
        public Matrix4(int location, ShaderUniforms shaderUniforms) {
            super(location, shaderUniforms);
        }

        @Override
        public ShaderUniforms value(Matrix4f value) {
            set(value);
            return shaderUniforms;

        }
    }

    public static class Texture extends ShaderUniform<ITexture> {
        public Texture(int location, ShaderUniforms shaderUniforms) {
            super(location, shaderUniforms);
        }

        @Override
        public ShaderUniforms value(ITexture value) {
            return value(value, 0);
        }

        public ShaderUniforms value(int textureId) {
            return value(textureId, 0);
        }

        public ShaderUniforms value(int textureId, int texture) {
            glActiveTexture(GL_TEXTURE0 + texture);
            glBindTexture(GL_TEXTURE_2D, textureId);
            glUniform1i(uniformLocation, texture);
            return shaderUniforms;
        }

        public ShaderUniforms value(ITexture textureId, int texture) {
            return value(textureId.getTextureId(), texture);
        }
    }

    public static class Struct extends ShaderUniform<GlUniformBuffer<?>> {
        private final int bindingPoint;

        public Struct(int location, int bindingPoint, ShaderUniforms shaderUniforms) {
            super(location, shaderUniforms);
            this.bindingPoint = bindingPoint;
        }

        @Override
        public ShaderUniforms value(GlUniformBuffer<?> value) {
            value.bind(bindingPoint);
            return shaderUniforms;

        }
    }
}