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

import io.homo.superresolution.core.graphics.impl.buffer.BufferDescription;
import io.homo.superresolution.core.graphics.impl.buffer.BufferUsage;
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.command.*;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.impl.pipeline.ComputePipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.GraphicsPipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineDescriptorSet;
import io.homo.superresolution.core.graphics.impl.pipeline.RenderPass;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderResourceDescription;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.ITextureView;
import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.util.Map;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanCommandDecoder implements ICommandDecoder {
    private final ResourceStateTracker stateTracker = new ResourceStateTracker();
    private VulkanDevice vulkanDevice;

    public VulkanCommandDecoder(VulkanDevice vulkanDevice) {
        this.vulkanDevice = vulkanDevice;
    }

    private static int vkLayoutFor(ResourceAccessType access) {
        return switch (access) {
            case UNDEFINED -> VK_IMAGE_LAYOUT_UNDEFINED;
            case SAMPLED_READ -> VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
            case STORAGE_READ, STORAGE_WRITE, STORAGE_READ_WRITE -> VK_IMAGE_LAYOUT_GENERAL;
            case COLOR_ATTACHMENT_WRITE -> VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
            case DEPTH_ATTACHMENT_WRITE -> VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL;
            case TRANSFER_SRC -> VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL;
            case TRANSFER_DST -> VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
        };
    }

    private static int vkStageFor(ResourceAccessType access) {
        return switch (access) {
            case UNDEFINED -> VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
            case SAMPLED_READ, STORAGE_READ, STORAGE_WRITE, STORAGE_READ_WRITE ->
                    VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT | VK_PIPELINE_STAGE_VERTEX_SHADER_BIT | VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
            case COLOR_ATTACHMENT_WRITE -> VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
            case DEPTH_ATTACHMENT_WRITE ->
                    VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT;
            case TRANSFER_SRC, TRANSFER_DST -> VK_PIPELINE_STAGE_TRANSFER_BIT;
        };
    }

    private static int vkAccessFor(ResourceAccessType access) {
        return switch (access) {
            case UNDEFINED -> 0;
            case SAMPLED_READ, STORAGE_READ -> VK_ACCESS_SHADER_READ_BIT;
            case STORAGE_WRITE -> VK_ACCESS_SHADER_WRITE_BIT;
            case STORAGE_READ_WRITE -> VK_ACCESS_SHADER_READ_BIT | VK_ACCESS_SHADER_WRITE_BIT;
            case COLOR_ATTACHMENT_WRITE -> VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;
            case DEPTH_ATTACHMENT_WRITE -> VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;
            case TRANSFER_SRC -> VK_ACCESS_TRANSFER_READ_BIT;
            case TRANSFER_DST -> VK_ACCESS_TRANSFER_WRITE_BIT;
        };
    }

    private static int vkStageFor(BufferUsage usage) {
        return switch (usage) {
            case StaticDraw, DynamicDraw -> VK_PIPELINE_STAGE_VERTEX_INPUT_BIT;
            case Ubo ->
                    VK_PIPELINE_STAGE_VERTEX_SHADER_BIT | VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT | VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT;
            case CopySrc, CopyDst -> VK_PIPELINE_STAGE_TRANSFER_BIT;
        };
    }

    private static int vkAccessFor(BufferUsage usage) {
        return switch (usage) {
            case StaticDraw, DynamicDraw -> VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT | VK_ACCESS_INDEX_READ_BIT;
            case Ubo -> VK_ACCESS_UNIFORM_READ_BIT;
            case CopySrc -> VK_ACCESS_TRANSFER_READ_BIT;
            case CopyDst -> VK_ACCESS_TRANSFER_WRITE_BIT;
        };
    }

    @Override
    public ResourceStateTracker getStateTracker() {
        return stateTracker;
    }

    @Override
    public void declareExternalResource(ITexture texture, ResourceAccessType currentState) {
        if (!(texture instanceof VulkanExternalTexture ext)) {
            throw new IllegalArgumentException(
                    "declareExternalResource: 仅允许外部导入纹理 (VulkanExternalTexture)");
        }
        stateTracker.setState(texture, new ResourceState(currentState));
        ext.setCurrentLayout(vkLayoutFor(currentState));
    }

    @Override
    public void restoreExternalResource(ICommandBuffer commandBuffer, ITexture texture, ResourceAccessType targetState) {
        if (!(texture instanceof VulkanExternalTexture ext)) {
            throw new IllegalArgumentException(
                    "restoreExternalResource: 仅允许外部导入纹理 (VulkanExternalTexture)");
        }
        ResourceState current = stateTracker.getState(texture);
        VulkanCommandBuffer vcb = (VulkanCommandBuffer) commandBuffer;
        VkCommandBuffer cmd = vcb.getNativeCommandBuffer();
        int newLayout = vkLayoutFor(targetState);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .srcAccessMask(vkAccessFor(current.accessType()))
                    .dstAccessMask(vkAccessFor(targetState))
                    .oldLayout(ext.getCurrentLayout())
                    .newLayout(newLayout)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .image(ext.handle())
                    .subresourceRange(VkImageSubresourceRange.calloc(stack)
                            .aspectMask(ext.getAspectMask())
                            .baseMipLevel(0)
                            .levelCount(ext.getMipLevels())
                            .baseArrayLayer(0)
                            .layerCount(1));
            vkCmdPipelineBarrier(
                    cmd,
                    vkStageFor(current.accessType()),
                    vkStageFor(targetState),
                    0, null, null, barrier
            );
        }
        ext.setCurrentLayout(newLayout);
        stateTracker.setState(texture, new ResourceState(targetState));
    }

    @Override
    public void clearTextureRGBA(ICommandBuffer commandBuffer, ITexture texture, float[] color) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkClearColorValue clearColor = VkClearColorValue.calloc(stack);
            clearColor.float32(0, color[0]);
            clearColor.float32(1, color[1]);
            clearColor.float32(2, color[2]);
            clearColor.float32(3, color[3]);
            VulkanTexture vulkanTexture = (VulkanTexture) texture;
            long imageHandle = vulkanTexture.handle();
            VulkanCommandBuffer vulkanCommandBuffer = (VulkanCommandBuffer) commandBuffer;
            VkCommandBuffer commandBufferHandle = vulkanCommandBuffer.getNativeCommandBuffer();
            VkImageSubresourceRange range = VkImageSubresourceRange.calloc(stack);
            range.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            range.baseMipLevel(0);
            range.levelCount(1);
            range.baseArrayLayer(0);
            range.layerCount(1);
            vkCmdClearColorImage(
                    commandBufferHandle,
                    imageHandle,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    clearColor,
                    range
            );
        }
    }

    @Override
    public void clearTextureDepth(ICommandBuffer commandBuffer, ITexture texture, float depth) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkClearDepthStencilValue clearDepth = VkClearDepthStencilValue.calloc(stack);
            clearDepth.depth(depth);
            clearDepth.stencil(0);
            VulkanTexture vulkanTexture = (VulkanTexture) texture;
            long imageHandle = vulkanTexture.handle();
            VulkanCommandBuffer vulkanCommandBuffer = (VulkanCommandBuffer) commandBuffer;
            VkCommandBuffer commandBufferHandle = vulkanCommandBuffer.getNativeCommandBuffer();
            VkImageSubresourceRange range = VkImageSubresourceRange.calloc(stack);
            range.aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT);
            range.baseMipLevel(0);
            range.levelCount(1);
            range.baseArrayLayer(0);
            range.layerCount(1);

            vkCmdClearDepthStencilImage(
                    commandBufferHandle,
                    imageHandle,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    clearDepth,
                    range
            );
        }
    }

    @Override
    public void clearTextureStencil(ICommandBuffer commandBuffer, ITexture texture, int stencil) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkClearDepthStencilValue clearStencil = VkClearDepthStencilValue.calloc(stack);
            clearStencil.depth(1.0f);
            clearStencil.stencil(stencil);
            VulkanTexture vulkanTexture = (VulkanTexture) texture;
            long imageHandle = vulkanTexture.handle();
            VulkanCommandBuffer vulkanCommandBuffer = (VulkanCommandBuffer) commandBuffer;
            VkCommandBuffer commandBufferHandle = vulkanCommandBuffer.getNativeCommandBuffer();
            VkImageSubresourceRange range = VkImageSubresourceRange.calloc(stack);
            range.aspectMask(VK_IMAGE_ASPECT_STENCIL_BIT);
            range.baseMipLevel(0);
            range.levelCount(1);
            range.baseArrayLayer(0);
            range.layerCount(1);

            vkCmdClearDepthStencilImage(
                    commandBufferHandle,
                    imageHandle,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    clearStencil,
                    range
            );
        }
    }

    @Override
    public void copyTexture(ICommandBuffer commandBuffer, ITexture src, ITexture dst, int srcX0, int srcY0, int srcX1, int srcY1, int srcLevel, int dstX0, int dstY0, int dstX1, int dstY1, int dstLevel) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VulkanTexture srcTexture = (VulkanTexture) src;
            VulkanTexture dstTexture = (VulkanTexture) dst;

            VkImageCopy.Buffer copyRegion = VkImageCopy.calloc(1, stack);
            copyRegion.srcSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            copyRegion.srcSubresource().mipLevel(srcLevel);
            copyRegion.srcSubresource().baseArrayLayer(0);
            copyRegion.srcSubresource().layerCount(1);
            copyRegion.srcOffset().set(srcX0, srcY0, 0);
            copyRegion.dstSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            copyRegion.dstSubresource().mipLevel(dstLevel);
            copyRegion.dstSubresource().baseArrayLayer(0);
            copyRegion.dstSubresource().layerCount(1);
            copyRegion.dstOffset().set(dstX0, dstY0, 0);

            VulkanCommandBuffer vulkanCommandBuffer = (VulkanCommandBuffer) commandBuffer;
            VkCommandBuffer commandBufferHandle = vulkanCommandBuffer.getNativeCommandBuffer();

            vkCmdCopyImage(
                    commandBufferHandle,
                    srcTexture.handle(),
                    VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                    dstTexture.handle(),
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    copyRegion
            );
        }
    }

    @Override
    public void copyBuffer(ICommandBuffer commandBuffer, IBuffer src, IBuffer dst, long srcOffset, long dstOffset, long size) {
        VulkanCommandBuffer vcb = (VulkanCommandBuffer) commandBuffer;
        VkCommandBuffer cmd = vcb.getNativeCommandBuffer();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCopy.Buffer copyRegion = VkBufferCopy.calloc(1, stack)
                    .srcOffset(srcOffset)
                    .dstOffset(dstOffset)
                    .size(size);
            vkCmdCopyBuffer(cmd, src.handle(), dst.handle(), copyRegion);
        }
    }

    @Override
    public void writeToBuffer(ICommandBuffer commandBuffer, IBuffer dst, long dstOffset, long size, ByteBuffer data) {
        if (!(commandBuffer instanceof VulkanCommandBuffer vcb)) {
            throw new IllegalArgumentException("writeToBuffer: commandBuffer类型错误: " + commandBuffer.getClass().getName());
        }
        if (!(dst instanceof VulkanBuffer vkBuffer)) {
            throw new IllegalArgumentException("writeToBuffer: buffer类型错误: " + dst.getClass().getName());
        }
        if (data == null) {
            throw new IllegalArgumentException("writeToBuffer: data为null");
        }
        if (dstOffset < 0) {
            throw new IllegalArgumentException("writeToBuffer: dstOffset不能为负数");
        }

        ByteBuffer src = data.duplicate();
        if (size <= 0) {
            return;
        }
        if (dstOffset + size > dst.getSize()) {
            throw new IllegalArgumentException("writeToBuffer: 写入范围超出缓冲大小");
        }

        if (vkBuffer.getUsage() == BufferUsage.CopySrc) {
            vkBuffer.writeHostVisible(src, Math.toIntExact(dstOffset));
            return;
        }

        VulkanBuffer stagingBuffer = new VulkanBuffer(
                vulkanDevice,
                BufferDescription.create()
                        .size(size)
                        .usage(BufferUsage.CopySrc)
                        .build()
        );
        stagingBuffer.writeHostVisible(src, 0);
        copyBuffer(commandBuffer, stagingBuffer, vkBuffer, 0, dstOffset, size);
        insertTransferWriteBarrier(vcb.getNativeCommandBuffer(), vkBuffer, dstOffset, size);
        vcb.addTransientResource(stagingBuffer);
    }

    @Override
    public void setViewport(ICommandBuffer commandBuffer, float x, float y, float width, float height) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
            viewport.x(x);
            viewport.y(y);
            viewport.width(width);
            viewport.height(height);
            viewport.minDepth(0.0f);
            viewport.maxDepth(1.0f);

            VulkanCommandBuffer vulkanCommandBuffer = (VulkanCommandBuffer) commandBuffer;
            VkCommandBuffer commandBufferHandle = vulkanCommandBuffer.getNativeCommandBuffer();

            vkCmdSetViewport(
                    commandBufferHandle,
                    0,
                    viewport
            );
        }
    }

    @Override
    public void setScissor(ICommandBuffer commandBuffer, int x, int y, int width, int height) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
            scissor.offset().set(x, y);
            scissor.extent().set(width, height);

            VulkanCommandBuffer vulkanCommandBuffer = (VulkanCommandBuffer) commandBuffer;
            VkCommandBuffer commandBufferHandle = vulkanCommandBuffer.getNativeCommandBuffer();

            vkCmdSetScissor(
                    commandBufferHandle,
                    0,
                    scissor
            );
        }

    }

    @Override
    public void setLineWidth(ICommandBuffer commandBuffer, float width) {
        VulkanCommandBuffer vulkanCommandBuffer = (VulkanCommandBuffer) commandBuffer;
        VkCommandBuffer commandBufferHandle = vulkanCommandBuffer.getNativeCommandBuffer();

        vkCmdSetLineWidth(
                commandBufferHandle,
                width
        );
    }

    @Override
    public void setBlendConstants(ICommandBuffer commandBuffer, float r, float g, float b, float a) {
        VulkanCommandBuffer vulkanCommandBuffer = (VulkanCommandBuffer) commandBuffer;
        VkCommandBuffer commandBufferHandle = vulkanCommandBuffer.getNativeCommandBuffer();

        float[] blendConstants = new float[]{r, g, b, a};
        vkCmdSetBlendConstants(
                commandBufferHandle,
                blendConstants
        );
    }

    @Override
    public void beginRenderPass(ICommandBuffer commandBuffer, RenderPass renderPass) {
        if (!(commandBuffer instanceof VulkanCommandBuffer vcb)) {
            throw new IllegalArgumentException("beginRenderPass: commandBuffer类型错误: " + commandBuffer.getClass().getName());
        }
        if (renderPass == null) {
            throw new IllegalArgumentException("beginRenderPass: renderPass为null");
        }
        if (!(renderPass instanceof VulkanRenderPass vkRenderPass)) {
            throw new IllegalArgumentException("beginRenderPass: renderPass类型错误: " + renderPass.getClass().getName());
        }

        VkCommandBuffer cmd = vcb.getNativeCommandBuffer();
        VulkanFramebuffer vkFramebuffer = (VulkanFramebuffer) vkRenderPass.frameBuffer();

        ITexture colorAttachment = vkFramebuffer.getColorAttachmentTexture();
        if (colorAttachment != null) {
            transitionTexture(cmd, colorAttachment, ResourceAccessType.COLOR_ATTACHMENT_WRITE);
        }

        ITexture depthAttachment = vkFramebuffer.getDepthAttachmentTexture();
        if (depthAttachment != null) {
            transitionTexture(cmd, depthAttachment, ResourceAccessType.DEPTH_ATTACHMENT_WRITE);
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(vkRenderPass.getRenderPass())
                    .framebuffer(vkRenderPass.getFramebufferHandle());
            renderPassBeginInfo.renderArea().offset().set(0, 0);
            renderPassBeginInfo.renderArea().extent().set(vkFramebuffer.getWidth(), vkFramebuffer.getHeight());

            int clearCount = 0;
            if (colorAttachment != null) {
                clearCount++;
            }
            if (depthAttachment != null) {
                clearCount++;
            }
            if (clearCount > 0) {
                VkClearValue.Buffer clearValues = VkClearValue.calloc(clearCount, stack);
                int idx = 0;
                if (colorAttachment != null) {
                    if (renderPass.clearState().shouldClearColorOnBegin(0)) {
                        float[] cc = renderPass.clearState().getColorClearValueOnBegin(0);
                        clearValues.get(idx).color().float32(0, cc[0]).float32(1, cc[1]).float32(2, cc[2]).float32(3, cc[3]);
                    }
                    idx++;
                }
                if (depthAttachment != null) {
                    VkClearDepthStencilValue depthClear = clearValues.get(idx).depthStencil();
                    if (renderPass.clearState().shouldClearDepthOnBegin()) {
                        depthClear.depth(renderPass.clearState().getDepthClearValueOnBegin());
                    } else {
                        depthClear.depth(1.0f);
                    }
                    if (renderPass.clearState().shouldClearStencilOnBegin()) {
                        depthClear.stencil(renderPass.clearState().getStencilClearValueOnBegin());
                    }
                }
                renderPassBeginInfo.pClearValues(clearValues);
            }

            vkCmdBeginRenderPass(cmd, renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE);
        }

        vcb._beginRenderPass(vkRenderPass);
    }

    @Override
    public void endRenderPass(ICommandBuffer commandBuffer) {
        if (!(commandBuffer instanceof VulkanCommandBuffer vcb)) {
            throw new IllegalArgumentException("endRenderPass: commandBuffer类型错误: " + commandBuffer.getClass().getName());
        }
        if (!vcb.isRenderPassActive()) {
            throw new IllegalStateException("endRenderPass: 当前没有活动的render pass");
        }

        VkCommandBuffer cmd = vcb.getNativeCommandBuffer();
        VulkanRenderPass activePass = vcb.getActiveRenderPass();
        VulkanFramebuffer vkFramebuffer = (VulkanFramebuffer) activePass.frameBuffer();

        vkCmdEndRenderPass(cmd);

        ITexture colorAttachment = vkFramebuffer.getColorAttachmentTexture();
        if (colorAttachment != null) {
            stateTracker.setState(colorAttachment, new ResourceState(ResourceAccessType.COLOR_ATTACHMENT_WRITE));
        }

        ITexture depthAttachment = vkFramebuffer.getDepthAttachmentTexture();
        if (depthAttachment != null) {
            stateTracker.setState(depthAttachment, new ResourceState(ResourceAccessType.DEPTH_ATTACHMENT_WRITE));
        }

        vcb._endRenderPass();
    }

    @Override
    public void bindPipeline(ICommandBuffer commandBuffer, GraphicsPipeline pipeline) {
        if (!(commandBuffer instanceof VulkanCommandBuffer vcb)) {
            throw new IllegalArgumentException("bindPipeline(graphics): commandBuffer类型错误: " + commandBuffer.getClass().getName());
        }
        if (!vcb.isRenderPassActive()) {
            throw new IllegalStateException("bindPipeline(graphics): 当前没有活动的render pass，请先调用 beginRenderPass");
        }
        if (pipeline == null) {
            throw new IllegalArgumentException("bindPipeline(graphics): pipeline为null");
        }
        if (!(pipeline instanceof VulkanGraphicsPipeline vkGraphicsPipeline)) {
            throw new IllegalArgumentException("bindPipeline(graphics): pipeline类型错误: " + pipeline.getClass().getName());
        }

        VulkanRenderPass activePass = vcb.getActiveRenderPass();
        if (pipeline.renderPass() != activePass) {
            throw new IllegalStateException("bindPipeline(graphics): pipeline.renderPass 与当前活动 render pass 不匹配");
        }

        VkCommandBuffer cmd = vcb.getNativeCommandBuffer();
        vkGraphicsPipeline.ensurePipelineCreated(activePass.getRenderPass());
        transitionDescriptorBindings(cmd, pipeline.descriptorSet(), pipeline);

        VulkanPipelineDescriptorSet vkDescriptorSet = (VulkanPipelineDescriptorSet) pipeline.descriptorSet();
        vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, vkGraphicsPipeline.getPipeline());
        vkDescriptorSet.pushDescriptors(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, vkGraphicsPipeline.getPipelineLayout());
        pipeline.applyDynamicStates(commandBuffer);
        vcb.bindGraphicsPipeline(vkGraphicsPipeline);
    }

    @Override
    public void bindPipeline(ICommandBuffer commandBuffer, ComputePipeline pipeline) {
        if (!(commandBuffer instanceof VulkanCommandBuffer vcb)) {
            throw new IllegalArgumentException("bindPipeline(compute): commandBuffer类型错误: " + commandBuffer.getClass().getName());
        }
        if (vcb.isRenderPassActive()) {
            throw new IllegalStateException("bindPipeline(compute): render pass进行中，不能绑定compute pipeline");
        }
        if (pipeline == null) {
            throw new IllegalArgumentException("bindPipeline(compute): pipeline为null");
        }
        if (!(pipeline instanceof VulkanComputePipeline vkComputePipeline)) {
            throw new IllegalArgumentException("bindPipeline(compute): pipeline类型错误: " + pipeline.getClass().getName());
        }

        VkCommandBuffer cmd = vcb.getNativeCommandBuffer();
        transitionDescriptorBindings(cmd, pipeline.descriptorSet(), pipeline);
        VulkanPipelineDescriptorSet vkDescriptorSet = (VulkanPipelineDescriptorSet) pipeline.descriptorSet();

        vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_COMPUTE, vkComputePipeline.getPipeline());
        vkDescriptorSet.pushDescriptors(cmd, VK_PIPELINE_BIND_POINT_COMPUTE, vkComputePipeline.getPipelineLayout());
        vcb.bindComputePipeline(vkComputePipeline);
    }

    @Override
    public void draw(ICommandBuffer commandBuffer, IVertexBuffer vertexBuffer, int vertexCount, int firstVertex) {
        VulkanCommandBuffer vcb = (VulkanCommandBuffer) commandBuffer;
        if (!vcb.isRenderPassActive()) {
            throw new IllegalStateException("draw: 当前没有活动的render pass，请先调用 beginRenderPass");
        }
        VulkanGraphicsPipeline vkGraphicsPipeline = vcb.getBoundGraphicsPipeline();
        if (vkGraphicsPipeline == null) {
            throw new IllegalStateException("draw: 当前未绑定图形管线，请先调用 bindPipeline(graphics)");
        }

        VkCommandBuffer cmd = vcb.getNativeCommandBuffer();
        GraphicsPipeline graphicsPipeline = vkGraphicsPipeline;

        if (vertexBuffer == null) {
            throw new IllegalArgumentException("draw: vertexBuffer为null");
        }
        if (vertexCount <= 0) {
            throw new IllegalArgumentException("draw: vertexCount必须为正数");
        }
        if (firstVertex < 0) {
            throw new IllegalArgumentException("draw: firstVertex不能为负数");
        }

        if (vertexBuffer != null) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                vkCmdBindVertexBuffers(cmd, 0, stack.longs(vertexBuffer.handle()), stack.longs(0));
            }
        }

        vkCmdDraw(cmd, vertexCount, 1, firstVertex, 0);
    }

    @Override
    public void dispatch(ICommandBuffer commandBuffer, int groupCountX, int groupCountY, int groupCountZ) {
        VulkanCommandBuffer vcb = (VulkanCommandBuffer) commandBuffer;
        if (vcb.isRenderPassActive()) {
            throw new IllegalStateException("dispatch: render pass进行中，不能执行compute dispatch");
        }
        VulkanComputePipeline vkComputePipeline = vcb.getBoundComputePipeline();
        if (vkComputePipeline == null) {
            throw new IllegalStateException("dispatch: 当前未绑定计算管线，请先调用 bindPipeline(compute)");
        }
        VkCommandBuffer cmd = vcb.getNativeCommandBuffer();

        vkCmdDispatch(cmd, groupCountX, groupCountY, groupCountZ);

        PipelineDescriptorSet descriptorSet = vkComputePipeline.descriptorSet();
        Map<String, PipelineDescriptorSet.ResourceBinding> bindings = descriptorSet.getBindings();
        for (Map.Entry<String, PipelineDescriptorSet.ResourceBinding> entry : bindings.entrySet()) {
            String name = entry.getKey();
            PipelineDescriptorSet.ResourceBinding binding = entry.getValue();
            ResourceAccessType access = deriveAccessType(vkComputePipeline, name, binding);
            ITexture trackTarget = resolveTrackingTarget(binding);
            if (trackTarget != null) {
                stateTracker.setState(trackTarget, new ResourceState(access));
            } else if (binding.resource() instanceof IBuffer buffer) {
                stateTracker.setState(buffer, new ResourceState(access));
            }
        }
    }

    @Override
    public void memoryBarrier(ICommandBuffer commandBuffer, MemoryBarrierType... barriers) {
        VulkanCommandBuffer vcb = (VulkanCommandBuffer) commandBuffer;
        VkCommandBuffer cmd = vcb.getNativeCommandBuffer();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkMemoryBarrier.Buffer memBarrier = VkMemoryBarrier.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_BARRIER)
                    .srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT)
                    .dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
            vkCmdPipelineBarrier(
                    cmd,
                    VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                    VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                    0,
                    memBarrier,
                    null,
                    null
            );
        }
    }

    @Override
    public IDevice getDevice() {
        return vulkanDevice;
    }

    void insertTransferWriteBarrier(VkCommandBuffer commandBuffer, VulkanBuffer buffer, long offset, long size) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferMemoryBarrier.Buffer barrier = VkBufferMemoryBarrier.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
                    .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                    .dstAccessMask(vkAccessFor(buffer.getUsage()))
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .buffer(buffer.handle())
                    .offset(offset)
                    .size(size);
            vkCmdPipelineBarrier(
                    commandBuffer,
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    vkStageFor(buffer.getUsage()),
                    0,
                    null,
                    barrier,
                    null
            );
        }
    }

    private ResourceAccessType deriveAccessType(ComputePipeline pipeline, String name,
                                                PipelineDescriptorSet.ResourceBinding binding) {
        return switch (binding.type()) {
            case SAMPLER_TEXTURE -> ResourceAccessType.SAMPLED_READ;
            case STORAGE_IMAGE -> {
                ShaderResourceDescription desc = pipeline.shader().getDescription()
                        .resourcesLayout().getResource(name);
                if (desc != null) {
                    yield switch (desc.access()) {
                        case Read -> ResourceAccessType.STORAGE_READ;
                        case Write -> ResourceAccessType.STORAGE_WRITE;
                        case Both -> ResourceAccessType.STORAGE_READ_WRITE;
                    };
                }
                yield ResourceAccessType.STORAGE_READ_WRITE;
            }
            case UNIFORM_BUFFER -> ResourceAccessType.SAMPLED_READ;
        };
    }

    private ResourceAccessType deriveAccessType(GraphicsPipeline pipeline, String name,
                                                PipelineDescriptorSet.ResourceBinding binding) {
        return switch (binding.type()) {
            case SAMPLER_TEXTURE -> ResourceAccessType.SAMPLED_READ;
            case STORAGE_IMAGE -> {
                ShaderResourceDescription desc = pipeline.shader().getDescription()
                        .resourcesLayout().getResource(name);
                if (desc != null) {
                    yield switch (desc.access()) {
                        case Read -> ResourceAccessType.STORAGE_READ;
                        case Write -> ResourceAccessType.STORAGE_WRITE;
                        case Both -> ResourceAccessType.STORAGE_READ_WRITE;
                    };
                }
                yield ResourceAccessType.STORAGE_READ_WRITE;
            }
            case UNIFORM_BUFFER -> ResourceAccessType.SAMPLED_READ;
        };
    }

    private void transitionDescriptorBindings(VkCommandBuffer cmd, PipelineDescriptorSet descriptorSet, ComputePipeline pipeline) {
        Map<String, PipelineDescriptorSet.ResourceBinding> bindings = descriptorSet.getBindings();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            for (Map.Entry<String, PipelineDescriptorSet.ResourceBinding> entry : bindings.entrySet()) {
                String name = entry.getKey();
                PipelineDescriptorSet.ResourceBinding binding = entry.getValue();
                ResourceAccessType target = deriveAccessType(pipeline, name, binding);
                emitImageBarrierIfNeeded(cmd, stack, binding, target);
            }
        }
    }

    private void transitionDescriptorBindings(VkCommandBuffer cmd, PipelineDescriptorSet descriptorSet, GraphicsPipeline pipeline) {
        Map<String, PipelineDescriptorSet.ResourceBinding> bindings = descriptorSet.getBindings();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            for (Map.Entry<String, PipelineDescriptorSet.ResourceBinding> entry : bindings.entrySet()) {
                String name = entry.getKey();
                PipelineDescriptorSet.ResourceBinding binding = entry.getValue();
                ResourceAccessType target = deriveAccessType(pipeline, name, binding);
                emitImageBarrierIfNeeded(cmd, stack, binding, target);
            }
        }
    }

    private void emitImageBarrierIfNeeded(VkCommandBuffer cmd, MemoryStack stack,
                                          PipelineDescriptorSet.ResourceBinding binding, ResourceAccessType target) {
        ITexture trackTarget = resolveTrackingTarget(binding);
        if (trackTarget == null) {
            return;
        }
        if (!(trackTarget instanceof VulkanLayoutTracked vlt)) {
            return;
        }

        ResourceState prev = stateTracker.getState(trackTarget);
        int newLayout = vkLayoutFor(target);
        int oldLayout = vlt.getCurrentLayout();
        boolean needsBarrier = prev.accessType().includesWrite() || oldLayout != newLayout;
        if (!needsBarrier) {
            return;
        }

        long imageHandle = resolveImageHandle(trackTarget);
        int aspectMask = resolveAspectMask(trackTarget);
        int baseMipLevel = 0;
        int levelCount = VK_REMAINING_MIP_LEVELS;

        if (binding.resource() instanceof ITextureView view) {
            baseMipLevel = view.getBaseMipLevel();
            levelCount = view.getMipLevelCount();
        }

        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                .srcAccessMask(vkAccessFor(prev.accessType()))
                .dstAccessMask(vkAccessFor(target))
                .oldLayout(oldLayout)
                .newLayout(newLayout)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .image(imageHandle)
                .subresourceRange(VkImageSubresourceRange.calloc(stack)
                        .aspectMask(aspectMask)
                        .baseMipLevel(baseMipLevel)
                        .levelCount(levelCount)
                        .baseArrayLayer(0)
                        .layerCount(1));
        vkCmdPipelineBarrier(
                cmd,
                vkStageFor(prev.accessType()),
                vkStageFor(target),
                0, null, null, barrier
        );
        vlt.setCurrentLayout(newLayout);
    }

    private void transitionTexture(VkCommandBuffer cmd, ITexture texture, ResourceAccessType target) {
        if (!(texture instanceof VulkanLayoutTracked vlt)) {
            return;
        }

        ResourceState prev = stateTracker.getState(texture);
        int newLayout = vkLayoutFor(target);
        int oldLayout = vlt.getCurrentLayout();
        boolean needsBarrier = prev.accessType().includesWrite() || oldLayout != newLayout;
        if (!needsBarrier) {
            return;
        }

        long imageHandle = resolveImageHandle(texture);
        int aspectMask = resolveAspectMask(texture);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .srcAccessMask(vkAccessFor(prev.accessType()))
                    .dstAccessMask(vkAccessFor(target))
                    .oldLayout(oldLayout)
                    .newLayout(newLayout)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .image(imageHandle)
                    .subresourceRange(VkImageSubresourceRange.calloc(stack)
                            .aspectMask(aspectMask)
                            .baseMipLevel(0)
                            .levelCount(VK_REMAINING_MIP_LEVELS)
                            .baseArrayLayer(0)
                            .layerCount(1));
            vkCmdPipelineBarrier(
                    cmd,
                    vkStageFor(prev.accessType()),
                    vkStageFor(target),
                    0, null, null, barrier
            );
        }
        vlt.setCurrentLayout(newLayout);
    }

    private ITexture resolveTrackingTarget(PipelineDescriptorSet.ResourceBinding binding) {
        if (binding.resource() instanceof ITextureView view) {
            return view.getParent();
        }
        if (binding.resource() instanceof ITexture texture) {
            return texture;
        }
        return null;
    }

    private long resolveImageHandle(ITexture texture) {
        if (texture instanceof VulkanTexture vt) {
            return vt.handle();
        }
        if (texture instanceof VulkanExternalTexture vet) {
            return vet.handle();
        }
        throw new IllegalArgumentException("Cannot resolve image handle from: " + texture.getClass());
    }

    private int resolveAspectMask(ITexture texture) {
        if (texture.getTextureFormat().isDepthStencil()) {
            return VK_IMAGE_ASPECT_DEPTH_BIT | VK_IMAGE_ASPECT_STENCIL_BIT;
        } else if (texture.getTextureFormat().isDepth()) {
            return VK_IMAGE_ASPECT_DEPTH_BIT;
        }
        return VK_IMAGE_ASPECT_COLOR_BIT;
    }
}
