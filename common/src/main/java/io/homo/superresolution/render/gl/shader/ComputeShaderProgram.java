package io.homo.superresolution.render.gl.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.SuperResolution;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import static io.homo.superresolution.render.gl.Gl.*;
import static io.homo.superresolution.render.gl.GlConst.GL_COMPILE_STATUS;
import static io.homo.superresolution.render.gl.GlConst.GL_COMPUTE_SHADER;

public  class ComputeShaderProgram extends AbstractShaderProgram {
    public ComputeShaderProgram(){super();}
    public static ComputeShaderProgramBuilder create() {
        return new ComputeShaderProgramBuilder();
    }
    public ComputeShaderProgram compileShader() {
        RenderSystem.assertOnRenderThread();
        int FRAGMENT_SHADER = glCreateShader(GL_COMPUTE_SHADER);
        glShaderSource(FRAGMENT_SHADER, this.getFragShaderText());
        glCompileShader(FRAGMENT_SHADER);
        if (glGetShaderi(FRAGMENT_SHADER, GL_COMPILE_STATUS) == 0) {
            try (PrintWriter out = new PrintWriter(new FileOutputStream("ERROR_FRAGMENT_SHADER_SRC.glsl"))) {
                out.println(this.getFragShaderText());
            } catch (IOException e) {
                SuperResolution.LOGGER.error(e.toString());
            }
            throw new RuntimeException("FRAGMENT_SHADER " + this.shaderName + " 无法编译着色器：" + glGetShaderInfoLog(FRAGMENT_SHADER, 32768));
        }

        this.shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, FRAGMENT_SHADER);
        glLinkProgram(shaderProgram);
        glDeleteShader(FRAGMENT_SHADER);
        return this;
    }
    public static class ComputeShaderProgramBuilder extends AbstractShaderProgramBuilder{
        @Override
        public ComputeShaderProgram build() {
            return (ComputeShaderProgram) setShaderText(new ComputeShaderProgram());
        }
    }
}
