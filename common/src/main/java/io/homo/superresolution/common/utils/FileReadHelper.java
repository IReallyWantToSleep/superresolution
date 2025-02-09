package io.homo.superresolution.common.utils;

import com.mojang.blaze3d.platform.NativeImage;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.upscale.fsr1.FSR1;
import org.lwjgl.system.MemoryUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;

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

    public static NativeImage readTexture(String path) {
        try (InputStream inputStream = FSR1.class.getResourceAsStream(path)) {
            if (inputStream != null) {
                try {
                    return NativeImage.read(inputStream);
                } catch (IOException e) {
                    return null;
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
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
