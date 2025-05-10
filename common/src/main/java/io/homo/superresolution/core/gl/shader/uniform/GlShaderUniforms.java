package io.homo.superresolution.core.gl.shader.uniform;

import io.homo.superresolution.core.gl.shader.AbstractGlShaderProgram;

public class GlShaderUniforms {
    private final AbstractGlShaderProgram program;

    public GlShaderUniforms(AbstractGlShaderProgram program) {
        this.program = program;
    }

    public GlShaderUniform.Vec2 strictVec2(String name) {
        return new GlShaderUniform.Vec2(getLocationOrThrow(name), this);
    }

    public GlShaderUniform.Texture strictTexture(String name) {
        return new GlShaderUniform.Texture(getLocationOrThrow(name), this);
    }

    public GlShaderUniform.Vec3 strictVec3(String name) {
        return new GlShaderUniform.Vec3(getLocationOrThrow(name), this);
    }

    public GlShaderUniform.Vec4 strictVec4(String name) {
        return new GlShaderUniform.Vec4(getLocationOrThrow(name), this);
    }

    public GlShaderUniform.Float strictFloat(String name) {
        return new GlShaderUniform.Float(getLocationOrThrow(name), this);
    }

    public GlShaderUniform.Int strictInt(String name) {
        return new GlShaderUniform.Int(getLocationOrThrow(name), this);
    }

    public GlShaderUniform.Bool strictBool(String name) {
        return new GlShaderUniform.Bool(getLocationOrThrow(name), this);
    }

    public GlShaderUniform.Matrix4 strictMatrix4(String name) {
        return new GlShaderUniform.Matrix4(getLocationOrThrow(name), this);
    }

    public GlShaderUniform.Struct strictStruct(String name, int bindingPoint) {
        return new GlShaderUniform.Struct(getLocationOrThrow(name), bindingPoint, this);
    }

    public GlShaderUniform.Vec2 safeVec2(String name) {
        return (GlShaderUniform.Vec2) createSafeUniform(name, GlShaderUniform.Vec2::new);
    }

    public GlShaderUniform.Vec3 safeVec3(String name) {
        return (GlShaderUniform.Vec3) createSafeUniform(name, GlShaderUniform.Vec3::new);
    }

    public GlShaderUniform.Vec4 safeVec4(String name) {
        return (GlShaderUniform.Vec4) createSafeUniform(name, GlShaderUniform.Vec4::new);
    }

    public GlShaderUniform.Float safeFloat(String name) {
        return (GlShaderUniform.Float) createSafeUniform(name, GlShaderUniform.Float::new);
    }

    public GlShaderUniform.Texture safeTexture(String name) {
        return (GlShaderUniform.Texture) createSafeUniform(name, GlShaderUniform.Texture::new);
    }

    public GlShaderUniform.Int safeInt(String name) {
        return (GlShaderUniform.Int) createSafeUniform(name, GlShaderUniform.Int::new);
    }

    public GlShaderUniform.Bool safeBool(String name) {
        return (GlShaderUniform.Bool) createSafeUniform(name, GlShaderUniform.Bool::new);
    }

    public GlShaderUniform.Matrix4 safeMatrix4(String name) {
        return (GlShaderUniform.Matrix4) createSafeUniform(name, GlShaderUniform.Matrix4::new);
    }

    public GlShaderUniform.Struct safeStruct(String name, int bindingPoint) {
        int loc = program.getUniformLocation(name);
        return new GlShaderUniform.Struct(loc, bindingPoint, this);
    }

    private int getLocationOrThrow(String name) {
        int loc = program.getUniformLocation(name);
        if (loc < 0) {
            throw new IllegalArgumentException("Uniform not found: " + name);
        }
        return loc;
    }

    private <T extends GlShaderUniform<?>> GlShaderUniform<?> createSafeUniform(String name, UniformConstructor<T> constructor) {
        int loc = program.getUniformLocation(name);
        return constructor.create(loc, this);
    }

    @FunctionalInterface
    private interface UniformConstructor<T> {
        T create(int location, GlShaderUniforms shaderUniforms);
    }
}