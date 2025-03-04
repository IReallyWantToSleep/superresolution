package io.homo.superresolution.common.render.gl.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.common.SuperResolution;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import static io.homo.superresolution.common.render.gl.Gl.*;
import static io.homo.superresolution.common.render.gl.GlConst.GL_COMPILE_STATUS;
import static io.homo.superresolution.common.render.gl.GlConst.GL_COMPUTE_SHADER;

public class GlComputeShaderProgram extends AbstractGlShaderProgram {
    public GlComputeShaderProgram() {
        super();
    }

    public static ComputeShaderProgramBuilder create() {
        return new ComputeShaderProgramBuilder();
    }

    public GlComputeShaderProgram compileShader() {
        RenderSystem.assertOnRenderThread();
        int COMPUTE_SHADER = glCreateShader(GL_COMPUTE_SHADER);
        String shaderCode = this.getFragShaderText();
        glShaderSource(COMPUTE_SHADER, shaderCode);
        glCompileShader(COMPUTE_SHADER);
        if (glGetShaderi(COMPUTE_SHADER, GL_COMPILE_STATUS) == 0) {
            try (PrintWriter out = new PrintWriter(new FileOutputStream("ERROR_FRAGMENT_SHADER_SRC.glsl"))) {
                out.println(this.getFragShaderText());
            } catch (IOException e) {
                SuperResolution.LOGGER.error(e.toString());
            }
            throw new RuntimeException("FRAGMENT_SHADER " + this.shaderName + " 无法编译着色器：" + glGetShaderInfoLog(COMPUTE_SHADER, 32768));
        }

        this.shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, COMPUTE_SHADER);
        glLinkProgram(shaderProgram);
        glDeleteShader(COMPUTE_SHADER);
        return this;
    }

    public static class ComputeShaderProgramBuilder extends AbstractShaderProgramBuilder {
        @Override
        public GlComputeShaderProgram build() {
            return (GlComputeShaderProgram) setShaderText(new GlComputeShaderProgram());
        }
    }
}
