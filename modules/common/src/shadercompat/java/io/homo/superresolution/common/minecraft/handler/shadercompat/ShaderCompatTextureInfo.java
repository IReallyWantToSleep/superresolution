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

package io.homo.superresolution.common.minecraft.handler.shadercompat;

import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsages;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
#if IS_VULKAN == 1
import io.homo.superresolution.common.minecraft.B3DVulkanBridge;
#endif

import java.util.List;
import java.util.function.Supplier;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL43.glCopyImageSubData;

public class ShaderCompatTextureInfo {
    private final Supplier<ITexture> sourceTextureSupplier;
    private final TextureRegion region;
    private final boolean isOutput;
    private final String name;
    #if IS_VULKAN == 1
    private ITexture internalTexture;
    private ITexture processedTexture;
    #else
    private GlTexture2D internalTexture;
    private GlTexture2D processedTexture;
    #endif

    public ShaderCompatTextureInfo(Supplier<ITexture> sourceTextureSupplier, TextureRegion region, boolean isOutput, String name) {
        this.sourceTextureSupplier = sourceTextureSupplier;
        this.region = region;
        this.isOutput = isOutput;
        this.name = name;
    }

    public static int resolveRegionValue(int value, boolean isWidth) {
        if (value == -1) return isWidth ?
                RenderHandlerManager.getRenderWidth() :
                RenderHandlerManager.getRenderHeight();
        if (value == -2) return isWidth ?
                RenderHandlerManager.getScreenWidth() :
                RenderHandlerManager.getScreenHeight();
        return value;
    }

    public ITexture getSourceTexture() {
        // Iris can replace a buffer's view without replacing the renderer or pass.
        return sourceTextureSupplier.get();
    }

    public int[] getResolvedRegion() {
        ITexture sourceTexture = getSourceTexture();
        if (sourceTexture == null) {
            throw new IllegalStateException("Shader compatibility input is unavailable: " + name);
        }
        return region.resolveChecked(RenderHandlerManager.getRenderSize(), RenderHandlerManager.getScreenSize(),
                sourceTexture.getWidth(), sourceTexture.getHeight());
    }

    public ITexture getInternalTexture() {
        if (!isOutput && canUseSourceTextureDirectly()) {
            return getSourceTexture();
        }
        return internalTexture;
    }

    public ITexture getPreProcessInputTexture() {
        if (canUseSourceTextureDirectly()) {
            return getSourceTexture();
        }
        return getInternalTexture();
    }

    public ITexture getPreProcessOutputTexture() {
        ensureProcessedTexture();
        return processedTexture;
    }

    public boolean canUseSourceTextureDirectly() {
        ITexture sourceTexture = getSourceTexture();
        if (sourceTexture == null) {
            return false;
        }
        int[] resolvedRegion = getResolvedRegion();
        return resolvedRegion[0] == 0 &&
                resolvedRegion[1] == 0 &&
                resolvedRegion[2] == sourceTexture.getWidth() &&
                resolvedRegion[3] == sourceTexture.getHeight();
    }

    public ITexture getAlgorithmTexture(boolean forceInternalTexture) {
        if (forceInternalTexture && processedTexture != null) {
            return processedTexture;
        }
        if (!forceInternalTexture && canUseSourceTextureDirectly()) {
            return getSourceTexture();
        }
        return getInternalTexture();
    }

    public void updateTexture() {
        ITexture sourceTexture = getSourceTexture();
        if (sourceTexture == null) return;

        int[] resolvedRegion = getResolvedRegion();
        int width = resolvedRegion[2];
        int height = resolvedRegion[3];
        int sourceX = resolvedRegion[0];
        int sourceY = resolvedRegion[1];

        if (!isOutput && canUseSourceTextureDirectly()) {
            return;
        }

        if (internalTexture == null) {
            createInternalTexture(width, height);
        } else if (internalTexture.getWidth() != width ||
                internalTexture.getHeight() != height ||
                internalTexture.getTextureFormat() != sourceTexture.getTextureFormat()) {
            awaitOwnedTextureUsers();
            internalTexture.destroy();
            destroyProcessedTexture();
            createInternalTexture(width, height);
        }

        if (isOutput) {
            copyTextureRegion(
                    internalTexture, 0, 0, width, height,
                    sourceTexture, sourceX, sourceY
            );
        } else if (!canUseSourceTextureDirectly()) {
            copyTextureRegion(
                    sourceTexture, sourceX, sourceY, width, height,
                    internalTexture, 0, 0
            );
        }
    }

