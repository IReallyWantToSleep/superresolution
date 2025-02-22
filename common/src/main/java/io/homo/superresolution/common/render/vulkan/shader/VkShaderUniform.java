package io.homo.superresolution.common.render.vulkan.shader;

import io.homo.superresolution.common.render.vulkan.VkDeviceManager;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VkShaderUniform {
    public VkShaderUniformType type;
    public int binding;
    public int stageFlags = VK_SHADER_STAGE_COMPUTE_BIT;
    public long sampler = -1;
    public int size;
    public String name;
    /// buffer
    public long bufferDeviceMemory = -1;
    public long bufferMemory = -1;
    public long bufferData = -1;
    public long bufferSize = 0;
    public PointerBuffer bufferDataPointer;

    public String getName() {
        return name;
    }

    public VkShaderUniform name(String name) {
        this.name = name;
        return this;
    }

    protected VkDescriptorSetLayoutBinding build() {
        VkDescriptorSetLayoutBinding bindingLayout = VkDescriptorSetLayoutBinding.calloc();
        bindingLayout.binding(binding);
        bindingLayout.descriptorType(type.getValue());
        bindingLayout.descriptorCount(1);
        bindingLayout.stageFlags(stageFlags);
        if (sampler != -1) {
            LongBuffer buf = MemoryStack.stackLongs(sampler);
            bindingLayout.pImmutableSamplers(buf);
        }
        return bindingLayout;
    }

    public VkShaderUniform type(VkShaderUniformType type) {
        this.type = type;
        return this;
    }

    public VkShaderUniform binding(int binding) {
        this.binding = binding;
        return this;
    }

    public VkShaderUniform stageFlags(int stageFlags) {
        this.stageFlags = stageFlags;
        return this;
    }

    public VkShaderUniform sampler(long sampler) {
        this.sampler = sampler;
        return this;
    }

    public VkShaderUniform size(int size) {
        this.size = size;
        return this;
    }

    protected VkDescriptorBufferInfo createBufferInfo(VkDeviceManager deviceManager) {
        LongBuffer buffer = MemoryStack.stackCallocLong(1);
        LongBuffer deviceBuffer = MemoryStack.stackCallocLong(1);
        bufferDataPointer = MemoryStack.stackCallocPointer(1);
        long align = deviceManager.physicalDeviceProperties.limits().minUniformBufferOffsetAlignment();
        bufferSize = ((size + align - 1) / align) * align;
        deviceManager.createBuffer(
                bufferSize,
                VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                buffer,
                deviceBuffer
        );
        bufferMemory = buffer.get(0);
        bufferDeviceMemory = deviceBuffer.get(0);
        vkMapMemory(deviceManager.device, bufferDeviceMemory, 0, bufferSize, 0, bufferDataPointer);
        this.bufferData = bufferDataPointer.get(0);
        VkDescriptorBufferInfo info = VkDescriptorBufferInfo.calloc();
        info.buffer(bufferMemory);
        info.offset(0);
        info.range(this.size);
        return info;
    }

    public ByteBuffer getBuffer() {
        return bufferDataPointer.getByteBuffer(0, size);
    }
}