/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
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

package io.homo.superresolution.core.graphics.vulkan;

import io.homo.superresolution.core.graphics.impl.framebuffer.*;
import io.homo.superresolution.core.graphics.impl.texture.*;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;

public class VulkanFramebuffer implements IFrameBuffer {
    private final VulkanDevice device;
    private final ITexture colorAttachment;
    private final ITexture depthAttachment;
    private final int width;
    private final int height;
    private String label;

    public VulkanFramebuffer(VulkanDevice device, FramebufferDescription description) {
        this.device = device;
        this.width = description.getWidth();
        this.height = description.getHeight();
        this.label = description.getLabel();

        if (description.getColorAttachment() != null) {
            this.colorAttachment = description.getColorAttachment();
        } else if (description.getColorFormat() != null) {
            this.colorAttachment = device.createTexture(
                    TextureDescription.create()
                            .type(TextureType.Texture2D)
                            .format(description.getColorFormat())
                            .size(width, height)
                            .usages(TextureUsages.create()
                                    .storage().sampler().attachmentColor())
                            .build()
            );
        } else {
            this.colorAttachment = null;
        }

        if (description.getDepthAttachment() != null) {
            this.depthAttachment = description.getDepthAttachment();
        } else if (description.getDepthFormat() != null) {
            this.depthAttachment = device.createTexture(
                    TextureDescription.create()
                            .type(TextureType.Texture2D)
                            .format(description.getDepthFormat())
                            .size(width, height)
                            .usages(TextureUsages.create()
                                    .storage().sampler().attachmentDepth())
                            .build()
            );
        } else {
            this.depthAttachment = null;
        }
    }

    public long resolveColorImageView() {
        if (colorAttachment == null) {
            return VK_NULL_HANDLE;
        }
        return resolveAttachmentView(colorAttachment);
    }

    public long resolveDepthImageView() {
        if (depthAttachment == null) {
            return VK_NULL_HANDLE;
        }
        return resolveAttachmentView(depthAttachment);
    }

    public long resolveColorImage() {
        if (colorAttachment == null) {
            return VK_NULL_HANDLE;
        }
        return resolveImage(colorAttachment);
    }

    public long resolveDepthImage() {
        if (depthAttachment == null) {
            return VK_NULL_HANDLE;
        }
        return resolveImage(depthAttachment);
    }

    private long resolveAttachmentView(ITexture textureOrView) {
        if (textureOrView instanceof VulkanTextureView vtv) {
            return vtv.handle();
        }
        if (textureOrView instanceof VulkanTexture vt) {
            return vt.getImageView();
        }
        if (textureOrView instanceof VulkanExternalTexture vet) {
            return vet.getImageView();
        }
        throw new IllegalArgumentException("Cannot resolve attachment view from: " + textureOrView.getClass());
    }

    private long resolveImage(ITexture textureOrView) {
        if (textureOrView instanceof VulkanTextureView vtv) {
            return ((VulkanTexture) vtv.getParent()).handle();
        }
        if (textureOrView instanceof VulkanTexture vt) {
            return vt.handle();
        }
        if (textureOrView instanceof VulkanExternalTexture vet) {
            return vet.handle();
        }
        throw new IllegalArgumentException("Cannot resolve image from: " + textureOrView.getClass());
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void clearFrameBuffer() {
        throw new UnsupportedOperationException("典型的OpenGL思维");
    }

    @Override
    public List<ColorAttachment> getColorAttachments() {
        List<ColorAttachment> list = new ArrayList<>();
        if (colorAttachment != null) {
            list.add(new ColorAttachment(0, colorAttachment));
        }
        return list;
    }

    @Override
    public DepthStencilAttachment getDepthStencilAttachment() {
        if (depthAttachment != null) {
            return new DepthStencilAttachment(depthAttachment);
        }
        return null;
    }

    @Override
    public int getTextureId(FrameBufferAttachmentType attachmentType) {
        return (int) switch (attachmentType) {
            case Color -> colorAttachment != null ? colorAttachment.handle() : -1;
            case Depth -> depthAttachment != null ? depthAttachment.handle() : -1;
            case DepthStencil -> depthAttachment != null ? depthAttachment.handle() : -1;
            case AnyDepth -> depthAttachment != null ? depthAttachment.handle() : -1;
        };
    }

    @Override
    public ITexture getTexture(FrameBufferAttachmentType attachmentType) {
        return switch (attachmentType) {
            case Color -> colorAttachment;
            case Depth, DepthStencil, AnyDepth -> depthAttachment;
        };
    }

    @Override
    public void setClearColorRGBA(float red, float green, float blue, float alpha) {
        throw new UnsupportedOperationException("典型的OpenGL思维");
    }

    @Override
    public TextureFormat getColorTextureFormat() {
        return colorAttachment != null ? colorAttachment.getTextureFormat() : null;
    }

    @Override
    public TextureFormat getDepthTextureFormat() {
        return depthAttachment != null ? depthAttachment.getTextureFormat() : null;
    }

    @Override
    public void label(String label) {
        this.label = label;
    }

    @Override
    public long handle() {
        return VK_NULL_HANDLE;
    }

    @Override
    public void destroy() {
    }

    public ITexture getColorAttachmentTexture() {
        return colorAttachment;
    }

    public ITexture getDepthAttachmentTexture() {
        return depthAttachment;
    }
}
