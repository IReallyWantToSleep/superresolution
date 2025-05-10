package io.homo.superresolution.core.utils;

import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.core.GraphicsCapabilities;
import io.homo.superresolution.core.gl.shader.AbstractGlShaderProgram;
import io.homo.superresolution.core.glslang.GlslangCompileShaderResult;
import io.homo.superresolution.core.glslang.GlslangShaderCompiler;
import io.homo.superresolution.core.glslang.enums.*;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL46.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.ARBGLSPIRV.GL_SHADER_BINARY_FORMAT_SPIR_V_ARB;

public class ShaderCache {
    public static final Logger LOGGER = LoggerFactory.getLogger("SuperResolution-ShaderCache");
    public static final Path CACHE_DIR = Path.of(Platform.currentPlatform.getGameFolder().toString(), "sr_shaderCache");
    public static final Map<String, ShaderBinary> CACHE = new HashMap<>();

    static {
        File cacheDir = CACHE_DIR.toFile();
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            LOGGER.error("Failed to create shader cache directory: {}", CACHE_DIR);
        }
    }

    private static String getShaderProgramMd5(AbstractGlShaderProgram shaderProgram) {
        String identityString = shaderProgram.shaderName +
                shaderProgram.getFragShaderText() +
                shaderProgram.getVertShaderText() +
                GraphicsCapabilities.getGLVersion()[0] +
                GraphicsCapabilities.getGLVersion()[1] +
                GL11.glGetString(GL11.GL_VENDOR) +
                GL11.glGetString(GL11.GL_RENDERER) +
                String.join("|", shaderProgram.getShaderDefineList());

        return Md5CaculateUtil.getMD5(identityString);
    }

    // 修改保存逻辑
    public static boolean saveProgramBinary(AbstractGlShaderProgram program) {
        String hash = getShaderProgramMd5(program);

        try {
            // 保存顶点着色器SPIR-V
            Path vertPath = CACHE_DIR.resolve(hash + ".vert.spv");
            compileShaderToSpirv(
                    program.getVertShaderText(),
                    vertPath.toString(),
                    EShLanguage.EShLangVertex
            );

            // 保存片段着色器SPIR-V
            Path fragPath = CACHE_DIR.resolve(hash + ".frag.spv");
            compileShaderToSpirv(
                    program.getFragShaderText(),
                    fragPath.toString(),
                    EShLanguage.EShLangFragment
            );

            return true;
        } catch (Exception e) {
            LOGGER.error("保存SPIR-V失败", e);
            return false;
        }
    }

    private static void compileShaderToSpirv(String src, String outputPath, EShLanguage stage) {
        GlslangCompileShaderResult result = GlslangShaderCompiler.compileShaderToSpirv(
                src,
                outputPath,
                stage,
                EShSource.EShSourceGlsl,
                EShClient.EShClientOpenGL,
                EShTargetClientVersion.EShTargetOpenGL_450,
                EShTargetLanguage.EShTargetSpv,
                EShTargetLanguageVersion.EShTargetSpv_1_0,
                450,
                EProfile.ECoreProfile,
                true,
                false
        );

        if (result.error().getValue() != 0) {
            throw new RuntimeException(stage + " SPIR-V编译失败:\n" + result.log());
        }
    }

    public static boolean checkProgramBinary(AbstractGlShaderProgram program) {
        String hash = getShaderProgramMd5(program);
        return Files.exists(CACHE_DIR.resolve(hash + ".vert.spv")) &&
                Files.exists(CACHE_DIR.resolve(hash + ".frag.spv"));
    }

    public static ShaderBinary[] getProgramBinary(AbstractGlShaderProgram program) {
        String hash = getShaderProgramMd5(program);
        return new ShaderBinary[]{
                loadBinary(hash + ".vert.spv"),
                loadBinary(hash + ".frag.spv")
        };
    }

    private static ShaderBinary loadBinary(String filename) {
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