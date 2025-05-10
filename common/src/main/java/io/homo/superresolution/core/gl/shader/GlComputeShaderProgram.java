package io.homo.superresolution.core.gl.shader;

import io.homo.superresolution.core.impl.shader.ShaderSource;
import io.homo.superresolution.core.utils.ShaderCache;

import static io.homo.superresolution.core.gl.Gl.*;
import static io.homo.superresolution.core.gl.GlConst.GL_COMPUTE_SHADER;

public class GlComputeShaderProgram extends AbstractGlShaderProgram {
    protected GlComputeShaderProgram() {
    }

    public static ComputeShaderProgramBuilder create() {
        return new ComputeShaderProgramBuilder();
    }

    @Override
    public GlComputeShaderProgram compileShader() {
        if (compiled) return this;
        validateShaderType();
        if (!ShaderCache.checkProgramBinary(this)) {
            ShaderCache.saveProgramBinary(this);
        }
        int computeShader = -1;
        try {
            ShaderSource computeSource = shaderSources.get(ShaderSource.Type.COMPUTE);
            computeShader = compileSingleShader(computeSource, GL_COMPUTE_SHADER);

            this.shaderProgram = glCreateProgram();
            glAttachShader(shaderProgram, computeShader);
            glLinkProgram(shaderProgram);
            checkProgram();

            compiled = true;
            updateDebugLabel(getDebugLabel());
            return this;
        } finally {
            if (computeShader != -1) glDeleteShader(computeShader);
        }
    }

    private void validateShaderType() {
        if (shaderSources.size() != 1 || !shaderSources.containsKey(ShaderSource.Type.COMPUTE)) {
            throw new IllegalStateException("计算着色器只需要一个着色器源码且类型必须为COMPUTE");
        }
    }

    public static class ComputeShaderProgramBuilder extends AbstractShaderProgramBuilder<GlComputeShaderProgram> {
        @Override
        public ComputeShaderProgramBuilder addShaderSource(ShaderSource source) {
            if (source.getType() != ShaderSource.Type.COMPUTE) {
                throw new IllegalArgumentException("计算着色器仅支持COMPUTE类型的ShaderSource");
            }
            return (ComputeShaderProgramBuilder) super.addShaderSource(source);
        }

        @Override
        public GlComputeShaderProgram build() {
            return updateShader(new GlComputeShaderProgram());
        }
    }
}