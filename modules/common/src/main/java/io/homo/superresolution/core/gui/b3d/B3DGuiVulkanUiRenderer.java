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

package io.homo.superresolution.core.gui.b3d;

#if MC_VER >= MC_26_2
import io.homo.superresolution.common.minecraft.B3DVulkanBridge;
import io.homo.superresolution.common.minecraft.MinecraftUtils;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.vulkan.VulkanCommandBuffer;
import io.homo.superresolution.thirdparty.nanovg.NanoVGRhiBridge;
#if MC_VER >= MC_26_3
import com.mojang.renderpearl.api.commands.GpuFence;
#else
import com.mojang.blaze3d.buffers.GpuFence;
#endif

public final class B3DGuiVulkanUiRenderer {
    private static B3DGuiFrameInput pendingInput;
    private static B3DGuiVulkanTarget target;
    private static B3DGuiPrimaryCommandRing commandRing;
    private static boolean pendingSubmission;

    private B3DGuiVulkanUiRenderer() {
    }

    public static boolean isAvailable() {
        return B3DVulkanBridge.isB3DVulkanBackend();
    }

    public static void register(B3DGuiFrameInput input) {
        if (!isAvailable()) {
            return;
        }
        pendingInput = input;
    }

    public static boolean hasPendingJobs() {
        return pendingInput != null;
    }

    public static void preRenderAll() {
        if (!isAvailable() || pendingInput == null) {
            return;
        }
        if (!RenderSystems.initBorrowedB3DVulkanIfAvailable()) {
            pendingInput = null;
            return;
        }
        ensureCommandRing();
        commandRing.reapCompleted();

        B3DGuiFrameInput input = pendingInput;
        pendingInput = null;
        ensureTarget(input.framebufferWidth(), input.framebufferHeight());
        if (target == null) {
            return;
        }

        B3DGuiPrimaryCommandRing.Slot slot = commandRing.acquire();
        if (slot == null) {
            return;
        }

        pendingSubmission = true;
        target.copyBackgroundFrom(MinecraftUtils.getMainRenderTarget());

        VulkanCommandBuffer commandBuffer = slot.commandBuffer();
        commandBuffer.begin();
        try {
            NanoVGRhiBridge.beginFrame(target.framebuffer(), commandBuffer, true);
            try {
                input.screen().preRender(target, input);
            } finally {
                NanoVGRhiBridge.endFrame();
            }
            commandBuffer.end();
        } catch (RuntimeException | Error failure) {
            NanoVGRhiBridge.cancelFrame();
            slot.discard();
            throw failure;
        }

        Object encoder = B3DVulkanBridge.createCommandEncoder();
        GpuFence fence = B3DVulkanBridge.createFence(encoder);
        // Keep the lease if execute() fails after appending the buffer to the game submission.
        slot.markSubmitted(fence);
        pendingSubmission = true;
        B3DVulkanBridge.execute(encoder, commandBuffer.getNativeCommandBuffer());
    }

    public static void ensureTarget(int width, int height) {
        if (!RenderSystems.initBorrowedB3DVulkanIfAvailable()) {
            return;
        }
        ensureCommandRing();
        commandRing.reapCompleted();
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        if (target != null && target.width() == safeWidth && target.height() == safeHeight) {
            return;
        }
        destroyTarget();
        target = B3DGuiVulkanTarget.create(safeWidth, safeHeight);
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
        pendingInput = null;
        if (commandRing != null) {
            waitForIdle();
        }
        destroyTarget();
        if (commandRing != null) {
            commandRing.close();
            commandRing = null;
        }
    }

    private static void destroyTarget() {
        if (target != null) {
            waitForIdle();
            NanoVGRhiBridge.setTargetFramebuffer(null);
            target.close();
            target = null;
        }
    }

    public static void waitForIdle() {
        if (pendingSubmission) {
            B3DVulkanBridge.submit(B3DVulkanBridge.createCommandEncoder());
            pendingSubmission = false;
        }
        RenderSystems.vulkan().finish();
        if (commandRing != null) {
            commandRing.reapCompleted();
        }
    }

    public static void onGameSubmission() {
        pendingSubmission = false;
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
