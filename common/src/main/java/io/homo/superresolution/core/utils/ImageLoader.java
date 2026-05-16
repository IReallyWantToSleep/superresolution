/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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

package io.homo.superresolution.core.utils;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.Gl;
import io.homo.superresolution.core.graphics.opengl.GlDevice;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import org.lwjgl.opengl.GL20;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class ImageLoader {
    public static ITexture load(GlDevice device, InputStream inputStream) {
        ByteBuffer imageFileData;
        byte[] buffer = new byte[8192];
        int bytesRead;
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            byte[] data = output.toByteArray();
            imageFileData = MemoryUtil.memAlloc(data.length);
            imageFileData.put(data);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        imageFileData.rewind();
        int width, height, channelsInFile;
        ByteBuffer textureData;
        try (MemoryStack memorystack = MemoryStack.stackPush()) {
            IntBuffer xBuffer = memorystack.mallocInt(1);
            IntBuffer yBuffer = memorystack.mallocInt(1);
            IntBuffer channelsInFileBuffer = memorystack.mallocInt(1);
            ByteBuffer bytebuffer = STBImage.stbi_load_from_memory(
                    imageFileData,
                    xBuffer,
                    yBuffer,
                    channelsInFileBuffer,
                    4
            );
            if (bytebuffer == null) {
                MemoryUtil.memFree(imageFileData);
                SuperResolution.LOGGER.error("Error while loading image from memory: {}", STBImage.stbi_failure_reason());
                return null;
            } else {
                textureData = bytebuffer;
                width = xBuffer.get(0);
                height = yBuffer.get(0);
                channelsInFile = channelsInFileBuffer.get(0);
                MemoryUtil.memFree(imageFileData);
            }
        }
        GlTexture2D texture = (GlTexture2D) device.createTexture(
                TextureDescription.create()
                        .width(width)
                        .height(height)
                        .usages(TextureUsages.create().transferDestination().sampler().storage())
                        .format(TextureFormat.RGBA8)
                        .type(TextureType.Texture2D)
                        .build()
        );
        ICommandBuffer commandBuffer = device.createCommandBuffer();
        commandBuffer.begin();
        commandBuffer.writeToTexture(texture, textureData, 0, 0, width, height, 0);
        commandBuffer.end();
        commandBuffer.submit(device);
        commandBuffer.waitForFence();
        MemoryUtil.memFree(textureData);
        return texture;
    }
}
