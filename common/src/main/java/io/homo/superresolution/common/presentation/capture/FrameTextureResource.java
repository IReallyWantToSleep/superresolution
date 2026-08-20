/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.presentation.capture;

import io.homo.superresolution.common.perf.PerformanceTracker;
import io.homo.superresolution.common.presentation.window.PresentationWindowState;
import io.homo.superresolution.common.upscale.InteropResourcesPreprocessor;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsages;
import io.homo.superresolution.core.graphics.opengl.GlState;
import io.homo.superresolution.core.graphics.opengl.texture.GlImportableTexture2D;
import io.homo.superresolution.core.graphics.vulkan.VkGlInteropSemaphore;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import io.homo.superresolution.core.graphics.vulkan.VulkanTexture;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.EXTSemaphore.GL_LAYOUT_TRANSFER_DST_EXT;
import static org.lwjgl.opengl.EXTSemaphore.GL_LAYOUT_TRANSFER_SRC_EXT;

final class FrameTextureResource {
    private final VulkanDevice device;
    private final String label;
    private VulkanTexture vkTexture;
    private GlImportableTexture2D glTexture;
    private VkGlInteropSemaphore ready;
    private VkGlInteropSemaphore release;
    private boolean ownsResources = true;
    private boolean valid;
    private boolean releasePending;

    FrameTextureResource(VulkanDevice device, String label) {
        this.device = device;
        this.label = label;
    }

    void beginFrame() {
        valid = false;
    }

    void copyFrom(ITexture source, boolean motionVector) {
        requireTexture2D(source);
        TextureFormat format = source.getTextureFormat().isDepth()
                ? TextureFormat.R32F
                : source.getTextureFormat();
        ensureOwned(source.getWidth(), source.getHeight(), format);
        awaitOwnedRelease();
        PerformanceTracker.push(PerformanceTracker.GL_CAPTURE_FLIP);
        try (GlState ignored = new GlState()) {
            if (motionVector) {
                InteropResourcesPreprocessor.flipMotionVectorY(source, glTexture);
            } else {
                InteropResourcesPreprocessor.flipY(source, glTexture);
            }
        } finally {
            PerformanceTracker.pop(PerformanceTracker.GL_CAPTURE_FLIP);
        }
        ready.signalVulkan(
                new int[]{Math.toIntExact(glTexture.handle())},
                new int[0],
                new int[]{GL_LAYOUT_TRANSFER_SRC_EXT}
        );
        vkTexture.setCurrentLayout(org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
        valid = true;
    }

    void borrow(
            VulkanTexture borrowedVkTexture,
            GlImportableTexture2D borrowedGlTexture,
            VkGlInteropSemaphore borrowedReady,
            VkGlInteropSemaphore borrowedRelease
    ) {
        if (borrowedVkTexture == null
                || borrowedGlTexture == null
                || borrowedReady == null
                || borrowedRelease == null) {
            return;
        }
        if (ownsResources) {
            awaitOwnedRelease();
            destroyOwned();
        }
        vkTexture = borrowedVkTexture;
        glTexture = borrowedGlTexture;
        ready = borrowedReady;
        release = borrowedRelease;
        ownsResources = false;
        valid = true;
    }

    void markSubmitted() {
        if (valid && ownsResources) {
            releasePending = true;
        }
    }

    void awaitOwnedRelease() {
        if (!releasePending || !ownsResources || release == null || glTexture == null) {
            return;
        }
        release.waitVulkanSignal(
                new int[]{Math.toIntExact(glTexture.handle())},
                new int[0],
                new int[]{GL_LAYOUT_TRANSFER_DST_EXT}
        );
        releasePending = false;
    }

    void destroy() {
        if (ownsResources) {
            awaitOwnedRelease();
            destroyOwned();
        } else {
            detachBorrowed();
        }
        valid = false;
    }

    boolean isValid() {
        return valid;
    }

    VulkanTexture vkTexture() {
        return valid ? vkTexture : null;
    }

    GlImportableTexture2D glTexture() {
        return valid ? glTexture : null;
    }

    long readySemaphore() {
        return ready.getVkSemaphoreHandle();
    }

    long releaseSemaphore() {
        return release.getVkSemaphoreHandle();
    }

    private void ensureOwned(int width, int height, TextureFormat format) {
        if (ownsResources
                && vkTexture != null
                && vkTexture.getWidth() == width
                && vkTexture.getHeight() == height
                && vkTexture.getTextureFormat() == format) {
            return;
        }
        if (ownsResources) {
            awaitOwnedRelease();
            destroyOwned();
        } else {
            detachBorrowed();
        }

        PresentationWindowState.requireOwnerThread();
        if (GLFW.glfwGetCurrentContext() != PresentationWindowState.renderHandle()) {
            throw new IllegalStateException("Capture texture creation requires the hidden OpenGL render context");
        }

        TextureDescription description = TextureDescription.create()
                .type(TextureType.Texture2D)
                .format(format)
                .size(width, height)
                .usages(TextureUsages.create()
                        .sampler()
                        .storage()
                        .attachmentColor()
                        .transferSource()
                        .transferDestination())
                .label(label)
                .build();
        vkTexture = device.createTextureExportable(description);
        glTexture = RenderSystems.opengl().device().createTextureImportable(vkTexture);
        ready = VkGlInteropSemaphore.create(device);
        release = VkGlInteropSemaphore.create(device);
        ownsResources = true;
    }

    private void destroyOwned() {
        GL11.glFinish();
        if (glTexture != null) {
            glTexture.destroy();
        }
        if (vkTexture != null) {
            vkTexture.destroy();
        }
        if (ready != null) {
            ready.destroy();
        }
        if (release != null) {
            release.destroy();
        }
        detachBorrowed();
    }

    private void detachBorrowed() {
        vkTexture = null;
        glTexture = null;
        ready = null;
        release = null;
        ownsResources = false;
        releasePending = false;
    }

    private static void requireTexture2D(ITexture source) {
        if (source == null || source.getTextureType() != TextureType.Texture2D) {
            throw new IllegalArgumentException("Frame capture only supports non-null 2D textures");
        }
    }
}
