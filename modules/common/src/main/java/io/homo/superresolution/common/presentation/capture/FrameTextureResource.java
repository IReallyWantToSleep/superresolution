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

package io.homo.superresolution.common.presentation.capture;

import io.homo.superresolution.common.perf.PerformanceTracker;
import io.homo.superresolution.common.presentation.window.PresentationWindowState;
import io.homo.superresolution.common.minecraft.B3DVulkanBridge;
import io.homo.superresolution.common.upscale.InteropResourcesPreprocessor;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.command.ResourceAccessType;
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
import io.homo.superresolution.core.graphics.vulkan.VulkanExternalTexture;
import io.homo.superresolution.core.graphics.vulkan.VulkanCommandBuffer;
import io.homo.superresolution.core.graphics.vulkan.VulkanResourceState;
import io.homo.superresolution.core.graphics.vulkan.VulkanTimestampProfiler;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkMemoryBarrier;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
#if MC_VER >= MC_26_3
import org.lwjgl.sdl.SDLVideo;
#else
import org.lwjgl.glfw.GLFW;
#endif
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.EXTSemaphore.GL_LAYOUT_TRANSFER_DST_EXT;
import static org.lwjgl.opengl.EXTSemaphore.GL_LAYOUT_TRANSFER_SRC_EXT;
import static org.lwjgl.vulkan.VK10.*;

final class FrameTextureResource {
    private static final int CAPTURE_FLIP_TEXTURE_UNITS = 1;

    private final VulkanDevice device;
    private final String label;
    private VulkanTexture vkTexture;
    private GlImportableTexture2D glTexture;
    private VkGlInteropSemaphore ready;
    private VkGlInteropSemaphore release;
    private boolean ownsResources = true;
    private boolean valid;
    private boolean releasePending;
    private boolean nativeVulkanOwned;

    FrameTextureResource(VulkanDevice device, String label) {
        this.device = device;
        this.label = label;
    }

    void beginFrame() {
        valid = false;
    }

