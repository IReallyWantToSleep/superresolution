package io.homo.superresolution.core.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Pointer;

import java.nio.LongBuffer;
import java.util.Collection;
import java.util.List;

import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

import org.lwjgl.vulkan.*;

public class Utils {
    public static PointerBuffer asPointerBuffer(MemoryStack stack, Collection<String> collection) {

        PointerBuffer buffer = stack.mallocPointer(collection.size());

        collection.stream()
                .map(stack::UTF8)
                .forEach(buffer::put);

        return buffer.rewind();
    }

    public static PointerBuffer asPointerBuffer(MemoryStack stack, List<? extends Pointer> list) {

        PointerBuffer buffer = stack.mallocPointer(list.size());

        list.forEach(buffer::put);

        return buffer.rewind();
    }

    public static LongBuffer createLongBuffer() {
        return MemoryStack.stackCallocLong(1);
    }

    public static void VK_CHECK(int code) {
        if (code != VK_SUCCESS) throw new VkException();
    }

    public static void VK_CHECK(int code, String msg) {
        if (code != VK_SUCCESS) throw new VkException(msg);
    }

    public static void transitionImageLayout(VkCommandBuffer commandBuffer, long image, int format, int oldLayout, int newLayout, boolean isCubeMap) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .oldLayout(oldLayout)
                    .newLayout(newLayout)
                    .srcQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                    .image(image);

            if (newLayout == VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL) {
                barrier.subresourceRange().aspectMask(VK10.VK_IMAGE_ASPECT_DEPTH_BIT);
                if (hasStencilComponent(format)) {
                    barrier.subresourceRange().aspectMask(barrier.subresourceRange().aspectMask() | VK10.VK_IMAGE_ASPECT_STENCIL_BIT);
                }
            } else if (newLayout == VK10.VK_IMAGE_LAYOUT_GENERAL) {
                barrier.subresourceRange().aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT);
            } else {
                barrier.subresourceRange().aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT);
            }

            barrier.subresourceRange().baseMipLevel(0)
                    .levelCount(1)
                    .baseArrayLayer(0)
                    .layerCount(isCubeMap ? 6 : 1);

            int sourceStage;
            int destinationStage;

            if (oldLayout == VK10.VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
                barrier.srcAccessMask(0);
                barrier.dstAccessMask(VK10.VK_ACCESS_TRANSFER_WRITE_BIT);

                sourceStage = VK10.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                destinationStage = VK10.VK_PIPELINE_STAGE_TRANSFER_BIT;
            } else if (oldLayout == VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout == VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {
                barrier.srcAccessMask(VK10.VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier.dstAccessMask(VK10.VK_ACCESS_SHADER_READ_BIT);

                sourceStage = VK10.VK_PIPELINE_STAGE_TRANSFER_BIT;
                destinationStage = VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
            } else if (oldLayout == VK10.VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL) {
                barrier.srcAccessMask(0);
                barrier.dstAccessMask(VK10.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK10.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);

                sourceStage = VK10.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                destinationStage = VK10.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;
            } else if (newLayout == VK10.VK_IMAGE_LAYOUT_GENERAL) {
                barrier.srcAccessMask(VK10.VK_ACCESS_MEMORY_READ_BIT | VK10.VK_ACCESS_MEMORY_WRITE_BIT);
                barrier.dstAccessMask(VK10.VK_ACCESS_MEMORY_READ_BIT | VK10.VK_ACCESS_MEMORY_WRITE_BIT);

                sourceStage = VK10.VK_PIPELINE_STAGE_ALL_COMMANDS_BIT;
                destinationStage = VK10.VK_PIPELINE_STAGE_ALL_COMMANDS_BIT;
            } else {
                throw new IllegalArgumentException("unsupported layout transition!");
            }

            VK10.vkCmdPipelineBarrier(
                    commandBuffer,
                    sourceStage, destinationStage,
                    0,
                    null,
                    null,
                    barrier
            );
        }
    }

    private static boolean hasStencilComponent(int format) {
        return format == VK10.VK_FORMAT_D32_SFLOAT_S8_UINT || format == VK10.VK_FORMAT_D24_UNORM_S8_UINT;
    }
}
