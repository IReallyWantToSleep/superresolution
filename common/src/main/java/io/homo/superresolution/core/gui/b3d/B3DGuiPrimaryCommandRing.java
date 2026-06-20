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
import io.homo.superresolution.core.graphics.impl.command.CommandBufferBehavior;
import io.homo.superresolution.core.graphics.impl.command.CommandPoolFlags;
import io.homo.superresolution.core.graphics.vulkan.VulkanCommandBuffer;
import io.homo.superresolution.core.graphics.vulkan.VulkanCommandPool;
import com.mojang.blaze3d.buffers.GpuFence;

import java.util.ArrayList;
import java.util.List;

public final class B3DGuiPrimaryCommandRing implements AutoCloseable {
    private static final int SLOT_COUNT = 5;

    private final VulkanCommandPool pool;
    private final Slot[] slots = new Slot[SLOT_COUNT];

    public B3DGuiPrimaryCommandRing() {
        pool = RenderSystems.vulkan().device().createCommandPool(CommandPoolFlags.Reset);
        for (int i = 0; i < slots.length; i++) {
            slots[i] = new Slot(i, pool.createCommandBuffer(CommandBufferBehavior.ReusableSequential));
        }
    }

    public Slot acquire() {
        for (Slot slot : slots) {
            if (slot.isReady()) {
                slot.prepareForRecording();
                return slot;
            }
        }
        return null;
    }

    public void reapCompleted() {
        for (Slot slot : slots) {
            slot.reapIfComplete();
        }
    }

    public boolean isGenerationInUse(int generation) {
        for (Slot slot : slots) {
            slot.reapIfComplete();
            if (slot.generation == generation) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void close() {
        for (Slot slot : slots) {
            slot.close();
        }
        pool.destroy();
    }

    public static final class Slot implements AutoCloseable {
        private final VulkanCommandBuffer commandBuffer;
        private final int index;
        private GpuFence fence;
        private int generation = -1;

        private Slot(int index, VulkanCommandBuffer commandBuffer) {
            this.index = index;
            this.commandBuffer = commandBuffer;
        }

        private boolean isReady() {
            reapIfComplete();
            return fence == null;
        }

        private void prepareForRecording() {
            commandBuffer.reset();
        }

        public VulkanCommandBuffer commandBuffer() {
            return commandBuffer;
        }

        public int index() {
            return index;
        }

        public void markSubmitted(GpuFence fence, int generation) {
            this.fence = fence;
            this.generation = generation;
            commandBuffer.markExternalSubmitted();
        }

        public int generation() {
            return generation;
        }

        private void reapIfComplete() {
            if (fence == null) {
                return;
            }
            if (!B3DVulkanBridge.awaitFence(fence, 0)) {
                return;
            }
            closeFence();
            commandBuffer.markExternalComplete();
            generation = -1;
        }

        private void closeFence() {
            GpuFence fenceToClose = fence;
            fence = null;
            if (fenceToClose != null) {
                fenceToClose.close();
            }
        }

        @Override
        public void close() {
            if (fence != null) {
                B3DVulkanBridge.awaitFence(fence, Long.MAX_VALUE);
                closeFence();
                commandBuffer.markExternalComplete();
            }
            commandBuffer.destroy();
        }
    }
}
#else
public final class B3DGuiPrimaryCommandRing implements AutoCloseable {
    @Override
    public void close() {
    }
}
#endif
