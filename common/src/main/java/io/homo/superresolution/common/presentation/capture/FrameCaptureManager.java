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

import io.homo.superresolution.api.InputResourceSet;
import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.api.event.AlgorithmDispatchEvent;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.presentation.vulkan.VulkanPresentationFeature;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.VulkanInteropAlgorithm;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.opengl.texture.GlImportableTexture2D;
import io.homo.superresolution.core.graphics.vulkan.VkGlInteropSemaphore;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import io.homo.superresolution.core.graphics.vulkan.VulkanTexture;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;

public final class FrameCaptureManager {
    private static final CaptureFrameRing FRAME_RING = new CaptureFrameRing();
    private static FrameResources lastFinishedFrame;
    private static boolean registered;

    private FrameCaptureManager() {
    }

    public static synchronized void initialize(VulkanDevice device) {
        if (!registered) {
            SuperResolutionAPI.EVENT_BUS.addListener(FrameCaptureManager::onAlgorithmDispatch);
            registered = true;
        }
        FRAME_RING.initialize(device);
    }

    public static boolean isInitialized() {
        return FRAME_RING.isInitialized();
    }

    public static void captureWorldColor() {
        if (!isInitialized() || !isWorldFrame()) {
            return;
        }
        ITexture color = originColorTexture();
        if (color != null) {
            beginFrame(RenderHandlerManager.getFrameCount()).copyWorldColor(color);
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
        beginFrame(RenderHandlerManager.getFrameCount()).copyFinalColor(color);
    }

    public static boolean captureVulkanInputs(
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
            return false;
        }
        FrameResources frame = beginFrame(logicalFrameIndex);
        frame.borrowDepth(depthVk, depthGl, depthReady, depthRelease);
        frame.borrowMotionVector(motionVk, motionGl, motionReady, motionRelease);
        return true;
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
        if (!VulkanPresentationFeature.isRequested()
                || !isInitialized()
                || event == null
                || event.getAlgorithm() instanceof VulkanInteropAlgorithm
                || !isWorldFrame()) {
            return;
        }
        DispatchResource dispatch = event.getDispatchResource();
        if (dispatch == null || dispatch.resources() == null) {
            return;
        }
        FrameResources frame = beginFrame(dispatch.frameCount());
        InputResourceSet resources = dispatch.resources();
        if (resources.depthTexture() != null) {
            frame.copyDepth(resources.depthTexture());
        }
        if (resources.motionVectorsTexture() != null) {
            frame.copyMotionVector(resources.motionVectorsTexture());
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
        #if MC_VER >= MC_26_1 && MC_VER < MC_26_2
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null
                || !minecraft.isGameLoadFinished()
                || minecraft.level == null
                || minecraft.screen != null) {
            return false;
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        return camera != null && camera.isInitialized() && !camera.isPanoramicMode();
        #else
        return false;
        #endif
    }
}