    void copyFrom(ITexture source, boolean motionVector) {
        requireTexture2D(source);
        if (B3DVulkanBridge.isB3DVulkanBackend()) {
            copyNativeVulkan(source, motionVector);
            return;
        }
        TextureFormat format = source.getTextureFormat().isDepth()
                ? TextureFormat.R32F
                : source.getTextureFormat();
        ensureOwned(source.getWidth(), source.getHeight(), format);
        awaitOwnedRelease();
        PerformanceTracker.push(PerformanceTracker.GL_CAPTURE_FLIP);
        try (GlState ignored = new GlState(
                GlState.STATE_PROGRAM | GlState.STATE_ACTIVE_TEXTURE | GlState.STATE_TEXTURES,
                CAPTURE_FLIP_TEXTURE_UNITS)) {
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
        if (valid && ownsResources && !nativeVulkanOwned) {
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
        return ready == null ? 0L : ready.getVkSemaphoreHandle();
    }

    long releaseSemaphore() {
        return release == null ? 0L : release.getVkSemaphoreHandle();
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

        boolean nativeVulkan = B3DVulkanBridge.isB3DVulkanBackend();
        if (!nativeVulkan) {
            PresentationWindowState.requireOwnerThread();
            #if MC_VER >= MC_26_3
            if (SDLVideo.SDL_GL_GetCurrentWindow() != PresentationWindowState.renderHandle()) {
            #else
            if (GLFW.glfwGetCurrentContext() != PresentationWindowState.renderHandle()) {
            #endif
                throw new IllegalStateException("Capture texture creation requires the hidden OpenGL render context");
            }
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
        if (nativeVulkan) {
            vkTexture = (VulkanTexture) device.createTexture(description);
        } else {
            vkTexture = device.createTextureExportable(description);
            glTexture = RenderSystems.opengl().device().createTextureImportable(vkTexture);
            ready = VkGlInteropSemaphore.create(device);
            release = VkGlInteropSemaphore.create(device);
        }
        ownsResources = true;
        nativeVulkanOwned = nativeVulkan;
    }

    private void destroyOwned() {
        if (glTexture != null) {
            GL11.glFinish();
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
        nativeVulkanOwned = false;
    }

    private void copyNativeVulkan(ITexture source, boolean motionVector) {
        requireNativeVulkanSource(source);
        ensureOwned(source.getWidth(), source.getHeight(), nativeCaptureFormat(source));
        B3DVulkanBridge.recordCommands(commands -> {
            VulkanCommandBuffer vkCommands = (VulkanCommandBuffer) commands;
            int gpuRegion = beginCaptureGpuRegion(vkCommands);
            // Minecraft keeps its images in GENERAL and owns their state tracking.
            // Cover game writes before the read and return the read dependency to it.
            nativeMemoryBarrier(vkCommands);
            if (source.getTextureFormat().isDepth()) {
                InteropResourcesPreprocessor.processInputTextures(commands,
                        null, null, source, vkTexture, null, null, null, null, null, true);
            } else if (motionVector) {
                InteropResourcesPreprocessor.processInputTextures(commands,
                        null, null, null, null, source, vkTexture, null, null, null, true);
            } else {
                InteropResourcesPreprocessor.processInputTextures(commands,
                        source, vkTexture, null, null, null, null, null, null, null, true);
            }
            restoreSnapshotForConsumers(vkCommands);
            nativeMemoryBarrier(vkCommands);
            endCaptureGpuRegion(vkCommands, gpuRegion);
        });
        valid = true;
    }

    /**
     * Captures depth and motion vectors on Minecraft's Vulkan device in one compute pass, so the
     * pair costs one dispatch and one set of barriers in Minecraft's frame instead of two. Each
     * output is written exactly as {@link #copyFrom} would write it.
     */
    static void copyNativeVulkanDepthAndMotionVector(
            FrameTextureResource depth, ITexture depthSource,
            FrameTextureResource motionVector, ITexture motionVectorSource
    ) {
        requireTexture2D(depthSource);
        requireTexture2D(motionVectorSource);
        requireNativeVulkanSource(depthSource);
        requireNativeVulkanSource(motionVectorSource);
        depth.ensureOwned(depthSource.getWidth(), depthSource.getHeight(), nativeCaptureFormat(depthSource));
        motionVector.ensureOwned(motionVectorSource.getWidth(), motionVectorSource.getHeight(),
                nativeCaptureFormat(motionVectorSource));
        // A depth-format source goes through the depth slot (R32F); any other format through the
        // color slot, which keeps it, as copyNativeVulkan does.
        boolean depthFormat = depthSource.getTextureFormat().isDepth();
        B3DVulkanBridge.recordCommands(commands -> {
            VulkanCommandBuffer vkCommands = (VulkanCommandBuffer) commands;
            int gpuRegion = beginCaptureGpuRegion(vkCommands);
            nativeMemoryBarrier(vkCommands);
            InteropResourcesPreprocessor.processInputTextures(commands,
                    depthFormat ? null : depthSource, depthFormat ? null : depth.vkTexture,
                    depthFormat ? depthSource : null, depthFormat ? depth.vkTexture : null,
                    motionVectorSource, motionVector.vkTexture,
                    null, null, null, true);
            depth.restoreSnapshotForConsumers(vkCommands);
            motionVector.restoreSnapshotForConsumers(vkCommands);
            nativeMemoryBarrier(vkCommands);
            endCaptureGpuRegion(vkCommands, gpuRegion);
        });
        depth.valid = true;
        motionVector.valid = true;
    }

    private static TextureFormat nativeCaptureFormat(ITexture source) {
        return source.getTextureFormat().isDepth()
                ? TextureFormat.R32F : nativeStorageFormat(source.getTextureFormat());
    }

    private static void requireNativeVulkanSource(ITexture source) {
        if (!(source instanceof VulkanTexture) && !(source instanceof VulkanExternalTexture)) {
            throw new IllegalArgumentException("Minecraft Vulkan capture requires a Vulkan texture");
        }
    }

    private static int beginCaptureGpuRegion(VulkanCommandBuffer commands) {
        VulkanTimestampProfiler profiler = commands.getDevice() instanceof VulkanDevice vulkanDevice
                ? vulkanDevice.timestampProfiler() : null;
        return profiler == null ? -1
                : profiler.beginRegionAfterPriorWork(commands.getNativeCommandBuffer(), PerformanceTracker.VK_FRAME_CAPTURE);
    }

    private static void endCaptureGpuRegion(VulkanCommandBuffer commands, int slot) {
        if (slot < 0 || !(commands.getDevice() instanceof VulkanDevice vulkanDevice)) {
            return;
        }
        VulkanTimestampProfiler profiler = vulkanDevice.timestampProfiler();
        if (profiler != null) {
            profiler.endRegion(commands.getNativeCommandBuffer(), slot);
        }
    }

    /**
     * The capture writes through a storage image. On Minecraft's Vulkan device that is limited
     * to the core storage formats, because Minecraft does not enable
     * {@code shaderStorageImageExtendedFormats}. RGBA16 (UNORM) is not core, and an HDR output
     * mod's PQ frame arrives in it. RGBA16F is core and resolves [0, 1] in 11-bit steps, finer
     * than the 10-bit window that frame is shown in.
     */
    private static TextureFormat nativeStorageFormat(TextureFormat format) {
        return format == TextureFormat.RGBA16 ? TextureFormat.RGBA16F : format;
    }

    private void restoreSnapshotForConsumers(VulkanCommandBuffer commands) {
        VulkanResourceState state = vkTexture.getCurrentResourceState();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .srcAccessMask(state.accessMask())
                    .dstAccessMask(VK_ACCESS_MEMORY_READ_BIT)
                    .oldLayout(state.layout())
                    .newLayout(VK_IMAGE_LAYOUT_GENERAL)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .image(vkTexture.handle());
            barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .baseMipLevel(0).levelCount(1).baseArrayLayer(0).layerCount(1);
            vkCmdPipelineBarrier(commands.getNativeCommandBuffer(), state.stageMask(),
                    VK_PIPELINE_STAGE_ALL_COMMANDS_BIT, 0, null, null, barrier);
        }
        vkTexture.setCurrentResourceState(new VulkanResourceState(VK_IMAGE_LAYOUT_GENERAL,
                VK_ACCESS_MEMORY_READ_BIT, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT,
                ResourceAccessType.STORAGE_READ));
    }

    private static void nativeMemoryBarrier(VulkanCommandBuffer commands) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkMemoryBarrier.Buffer barrier = VkMemoryBarrier.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_BARRIER)
                    .srcAccessMask(VK_ACCESS_MEMORY_READ_BIT | VK_ACCESS_MEMORY_WRITE_BIT)
                    .dstAccessMask(VK_ACCESS_MEMORY_READ_BIT | VK_ACCESS_MEMORY_WRITE_BIT);
            vkCmdPipelineBarrier(commands.getNativeCommandBuffer(),
                    VK_PIPELINE_STAGE_ALL_COMMANDS_BIT, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT,
                    0, barrier, null, null);
        }
    }

    private static void requireTexture2D(ITexture source) {
        if (source == null || source.getTextureType() != TextureType.Texture2D) {
            throw new IllegalArgumentException("Frame capture only supports non-null 2D textures");
        }
    }
}
