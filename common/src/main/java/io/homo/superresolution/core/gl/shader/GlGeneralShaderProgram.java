package io.homo.superresolution.core.gl.shader;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.core.gl.Gl;
import io.homo.superresolution.core.impl.shader.ShaderSource;
import io.homo.superresolution.core.utils.ShaderCache;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static io.homo.superresolution.core.gl.Gl.*;
import static io.homo.superresolution.core.gl.GlConst.*;

public class GlGeneralShaderProgram extends AbstractGlShaderProgram {
    protected GlGeneralShaderProgram() {
    }

    public static GeneralShaderProgramBuilder create() {
        return new GeneralShaderProgramBuilder();
    }

    @Override
    public GlGeneralShaderProgram compileShader() {
        if (compiled) return this;
        validateShaderTypes();
        if (!ShaderCache.checkProgramBinary(this)) {
            ShaderCache.saveProgramBinary(this);
        }
        List<Integer> shaders = new ArrayList<>();
        try {
            ShaderSource vertSource = shaderSources.get(ShaderSource.Type.VERTEX);
            int vertShader = compileSingleShader(vertSource, GL_VERTEX_SHADER);
            shaders.add(vertShader);
            ShaderSource fragSource = shaderSources.get(ShaderSource.Type.FRAGMENT);
            int fragShader = compileSingleShader(fragSource, GL_FRAGMENT_SHADER);
            shaders.add(fragShader);
            this.shaderProgram = glCreateProgram();
            shaders.forEach(s -> glAttachShader(shaderProgram, s));
            glLinkProgram(shaderProgram);
            checkProgram();
            compiled = true;
            updateDebugLabel(getDebugLabel());
            return this;
        } finally {
            shaders.forEach(Gl::glDeleteShader);
        }
    }

    private void validateShaderTypes() {
        Set<ShaderSource.Type> types = shaderSources.keySet();
        if (!types.contains(ShaderSource.Type.VERTEX) || !types.contains(ShaderSource.Type.FRAGMENT)) {
            throw new IllegalStateException("通用着色器必须同时拥有VERTEX与FRAGMENT类型的ShaderSource");
        }
        if (types.stream().anyMatch(t -> t != ShaderSource.Type.VERTEX && t != ShaderSource.Type.FRAGMENT)) {
            throw new IllegalStateException("通用着色器仅支持VERTEX与FRAGMENT类型的ShaderSource");
        }
    }

    public static class GeneralShaderProgramBuilder extends AbstractShaderProgramBuilder<GlGeneralShaderProgram> {
        @Override
        public GeneralShaderProgramBuilder addShaderSource(ShaderSource source) {
            if (source.getType() != ShaderSource.Type.VERTEX && source.getType() != ShaderSource.Type.FRAGMENT) {
                throw new IllegalArgumentException("通用着色器仅支持VERTEX与FRAGMENT类型的ShaderSource");
            }
            return (GeneralShaderProgramBuilder) super.addShaderSource(source);
        }

        @Override
        public GlGeneralShaderProgram build() {
            return updateShader(new GlGeneralShaderProgram());
        }
    }
}