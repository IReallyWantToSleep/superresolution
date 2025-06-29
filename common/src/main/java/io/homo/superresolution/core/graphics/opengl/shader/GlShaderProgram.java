package io.homo.superresolution.core.graphics.opengl.shader;

import io.homo.superresolution.core.graphics.impl.IDebuggableObject;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.opengl.Gl;
import io.homo.superresolution.core.graphics.opengl.shader.uniform.GlShaderUniforms;
import io.homo.superresolution.core.graphics.shader.ShaderCompiler;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL45;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static io.homo.superresolution.common.SuperResolution.LOGGER;
import static io.homo.superresolution.core.graphics.opengl.Gl.glAttachShader;
import static io.homo.superresolution.core.graphics.opengl.Gl.glCreateProgram;
import static io.homo.superresolution.core.graphics.opengl.Gl.glLinkProgram;
import static io.homo.superresolution.core.graphics.opengl.Gl.glSafeObjectLabel;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL41.glShaderBinary;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.opengl.GL46.glSpecializeShader;

public class GlShaderProgram implements IShaderProgram<GlShaderUniforms>, IDebuggableObject {
    private final ShaderDescription description;
    private int handle;
    private boolean isCompiled = false;
    private GlShaderUniforms uniforms;

    public GlShaderProgram(ShaderDescription description) {
        this.description = description;
        this.handle = Gl.glCreateProgram();
    }

    @Override
    public int handle() {
        return handle;
    }

    @Override
    public String getDebugLabel() {
        return description.shaderName() + "-" + handle;
    }

    @Override
    public void updateDebugLabel(String newLabel) {
        glSafeObjectLabel(GL_PROGRAM, handle, newLabel);
    }

    protected void checkProgram() {
        if (GL20.glGetProgrami(handle, GL20.GL_LINK_STATUS) == GL20.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(handle);
            GL20.glDeleteProgram(handle);
            throw new RuntimeException("着色器程序链接状态不为GL_TRUE:\n" + log);
        }
    }

    protected GlShader compileSingleShader(ShaderSource source) {
        int glShaderType = switch (source.getType()) {
            case VERTEX -> GL45.GL_VERTEX_SHADER;
            case COMPUTE -> GL45.GL_COMPUTE_SHADER;
            case FRAGMENT -> GL45.GL_FRAGMENT_SHADER;
        };
        Objects.requireNonNull(source, "ShaderSource cannot be null");
        GlShader shader = new GlShader(source.getType());
        if (shader.id() == 0) {
            throw new RuntimeException("Failed to create shader object (Type: " + glShaderType + ")");
        }
        try {
            ShaderCompiler.ShaderBinary binary = ShaderCompiler.getOpenGLShaderBinary(this, source.getType());
            if (binary == null) {
                throw new RuntimeException("SPIR-V binary not found for " + source.getType());
            }

            glShaderBinary(new int[]{shader.id()}, binary.format(), binary.binary());
            glSpecializeShader(shader.id(), "main", null, (int[]) null);

            if (glGetShaderi(shader.id(), GL_COMPILE_STATUS) == GL_FALSE) {
                String infoLog = glGetShaderInfoLog(shader.id());
                String errorDetails = String.format(
                        "%s Shader SPIR-V加载失败\n类型: %s\n错误日志:\n%s",
                        source.getType(),
                        source.getType().name(),
                        infoLog
                );
                LOGGER.error(errorDetails);
                saveErrorArtifacts(source.getType(), source.getSource(), infoLog);
                throw new ShaderCompileException(errorDetails);
            }

            glSafeObjectLabel(GL_SHADER, shader.id(), "Shader_" + source.getType());
            return shader;
        } catch (Exception e) {
            shader.destroy();
            throw e;
        }
    }

    private void saveErrorArtifacts(ShaderType type, String sourceCode, String log) {
        String time = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        String baseName = String.format("errorArtifact_%s.%s", type.name(), time);
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

    private void validateShaderTypes() {
        Set<ShaderType> types = description.sourceMap().keySet();
        if (types.contains(ShaderType.VERTEX) || types.contains(ShaderType.FRAGMENT)) {
            if (!types.contains(ShaderType.VERTEX) || !types.contains(ShaderType.FRAGMENT)) {
                throw new IllegalStateException("通用着色器必须同时拥有VERTEX与FRAGMENT类型的ShaderSource");
            }
            if (types.stream().anyMatch(t -> t != ShaderType.VERTEX && t != ShaderType.FRAGMENT)) {
                throw new IllegalStateException("通用着色器仅支持VERTEX与FRAGMENT类型的ShaderSource");
            }
        } else {
            if (types.size() != 1 || !types.contains(ShaderType.COMPUTE)) {
                throw new IllegalStateException("计算着色器只需要一个着色器源码且类型必须为COMPUTE");
            }
        }

    }

    @Override
    public void compile() {
        EnumMap<ShaderType, ShaderSource> shaderSources = description.sourceMap();
        validateShaderTypes();
        if (!ShaderCompiler.checkOpenGLProgramBinary(this)) {
            ShaderCompiler.saveOpenGLProgramBinary(this);
        }
        List<GlShader> shaders = new ArrayList<>();
        try {
            shaderSources.forEach((type, source) -> {
                GlShader shader = compileSingleShader(source);
                shaders.add(shader);
            });
            this.handle = glCreateProgram();
            shaders.forEach(s -> glAttachShader(handle, s.id()));
            glLinkProgram(handle);
            checkProgram();
            updateDebugLabel(getDebugLabel());
            this.isCompiled = true;
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
        uniforms.destroy();
        Gl.glDeleteProgram(this.handle);
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
