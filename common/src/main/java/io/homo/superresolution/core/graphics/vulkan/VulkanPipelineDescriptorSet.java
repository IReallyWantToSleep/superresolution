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

import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineDescriptorSet;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderResourcesLayout;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderResourceDescription;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderResourceType;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.ITextureView;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.Map;

import static io.homo.superresolution.core.graphics.vulkan.utils.VulkanUtils.VK_CHECK;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRPushDescriptor.VK_DESCRIPTOR_SET_LAYOUT_CREATE_PUSH_DESCRIPTOR_BIT_KHR;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanPipelineDescriptorSet extends PipelineDescriptorSet {
    private final VulkanDevice device;
    private long descriptorSetLayout = VK_NULL_HANDLE;

    public VulkanPipelineDescriptorSet(VulkanDevice device, IShaderProgram shader) {
        super(shader);
        this.device = device;
        createDescriptorSetLayout();
    }

    private void createDescriptorSetLayout() {
        ShaderResourcesLayout layout = shader.getDescription().resourcesLayout();
        Map<String, ShaderResourceDescription> resources = layout.getResources();

        try (MemoryStack stack = stackPush()) {
            VkDescriptorSetLayoutBinding.Buffer layoutBindings =
                    VkDescriptorSetLayoutBinding.calloc(resources.size(), stack);

            int i = 0;
            for (ShaderResourceDescription res : resources.values()) {
                layoutBindings.get(i)
                        .binding(res.binding())
                        .descriptorType(toVkDescriptorType(res.type()))
                        .descriptorCount(1)
                        .stageFlags(VK_SHADER_STAGE_ALL)
                        .pImmutableSamplers(null);
                i++;
            }

            VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                    .flags(VK_DESCRIPTOR_SET_LAYOUT_CREATE_PUSH_DESCRIPTOR_BIT_KHR)
                    .pBindings(layoutBindings);

            LongBuffer pLayout = stack.mallocLong(1);
            VK_CHECK(vkCreateDescriptorSetLayout(device.getVkDevice(), layoutInfo, null, pLayout),
                    "Failed to create descriptor set layout");
            descriptorSetLayout = pLayout.get(0);
        }
    }

    @Override
    protected void updateImpl() {
    }

    void pushDescriptors(VkCommandBuffer cmd, int bindPoint, long pipelineLayout) {
        if (bindings.isEmpty()) return;

        try (MemoryStack stack = stackPush()) {
            VkWriteDescriptorSet.Buffer writes = VkWriteDescriptorSet.calloc(bindings.size(), stack);

            int i = 0;
            for (Map.Entry<String, ResourceBinding> entry : bindings.entrySet()) {
                ResourceBinding binding = entry.getValue();
                VkWriteDescriptorSet write = writes.get(i);
                write.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                        .dstBinding(binding.bindingPoint())
                        .dstArrayElement(0)
                        .descriptorCount(1);

                switch (binding.type()) {
                    case UNIFORM_BUFFER -> {
                        IBuffer buffer = (IBuffer) binding.resource();
                        VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1, stack)
                                .buffer(buffer.handle())
                                .offset(0)
                                .range(buffer.getSize());
                        write.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                                .pBufferInfo(bufferInfo);
                    }
                    case SAMPLER_TEXTURE -> {
                        ITexture texture = (ITexture) binding.resource();
                        long imageView = resolveImageView(texture);
                        long sampler = binding.sampler() != null
                                ? binding.sampler().handle()
                                : getOrCreateDefaultSampler();

                        VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack)
                                .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                                .imageView(imageView)
                                .sampler(sampler);
                        write.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                                .pImageInfo(imageInfo);
                    }
                    case STORAGE_IMAGE -> {
                        ITexture texture = (ITexture) binding.resource();
                        long imageView = resolveStorageImageView(texture);

                        VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack)
                                .imageLayout(VK_IMAGE_LAYOUT_GENERAL)
                                .imageView(imageView)
                                .sampler(VK_NULL_HANDLE);
                        write.descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
                                .pImageInfo(imageInfo);
                    }
                }
                i++;
            }

            KHRPushDescriptor.vkCmdPushDescriptorSetKHR(cmd, bindPoint, pipelineLayout, 0, writes);
        }
        dirty = false;
    }

    boolean needsPush() {
        return dirty;
    }

    @Override
    public void apply() {
    }

    private long resolveImageView(ITexture textureOrView) {
        if (textureOrView instanceof VulkanTextureView vtv) {
            return vtv.handle();
        }
        if (textureOrView instanceof VulkanTexture vt) {
            return vt.getImageView();
        }
        if (textureOrView instanceof VulkanExternalTexture vet) {
            return vet.getImageView();
        }
        throw new IllegalArgumentException("Cannot resolve image view from: " + textureOrView.getClass());
    }

    private long resolveStorageImageView(ITexture textureOrView) {
        if (textureOrView instanceof VulkanTextureView vtv) {
            return vtv.handle();
        }
        if (textureOrView instanceof VulkanTexture vt) {
            return vt.getImageView();
        }
        if (textureOrView instanceof VulkanExternalTexture vet) {
            return vet.getImageView();
        }
        throw new IllegalArgumentException("Cannot resolve storage image view from: " + textureOrView.getClass());
    }

    private long defaultSampler = VK_NULL_HANDLE;

    private long getOrCreateDefaultSampler() {
        if (defaultSampler == VK_NULL_HANDLE) {
            try (MemoryStack stack = stackPush()) {
                VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                        .magFilter(VK_FILTER_NEAREST)
                        .minFilter(VK_FILTER_NEAREST)
                        .mipmapMode(VK_SAMPLER_MIPMAP_MODE_NEAREST)
                        .addressModeU(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                        .addressModeV(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                        .addressModeW(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                        .minLod(0.0f)
                        .maxLod(0.0f);

                LongBuffer pSampler = stack.mallocLong(1);
                VK_CHECK(vkCreateSampler(device.getVkDevice(), samplerInfo, null, pSampler),
                        "Failed to create default sampler");
                defaultSampler = pSampler.get(0);
            }
        }
        return defaultSampler;
    }

    private static int toVkDescriptorType(ShaderResourceType type) {
        return switch (type) {
            case UniformBuffer -> VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
            case SamplerTexture -> VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
            case StorageTexture -> VK_DESCRIPTOR_TYPE_STORAGE_IMAGE;
        };
    }

    public long getDescriptorSetLayout() {
        return descriptorSetLayout;
    }

    public void destroy() {
        if (defaultSampler != VK_NULL_HANDLE) {
            vkDestroySampler(device.getVkDevice(), defaultSampler, null);
            defaultSampler = VK_NULL_HANDLE;
        }
        if (descriptorSetLayout != VK_NULL_HANDLE) {
            vkDestroyDescriptorSetLayout(device.getVkDevice(), descriptorSetLayout, null);
            descriptorSetLayout = VK_NULL_HANDLE;
        }
    }
}
