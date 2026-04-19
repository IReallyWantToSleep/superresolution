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

package io.homo.superresolution.core.graphics.vulkan;

import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.pipeline.PassClearState;
import io.homo.superresolution.core.graphics.impl.pipeline.RenderPass;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static io.homo.superresolution.core.graphics.vulkan.utils.VulkanUtils.VK_CHECK;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanRenderPass extends RenderPass {
    private final VulkanDevice device;
    private long renderPass = VK_NULL_HANDLE;
    private long framebuffer = VK_NULL_HANDLE;

    public VulkanRenderPass(VulkanDevice device,
                            IFrameBuffer frameBuffer, PassClearState clearState) {
        super(frameBuffer, clearState);
        this.device = device;

        if (!(frameBuffer instanceof VulkanFramebuffer vkFb)) {
            throw new IllegalArgumentException("FrameBuffer must be a VulkanFramebuffer");
        }

        createRenderPass(vkFb);
        createFramebuffer(vkFb);
    }

    private void createRenderPass(VulkanFramebuffer vkFb) {
        boolean hasColor = vkFb.getColorAttachmentTexture() != null;
        boolean hasDepth = vkFb.getDepthAttachmentTexture() != null;
        int attachmentCount = (hasColor ? 1 : 0) + (hasDepth ? 1 : 0);

        try (MemoryStack stack = stackPush()) {
            VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(attachmentCount, stack);
            int colorIdx = -1;
            int depthIdx = -1;

            int idx = 0;
            if (hasColor) {
                colorIdx = idx;
                TextureFormat colorFmt = vkFb.getColorAttachmentTexture().getTextureFormat();
                boolean clearOnBegin = clearState.shouldClearColorOnBegin(0);
                attachments.get(idx)
                        .format(colorFmt.vk())
                        .samples(VK_SAMPLE_COUNT_1_BIT)
                        .loadOp(clearOnBegin ? VK_ATTACHMENT_LOAD_OP_CLEAR : VK_ATTACHMENT_LOAD_OP_LOAD)
                        .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                        .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                        .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                        .initialLayout(clearOnBegin ? VK_IMAGE_LAYOUT_UNDEFINED : VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                        .finalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
                idx++;
            }
            if (hasDepth) {
                depthIdx = idx;
                TextureFormat depthFmt = vkFb.getDepthAttachmentTexture().getTextureFormat();
                boolean clearDepthOnBegin = clearState.shouldClearDepthOnBegin();
                attachments.get(idx)
                        .format(depthFmt.vk())
                        .samples(VK_SAMPLE_COUNT_1_BIT)
                        .loadOp(clearDepthOnBegin ? VK_ATTACHMENT_LOAD_OP_CLEAR : VK_ATTACHMENT_LOAD_OP_LOAD)
                        .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                        .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                        .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                        .initialLayout(clearDepthOnBegin ? VK_IMAGE_LAYOUT_UNDEFINED : VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
                        .finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
                idx++;
            }

            // Subpass
            VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack)
                    .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);

            if (hasColor) {
                VkAttachmentReference.Buffer colorRef = VkAttachmentReference.calloc(1, stack)
                        .attachment(colorIdx)
                        .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
                subpass.colorAttachmentCount(1).pColorAttachments(colorRef);
            }
            if (hasDepth) {
                VkAttachmentReference depthRef = VkAttachmentReference.calloc(stack)
                        .attachment(depthIdx)
                        .layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
                subpass.pDepthStencilAttachment(depthRef);
            }

            VkSubpassDependency.Buffer dependency = VkSubpassDependency.calloc(1, stack)
                    .srcSubpass(VK_SUBPASS_EXTERNAL)
                    .dstSubpass(0)
                    .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT)
                    .srcAccessMask(0)
                    .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT)
                    .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);

            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                    .pAttachments(attachments)
                    .pSubpasses(subpass)
                    .pDependencies(dependency);

            LongBuffer pRenderPass = stack.mallocLong(1);
            VK_CHECK(vkCreateRenderPass(device.getVkDevice(), renderPassInfo, null, pRenderPass),
                    "Failed to create render pass");
            renderPass = pRenderPass.get(0);
        }
    }

    private void createFramebuffer(VulkanFramebuffer vkFb) {
        boolean hasColor = vkFb.getColorAttachmentTexture() != null;
        boolean hasDepth = vkFb.getDepthAttachmentTexture() != null;

        try (MemoryStack stack = stackPush()) {
            int viewCount = (hasColor ? 1 : 0) + (hasDepth ? 1 : 0);
            LongBuffer attachmentViews = stack.mallocLong(viewCount);
            int idx = 0;
            if (hasColor) {
                attachmentViews.put(idx++, vkFb.resolveColorImageView());
            }
            if (hasDepth) {
                attachmentViews.put(idx++, vkFb.resolveDepthImageView());
            }

            VkFramebufferCreateInfo fbInfo = VkFramebufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                    .renderPass(renderPass)
                    .pAttachments(attachmentViews)
                    .width(vkFb.getWidth())
                    .height(vkFb.getHeight())
                    .layers(1);

            LongBuffer pFramebuffer = stack.mallocLong(1);
            VK_CHECK(vkCreateFramebuffer(device.getVkDevice(), fbInfo, null, pFramebuffer),
                    "Failed to create framebuffer");
            framebuffer = pFramebuffer.get(0);
        }
    }

    public long getRenderPass() {
        return renderPass;
    }

    public long getFramebufferHandle() {
        return framebuffer;
    }

    @Override
    public void execute(ICommandBuffer cmd, IVertexBuffer vertexBuffer) {
    }

    @Override
    public void execute(ICommandBuffer cmd) {
    }

    @Override
    public void destroy() {
        if (framebuffer != VK_NULL_HANDLE) {
            vkDestroyFramebuffer(device.getVkDevice(), framebuffer, null);
            framebuffer = VK_NULL_HANDLE;
        }
        if (renderPass != VK_NULL_HANDLE) {
            vkDestroyRenderPass(device.getVkDevice(), renderPass, null);
            renderPass = VK_NULL_HANDLE;
        }
    }
}
