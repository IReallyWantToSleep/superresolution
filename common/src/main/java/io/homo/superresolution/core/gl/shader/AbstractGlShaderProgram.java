package io.homo.superresolution.core.gl.shader;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.core.GraphicsCapabilities;
import io.homo.superresolution.core.gl.shader.uniform.GlShaderUniforms;
import io.homo.superresolution.core.impl.Destroyable;
import io.homo.superresolution.core.impl.IDebuggableObject;
import io.homo.superresolution.core.impl.shader.ShaderSource;
import io.homo.superresolution.core.utils.ShaderCache;
import org.lwjgl.opengl.GL20;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static io.homo.superresolution.common.SuperResolution.LOGGER;
import static io.homo.superresolution.core.gl.Gl.glSafeObjectLabel;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.opengl.GL46.glSpecializeShader;

public abstract class AbstractGlShaderProgram implements Destroyable, IDebuggableObject {
    private final Map<String, Integer> uniformLocationCache = new HashMap<>();
    public String shaderName;
    public int shaderProgram;
    public boolean compiled;
    protected Map<ShaderSource.Type, ShaderSource> shaderSources = new EnumMap<>(ShaderSource.Type.class);
    protected Map<String, String> shaderDefineList;

    protected AbstractGlShaderProgram() {
    }

    protected int compileSingleShader(ShaderSource source, int glShaderType) {
        Objects.requireNonNull(source, "ShaderSource cannot be null");
        validateShaderType(glShaderType);
        int shader = glCreateShader(glShaderType);
        if (shader == 0) {
            throw new RuntimeException("Failed to create shader object (Type: " + glShaderType + ")");
        }
        try {
            ShaderCache.ShaderBinary binary = ShaderCache.getShaderBinary(this, source.getType());
            if (binary == null) {
                throw new RuntimeException("SPIR-V binary not found for " + source.getType());
            }

            glShaderBinary(new int[]{shader}, binary.format(), binary.binary());
            glSpecializeShader(shader, "main", null, (int[]) null);

            if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
                String infoLog = glGetShaderInfoLog(shader);
                String errorDetails = String.format(
                        "%s Shader SPIR-V加载失败\n类型: %s\n错误日志:\n%s",
                        source.getType(),
                        getShaderTypeName(glShaderType),
                        infoLog
                );
                LOGGER.error(errorDetails);
                saveErrorArtifacts(source.getType(), "", infoLog);
                throw new ShaderCompileException(errorDetails);
            }

            glSafeObjectLabel(GL_SHADER, shader, "Shader_" + source.getType());
            return shader;
        } catch (Exception e) {
            glDeleteShader(shader);
            throw e;
        }
    }

    private void validateShaderType(int type) {
        final int[] VALID_TYPES = {
                GL_VERTEX_SHADER,
                GL_FRAGMENT_SHADER,
                GL_COMPUTE_SHADER,
        };

        if (!Arrays.stream(VALID_TYPES).anyMatch(t -> t == type)) {
            throw new IllegalArgumentException("无效的着色器类型: 0x" + Integer.toHexString(type));
        }
    }

    private String getShaderTypeName(int type) {
        return switch (type) {
            case GL_VERTEX_SHADER -> "Vertex";
            case GL_FRAGMENT_SHADER -> "Fragment";
            case GL_COMPUTE_SHADER -> "Compute";
            default -> "Unknown";
        };
    }

    private void saveErrorArtifacts(ShaderSource.Type type, String sourceCode, String log) {
        String baseName = String.format("ERROR_%s", type.name());
        Path sourcePath = Path.of(baseName + ".glsl");
        try {
            Files.write(sourcePath, sourceCode.getBytes(StandardCharsets.UTF_8));
            LOGGER.info("保存错误着色器源码至: {}", sourcePath);
        } catch (IOException e) {
            LOGGER.error("无法保存着色器源码文件: {}", e.getMessage());
        }

    }

    private void saveErrorShader(ShaderSource.Type type, String source) {
        try (PrintWriter out = new PrintWriter("ERROR_" + type.name() + ".glsl")) {
            out.println(source);
        } catch (IOException e) {
            LOGGER.error("保存编译错误的着色器时失败: {}", e.getMessage());
        }
    }

    public Map<String, String> getShaderDefineList() {
        return shaderDefineList;
    }

    public Map<ShaderSource.Type, ShaderSource> getShaderSources() {
        return shaderSources;
    }

    @Override
    public String getDebugLabel() {
        return shaderName + "-" + shaderProgram;
    }

    @Override
    public void updateDebugLabel(String newLabel) {
        glSafeObjectLabel(GL_PROGRAM, shaderProgram, newLabel);
    }

    protected void checkProgram() {
        if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == GL20.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(shaderProgram);
            GL20.glDeleteProgram(shaderProgram);
            throw new RuntimeException("着色器程序链接状态不为GL_TRUE:\n" + log);
        }
    }

    @Override
    public void destroy() {
        glDeleteProgram(shaderProgram);
        shaderSources.clear();
        shaderDefineList.clear();
    }

    public abstract AbstractGlShaderProgram compileShader();

    public void use() {
        glUseProgram(this.shaderProgram);
    }

    public void clear() {
        glUseProgram(0);
    }

    public int getUniformLocation(String name) {
        if (uniformLocationCache.containsKey(name)) return uniformLocationCache.get(name);
        int i = glGetUniformLocation(this.shaderProgram, name);
        uniformLocationCache.put(name, i);
        return i;
    }

    public GlShaderUniforms uniforms() {
        return new GlShaderUniforms(this);
    }

    public static class ShaderCompileException extends RuntimeException {
        public ShaderCompileException(String message) {
            super(message);
        }
    }
}
