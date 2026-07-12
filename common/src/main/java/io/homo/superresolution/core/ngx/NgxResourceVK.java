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

    public long nativeAddress() {
        if (nativeBuffer == null) {
            throw new IllegalStateException("NGX resource is closed");
        }
        sync();
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
    }

    @Override
    public void close() {
        if (nativeBuffer != null) {
            MemoryUtil.memFree(nativeBuffer);
            nativeBuffer = null;
        }
    }
}
