package io.homo.superresolution.core.gl.shader;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.core.utils.ShaderCache;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import static io.homo.superresolution.core.gl.Gl.*;
import static io.homo.superresolution.core.gl.GlConst.GL_COMPILE_STATUS;
import static io.homo.superresolution.core.gl.GlConst.GL_COMPUTE_SHADER;

public class GlComputeShaderProgram extends AbstractGlShaderProgram {
    public GlComputeShaderProgram() {
        super();
    }

    public static ComputeShaderProgramBuilder create() {
        return new ComputeShaderProgramBuilder();
    }

    public GlComputeShaderProgram compileShader() {
        if (compiled) return this;
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
        this.checkProgram();
        glDeleteShader(COMPUTE_SHADER);
        if (enableCache) {
            ShaderCache.saveProgramBinary(this);
        }
        compiled = true;
        updateDebugLabel(getDebugLabel());
        return this;
    }

    public static class ComputeShaderProgramBuilder extends AbstractShaderProgramBuilder<GlComputeShaderProgram> {
        @Override
        public GlComputeShaderProgram build() {
            return checkShaderCache() ? updateShader(new GlComputeShaderProgram().fromBin(getShaderCache())) : updateShader(new GlComputeShaderProgram());

        }
    }
}
