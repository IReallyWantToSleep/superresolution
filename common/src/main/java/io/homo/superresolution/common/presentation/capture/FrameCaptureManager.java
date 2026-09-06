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

import io.homo.superresolution.api.InputResourceSet;
import io.homo.superresolution.api.InputResourceType;
import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.api.event.AlgorithmDispatchEvent;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.minecraft.GameFrameIndex;
import io.homo.superresolution.common.minecraft.MinecraftUtils;
import io.homo.superresolution.common.presentation.vulkan.FramePacingTiming;
import io.homo.superresolution.common.presentation.PresentationBackendManager;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.interoplayer.GlVulkanInteropAlgorithm;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.opengl.texture.GlImportableTexture2D;
import io.homo.superresolution.core.graphics.vulkan.VkGlInteropSemaphore;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import io.homo.superresolution.core.graphics.vulkan.VulkanTexture;
import net.minecraft.client.Minecraft;

public final class FrameCaptureManager {
    private static final CaptureFrameRing FRAME_RING = new CaptureFrameRing();
    private static volatile FrameResources lastFinishedFrame;
    private static boolean registered;

    private FrameCaptureManager() {
    }

    public static synchronized void initialize(
            VulkanDevice device,
            FramePacingTiming framePacingTiming
    ) {
        if (!registered) {
            SuperResolutionAPI.EVENT_BUS.addListener(FrameCaptureManager::onAlgorithmDispatch);
            registered = true;
        }
        FRAME_RING.initialize(device, framePacingTiming);
    }

    public static boolean isInitialized() {
        return FRAME_RING.isInitialized();
    }

    public static void captureHudlessColor() {
        if (!isInitialized() || !isWorldFrame()) {
            return;
        }
        FrameResources frame = FRAME_RING.activeFrameOrNull();
        ITexture color = originColorTexture();
        if (frame != null && color != null) {
            frame.copyHudlessColor(color);
        }
    }

    public static void captureFinalColor() {
        if (!isInitialized()) {
            return;
        }
        ITexture color = originColorTexture();
        if (color == null) {
            throw new IllegalStateException("Vulkan presentation final color is unavailable");
        }
        beginFrame(GameFrameIndex.current()).copyFinalColor(color);
    }

    public static FrameResources captureVulkanInputs(
            int logicalFrameIndex,
            VulkanTexture depthVk,
            GlImportableTexture2D depthGl,
            VkGlInteropSemaphore depthReady,
            VkGlInteropSemaphore depthRelease,
            VulkanTexture motionVk,
            GlImportableTexture2D motionGl,
            VkGlInteropSemaphore motionReady,
            VkGlInteropSemaphore motionRelease
    ) {
        if (!isInitialized() || !isWorldFrame()) {
            return null;
        }
        FrameResources frame = beginFrame(logicalFrameIndex);
        frame.borrowDepth(depthVk, depthGl, depthReady, depthRelease);
        frame.borrowMotionVector(motionVk, motionGl, motionReady, motionRelease);
        return frame;
    }

    public static FrameResources finishFrame() {
        if (!isInitialized()) {
            return null;
        }
        FrameResources frame = FRAME_RING.finishFrame();
        lastFinishedFrame = frame;
        return frame;
    }

    public static FrameResources peekCurrentFrame() {
        FrameResources active = FRAME_RING.activeFrameOrNull();
        if (active != null) {
            return active;
        }
        return lastFinishedFrame;
    }

    public static synchronized void shutdown() {
        if (FRAME_RING.isInitialized()) {
            FRAME_RING.destroy();
        }
    }

    private static void onAlgorithmDispatch(AlgorithmDispatchEvent event) {
        if (!PresentationBackendManager.isVulkanPresentationRequested()
                || !isInitialized()
                || event == null
                || event.getAlgorithm() instanceof GlVulkanInteropAlgorithm
                || !isWorldFrame()) {
            return;
        }
        DispatchResource dispatch = event.getDispatchResource();
        if (dispatch == null || dispatch.resources() == null) {
            return;
        }
        FrameResources frame = beginFrame(dispatch.frameCount());
        InputResourceSet resources = dispatch.resources();
        if (resources.has(InputResourceType.Depth)) {
            frame.copyDepth(resources.get(InputResourceType.Depth));
        }
        if (resources.has(InputResourceType.MotionVectors)) {
            frame.copyMotionVector(resources.get(InputResourceType.MotionVectors));
        }
    }

    private static FrameResources beginFrame(int logicalFrameIndex) {
        lastFinishedFrame = null;
        return FRAME_RING.beginFrame(logicalFrameIndex);
    }

    private static ITexture originColorTexture() {
        IFrameBuffer framebuffer = SuperResolutionAPI.getOriginMinecraftFrameBuffer();
        return framebuffer == null ? null : framebuffer.getTexture(FrameBufferAttachmentType.Color);
    }

    private static boolean isWorldFrame() {
        #if MC_VER >= MC_26_1 && MC_VER <= MC_26_2 || MC_VER >= MC_1_21_11 && MC_VER < MC_26_1 || MC_VER >= MC_1_21 && MC_VER < MC_1_21_2  || MC_VER == MC_1_20_1
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null
                || !SuperResolution.gameIsLoaded
                || minecraft.level == null) {
            return false;
        }
        if (MinecraftUtils.getCamera() == null
                || !MinecraftUtils.getCamera().isInitialized()) {
            return false;
        }
        #if MC_VER >= MC_26_1
        return !MinecraftUtils.getCamera().isPanoramicMode();
        #else
        return true;
        #endif
        #else
        return false;
        #endif
    }
}
