package io.homo.superresolution.core.graphics.shader;

import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.core.SuperResolutionNative;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import io.homo.superresolution.core.graphics.glslang.GlslangCompileShaderResult;
import io.homo.superresolution.core.graphics.glslang.GlslangShaderCompiler;
import io.homo.superresolution.core.graphics.glslang.enums.*;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.opengl.Gl;
import io.homo.superresolution.core.graphics.opengl.shader.ShaderCompileException;
import io.homo.superresolution.core.utils.Md5CaculateUtil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.ARBGLSPIRV.GL_SHADER_BINARY_FORMAT_SPIR_V_ARB;

public class ShaderCompiler {
    public static final Logger LOGGER = LoggerFactory.getLogger("SuperResolution-ShaderCompiler");
    public static final Path CACHE_DIR = Path.of(Platform.currentPlatform.getGameFolder().toString(), "sr_shaderCache");

    static {
        createCacheDir();
    }

    // ========= Vulkan =========
    public static boolean saveVulkanProgramBinary(IShaderProgram<?> program) {
        return saveProgramBinaryWithApi(program, "vk");
    }

    public static boolean checkVulkanProgramBinary(IShaderProgram<?> program) {
        return checkProgramBinaryWithApi(program, "vk");
    }

    public static ShaderBinary getVulkanShaderBinary(IShaderProgram<?> program, ShaderType type) {
        return getShaderBinaryWithApi(program, type, "vk");
    }

    // ========= OpenGL =========
    public static boolean saveOpenGLProgramBinary(IShaderProgram<?> program) {
        return saveProgramBinaryWithApi(program, "ogl");
    }

    public static boolean checkOpenGLProgramBinary(IShaderProgram<?> program) {
        return checkProgramBinaryWithApi(program, "ogl");
    }

    public static ShaderBinary getOpenGLShaderBinary(IShaderProgram<?> program, ShaderType type) {
        return getShaderBinaryWithApi(program, type, "ogl");
    }

    private static boolean saveProgramBinaryWithApi(IShaderProgram<?> program, String apiTag) {
        createCacheDir();

        String hash = getShaderProgramMd5(program, apiTag);
        GlslangCompileShaderResult currentSourceResult = null;
        try {
            for (Map.Entry<ShaderType, ShaderSource> entry : program.getDescription().sourceMap().entrySet()) {
                ShaderType type = entry.getKey();
                ShaderSource source = entry.getValue();
                Path path = CACHE_DIR.resolve(program.getDescription().shaderName() + "." + hash + "." + type.name().toLowerCase() + "." + apiTag + ".spv");

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

                LOGGER.info("开始SPIR-V编译: 类型={}, API={}, 缓存路径={}", type.name(), apiTag, path);

                if (currentSourceResult.error() != GlslangCompileShaderError.OK) {
                    LOGGER.error("着色器编译失败[{}]，错误类型={}，日志={}", type.name(), currentSourceResult.error().name(), currentSourceResult.log());
                    throw new ShaderCompileException(currentSourceResult.log());
                }

                ByteBuffer buffer = currentSourceResult.spirvBuffer();
                long size = currentSourceResult.spirVDataSize();

                if (buffer == null || size <= 0) {
                    LOGGER.error("SPIR-V缓冲区为空或大小非法，type={}, size={}", type.name(), size);
                    throw new IOException("SPIR-V缓冲区为空或大小非法");
                }

                LOGGER.info("保存SPIR-V，大小={} bytes, 路径={}", size, path);

                if (SuperResolutionConfig.isDebugDumpShader()) {
                    try {
                        Path srcPath = Path.of(CACHE_DIR.toAbsolutePath().toString(),
                                program.getDescription().shaderName() + "." + type.name().toLowerCase() + "." + apiTag + ".source.glsl");
                        Path prePath = Path.of(CACHE_DIR.toAbsolutePath().toString(),
                                program.getDescription().shaderName() + "." + type.name().toLowerCase() + "." + apiTag + ".preprocessed.glsl");
                        LOGGER.debug("写出GLSL源码调试文件: {}，{}", srcPath, prePath);
                        Files.writeString(srcPath, currentSourceResult.sourceCode());
                        Files.writeString(prePath, currentSourceResult.preprocessedCode());
                    } catch (IOException e0) {
                        LOGGER.error("无法保存着色器源码文件: {}", e0.getMessage());
                    }
                }

                try {
                    byte[] outBytes = new byte[(int) size];
                    buffer.position(0);
                    buffer.get(outBytes);
                    Files.write(path, outBytes);
                    LOGGER.info("SPIR-V保存成功: {}", path);
                } catch (IOException e) {
                    LOGGER.error("保存SPIR-V失败", e);
                }

                SuperResolutionNative.freeDirectBuffer(buffer);
                LOGGER.debug("释放DirectBuffer完成");
            }
            return true;
        } catch (ShaderCompileException | IOException e) {
            try {
                if (currentSourceResult != null) {
                    LOGGER.debug("着色器编译异常类型: {}", currentSourceResult.error().name());
                    LOGGER.debug("编译日志: {}", currentSourceResult.log());

                    Path errorSourcePath = Path.of(CACHE_DIR.toString(),
                            program.getDescription().shaderName() + ".error." + apiTag + ".source.glsl");
                    Path errorPrePath = Path.of(CACHE_DIR.toString(),
                            program.getDescription().shaderName() + ".error." + apiTag + ".preprocessed.glsl");
                    Path errorLogPath = Path.of(CACHE_DIR.toString(),
                            program.getDescription().shaderName() + ".error." + apiTag + ".log");

                    Files.writeString(errorSourcePath, currentSourceResult.sourceCode());
                    Files.writeString(errorPrePath, currentSourceResult.preprocessedCode());
                    Files.writeString(errorLogPath, currentSourceResult.log());
                    LOGGER.info("保存错误着色器源码至: {}, {}, {}", errorSourcePath, errorPrePath, errorLogPath);
                }
            } catch (IOException e0) {
                LOGGER.error("无法保存着色器源码文件: {}", e0.getMessage());
            }
            LOGGER.error("保存SPIR-V失败", e);
            return false;
        }
    }

