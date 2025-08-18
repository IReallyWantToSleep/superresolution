package io.homo.superresolution.core.graphics.impl.framebuffer;

import io.homo.superresolution.common.minecraft.MinecraftRenderTargetWrapper;
import io.homo.superresolution.core.graphics.impl.texture.*;

public class FrameBufferTextureAdapter implements ITexture {
    private final IFrameBuffer frameBuffer;
    private final FrameBufferAttachmentType attachmentType;

    FrameBufferTextureAdapter(IFrameBuffer frameBuffer, FrameBufferAttachmentType attachmentType) {
        this.frameBuffer = frameBuffer;
        this.attachmentType = attachmentType;
    }

    public static FrameBufferTextureAdapter ofColor(IFrameBuffer frameBuffer) {
        return of(frameBuffer, FrameBufferAttachmentType.Color);
    }

    public static FrameBufferTextureAdapter ofDepth(IFrameBuffer frameBuffer) {
        return of(frameBuffer, FrameBufferAttachmentType.AnyDepth);
    }

    public static FrameBufferTextureAdapter of(IFrameBuffer frameBuffer, FrameBufferAttachmentType attachmentType) {
        return new FrameBufferTextureAdapter(frameBuffer, attachmentType);
    }


    @Override
    public long handle() {
        return frameBuffer.getTextureId(attachmentType);
    }


    @Override
    public TextureFormat getTextureFormat() {
        return attachmentType.equals(FrameBufferAttachmentType.Color) ? frameBuffer.getColorTextureFormat() : frameBuffer.getDepthTextureFormat();
    }

    @Override
    public TextureUsages getTextureUsages() {
        if (frameBuffer instanceof MinecraftRenderTargetWrapper) {
            return TextureUsages.create().storage().sampler();
        }
        return frameBuffer.getTexture(attachmentType).getTextureUsages();
    }

    @Override
    public TextureType getTextureType() {
        if (frameBuffer instanceof MinecraftRenderTargetWrapper) {
            return TextureType.Texture2D;
        }
        return frameBuffer.getTexture(attachmentType).getTextureType();
    }

    @Override
    public TextureFilterMode getTextureFilterMode() {
        if (frameBuffer instanceof MinecraftRenderTargetWrapper) {
            return TextureFilterMode.NEAREST;
        }
        return frameBuffer.getTexture(attachmentType).getTextureFilterMode();
    }

    @Override
    public TextureWrapMode getTextureWrapMode() {
        if (frameBuffer instanceof MinecraftRenderTargetWrapper) {
            return TextureWrapMode.CLAMP_TO_EDGE;
        }
        return frameBuffer.getTexture(attachmentType).getTextureWrapMode();
    }

    @Override
    public TextureMipmapSettings getMipmapSettings() {
        if (frameBuffer instanceof MinecraftRenderTargetWrapper) {
            return TextureMipmapSettings.disabled();
        }
        return frameBuffer.getTexture(attachmentType).getMipmapSettings();
    }

    @Override
    public TextureDescription getTextureDescription() {
        return TextureDescription.create()
                .filterMode(getTextureFilterMode())
                .format(getTextureFormat())
                .size(getWidth(), getHeight())
                .type(getTextureType())
                .wrapMode(getTextureWrapMode())
                .mipmapSettings(getMipmapSettings())
                .usages(getTextureUsages())
                .build();
    }

    @Override
    public int getWidth() {
        return frameBuffer.getWidth();
    }

    @Override
    public int getHeight() {
        return frameBuffer.getHeight();
    }

    @Override
    public void destroy() {
        throw new UnsupportedOperationException("This method is not implemented yet.");
    }

    @Override
    public void resize(int width, int height) {
        throw new UnsupportedOperationException("This method is not implemented yet.");
    }
}
