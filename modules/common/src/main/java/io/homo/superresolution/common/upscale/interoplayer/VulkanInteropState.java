package io.homo.superresolution.common.upscale.interoplayer;

import io.homo.superresolution.core.graphics.impl.command.ResourceAccessType;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.vulkan.VulkanCommandBuffer;
import io.homo.superresolution.core.graphics.vulkan.VulkanExternalTexture;
import io.homo.superresolution.core.graphics.vulkan.VulkanResourceState;
import io.homo.superresolution.core.graphics.vulkan.VulkanTexture;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkImageMemoryBarrier;

import java.util.IdentityHashMap;
import java.util.Map;

import static org.lwjgl.vulkan.VK10.*;

/** State changes made while recording are committed only with that recording. */
final class VulkanInteropState {
    private final Map<ITexture, VulkanResourceState> before = new IdentityHashMap<>();

    void remember(ITexture texture) {
        before.putIfAbsent(texture, state(texture));
    }

    void rollback() {
        before.forEach(VulkanInteropState::setState);
        before.clear();
    }

    void commit() {
        before.clear();
    }

    void transition(VulkanCommandBuffer commands, ITexture texture, int layout,
                    int access, int stage) {
        transition(commands, texture, layout, access, stage,
                VK_QUEUE_FAMILY_IGNORED, VK_QUEUE_FAMILY_IGNORED);
    }

    void transition(VulkanCommandBuffer commands, ITexture texture, int layout,
                    int access, int stage, int sourceFamily, int destinationFamily) {
        remember(texture);
        VulkanResourceState previous = state(texture);
        boolean fixed = texture instanceof VulkanExternalTexture external && external.isFixedLayout();
        if (fixed) {
            layout = previous.layout();
        }
        int aspect = texture.getTextureFormat().isDepthStencil()
                ? VK_IMAGE_ASPECT_DEPTH_BIT | VK_IMAGE_ASPECT_STENCIL_BIT
                : texture.getTextureFormat().isDepth() ? VK_IMAGE_ASPECT_DEPTH_BIT : VK_IMAGE_ASPECT_COLOR_BIT;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .oldLayout(previous.layout()).newLayout(layout)
                    .srcAccessMask(previous.accessMask()).dstAccessMask(access)
                    .srcQueueFamilyIndex(sourceFamily).dstQueueFamilyIndex(destinationFamily)
                    .image(texture.handle());
            barrier.subresourceRange().aspectMask(aspect).baseMipLevel(0)
                    .levelCount(texture.getMipmapSettings().getLevels())
                    .baseArrayLayer(0).layerCount(1);
            vkCmdPipelineBarrier(commands.getNativeCommandBuffer(),
                    previous.stageMask(), stage, 0, null, null, barrier);
        }
        setState(texture, new VulkanResourceState(layout, access, stage, ResourceAccessType.UNDEFINED));
    }

    static VulkanResourceState state(ITexture texture) {
        if (texture instanceof VulkanTexture owned) {
            return owned.getCurrentResourceState();
        }
        if (texture instanceof VulkanExternalTexture external) {
            return external.getCurrentResourceState();
        }
        throw new IllegalArgumentException("Expected a Vulkan texture: " + texture);
    }

    private static void setState(ITexture texture, VulkanResourceState state) {
        if (texture instanceof VulkanTexture owned) {
            owned.setCurrentResourceState(state);
        } else if (texture instanceof VulkanExternalTexture external) {
            external.setCurrentResourceState(state);
        }
    }
}
