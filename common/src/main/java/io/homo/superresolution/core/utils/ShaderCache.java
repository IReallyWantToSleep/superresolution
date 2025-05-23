package io.homo.superresolution.core.utils;

import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.core.GraphicsCapabilities;
import io.homo.superresolution.core.SuperResolutionNative;
import io.homo.superresolution.core.gl.shader.AbstractGlShaderProgram;
import io.homo.superresolution.core.glslang.GlslangCompileShaderResult;
import io.homo.superresolution.core.glslang.GlslangShaderCompiler;
import io.homo.superresolution.core.glslang.enums.*;
import io.homo.superresolution.core.impl.shader.ShaderSource;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL46.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static io.homo.superresolution.common.SuperResolution.LOGGER;
import static org.lwjgl.opengl.ARBGLSPIRV.GL_SHADER_BINARY_FORMAT_SPIR_V_ARB;

public class ShaderCache {
    public static final Logger LOGGER = LoggerFactory.getLogger("SuperResolution-ShaderCache");
    public static final Path CACHE_DIR = Path.of(Platform.currentPlatform.getGameFolder().toString(), "sr_shaderCache");
    public static final Map<String, ShaderBinary> CACHE = new HashMap<>();

    static {
        createCacheDir();
    }

    public static void createCacheDir() {
        File cacheDir = CACHE_DIR.toFile();
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            LOGGER.error("无法创建着色器缓存目录: {}", CACHE_DIR);
        }
    }

    private static String getShaderProgramMd5(AbstractGlShaderProgram shaderProgram) {
        createCacheDir();

        StringBuilder identityBuilder = new StringBuilder();

        for (ShaderSource.Type type : ShaderSource.Type.values()) {
            ShaderSource sources = shaderProgram.getShaderSources().get(type);
            if (sources != null) {
                identityBuilder.append(type.name()).append(":");
                identityBuilder.append(sources.getSource());
            }
        }
        List<String> sortedDefines = shaderProgram.getShaderDefineList().entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .sorted()
                .collect(Collectors.toList());
        identityBuilder
                .append(shaderProgram.shaderName)
                .append(String.join("|", sortedDefines))
                .append(GraphicsCapabilities.getGLVersion()[0])
                .append(GraphicsCapabilities.getGLVersion()[1])
                .append(GL11.glGetString(GL11.GL_VENDOR))
                .append(GL11.glGetString(GL11.GL_RENDERER));
        return Md5CaculateUtil.getMD5(identityBuilder.toString());
    }

    public static boolean saveProgramBinary(AbstractGlShaderProgram program) {
        createCacheDir();

        String hash = getShaderProgramMd5(program);
        GlslangCompileShaderResult currentSourceResult = null;
        try {
            for (Map.Entry<ShaderSource.Type, ShaderSource> entry : program.getShaderSources().entrySet()) {
                ShaderSource.Type type = entry.getKey();
                ShaderSource source = entry.getValue();
                String suffix = getShaderSuffix(type);
                Path path = CACHE_DIR.resolve(program.shaderName + "." + hash + "." + suffix);
                currentSourceResult = compileShaderToSpirv(
                        source.getSource(),
                        mapToGlslangType(type)
                );

                LOGGER.info("开始SPIR-V编译: 类型={}，缓存路径={}", type.name(), path);

                if (currentSourceResult.error() != GlslangCompileShaderError.OK) {
                    LOGGER.error("着色器编译失败[{}]，错误类型={}，日志={}", type.name(), currentSourceResult.error().name(), currentSourceResult.log());
                    throw new AbstractGlShaderProgram.ShaderCompileException(currentSourceResult.log());
                }

                ByteBuffer buffer = currentSourceResult.spirvBuffer();
                long size = currentSourceResult.spirVDataSize();

                if (buffer == null || size <= 0) {
                    LOGGER.error("SPIR-V缓冲区为空或大小非法，type={}, size={}", type.name(), size);
                    throw new IOException("SPIR-V缓冲区为空或大小非法");
                }

                LOGGER.info("保存SPIR-V，大小={} bytes, 路径={}", size, path);

                if (Config.getInstance().isDebugDumpShader()) {
                    try {
                        Path srcPath = Path.of(CACHE_DIR.toAbsolutePath().toString(), program.shaderName + ".source.glsl");
                        Path prePath = Path.of(CACHE_DIR.toAbsolutePath().toString(), program.shaderName + ".preprocessed.glsl");
                        LOGGER.debug("写出GLSL源码调试文件: {}，{}", srcPath, prePath);
                        Files.write(srcPath, currentSourceResult.sourceCode().getBytes(StandardCharsets.UTF_8));
                        Files.write(prePath, currentSourceResult.preprocessedCode().getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e0) {

                        LOGGER.error("无法保存着色器源码文件: {}", e0.getMessage());
                        e0.printStackTrace();
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
                    e.printStackTrace();
                }

                SuperResolutionNative.freeDirectBuffer(buffer);
                LOGGER.debug("释放DirectBuffer完成");
            }
            return true;
        } catch (AbstractGlShaderProgram.ShaderCompileException | IOException e) {
            Path sourcePath = Path.of(program.shaderName + ".glsl");
            try {
                LOGGER.debug("着色器编译异常类型: {}", currentSourceResult != null ? currentSourceResult.error().name() : "null");
                LOGGER.debug("编译日志: {}", currentSourceResult != null ? currentSourceResult.log() : "null");
                LOGGER.debug("源代码: {}", currentSourceResult != null ? currentSourceResult.sourceCode() : "null");
                LOGGER.debug("预处理代码: {}", currentSourceResult != null ? currentSourceResult.preprocessedCode() : "null");
                Files.write(Path.of(program.shaderName + ".error.source.glsl"),
                        currentSourceResult != null ? currentSourceResult.sourceCode().getBytes(StandardCharsets.UTF_8) : new byte[0]);
                Files.write(Path.of(program.shaderName + ".error.preprocessed.glsl"),
                        currentSourceResult != null ? currentSourceResult.preprocessedCode().getBytes(StandardCharsets.UTF_8) : new byte[0]);
                Files.write(Path.of(program.shaderName + ".error.log"),
                        currentSourceResult != null ? currentSourceResult.log().getBytes(StandardCharsets.UTF_8) : new byte[0]);
                LOGGER.info("保存错误着色器源码至: {}", sourcePath);
            } catch (IOException e0) {
                LOGGER.error("无法保存着色器源码文件: {}", e0.getMessage());
            }
            LOGGER.error("保存SPIR-V失败", e);
            e.printStackTrace();
            return false;
        }
    }

    private static EShLanguage mapToGlslangType(ShaderSource.Type type) {
        return switch (type) {
            case VERTEX -> EShLanguage.EShLangVertex;
            case FRAGMENT -> EShLanguage.EShLangFragment;
            case COMPUTE -> EShLanguage.EShLangCompute;
        };
    }

    public static boolean checkProgramBinary(AbstractGlShaderProgram program) {
        createCacheDir();

        String hash = getShaderProgramMd5(program);
        for (ShaderSource.Type type : program.getShaderSources().keySet()) {
            String suffix = getShaderSuffix(type);
            if (!Files.exists(CACHE_DIR.resolve(program.shaderName + "." + hash + "." + suffix))) {
                LOGGER.info("未找到缓存文件: {}", CACHE_DIR.resolve(program.shaderName + "." + hash + "." + suffix));
                return false;
            }
        }
        LOGGER.info("着色器缓存文件存在。");
        return true;
    }

    public static ShaderBinary getShaderBinary(AbstractGlShaderProgram program, ShaderSource.Type type) {
        createCacheDir();

        String hash = getShaderProgramMd5(program);
        String filename = program.shaderName + "." + hash + "." + getShaderSuffix(type);
        LOGGER.info("加载缓存二进制: {}", filename);
        return loadBinary(filename);
    }

    private static String getShaderSuffix(ShaderSource.Type type) {
        return switch (type) {
            case VERTEX -> "vert.spv";
            case FRAGMENT -> "frag.spv";
            case COMPUTE -> "comp.spv";
        };
    }

    private static GlslangCompileShaderResult compileShaderToSpirv(
            String src,
            EShLanguage stage
    ) {
        createCacheDir();
        LOGGER.debug("调用GlslangShaderCompiler编译SPIR-V");
        GlslangCompileShaderResult result = GlslangShaderCompiler.compileShaderToSpirv(
                src,
                stage,
                EShSource.EShSourceGlsl,
                EShClient.EShClientOpenGL,
                EShTargetClientVersion.EShTargetOpenGL_450,
                EShTargetLanguage.EShTargetSpv,
                EShTargetLanguageVersion.EShTargetSpv_1_4,
                460,
                EProfile.ENoProfile,
                true,
                false
        );
        LOGGER.debug("编译SPIR-V结束，错误码={}, 数据大小={}",
                result.error(), result.spirVDataSize());
        return result;
    }

    private static ShaderBinary loadBinary(String filename) {
        createCacheDir();

        Path path = CACHE_DIR.resolve(filename);
        try {
            byte[] data = Files.readAllBytes(path);
            ByteBuffer buffer = MemoryUtil.memAlloc(data.length);
            buffer.put(data).flip();
            LOGGER.info("成功加载SPIR-V缓存文件: {}", filename);
            return new ShaderBinary(buffer, data.length, GL_SHADER_BINARY_FORMAT_SPIR_V_ARB);
        } catch (IOException e) {
            LOGGER.error("加载SPIR-V失败: {}", filename);
            e.printStackTrace();
            return null;
        }
    }

    public record ShaderBinary(ByteBuffer binary, int size, int format) implements AutoCloseable {
        @Override
        public void close() {
            MemoryUtil.memFree(binary);
        }
    }
}