package io.homo.superresolution.core.utils;

import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.core.GraphicsCapabilities;
import io.homo.superresolution.core.gl.shader.AbstractGlShaderProgram;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL41;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ShaderCache {
    public static final Logger LOGGER = LoggerFactory.getLogger("SuperResolution-ShaderCache");
    public static final Path CACHE_DIR = Path.of(Platform.currentPlatform.getGameFolder().toString(), "sr_shaderCache");

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

    public static boolean saveProgramBinary(AbstractGlShaderProgram shaderProgram) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer lengthBuf = stack.mallocInt(1);
            GL41.glGetProgramiv(shaderProgram.shaderProgram, GL41.GL_PROGRAM_BINARY_LENGTH, lengthBuf);
            int binaryLength = lengthBuf.get(0);
            if (binaryLength <= 0 || binaryLength > 10 * 1024 * 1024) {
                LOGGER.warn("Invalid binary length: {}", binaryLength);
                return false;
            }

            ByteBuffer binaryBuffer = MemoryUtil.memAlloc(binaryLength);
            try {
                IntBuffer formatBuf = stack.mallocInt(1);
                GL41.glGetProgramBinary(shaderProgram.shaderProgram, lengthBuf, formatBuf, binaryBuffer);

                Path outputPath = CACHE_DIR.resolve(getShaderProgramMd5(shaderProgram) + ".shaderbin");
                try (DataOutputStream dos = new DataOutputStream(
                        new BufferedOutputStream(new FileOutputStream(outputPath.toFile())))) {

                    dos.writeInt(formatBuf.get(0));
                    byte[] data = new byte[binaryLength];
                    binaryBuffer.get(data);
                    dos.write(data);
                    LOGGER.info("着色器写入缓存成功 {}", getShaderProgramMd5(shaderProgram));
                    return true;
                }
            } finally {
                MemoryUtil.memFree(binaryBuffer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save shader binary: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            LOGGER.error("Unexpected error during shader saving", e);
            return false;
        }
    }

    public static boolean checkProgramBinary(AbstractGlShaderProgram shaderProgram) {
        return Files.exists(CACHE_DIR.resolve(getShaderProgramMd5(shaderProgram) + ".shaderbin"));
    }

    public static ShaderBinary getProgramBinary(AbstractGlShaderProgram shaderProgram) {
        Path binaryPath = CACHE_DIR.resolve(getShaderProgramMd5(shaderProgram) + ".shaderbin");
        if (!Files.exists(binaryPath)) return null;

        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(binaryPath)))) {
            int format = dis.readInt();
            if (!isBinaryFormatSupported(format)) {
                LOGGER.warn("Unsupported binary format: 0x{}", Integer.toHexString(format));
                return null;
            }

            byte[] data = new byte[dis.available()];
            dis.readFully(data);
            if (!validateBinary(data)) {
                LOGGER.warn("Invalid binary data detected");
                return null;
            }

            ByteBuffer buffer = MemoryUtil.memAlloc(data.length);
            try {
                buffer.put(data).flip();
                LOGGER.info("读取着色器缓存成功 {}", getShaderProgramMd5(shaderProgram));
                return new ShaderBinary(buffer, data.length, format);
            } catch (Exception e) {
                MemoryUtil.memFree(buffer);
                throw e;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load shader binary: {}", e.getMessage());
            return null;
        }
    }

    private static boolean isBinaryFormatSupported(int format) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer countBuf = stack.mallocInt(1);
            GL41.glGetIntegerv(GL41.GL_NUM_PROGRAM_BINARY_FORMATS, countBuf);
            int count = countBuf.get(0);
            if (count == 0) return false;
            IntBuffer formats = stack.mallocInt(count);
            GL41.glGetIntegerv(GL41.GL_PROGRAM_BINARY_FORMATS, formats);
            for (int i = 0; i < count; i++) {
                if (formats.get(i) == format) {
                    return true;
                }
            }
            return false;
        }
    }

    private static boolean validateBinary(byte[] data) {
        return data.length > 0 && data.length <= 10 * 1024 * 1024; // 合理大小范围
    }

    public record ShaderBinary(ByteBuffer binary, int size, int format) implements AutoCloseable {
        @Override
        public void close() {
            MemoryUtil.memFree(binary);
        }
    }
}