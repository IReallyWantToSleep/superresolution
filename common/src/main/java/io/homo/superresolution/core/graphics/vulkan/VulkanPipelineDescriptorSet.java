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
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.homo.superresolution.core.graphics.vulkan.VulkanUtils.VK_CHECK;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRPushDescriptor.VK_DESCRIPTOR_SET_LAYOUT_CREATE_PUSH_DESCRIPTOR_BIT_KHR;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanPipelineDescriptorSet extends PipelineDescriptorSet {
    private final VulkanDevice device;
    private long descriptorSetLayout = VK_NULL_HANDLE;
    private final Map<Integer, Long> samplerCache = new HashMap<>();
    private final DescriptorLayoutKey descriptorLayoutKey;

    public VulkanPipelineDescriptorSet(VulkanDevice device, IShaderProgram shader) {
        super(shader);
        this.device = device;
        this.descriptorLayoutKey = createDescriptorLayoutKey(shader.getDescription().resourcesLayout());
        createDescriptorSetLayout();
    }

    private static int toVkDescriptorType(ShaderResourceType type) {
        return switch (type) {
            case UniformBuffer -> VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
            case SamplerTexture -> VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
            case StorageTexture -> VK_DESCRIPTOR_TYPE_STORAGE_IMAGE;
        };
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
            device.setDebugName(VK_OBJECT_TYPE_DESCRIPTOR_SET_LAYOUT, descriptorSetLayout, "DescriptorSetLayout:" + shader.getDescription().shaderName());
        }
    }

    void pushDescriptorsIfNeeded(VulkanCommandBuffer commandBuffer, VkCommandBuffer cmd, int bindPoint, long pipelineLayout) {
        if (bindings.isEmpty()) {
            dirty = false;
            return;
        }

        List<DescriptorBindingSnapshotKey> requestedBindings = createDescriptorBindingSnapshotKeys();
        List<DescriptorBindingSnapshotKey> bindingsToPush = commandBuffer.collectDescriptorUpdates(
                bindPoint,
                0,
                descriptorLayoutKey,
                requestedBindings
        );
        if (bindingsToPush.isEmpty()) {
            dirty = false;
            return;
        }

        try (MemoryStack stack = stackPush()) {
            VkWriteDescriptorSet.Buffer writes = VkWriteDescriptorSet.calloc(bindingsToPush.size(), stack);

            for (int i = 0; i < bindingsToPush.size(); i++) {
                DescriptorBindingSnapshotKey binding = bindingsToPush.get(i);
                VkWriteDescriptorSet write = writes.get(i);
                write.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                        .dstBinding(binding.binding())
                        .dstArrayElement(0)
                        .descriptorCount(1);

                switch (binding.descriptorType()) {
                    case VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER -> {
                        VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1, stack)
                                .buffer(binding.buffer())
                                .offset(binding.bufferOffset())
                                .range(binding.bufferRange());
                        write.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                                .pBufferInfo(bufferInfo);
                    }
                    case VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER -> {
                        VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack)
                                .imageLayout(binding.imageLayout())
                                .imageView(binding.imageView())
                                .sampler(binding.sampler());
                        write.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                                .pImageInfo(imageInfo);
                    }
                    case VK_DESCRIPTOR_TYPE_STORAGE_IMAGE -> {
                        VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack)
                                .imageLayout(binding.imageLayout())
                                .imageView(binding.imageView())
                                .sampler(VK_NULL_HANDLE);
                        write.descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
                                .pImageInfo(imageInfo);
                    }
                    default -> throw new IllegalStateException("Unsupported descriptor type: " + binding.descriptorType());
                }
            }

            KHRPushDescriptor.vkCmdPushDescriptorSetKHR(cmd, bindPoint, pipelineLayout, 0, writes);
        }
        commandBuffer.recordDescriptorPush(bindPoint, 0, descriptorLayoutKey, bindingsToPush);
        dirty = false;
    }

    boolean needsPush() {
        return dirty;
    }

    @Override
    public void apply() {
    }

    @Override
    protected void updateImpl() {
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

    private long getOrCreateSamplerForTexture(ITexture texture) {
        int filterMode = texture.getTextureFilterMode().vk();
        int wrapMode = texture.getTextureWrapMode().vk();
        int key = (filterMode << 16) | wrapMode;
        Long sampler = samplerCache.get(key);
        if (sampler != null) {
            return sampler;
        }
        try (MemoryStack stack = stackPush()) {
            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                    .magFilter(filterMode)
                    .minFilter(filterMode)
                    .mipmapMode(VK_SAMPLER_MIPMAP_MODE_NEAREST)
                    .addressModeU(wrapMode)
                    .addressModeV(wrapMode)
                    .addressModeW(wrapMode)
                    .minLod(0.0f)
                    .maxLod(0.0f);

            LongBuffer pSampler = stack.mallocLong(1);
            VK_CHECK(vkCreateSampler(device.getVkDevice(), samplerInfo, null, pSampler),
                    "Failed to create sampler for texture");
            long handle = pSampler.get(0);
            samplerCache.put(key, handle);
            device.setDebugName(VK_OBJECT_TYPE_SAMPLER, handle, descriptorSamplerDebugLabel(texture, filterMode, wrapMode));
            return handle;
        }
    }

    private String descriptorSamplerDebugLabel(ITexture texture, int filterMode, int wrapMode) {
        String textureLabel = texture.getTextureDescription().getLabel();
        if (textureLabel != null && !textureLabel.isBlank()) {
            return "DescriptorSampler:" + textureLabel;
        }
        return "DescriptorSampler filter=" + filterMode + " wrap=" + wrapMode;
    }

    public long getDescriptorSetLayout() {
        return descriptorSetLayout;
    }

    public DescriptorLayoutKey getDescriptorLayoutKey() {
        return descriptorLayoutKey;
    }

    public void destroy() {
        for (long sampler : samplerCache.values()) {
            if (sampler != VK_NULL_HANDLE) {
                device.queueForDestroy(() -> vkDestroySampler(device.getVkDevice(), sampler, null));
            }
        }
        samplerCache.clear();
        long descriptorSetLayoutToDestroy = descriptorSetLayout;
        descriptorSetLayout = VK_NULL_HANDLE;
        if (descriptorSetLayoutToDestroy != VK_NULL_HANDLE) {
            device.queueForDestroy(() -> vkDestroyDescriptorSetLayout(device.getVkDevice(), descriptorSetLayoutToDestroy, null));
        }
    }

    private static DescriptorLayoutKey createDescriptorLayoutKey(ShaderResourcesLayout layout) {
        List<DescriptorLayoutBindingKey> bindingKeys = new ArrayList<>();
        for (ShaderResourceDescription res : layout.getResources().values()) {
            bindingKeys.add(new DescriptorLayoutBindingKey(
                    0,
                    res.binding(),
                    toVkDescriptorType(res.type()),
                    1,
                    VK_SHADER_STAGE_ALL,
                    0L
            ));
        }
        bindingKeys.sort(Comparator.comparingInt(DescriptorLayoutBindingKey::setIndex)
                .thenComparingInt(DescriptorLayoutBindingKey::binding));
        return new DescriptorLayoutKey(List.copyOf(bindingKeys));
    }

    private List<DescriptorBindingSnapshotKey> createDescriptorBindingSnapshotKeys() {
        List<DescriptorBindingSnapshotKey> snapshotKeys = new ArrayList<>();
        for (Map.Entry<String, ResourceBinding> entry : bindings.entrySet()) {
            ResourceBinding binding = entry.getValue();
            switch (binding.type()) {
                case UNIFORM_BUFFER -> {
                    IBuffer buffer = (IBuffer) binding.resource();
                    snapshotKeys.add(new DescriptorBindingSnapshotKey(
                            binding.bindingPoint(),
                            VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER,
                            buffer.handle(),
                            binding.offset(),
                            binding.range(),
                            0L,
                            0,
                            0L
                    ));
                }
                case SAMPLER_TEXTURE -> {
                    ITexture texture = (ITexture) binding.resource();
                    long imageView = resolveImageView(texture);
                    long sampler = binding.sampler() != null
                            ? binding.sampler().handle()
                            : getOrCreateSamplerForTexture(texture);
                    snapshotKeys.add(new DescriptorBindingSnapshotKey(
                            binding.bindingPoint(),
                            VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                            0L,
                            0L,
                            0L,
                            imageView,
                            VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                            sampler
                    ));
                }
                case STORAGE_IMAGE -> {
                    ITexture texture = (ITexture) binding.resource();
                    long imageView = resolveStorageImageView(texture);
                    snapshotKeys.add(new DescriptorBindingSnapshotKey(
                            binding.bindingPoint(),
                            VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                            0L,
                            0L,
                            0L,
                            imageView,
                            VK_IMAGE_LAYOUT_GENERAL,
                            0L
                    ));
                }
            }
        }
        snapshotKeys.sort(Comparator.comparingInt(DescriptorBindingSnapshotKey::binding)
                .thenComparingInt(DescriptorBindingSnapshotKey::descriptorType));
        return List.copyOf(snapshotKeys);
    }

    public record DescriptorLayoutKey(List<DescriptorLayoutBindingKey> bindings) {
    }

    public record DescriptorLayoutBindingKey(
            int setIndex,
            int binding,
            int descriptorType,
            int descriptorCount,
            int stageFlags,
            long immutableSamplerIdentity
    ) {
    }

    public record DescriptorBindingSnapshotKey(
            int binding,
            int descriptorType,
            long buffer,
            long bufferOffset,
            long bufferRange,
            long imageView,
            int imageLayout,
            long sampler
    ) {
    }
}
