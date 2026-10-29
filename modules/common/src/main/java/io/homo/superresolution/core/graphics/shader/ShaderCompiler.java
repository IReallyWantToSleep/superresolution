/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
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

package io.homo.superresolution.core.graphics.shader;

import io.homo.superresolution.api.platform.Platform;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.core.SuperResolutionConstants;
import io.homo.superresolution.core.SuperResolutionNative;
import io.homo.superresolution.core.graphics.glslang.GlslangCompileShaderResult;
import io.homo.superresolution.core.graphics.glslang.GlslangShaderCompiler;
import io.homo.superresolution.core.graphics.glslang.enums.*;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderCompileException;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.opengl.Gl;
import io.homo.superresolution.core.utils.Md5CaculateUtil;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.ARBGLSPIRV.GL_SHADER_BINARY_FORMAT_SPIR_V_ARB;

public class ShaderCompiler {
    public static final Logger LOGGER = LoggerFactory.getLogger("SuperResolution/ShaderCompiler");

    static {
        createCacheDir();
    }

    // ========= Vulkan =========
    public static boolean saveVulkanProgramBinary(IShaderProgram program) {
        return saveProgramBinaryWithApi(program, "vk");
    }

    public static boolean checkVulkanProgramBinary(IShaderProgram program) {
        return checkProgramBinaryWithApi(program, "vk");
    }

    public static ShaderBinary getVulkanShaderBinary(IShaderProgram program, ShaderType type) {
        return getShaderBinaryWithApi(program, type, "vk");
    }

    // ========= OpenGL =========
    public static boolean saveOpenGLProgramBinary(IShaderProgram program) {
        return saveProgramBinaryWithApi(program, "ogl");
    }

    public static boolean checkOpenGLProgramBinary(IShaderProgram program) {
        return checkProgramBinaryWithApi(program, "ogl");
    }

    public static ShaderBinary getOpenGLShaderBinary(IShaderProgram program, ShaderType type) {
        return getShaderBinaryWithApi(program, type, "ogl");
    }

    private static boolean saveProgramBinaryWithApi(IShaderProgram program, String apiTag) {
        createCacheDir();

        String hash = getShaderProgramMd5(program, apiTag);
        GlslangCompileShaderResult currentSourceResult = null;
        try {
            for (Map.Entry<ShaderType, ShaderSource> entry : program.getDescription().sourceMap().entrySet()) {
                ShaderType type = entry.getKey();
                ShaderSource source = entry.getValue();
                Path path = SuperResolutionConstants.SHADER_CACHE_DIR.getPath().resolve(program.getDescription().shaderName() + "." + hash + "." + type.name().toLowerCase() + "." + apiTag + ".spv");

                EShClient client = isVulkan(apiTag) ?
                        EShClient.EShClientVulkan : EShClient.EShClientOpenGL;
                EShTargetClientVersion clientVersion = isVulkan(apiTag) ?
                        EShTargetClientVersion.EShTargetVulkan_1_2 : EShTargetClientVersion.EShTargetOpenGL_450;

                currentSourceResult = compileShaderToSpirv(
                        source.getSource(),
                        mapToGlslangType(type),
                        client,
                        clientVersion
                );

                LOGGER.debug("Starting SPIR-V compilation: type={}, API={}, cache path={}", type.name(), apiTag, path);

                if (currentSourceResult.error() != GlslangCompileShaderError.OK) {
                    LOGGER.error("Shader compilation failed [{}], error type={}, log={}", type.name(), currentSourceResult.error().name(), currentSourceResult.log());
                    throw new ShaderCompileException(currentSourceResult.log());
                }

                ByteBuffer buffer = currentSourceResult.spirvBuffer();
                long size = currentSourceResult.spirVDataSize();

                if (buffer == null || size <= 0) {
                    LOGGER.error("SPIR-V buffer is empty or has an invalid size, type={}, size={}", type.name(), size);
                    throw new IOException("SPIR-V buffer is empty or has an invalid size");
                }

                LOGGER.debug("Saving SPIR-V: size={} bytes, path={}", size, path);

                if (SuperResolutionConfig.isDebugDumpShader()) {
                    try {
                        Path srcPath = Path.of(SuperResolutionConstants.DEBUG_DIR.getPath().toAbsolutePath().toString(),
                                program.getDescription().shaderName() + "." + type.name().toLowerCase() + "." + apiTag + ".source.glsl");
                        Path prePath = Path.of(SuperResolutionConstants.DEBUG_DIR.getPath().toAbsolutePath().toString(),
                                program.getDescription().shaderName() + "." + type.name().toLowerCase() + "." + apiTag + ".preprocessed.glsl");
                        LOGGER.debug("Writing GLSL source debug files: {}, {}", srcPath, prePath);
                        Files.writeString(srcPath, currentSourceResult.sourceCode());
                        Files.writeString(prePath, currentSourceResult.preprocessedCode());
                    } catch (IOException e0) {
                        LOGGER.error("Unable to save shader source file: {}", e0.getMessage());
                    }
                }

                try {
                    byte[] outBytes = new byte[(int) size];
                    buffer.position(0);
                    buffer.get(outBytes);
                    Files.write(path, outBytes);
                    LOGGER.debug("Saved SPIR-V: {}", path);
                } catch (IOException e) {
                    LOGGER.error("Failed to save SPIR-V", e);
                }

                SuperResolutionNative.freeDirectBuffer(buffer);
                LOGGER.debug("DirectBuffer released");
            }
            return true;
        } catch (ShaderCompileException | IOException e) {
            try {
                if (currentSourceResult != null) {
                    LOGGER.debug("Shader compilation error type: {}", currentSourceResult.error().name());
                    LOGGER.debug("Compilation log: {}", currentSourceResult.log());

                    Path errorSourcePath = Path.of(SuperResolutionConstants.ERROR_DIR.toString(),
                            program.getDescription().shaderName() + ".error." + apiTag + ".source.glsl");
                    Path errorPrePath = Path.of(SuperResolutionConstants.ERROR_DIR.toString(),
                            program.getDescription().shaderName() + ".error." + apiTag + ".preprocessed.glsl");
                    Path errorLogPath = Path.of(SuperResolutionConstants.ERROR_DIR.toString(),
                            program.getDescription().shaderName() + ".error." + apiTag + ".log");

                    Files.writeString(errorSourcePath, currentSourceResult.sourceCode());
                    Files.writeString(errorPrePath, currentSourceResult.preprocessedCode());
                    Files.writeString(errorLogPath, currentSourceResult.log());
                    LOGGER.info("Saved failed shader source to: {}, {}, {}", errorSourcePath, errorPrePath, errorLogPath);
                }
            } catch (IOException e0) {
                LOGGER.error("Unable to save shader source file: {}", e0.getMessage());
            }
            LOGGER.error("Failed to save SPIR-V", e);
            return false;
        }
    }

