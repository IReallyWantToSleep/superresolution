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

import io.homo.superresolution.core.graphics.impl.pipeline.ComputePipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineDescriptorSet;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkComputePipelineCreateInfo;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;

import java.nio.LongBuffer;

import static io.homo.superresolution.core.graphics.vulkan.VulkanUtils.VK_CHECK;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanComputePipeline extends ComputePipeline {
    private final VulkanDevice device;
    private long pipelineLayout = VK_NULL_HANDLE;
    private long pipeline = VK_NULL_HANDLE;

    public VulkanComputePipeline(VulkanDevice device, IShaderProgram shader, PipelineDescriptorSet descriptorSet) {
        super(shader, descriptorSet);
        this.device = device;

        if (!(shader instanceof VulkanShaderProgram vkShader)) {
            throw new IllegalArgumentException("Shader must be a VulkanShaderProgram");
        }
        if (!(descriptorSet instanceof VulkanPipelineDescriptorSet vkDescSet)) {
            throw new IllegalArgumentException("DescriptorSet must be a VulkanPipelineDescriptorSet");
        }

        if (!vkShader.isCompiled()) {
            vkShader.compile();
        }

        createPipelineLayout(vkDescSet);
        createPipeline(vkShader);
    }

    private void createPipelineLayout(VulkanPipelineDescriptorSet descriptorSet) {
        try (MemoryStack stack = stackPush()) {
            VkPipelineLayoutCreateInfo layoutInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                    .pSetLayouts(stack.longs(descriptorSet.getDescriptorSetLayout()));

            LongBuffer pLayout = stack.mallocLong(1);
            VK_CHECK(vkCreatePipelineLayout(device.getVkDevice(), layoutInfo, null, pLayout),
                    "Failed to create compute pipeline layout");
            pipelineLayout = pLayout.get(0);
        }
    }

    private void createPipeline(VulkanShaderProgram shader) {
        try (MemoryStack stack = stackPush()) {
            VkPipelineShaderStageCreateInfo stageInfo = VkPipelineShaderStageCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK_SHADER_STAGE_COMPUTE_BIT)
                    .module(shader.getShaderModule(ShaderType.Compute))
                    .pName(stack.UTF8("main"));

            VkComputePipelineCreateInfo.Buffer pipelineInfo = VkComputePipelineCreateInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO)
                    .stage(stageInfo)
                    .layout(pipelineLayout);

            LongBuffer pPipeline = stack.mallocLong(1);
            VK_CHECK(vkCreateComputePipelines(device.getVkDevice(), VK_NULL_HANDLE, pipelineInfo, null, pPipeline),
                    "Failed to create compute pipeline");
            pipeline = pPipeline.get(0);
        }
    }

    public long getPipeline() {
        return pipeline;
    }

    public long getPipelineLayout() {
        return pipelineLayout;
    }

    public VulkanPipelineDescriptorSet.DescriptorLayoutKey getDescriptorLayoutKey() {
        return ((VulkanPipelineDescriptorSet) descriptorSet()).getDescriptorLayoutKey();
    }

    @Override
    public void destroy() {
        if (pipeline != VK_NULL_HANDLE) {
            vkDestroyPipeline(device.getVkDevice(), pipeline, null);
            pipeline = VK_NULL_HANDLE;
        }
        if (pipelineLayout != VK_NULL_HANDLE) {
            vkDestroyPipelineLayout(device.getVkDevice(), pipelineLayout, null);
            pipelineLayout = VK_NULL_HANDLE;
        }
    }
}
