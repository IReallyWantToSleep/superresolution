/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.core.gui.b3d;

#if MC_VER >= MC_26_2
import io.homo.superresolution.common.minecraft.B3DVulkanBridge;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.vulkan.VulkanCommandBuffer;
import io.homo.superresolution.thirdparty.nanovg.NanoVGRhiBridge;
import net.minecraft.client.Minecraft;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class B3DGuiVulkanUiRenderer {
    private static final List<B3DGuiFrameJob> PENDING_JOBS = new ArrayList<>();
    private static B3DGuiVulkanTarget target;
    private static B3DGuiPrimaryCommandRing commandRing;
    private static final List<B3DGuiVulkanTarget> DEFERRED_DESTROY_TARGETS = new ArrayList<>();
    private static int generation;

    private B3DGuiVulkanUiRenderer() {
    }

    public static boolean isAvailable() {
        return B3DVulkanBridge.isB3DVulkanBackend();
    }

    public static void register(B3DGuiFrameInput input) {
        if (!isAvailable()) {
            return;
        }
        PENDING_JOBS.add(new B3DGuiFrameJob(input));
    }

    public static boolean hasPendingJobs() {
        return !PENDING_JOBS.isEmpty();
    }

    public static void preRenderAll() {
        if (!isAvailable() || PENDING_JOBS.isEmpty()) {
            return;
        }
        if (!RenderSystems.initBorrowedB3DVulkanIfAvailable()) {
            PENDING_JOBS.clear();
            return;
        }
        ensureCommandRing();
        commandRing.reapCompleted();
        reapDeferredTargets();

        B3DGuiFrameJob job = PENDING_JOBS.get(PENDING_JOBS.size() - 1);
        PENDING_JOBS.clear();
        B3DGuiFrameInput input = job.input();
        ensureTarget(input.framebufferWidth(), input.framebufferHeight());
        if (target == null) {
            return;
        }

        B3DGuiPrimaryCommandRing.Slot slot = commandRing.acquire();
        if (slot == null) {
            return;
        }

        VulkanCommandBuffer commandBuffer = slot.commandBuffer();
        NanoVGRhiBridge.beginB3DVulkanBatch(RenderSystems.vulkan().device(), target.framebuffer(), commandBuffer, slot.index());
        input.screen().preRender(target, input);
        NanoVGRhiBridge.endB3DVulkanBatch();

        Object encoder = B3DVulkanBridge.createCommandEncoder();
        VkCommandBuffer nativeCommandBuffer = commandBuffer.getNativeCommandBuffer();
        B3DVulkanBridge.execute(encoder, nativeCommandBuffer);
        slot.markSubmitted(B3DVulkanBridge.createFence(encoder), target.generation());
    }

    public static void ensureTarget(int width, int height) {
        if (!RenderSystems.initBorrowedB3DVulkanIfAvailable()) {
            return;
        }
        ensureCommandRing();
        commandRing.reapCompleted();
        reapDeferredTargets();
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        if (target != null && target.width() == safeWidth && target.height() == safeHeight) {
            return;
        }
        destroyTarget();
        target = B3DGuiVulkanTarget.create(safeWidth, safeHeight, ++generation);
    }

    public static B3DGuiVulkanTarget target() {
        return target;
    }

    private static void ensureCommandRing() {
        if (commandRing == null) {
            commandRing = new B3DGuiPrimaryCommandRing();
        }
    }

    public static void destroy() {
        PENDING_JOBS.clear();
        destroyTarget();
        if (commandRing != null) {
            commandRing.close();
            commandRing = null;
        }
        for (B3DGuiVulkanTarget target : DEFERRED_DESTROY_TARGETS) {
            target.close();
        }
        DEFERRED_DESTROY_TARGETS.clear();
    }

    private static void destroyTarget() {
        if (target != null) {
            DEFERRED_DESTROY_TARGETS.add(target);
            target = null;
        }
    }

    private static void reapDeferredTargets() {
        if (commandRing == null || DEFERRED_DESTROY_TARGETS.isEmpty()) {
            return;
        }
        Iterator<B3DGuiVulkanTarget> iterator = DEFERRED_DESTROY_TARGETS.iterator();
        while (iterator.hasNext()) {
            B3DGuiVulkanTarget oldTarget = iterator.next();
            if (!commandRing.isGenerationInUse(oldTarget.generation())) {
                oldTarget.close();
                iterator.remove();
            }
        }
    }
}
#else
public final class B3DGuiVulkanUiRenderer {
    private B3DGuiVulkanUiRenderer() {
    }

    public static boolean isAvailable() {
        return false;
    }
}
#endif