    public static void createCacheDir() {
        File cacheDir = SuperResolutionConstants.SHADER_CACHE_DIR.getFile();
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            LOGGER.error("Unable to create shader cache directory: {}", SuperResolutionConstants.SHADER_CACHE_DIR);
        }
    }

    private static String getShaderProgramMd5(IShaderProgram shaderProgram, String apiTag) {
        if (isVulkan(apiTag)) {
            return getVulkanShaderProgramMd5(shaderProgram);
        } else {
            return getOpenGLShaderProgramMd5(shaderProgram);
        }
    }

    private static boolean isVulkan(String apiTag) {
        return apiTag.equals("vk");
    }

    private static GlslangCompileShaderResult compileShaderToSpirv(
            String src,
            EShLanguage stage,
            EShClient client,
            EShTargetClientVersion clientVersion
    ) {
        createCacheDir();
        LOGGER.debug("Invoking GlslangShaderCompiler to compile SPIR-V");

        GlslangCompileShaderResult result = GlslangShaderCompiler.compileShaderToSpirv(
                src,
                stage,
                EShSource.EShSourceGlsl,
                client,
                clientVersion,
                EShTargetLanguage.EShTargetSpv,
                EShTargetLanguageVersion.EShTargetSpv_1_4,
                Gl.isLegacy() ? 410 : 460,
                EProfile.ENoProfile,
                true,
                false
        );

        LOGGER.debug("SPIR-V compilation completed, error code={}, data size={}",
                result.error(), result.spirVDataSize());
        return result;
    }

    private static EShLanguage mapToGlslangType(ShaderType type) {
        return switch (type) {
            case Vertex -> EShLanguage.EShLangVertex;
            case Fragment -> EShLanguage.EShLangFragment;
            case Compute -> EShLanguage.EShLangCompute;
        };
    }

    private static String getVulkanShaderProgramMd5(IShaderProgram shaderProgram) {
        StringBuilder identityBuilder = new StringBuilder();
        ArrayList<String> sortedDefines = new ArrayList<>(new ArrayList<>(shaderProgram.getDescription().definesMap().entrySet()).stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue()).toList());

        for (ShaderType type : ShaderType.values()) {
            ShaderSource sources = shaderProgram.getDescription().sourceMap().get(type);
            if (sources != null) {
                identityBuilder.append(type.name()).append(":");
                identityBuilder.append(sources.getSource());
                sortedDefines.addAll(sources.getShaderDefines().values());
            }
        }
        sortedDefines = (ArrayList<String>) sortedDefines.stream().sorted()
                .collect(Collectors.toList());

        identityBuilder
                .append(shaderProgram.getDescription().shaderName())
                .append(String.join("|", sortedDefines))
                .append(Platform.currentPlatform.getMinecraftVersion())
                .append(Platform.currentPlatform.getModVersionString(SuperResolution.MOD_ID));
        return Md5CaculateUtil.getMD5(identityBuilder.toString());
    }

    private static String getOpenGLShaderProgramMd5(IShaderProgram shaderProgram) {
        StringBuilder identityBuilder = new StringBuilder();

        for (ShaderType type : ShaderType.values()) {
            ShaderSource sources = shaderProgram.getDescription().sourceMap().get(type);
            if (sources != null) {
                identityBuilder.append(type.name()).append(":");
                identityBuilder.append(sources.getSource());
            }
        }
        List<String> sortedDefines = shaderProgram.getDescription().definesMap().entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .sorted()
                .collect(Collectors.toList());
        identityBuilder
                .append(shaderProgram.getDescription().shaderName())
                .append(String.join("|", sortedDefines))
                .append(Platform.currentPlatform.getMinecraftVersion())
                .append(Platform.currentPlatform.getModVersionString(SuperResolution.MOD_ID));
        return Md5CaculateUtil.getMD5(identityBuilder.toString());
    }

    private static boolean checkProgramBinaryWithApi(IShaderProgram program, String apiTag) {
        createCacheDir();

        if (Platform.currentPlatform.isDevelopmentEnvironment()) {
            return false;
        }

        String hash = getShaderProgramMd5(program, apiTag);
        for (ShaderType type : program.getDescription().sourceMap().keySet()) {
            Path path = SuperResolutionConstants.SHADER_CACHE_DIR.getPath().resolve(
                    program.getDescription().shaderName() + "." + hash + "." + type.name().toLowerCase() + "." + apiTag + ".spv"
            );

            if (!Files.exists(path)) {
                LOGGER.debug("Cache file not found: {}", path);
                return false;
            }
        }
        LOGGER.debug("Shader cache file exists.");
        return true;
    }

    private static ShaderBinary getShaderBinaryWithApi(IShaderProgram program, ShaderType type, String apiTag) {
        createCacheDir();

        String hash = getShaderProgramMd5(program, apiTag);
        String filename = program.getDescription().shaderName() + "." + hash + "." + type.name().toLowerCase() + "." + apiTag + ".spv";
        LOGGER.debug("Loading cached binary: {}", filename);
        return loadBinaryWithApi(filename, apiTag);
    }

    private static ShaderBinary loadBinaryWithApi(String filename, String apiTag) {
        createCacheDir();

        Path path = SuperResolutionConstants.SHADER_CACHE_DIR.getPath().resolve(filename);
        try {
            byte[] data = Files.readAllBytes(path);
            if (data.length == 0 || data.length > 1024 * 1024 * 2) { // 最大2mb
                LOGGER.error("Invalid SPIR-V cache size: {}", data.length);
                return null;
            }

            ByteBuffer buffer;
            buffer = MemoryUtil.memAlloc(data.length);
            buffer.put(data).flip();
            LOGGER.debug("Loaded SPIR-V cache file: {}", filename);
            int format = isVulkan(apiTag) ? -1 : GL_SHADER_BINARY_FORMAT_SPIR_V_ARB;
            return new ShaderBinary(buffer, data.length, format);

        } catch (IOException e) {
            LOGGER.error("Failed to load SPIR-V: {}", filename, e);
            return null;
        }
    }

    public static class ShaderBinary implements AutoCloseable {
        private final ByteBuffer binary;
        private final int size;
        private final int format;
        private volatile boolean closed = false;

        public ShaderBinary(ByteBuffer binary, int size, int format) {
            this.binary = binary;
            this.size = size;
            this.format = format;
        }

        public ByteBuffer binary() {
            return binary;
        }

        public int size() {
            return size;
        }

        public int format() {
            return format;
        }

        @Override
        public void close() {
            if (!closed) {
                synchronized (this) {
                    if (!closed) {
                        LOGGER.debug("Released {} bytes of shader code memory", size);
                        MemoryUtil.memFree(binary);
                        closed = true;
                    }
                }
            }
        }
    }
}