    public void createInternalTexture(int width, int height) {
        ITexture sourceTexture = getSourceTexture();
        if (sourceTexture == null) return;
        internalTexture = createTexture(width, height, "SRIrisCompatInternalTexture-%s".formatted(this.name));
    }

    private void ensureProcessedTexture() {
        ITexture sourceTexture = getSourceTexture();
        if (sourceTexture == null) return;
        int[] resolvedRegion = getResolvedRegion();
        int width = resolvedRegion[2];
        int height = resolvedRegion[3];
        if (processedTexture == null) {
            processedTexture = createTexture(width, height, "SRIrisCompatProcessedTexture-%s".formatted(this.name));
        } else if (processedTexture.getWidth() != width ||
                processedTexture.getHeight() != height ||
                processedTexture.getTextureFormat() != sourceTexture.getTextureFormat()) {
            awaitOwnedTextureUsers();
            processedTexture.destroy();
            processedTexture = createTexture(width, height, "SRIrisCompatProcessedTexture-%s".formatted(this.name));
        }
    }

    #if IS_VULKAN == 1
    private ITexture createTexture(int width, int height, String label) {
    #else
    private GlTexture2D createTexture(int width, int height, String label) {
    #endif
        ITexture sourceTexture = getSourceTexture();
        if (sourceTexture == null) return null;
        #if IS_VULKAN == 1
        TextureUsages usages = TextureUsages.create().sampler().transferSource().transferDestination();
        if (sourceTexture.getTextureFormat().isDepth()) {
            usages.attachmentDepth();
        } else {
            usages.storage();
        }
        return io.homo.superresolution.core.RenderSystems.current().device().createTexture(
                TextureDescription.create()
                        .width(width).height(height)
                        .type(TextureType.Texture2D).mipmapsDisabled()
                        .usages(usages)
                        .format(sourceTexture.getTextureFormat()).label(label).build());
        #else
        return GlTexture2D.create(
                TextureDescription.create()
                        .width(width)
                        .height(height)
                        .type(TextureType.Texture2D)
                        .mipmapsDisabled()
                        .usages(TextureUsages.create().sampler().storage().transferSource().transferDestination())
                        .format(sourceTexture.getTextureFormat())
                        .label(label)
                        .build()
        );
        #endif
    }

    #if IS_VULKAN == 1
    public void replaceInternalTexture(ITexture newTexture) {
    #else
    public void replaceInternalTexture(GlTexture2D newTexture) {
    #endif
        awaitOwnedTextureUsers();
        if (internalTexture != null) {
            internalTexture.destroy();
        }
        destroyProcessedTexture();
        this.internalTexture = newTexture;
    }

    public void destroy() {
        awaitOwnedTextureUsers();
        if (internalTexture != null) {
            internalTexture.destroy();
            internalTexture = null;
        }
        destroyProcessedTexture();
    }

    private void awaitOwnedTextureUsers() {
        #if IS_VULKAN == 1
        if ((internalTexture != null || processedTexture != null) && B3DVulkanBridge.isB3DVulkanBackend()) {
            B3DVulkanBridge.awaitIdle();
        }
        #endif
    }

    private void destroyProcessedTexture() {
        if (processedTexture != null) {
            processedTexture.destroy();
            processedTexture = null;
        }
    }

    public void copyTextureRegion(
            ITexture src, int srcX, int srcY, int srcWidth, int srcHeight,
            ITexture dest, int destX, int destY
    ) {
        #if IS_VULKAN == 1
        if (B3DVulkanBridge.isB3DVulkanBackend()) {
            B3DVulkanBridge.recordCommands(commandBuffer -> commandBuffer.copyTexture(
                    src, dest, srcX, srcY, srcX + srcWidth, srcY + srcHeight, 0,
                    destX, destY, destX + srcWidth, destY + srcHeight, 0));
            return;
        }
        #endif
        glCopyImageSubData(
                (int) src.handle(), GL_TEXTURE_2D, 0,
                srcX, srcY, 0,
                (int) dest.handle(), GL_TEXTURE_2D, 0,
                destX, destY, 0,
                srcWidth, srcHeight, 1
        );
    }
}
