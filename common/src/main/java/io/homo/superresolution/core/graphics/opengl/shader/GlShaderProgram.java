/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.core.graphics.opengl.shader;

import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.core.graphics.glslang.GlslangCompileShaderResult;
import io.homo.superresolution.core.graphics.glslang.GlslangShaderCompiler;
import io.homo.superresolution.core.graphics.glslang.enums.*;
import io.homo.superresolution.core.graphics.impl.IDebuggableObject;
import io.homo.superresolution.core.graphics.impl.shader.ShaderCompileException;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.opengl.Gl;
import io.homo.superresolution.core.graphics.opengl.shader.uniform.GlShaderUniforms;
import io.homo.superresolution.core.graphics.shader.ShaderCompiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

import static io.homo.superresolution.common.SuperResolution.LOGGER;
import static io.homo.superresolution.core.graphics.opengl.GlDebug.objectLabel;
import static org.lwjgl.opengl.GL46.*;

public class GlShaderProgram implements IShaderProgram<GlShaderUniforms>, IDebuggableObject {
    private final ShaderDescription description;
    private int handle;
    private boolean isCompiled = false;
    private GlShaderUniforms uniforms;

    public GlShaderProgram(ShaderDescription description) {
        this.description = description;
        this.handle = glCreateProgram();
    }

    @Override
    public long handle() {
        return handle;
    }

    @Override
    public String getDebugLabel() {
        return description.shaderName() + "-" + handle;
    }

    @Override
    public void updateDebugLabel(String newLabel) {
        objectLabel(GL_PROGRAM, handle, newLabel);
    }

