package io.homo.superresolution.core.graphics.opengl;


import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;

public class OpenGLShaderFactory {
    public static GlShaderProgram createShader(ShaderDescription description) {
        GlShaderProgram program = new GlShaderProgram(description);
        return program;
    }
}
