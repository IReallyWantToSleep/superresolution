package io.homo.superresolution.common.upscale.interoplayer;

import io.homo.superresolution.core.graphics.vulkan.VulkanCommandBuffer;
import io.homo.superresolution.srapi.SRRenderApiType;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.util.Objects;

/** One recording lease on a command buffer owned by the interop implementation. */
public final class VulkanCommandContext implements InteropCommandContext {
    private final VulkanCommandBuffer commandBuffer;
    private final boolean borrowed;
    private boolean closed;

    public VulkanCommandContext(VulkanCommandBuffer commandBuffer) {
        this(commandBuffer, false);
    }

    public VulkanCommandContext(VulkanCommandBuffer commandBuffer, boolean borrowed) {
        this.commandBuffer = Objects.requireNonNull(commandBuffer, "commandBuffer");
        this.borrowed = borrowed;
    }

    @Override
    public SRRenderApiType api() {
        return SRRenderApiType.VULKAN;
    }

    @Override
    public VulkanCommandBuffer commandBuffer() {
        requireOpen();
        return commandBuffer;
    }

    public VkCommandBuffer nativeCommandBuffer() {
        return commandBuffer().getNativeCommandBuffer();
    }

    @Override
    public long nativeHandle() {
        return nativeCommandBuffer().address();
    }

    @Override
    public boolean borrowed() {
        return borrowed;
    }

    @Override
    public void close() {
        closed = true;
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Vulkan command context is closed");
        }
    }
}
