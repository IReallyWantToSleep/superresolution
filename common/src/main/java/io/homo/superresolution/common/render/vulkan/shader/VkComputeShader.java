package io.homo.superresolution.common.render.vulkan.shader;

import io.homo.superresolution.common.render.vulkan.VkDeviceManager;
import io.homo.superresolution.common.render.vulkan.VkException;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import static org.lwjgl.system.MemoryUtil.memUTF8;
import static org.lwjgl.vulkan.VK10.*;

public class VkComputeShader {
    private final VkDeviceManager deviceManager;
    public long descriptorSetLayout = -1;
    public long descriptorSet = -1;
    public long pipelineLayout = -1;
    public long shaderModule = -1;
    public long pipeline = -1;
    public ArrayList<VkShaderUniform> uniforms = new ArrayList<>();
    public HashMap<Integer, VkShaderUniform> uniformsMap = new HashMap<>();
    private ByteBuffer shaderBin;

    public VkComputeShader(VkDeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }

    public VkComputeShader build() {
        if (shaderBin == null) throw new VkException();
        loadShader();
        createPipeline();
        return this;
    }

    public ByteBuffer getShaderBin() {
        return shaderBin;
    }

    public VkComputeShader setShaderBin(ByteBuffer bytes) {
        this.shaderBin = bytes;
        return this;
    }

    public VkComputeShader addUniform(VkShaderUniform uniform) {
        uniforms.add(uniform);
        uniformsMap.put(uniform.binding, uniform);
        return this;
    }

    public VkShaderUniform getUniform(int binding) {
        return uniformsMap.get(binding);
    }

    private void loadShader() {
        VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.create();
        createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
        createInfo.pCode(this.shaderBin);
        LongBuffer ptr = MemoryStack.stackCallocLong(1);
        vkCreateShaderModule(deviceManager.device, createInfo, null, ptr);
        shaderModule = ptr.get(0);
    }

    private void createPipeline() {
        if (shaderModule == -1) throw new VkException();
        {
            VkDescriptorSetLayoutBinding.Buffer bindLayout = VkDescriptorSetLayoutBinding.calloc(uniforms.size());
            for (VkShaderUniform uni : uniforms) {
                bindLayout.put(uni.build());
            }
            bindLayout.flip();
            VkDescriptorSetLayoutCreateInfo info = VkDescriptorSetLayoutCreateInfo.calloc();
            info.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
            info.pBindings(bindLayout);
            LongBuffer ptr = MemoryStack.stackCallocLong(1);
            vkCreateDescriptorSetLayout(deviceManager.device, info, null, ptr);
            descriptorSetLayout = ptr.get(0);
        }
        {
            VkDescriptorSetAllocateInfo info = VkDescriptorSetAllocateInfo.calloc();
            info.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
            info.descriptorPool(deviceManager.descriptorPool);
            info.pSetLayouts(MemoryStack.stackLongs(descriptorSetLayout));
            LongBuffer setsPtr = MemoryStack.stackCallocLong(1);
            vkAllocateDescriptorSets(deviceManager.device, info, setsPtr);
            descriptorSet = setsPtr.get(0);
        }
        {
            int bufferUniformCount = 0;
            for (VkShaderUniform uni : uniforms) if (uni.type == VkShaderUniformType.buffer) bufferUniformCount++;
            VkWriteDescriptorSet.Buffer writeDescSets = VkWriteDescriptorSet.calloc(bufferUniformCount);
            for (VkShaderUniform uni : uniforms) {
                if (uni.type != VkShaderUniformType.buffer) continue;
                VkDescriptorBufferInfo.Buffer bufferInfos = VkDescriptorBufferInfo.calloc(1);
                bufferInfos.put(uni.createBufferInfo(deviceManager));
                bufferInfos.flip();
                VkWriteDescriptorSet writeDescSet = VkWriteDescriptorSet.calloc();
                writeDescSet.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                writeDescSet.dstSet(descriptorSet);
                writeDescSet.dstBinding(uni.binding);
                writeDescSet.descriptorCount(1);
                writeDescSet.descriptorType(uni.type.getValue());
                writeDescSet.dstArrayElement(0);
                writeDescSet.pBufferInfo(bufferInfos);
                writeDescSets.put(writeDescSet);
            }
            writeDescSets.flip();
            vkUpdateDescriptorSets(deviceManager.device, writeDescSets, null);
        }
        {
            int bufferUniformCount = 0;
            for (VkShaderUniform uni : uniforms) if (uni.type == VkShaderUniformType.buffer) bufferUniformCount++;
            VkPushConstantRange.Buffer pushConstRange = VkPushConstantRange.calloc(bufferUniformCount);
            for (VkShaderUniform uni : uniforms) {
                if (uni.type != VkShaderUniformType.buffer) continue;
                VkPushConstantRange pushConst = VkPushConstantRange.calloc();
                pushConst.stageFlags(VK_SHADER_STAGE_COMPUTE_BIT);
                pushConst.size(uni.size);
                pushConst.offset(0);
                pushConstRange.put(pushConst);
            }
            pushConstRange.flip();
            VkPipelineLayoutCreateInfo info = VkPipelineLayoutCreateInfo.calloc();
            info.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
            LongBuffer descriptorSetLayout = MemoryStack.stackLongs(this.descriptorSetLayout);
            info.pSetLayouts(descriptorSetLayout);
            info.pPushConstantRanges(pushConstRange);
            LongBuffer ptr = MemoryStack.stackCallocLong(1);
            vkCreatePipelineLayout(deviceManager.device, info, null, ptr);
            pipelineLayout = ptr.get(0);
        }

        {
            VkPipelineShaderStageCreateInfo pipeShaderStageCreateInfo = VkPipelineShaderStageCreateInfo.create();
            pipeShaderStageCreateInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            pipeShaderStageCreateInfo.stage(VK_SHADER_STAGE_COMPUTE_BIT);
            pipeShaderStageCreateInfo.module(shaderModule);
            pipeShaderStageCreateInfo.pName(memUTF8("main"));
            VkComputePipelineCreateInfo.Buffer infos = VkComputePipelineCreateInfo.calloc(1);
            VkComputePipelineCreateInfo info = VkComputePipelineCreateInfo.calloc();
            info.sType(VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO);
            info.stage(pipeShaderStageCreateInfo);
            info.layout(pipelineLayout);
            infos.put(info);
            infos.flip();
            LongBuffer ptr = MemoryStack.stackCallocLong(1);
            vkCreateComputePipelines(deviceManager.device, VK_NULL_HANDLE, infos, null, ptr);
            pipeline = ptr.get(0);
        }
    }
}