    public static void createCacheDir() {
        File cacheDir = CACHE_DIR.toFile();
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            LOGGER.error("无法创建着色器缓存目录: {}", CACHE_DIR);
        }
    }

    private static String getShaderProgramMd5(IShaderProgram<?> shaderProgram, String apiTag) {
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
        LOGGER.debug("调用GlslangShaderCompiler编译SPIR-V");

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

        LOGGER.debug("编译SPIR-V结束，错误码={}, 数据大小={}",
                result.error(), result.spirVDataSize());
        return result;
    }

    private static EShLanguage mapToGlslangType(ShaderType type) {
        return switch (type) {
            case VERTEX -> EShLanguage.EShLangVertex;
            case FRAGMENT -> EShLanguage.EShLangFragment;
            case COMPUTE -> EShLanguage.EShLangCompute;
        };
    }

    private static String getVulkanShaderProgramMd5(IShaderProgram<?> shaderProgram) {
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
                .append(Arrays.toString(GraphicsCapabilities.getVulkanVersion()));
        return Md5CaculateUtil.getMD5(identityBuilder.toString());
    }

    private static String getOpenGLShaderProgramMd5(IShaderProgram<?> shaderProgram) {
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
                .append(GraphicsCapabilities.getGLVersion()[0])
                .append(GraphicsCapabilities.getGLVersion()[1])
                .append(GL11.glGetString(GL11.GL_VENDOR))
                .append(GL11.glGetString(GL11.GL_RENDERER));
        return Md5CaculateUtil.getMD5(identityBuilder.toString());
    }

    private static boolean checkProgramBinaryWithApi(IShaderProgram<?> program, String apiTag) {
        createCacheDir();

        if (Platform.currentPlatform.isDevelopmentEnvironment()) return false;

        String hash = getShaderProgramMd5(program, apiTag);
        for (ShaderType type : program.getDescription().sourceMap().keySet()) {
            Path path = CACHE_DIR.resolve(
                    program.getDescription().shaderName() + "." + hash + "." + type.name().toLowerCase() + "." + apiTag + ".spv"
            );

            if (!Files.exists(path)) {
                LOGGER.info("未找到缓存文件: {}", path);
                return false;
            }
        }
        LOGGER.info("着色器缓存文件存在。");
        return true;
    }

    private static ShaderBinary getShaderBinaryWithApi(IShaderProgram<?> program, ShaderType type, String apiTag) {
        createCacheDir();

        String hash = getShaderProgramMd5(program, apiTag);
        String filename = program.getDescription().shaderName() + "." + hash + "." + type.name().toLowerCase() + "." + apiTag + ".spv";
        LOGGER.info("加载缓存二进制: {}", filename);
        return loadBinaryWithApi(filename, apiTag);
    }

    private static ShaderBinary loadBinaryWithApi(String filename, String apiTag) {
        createCacheDir();

        Path path = CACHE_DIR.resolve(filename);
        try {
            byte[] data = Files.readAllBytes(path);
            if (data.length == 0 || data.length > 1024 * 1024 * 2) { // 最大2mb
                LOGGER.error("SPIR-V缓存大小异常: {}", data.length);
                return null;
            }

            ByteBuffer buffer;
            buffer = MemoryUtil.memAlloc(data.length);
            buffer.put(data).flip();
            LOGGER.info("成功加载SPIR-V缓存文件: {}", filename);
            int format = isVulkan(apiTag) ? -1 : GL_SHADER_BINARY_FORMAT_SPIR_V_ARB;
            return new ShaderBinary(buffer, data.length, format);

        } catch (IOException e) {
            LOGGER.error("加载SPIR-V失败: {}", filename, e);
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
                        LOGGER.info("释放着色器代码内存 {} bytes", size);
                        MemoryUtil.memFree(binary);
                        closed = true;
                    }
                }
            }
        }
    }
}