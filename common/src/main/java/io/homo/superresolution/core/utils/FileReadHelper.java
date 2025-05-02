package io.homo.superresolution.core.utils;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.core.impl.Pair;
import io.homo.superresolution.core.impl.Vec2;
import io.homo.superresolution.common.upscale.fsr1.FSR1;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import static org.lwjgl.stb.STBImage.*;

public class FileReadHelper {
    public static ArrayList<String> readText(String path) {
        InputStream inputStream = FSR1.class.getResourceAsStream(path);
        ArrayList<String> lines = new ArrayList<>();

        if (inputStream != null) {
            try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                 BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    lines.add(line);
                }
            } catch (IOException e) {
                SuperResolution.LOGGER.error(e.toString());
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    SuperResolution.LOGGER.error(e.toString());
                }
            }
        }
        return lines;
    }

    public static Pair<Vec2, ByteBuffer> readTexture(String path) {
        ByteBuffer imageBuffer = null;
        try (InputStream is = FSR1.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Texture not found: " + path);
            }
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(chunk)) != -1) {
                byteStream.write(chunk, 0, bytesRead);
            }
            byte[] bytes = byteStream.toByteArray();

            imageBuffer = MemoryUtil.memAlloc(bytes.length);
            imageBuffer.put(bytes).flip();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);

                ByteBuffer image = stbi_load_from_memory(
                        imageBuffer,
                        w,
                        h,
                        channels,
                        4
                );

                if (image == null) {
                    MemoryUtil.memFree(imageBuffer);
                    imageBuffer = null;
                    throw new RuntimeException("Failed to load texture: " + stbi_failure_reason());
                }

                MemoryUtil.memFree(imageBuffer);
                imageBuffer = null;

                return Pair.of(
                        new Vec2(w.get(), h.get()),
                        image
                );
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read texture: " + path, e);
        } finally {
            if (imageBuffer != null) {
                MemoryUtil.memFree(imageBuffer);
            }
        }
    }


    public static ByteBuffer readSpvFile(String path) {
        try (InputStream is = FSR1.class.getResourceAsStream(path)) {
            if (is == null) return null;
            int fileSize = is.available();
            int alignedSize = (fileSize + 3) & ~3;
            ByteBuffer buffer = MemoryUtil.memAlloc(alignedSize);
            byte[] chunk = new byte[4096];
            int totalRead = 0;
            int bytesRead;
            while ((bytesRead = is.read(chunk)) != -1) {
                buffer.put(chunk, 0, bytesRead);
                totalRead += bytesRead;
            }
            while (totalRead < alignedSize) {
                buffer.put((byte) 0);
                totalRead++;
            }
            return buffer.flip();
        } catch (Exception e) {
            return null;
        }
    }
}