    protected void checkProgram() {
        if (glGetProgrami(handle, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(handle);
            String errorDetails = String.format(
                    "着色器程序 '%s' 链接失败，暂时忽略\n错误日志:\n%s",
                    description.shaderName(),
                    log
            );
            LOGGER.error(errorDetails);

            saveLinkErrorArtifacts(log);
            /*
            glDeleteProgram(handle);
            handle = 0;

            throw new ShaderCompileException(errorDetails);
            */

        }
    }

    protected String preprocessShaderCode(String code) {
        List<String> codeLines = List.of(code.split("\n"));
        List<String> preprocessedCodeLines = new ArrayList<>();
        for (String line : codeLines) {
            if (line.trim().startsWith("#line")) continue;
            if (line.trim().startsWith("#extension") && line.contains("GL_GOOGLE_include_directive")) continue;
            preprocessedCodeLines.add(line);
        }
        return String.join("\n", preprocessedCodeLines);
    }

    protected GlShader compileSingleShader(ShaderSource source, boolean compat) {
        int glShaderType = switch (source.getType()) {
            case Vertex -> GL_VERTEX_SHADER;
            case Compute -> GL_COMPUTE_SHADER;
            case Fragment -> GL_FRAGMENT_SHADER;
        };
        Objects.requireNonNull(source, "ShaderSource cannot be null");
        GlShader shader = new GlShader(source.getType());
        if (shader.id() == 0) {
            throw new RuntimeException("Failed to create shader object (Type: " + glShaderType + ")");
        }
        try {
            String sourceCode = source.getSource();
            if (compat) {
                ShaderCompiler.LOGGER.info("使用兼容性着色器编译器编译着色器 {}", description.shaderName());
                GlslangCompileShaderResult result = GlslangShaderCompiler.compileShaderToSpirv(
                        source.getSource(),
                        switch (source.getType()) {
                            case Vertex -> EShLanguage.EShLangVertex;
                            case Fragment -> EShLanguage.EShLangFragment;
                            case Compute -> EShLanguage.EShLangCompute;
                        },
                        EShSource.EShSourceGlsl,
                        EShClient.EShClientOpenGL,
                        EShTargetClientVersion.EShTargetOpenGL_450,
                        EShTargetLanguage.EShTargetSpv,
                        EShTargetLanguageVersion.EShTargetSpv_1_4,
                        Gl.isLegacy() ? 410 : 460,
                        EProfile.ENoProfile,
                        true,
                        false
                );
                sourceCode = preprocessShaderCode(result.preprocessedCode());
                if (result.error() == GlslangCompileShaderError.PREPROCESS_ERROR) {
                    String errorDetails = String.format(
                            "%s Shader 预处理失败\n类型: %s\n错误日志:\n%s",
                            source.getType(),
                            result.error().name(),
                            result.log()
                    );
                    LOGGER.error(errorDetails);
                    saveErrorArtifacts(source.getType(), sourceCode, result.log());
                    throw new ShaderCompileException(errorDetails);
                }
                glShaderSource(shader.id(), sourceCode);
                glCompileShader(shader.id());
            } else {
                ShaderCompiler.ShaderBinary binary = ShaderCompiler.getOpenGLShaderBinary(this, source.getType());
                if (binary == null) {
                    throw new RuntimeException("SPIR-V binary not found for " + source.getType());
                }
                glShaderBinary(new int[]{shader.id()}, binary.format(), binary.binary());
                glSpecializeShader(shader.id(), "main", null, (int[]) null);
                binary.close();
            }

            if (glGetShaderi(shader.id(), GL_COMPILE_STATUS) == GL_FALSE) {
                String infoLog = glGetShaderInfoLog(shader.id());
                String errorDetails;
                if (SuperResolutionConfig.isEnableCompatShaderCompiler()) {
                    errorDetails = String.format(
                            "%s Shader 编译失败\n错误日志:\n%s",
                            source.getType().name(),
                            infoLog
                    );
                } else {
                    errorDetails = String.format(
                            "%s Shader SPIR-V加载失败\n错误日志:\n%s",
                            source.getType().name(),
                            infoLog
                    );
                }
                LOGGER.error(errorDetails);
                saveErrorArtifacts(source.getType(), sourceCode, infoLog);
                throw new ShaderCompileException(errorDetails);
            }

            objectLabel(GL_SHADER, shader.id(), "Shader_" + source.getType());
            return shader;
        } catch (Exception e) {
            shader.destroy();
            throw e;
        }
    }

    private void saveErrorArtifacts(ShaderType type, String sourceCode, String log) {
        String time = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String baseName = String.format("errorArtifact_%s_%s.%s", description.shaderName(), type.name(), time);
        Path sourcePath = Path.of(baseName + ".glsl");
        Path logPath = Path.of(baseName + ".log");
        try {
            Files.writeString(sourcePath, sourceCode);
            Files.writeString(logPath, log);
            LOGGER.info("保存错误着色器源码至: {}, 日志至: {}", sourcePath, logPath);
        } catch (IOException e) {
            LOGGER.error("无法保存着色器源码或日志文件: {}", e.getMessage());
        }
    }

    private void saveLinkErrorArtifacts(String log) {
        String time = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String baseName = String.format("linkError_%s.%s", description.shaderName(), time);
        Path logPath = Path.of(baseName + ".log");
        Path infoPath = Path.of(baseName + ".info");

        try {
            Files.writeString(logPath, log);

            StringBuilder info = new StringBuilder();
            info.append("着色器程序: ").append(description.shaderName()).append("\n");
            info.append("链接状态: GL_FALSE\n");
            info.append("\n附加的着色器信息:\n");

            int attachedShaders = glGetProgrami(handle, GL_ATTACHED_SHADERS);
            info.append("附加的着色器数量: ").append(attachedShaders).append("\n");
            description.sourceMap().forEach((type, source) -> {
                info.append("附加的着色器类型: ").append(type.name()).append("\n");
                info.append("附加的着色器代码: ").append(source.getSource()).append("\n");
            });

            Files.writeString(infoPath, info.toString());
            LOGGER.info("保存链接错误日志至: {}, 程序信息至: {}", logPath, infoPath);
        } catch (IOException e) {
            LOGGER.error("无法保存链接错误信息: {}", e.getMessage());
        }
    }

    private void validateShaderTypes() {
        Set<ShaderType> types = description.sourceMap().keySet();
        if (types.contains(ShaderType.Vertex) || types.contains(ShaderType.Fragment)) {
            if (!types.contains(ShaderType.Vertex) || !types.contains(ShaderType.Fragment)) {
                throw new IllegalStateException("通用着色器必须同时拥有VERTEX与FRAGMENT类型的ShaderSource");
            }
            if (types.stream().anyMatch(t -> t != ShaderType.Vertex && t != ShaderType.Fragment)) {
                throw new IllegalStateException("通用着色器仅支持VERTEX与FRAGMENT类型的ShaderSource");
            }
        } else {
            if (types.size() != 1 || !types.contains(ShaderType.Compute)) {
                throw new IllegalStateException("计算着色器只需要一个着色器源码且类型必须为COMPUTE");
            }
        }

    }

    @Override
    public void compile() {
        compile(SuperResolutionConfig.isEnableCompatShaderCompiler());
    }

    public void compile(boolean compat) {
        EnumMap<ShaderType, ShaderSource> shaderSources = description.sourceMap();
        validateShaderTypes();
        if (!(SuperResolutionConfig.isEnableCompatShaderCompiler() || compat)) {
            if (!ShaderCompiler.checkOpenGLProgramBinary(this)) {
                ShaderCompiler.saveOpenGLProgramBinary(this);
            }
        }
        List<GlShader> shaders = new ArrayList<>();
        try {
            shaderSources.forEach((type, source) -> {
                GlShader shader = compileSingleShader(
                        source,
                        SuperResolutionConfig.isEnableCompatShaderCompiler() || compat
                );
                shaders.add(shader);
            });
            if (this.handle != 0) {
                glDeleteProgram(this.handle);
            }
            this.handle = glCreateProgram();
            glFlush();
            shaders.forEach(s -> glAttachShader(handle, s.id()));
            glLinkProgram(handle);
            glFlush();
            checkProgram();

            updateDebugLabel(getDebugLabel());
            this.isCompiled = true;
        } catch (ShaderCompileException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("着色器程序 '{}' 编译过程中发生未预期的错误", description.shaderName(), e);
            e.printStackTrace();
            throw new ShaderCompileException("着色器程序编译失败: " + e.getMessage());
        } finally {
            shaders.forEach(GlShader::destroy);
        }
        this.uniforms = new GlShaderUniforms(this, this.getDescription());

    }

    @Override
    public boolean isCompiled() {
        return isCompiled;
    }

    @Override
    public void destroy() {
        if (uniforms != null) {
            uniforms.destroy();
        }
        if (handle != 0) {
            glDeleteProgram(this.handle);
            handle = 0;
        }
    }

    @Override
    public ShaderDescription getDescription() {
        return description;
    }

    @Override
    public GlShaderUniforms uniforms() {
        if (!isCompiled) throw new RuntimeException("着色器尚未编译");
        return uniforms;
    }
}