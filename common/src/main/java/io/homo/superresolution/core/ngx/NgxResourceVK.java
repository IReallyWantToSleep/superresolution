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

package io.homo.superresolution.core.ngx;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class NgxResourceVK implements AutoCloseable {
    private static final int IMAGE_VIEW_INFO_SIZE = 48;
    private static final int RESOURCE_SIZE = 56;

    public final NgxImageViewInfoVK imageViewInfo = new NgxImageViewInfoVK();
    public final NgxBufferInfoVK bufferInfo = new NgxBufferInfoVK();
    public int type = NgxConstants.RESOURCE_VK_IMAGE_VIEW;
    public boolean readWrite;

    private ByteBuffer nativeBuffer = MemoryUtil.memAlloc(RESOURCE_SIZE).order(ByteOrder.nativeOrder());
    private boolean nativeBufferStale = true;

    /**
     * Marks the mirrored native struct as needing a rewrite. Callers that mutate the
     * public fields after the resource has been used must invoke this; the fields are
     * fixed at creation for every resource this mod builds, so in practice the struct
     * is written once instead of on each of the ~20 lookups a DLSS evaluate performs.
     */
    public void markDirty() {
        nativeBufferStale = true;
    }

    public long nativeAddress() {
        if (nativeBuffer == null) {
            throw new IllegalStateException("NGX resource is closed");
        }
        if (nativeBufferStale) {
            sync();
        }
        return MemoryUtil.memAddress(nativeBuffer);
    }

    public void sync() {
        if (nativeBuffer == null) {
            throw new IllegalStateException("NGX resource is closed");
        }
        nativeBuffer.clear();
        if (type == NgxConstants.RESOURCE_VK_IMAGE_VIEW) {
            NgxImageSubresourceRange range = imageViewInfo.subresourceRange;
            nativeBuffer.putLong(0, imageViewInfo.imageView);
            nativeBuffer.putLong(8, imageViewInfo.image);
            nativeBuffer.putInt(16, range.aspectMask);
            nativeBuffer.putInt(20, range.baseMipLevel);
            nativeBuffer.putInt(24, range.levelCount);
            nativeBuffer.putInt(28, range.baseArrayLayer);
            nativeBuffer.putInt(32, range.layerCount);
            nativeBuffer.putInt(36, imageViewInfo.format);
            nativeBuffer.putInt(40, imageViewInfo.width);
            nativeBuffer.putInt(44, imageViewInfo.height);
        } else {
            nativeBuffer.putLong(0, bufferInfo.buffer);
            nativeBuffer.putInt(8, bufferInfo.sizeInBytes);
        }
        nativeBuffer.putInt(IMAGE_VIEW_INFO_SIZE, type);
        nativeBuffer.put(IMAGE_VIEW_INFO_SIZE + Integer.BYTES, (byte) (readWrite ? 1 : 0));
        nativeBufferStale = false;
    }

    @Override
    public void close() {
        if (nativeBuffer != null) {
            MemoryUtil.memFree(nativeBuffer);
            nativeBuffer = null;
        }
    }
}
