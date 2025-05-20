package io.homo.superresolution.core.utils;

import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.core.GraphicsCapabilities;
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
            LOGGER.error("Failed to create shader cache directory: {}", CACHE_DIR);
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
        ShaderSource currentSource = null;
        GlslangCompileShaderResult currentSourceResult = null;
        try {
            for (Map.Entry<ShaderSource.Type, ShaderSource> entry : program.getShaderSources().entrySet()) {
                ShaderSource.Type type = entry.getKey();
                ShaderSource source = entry.getValue();
                currentSource = source;
                String suffix = getShaderSuffix(type);
                Path path = CACHE_DIR.resolve(hash + "." + suffix);
                currentSourceResult = compileShaderToSpirv(source.getSource(), path.toString(), mapToGlslangType(type));
                if (currentSourceResult.error() != GlslangCompileShaderError.OK) {
                    throw new AbstractGlShaderProgram.ShaderCompileException(currentSourceResult.log());
                }
                if (Config.getInstance().isDebugDumpShader()) {
                    try {
                        Files.write(Path.of(CACHE_DIR.toAbsolutePath().toString(), program.shaderName + ".source.glsl"), currentSourceResult.sourceCode().getBytes(StandardCharsets.UTF_8));
                        Files.write(Path.of(CACHE_DIR.toAbsolutePath().toString(), program.shaderName + ".preprocessed.glsl"), currentSourceResult.preprocessedCode().getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e0) {
                        LOGGER.error("无法保存着色器源码文件: {}", e0.getMessage());
                    }
                }
            }
            return true;
        } catch (AbstractGlShaderProgram.ShaderCompileException e) {
            Path sourcePath = Path.of(program.shaderName + ".glsl");
            try {
                LOGGER.info(currentSourceResult.error().name());
                LOGGER.info(currentSourceResult.log());
                LOGGER.info(currentSourceResult.sourceCode());
                LOGGER.info(currentSourceResult.preprocessedCode());
                Files.write(Path.of(program.shaderName + ".error.source.glsl"), currentSourceResult.sourceCode().getBytes(StandardCharsets.UTF_8));
                Files.write(Path.of(program.shaderName + ".error.preprocessed.glsl"), currentSourceResult.preprocessedCode().getBytes(StandardCharsets.UTF_8));
                Files.write(Path.of(program.shaderName + ".error.log"), currentSourceResult.log().getBytes(StandardCharsets.UTF_8));
                LOGGER.info("保存错误着色器源码至: {}", sourcePath);
            } catch (IOException e0) {
                LOGGER.error("无法保存着色器源码文件: {}", e0.getMessage());
            }
            LOGGER.error("保存SPIR-V失败", e);
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
            if (!Files.exists(CACHE_DIR.resolve(hash + "." + suffix))) {
                return false;
            }
        }
        return true;
    }

    public static ShaderBinary getShaderBinary(AbstractGlShaderProgram program, ShaderSource.Type type) {
        createCacheDir();

        String hash = getShaderProgramMd5(program);
        String filename = hash + "." + getShaderSuffix(type);
        return loadBinary(filename);
    }

    private static String getShaderSuffix(ShaderSource.Type type) {
        return switch (type) {
            case VERTEX -> "vert.spv";
            case FRAGMENT -> "frag.spv";
            case COMPUTE -> "comp.spv";
        };
    }


    private static GlslangCompileShaderResult compileShaderToSpirv(String src, String outputPath, EShLanguage stage) {
        createCacheDir();

        GlslangCompileShaderResult result = GlslangShaderCompiler.compileShaderToSpirv(
                src,
                outputPath,
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
        return result;
    }


    private static ShaderBinary loadBinary(String filename) {
        createCacheDir();
        
        Path path = CACHE_DIR.resolve(filename);
        try {
            byte[] data = Files.readAllBytes(path);
            ByteBuffer buffer = MemoryUtil.memAlloc(data.length);
            buffer.put(data).flip();
            return new ShaderBinary(buffer, data.length, GL_SHADER_BINARY_FORMAT_SPIR_V_ARB);
        } catch (IOException e) {
            LOGGER.error("加载SPIR-V失败: {}", filename);
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