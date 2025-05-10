package io.homo.superresolution.core.gl.shader.uniform;

import io.homo.superresolution.core.gl.shader.AbstractGlShaderProgram;
import io.homo.superresolution.core.gl.buffer.GlUniformBuffer;
import org.joml.Matrix4f;

public class ShaderUniforms {
    private final AbstractGlShaderProgram program;

    public ShaderUniforms(AbstractGlShaderProgram program) {
        this.program = program;
    }

    public ShaderUniform.Vec2 strictVec2(String name) {
        return new ShaderUniform.Vec2(getLocationOrThrow(name), this);
    }

    public ShaderUniform.Texture strictTexture(String name) {
        return new ShaderUniform.Texture(getLocationOrThrow(name), this);
    }

    public ShaderUniform.Vec3 strictVec3(String name) {
        return new ShaderUniform.Vec3(getLocationOrThrow(name), this);
    }

    public ShaderUniform.Vec4 strictVec4(String name) {
        return new ShaderUniform.Vec4(getLocationOrThrow(name), this);
    }

    public ShaderUniform.Float strictFloat(String name) {
        return new ShaderUniform.Float(getLocationOrThrow(name), this);
    }

    public ShaderUniform.Int strictInt(String name) {
        return new ShaderUniform.Int(getLocationOrThrow(name), this);
    }

    public ShaderUniform.Bool strictBool(String name) {
        return new ShaderUniform.Bool(getLocationOrThrow(name), this);
    }

    public ShaderUniform.Matrix4 strictMatrix4(String name) {
        return new ShaderUniform.Matrix4(getLocationOrThrow(name), this);
    }

    public ShaderUniform.Struct strictStruct(String name, int bindingPoint) {
        return new ShaderUniform.Struct(getLocationOrThrow(name), bindingPoint, this);
    }

    public ShaderUniform.Vec2 safeVec2(String name) {
        return createSafeUniform(name, ShaderUniform.Vec2::new);
    }

    public ShaderUniform.Vec3 safeVec3(String name) {
        return createSafeUniform(name, ShaderUniform.Vec3::new);
    }

    public ShaderUniform.Vec4 safeVec4(String name) {
        return createSafeUniform(name, ShaderUniform.Vec4::new);
    }

    public ShaderUniform.Float safeFloat(String name) {
        return createSafeUniform(name, ShaderUniform.Float::new);
    }

    public ShaderUniform.Texture safeTexture(String name) {
        return createSafeUniform(name, ShaderUniform.Texture::new);
    }

    public ShaderUniform.Int safeInt(String name) {
        return createSafeUniform(name, ShaderUniform.Int::new);
    }

    public ShaderUniform.Bool safeBool(String name) {
        return createSafeUniform(name, ShaderUniform.Bool::new);
    }

    public ShaderUniform.Matrix4 safeMatrix4(String name) {
        return createSafeUniform(name, ShaderUniform.Matrix4::new);
    }

    public ShaderUniform.Struct safeStruct(String name, int bindingPoint) {
        int loc = program.getUniformLocation(name);
        return loc >= 0 ? new ShaderUniform.Struct(loc, bindingPoint, this) : new NullStruct(this);
    }

    private int getLocationOrThrow(String name) {
        int loc = program.getUniformLocation(name);
        if (loc < 0) {
            throw new IllegalArgumentException("Uniform not found: " + name);
        }
        return loc;
    }

    private <T extends ShaderUniform<?>> T createSafeUniform(String name, UniformConstructor<T> constructor) {
        int loc = program.getUniformLocation(name);
        return loc >= 0 ? constructor.create(loc, this) : (T) new NullUniform<T>(this);
    }

    @FunctionalInterface
    private interface UniformConstructor<T> {
        T create(int location, ShaderUniforms shaderUniforms);
    }

    private static class NullUniform<T> extends ShaderUniform<T> {
        NullUniform(ShaderUniforms shaderUniforms) {
            super(-1, shaderUniforms);
        }

        @Override
        public ShaderUniforms value(T value) {
            return shaderUniforms;
        }
    }

    private static class NullStruct extends ShaderUniform.Struct {
        NullStruct(ShaderUniforms shaderUniforms) {
            super(-1, -1, shaderUniforms);
        }

        @Override
        public ShaderUniforms value(GlUniformBuffer<?> value) {
            return shaderUniforms;
        }
    }
}