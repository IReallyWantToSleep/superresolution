package io.homo.superresolution.common.upscale.nis;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.render.MinecraftRenderingStates;
import io.homo.superresolution.common.render.gl.texture.Texture;
import io.homo.superresolution.common.render.interop.SharedTexture;
import io.homo.superresolution.common.render.vulkan.TextureFormat;
import io.homo.superresolution.common.render.vulkan.VkComputeShader;
import io.homo.superresolution.common.render.vulkan.VkShaderUniform;
import io.homo.superresolution.common.render.vulkan.VkShaderUniformType;
import io.homo.superresolution.common.upscale.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.AlgorithmType;
import io.homo.superresolution.common.upscale.nis.enums.NISHDRMode;
import io.homo.superresolution.common.upscale.nis.struct.NISConfig;
import io.homo.superresolution.common.utils.FileReadHelper;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static io.homo.superresolution.common.render.gl.Gl.*;
import static io.homo.superresolution.common.render.gl.GlConst.GL_FRAMEBUFFER;
import static io.homo.superresolution.common.render.gl.GlConst.GL_TEXTURE_2D;
import static io.homo.superresolution.common.upscale.nis.NVIDIAImageScalingConst.*;
import static org.lwjgl.vulkan.VK10.*;

public class NVIDIAImageScaling extends AbstractAlgorithm {
    private VkComputeShader shader;
    private SharedTexture inputSharedTexture;
    private SharedTexture outputSharedTexture;
    private NISConfig config;
    private RenderTarget output;

    public static NVIDIAImageScaling create() {
        return new NVIDIAImageScaling();
    }

    @Override
    protected boolean isSupport() {
        return AlgorithmType.NIS.getValue().check().support();
    }

    public void initShader() {
        shader = new VkComputeShader(SuperResolution.interopManager.vulkanApp.deviceManager);
        shader.addUniform(new VkShaderUniform()
                .binding(CB_BINDING)
                .type(VkShaderUniformType.buffer)
                .size(256)
                .name("const_buffer")
        );
        shader.addUniform(new VkShaderUniform()
                .binding(SAMPLER_BINDING)
                .type(VkShaderUniformType.sampler)
                .sampler(SuperResolution.interopManager.vulkanApp.deviceManager.textureSampler)
                .name("samplerLinearClamp")
        );
        shader.addUniform(new VkShaderUniform()
                .binding(IN_TEX_BINDING)
                .type(VkShaderUniformType.storageImage)
                .name("in_texture")
        );
        shader.addUniform(new VkShaderUniform()
                .binding(OUT_TEX_BINDING)
                .type(VkShaderUniformType.sampledImage)
                .name("out_texture")
        );
        shader.addUniform(new VkShaderUniform()
                .binding(COEF_SCALAR_BINDING)
                .type(VkShaderUniformType.sampledImage)
                .name("coef_scaler")
        );
        shader.addUniform(new VkShaderUniform()
                .binding(COEF_USM_BINDING)
                .type(VkShaderUniformType.sampledImage)
                .name("coef_usm")
        );

        shader.setShaderBin(FileReadHelper.readSpvFile("/shader/nis_scaler_glsl.spv"));
        shader.build();
        shader.getShaderBin().clear();
    }

    @Override
    public void init() {
        config = new NISConfig();
        initShader();
        input = MinecraftRenderingStates.getRenderTarget();
        output = MinecraftRenderingStates.getOriginRenderTarget();
        inputSharedTexture = new SharedTexture(input.width, input.height, TextureFormat.RGBA8, SuperResolution.interopManager.vulkanApp.deviceManager);
        inputSharedTexture.create();
        outputSharedTexture = new SharedTexture(output.width, output.height, TextureFormat.RGBA8, SuperResolution.interopManager.vulkanApp.deviceManager);
        outputSharedTexture.create();
    }

    private void copyTexture() {
        glBindFramebuffer(GL_FRAMEBUFFER, input.frameBufferId);
        glBindTexture(GL_TEXTURE_2D, inputSharedTexture.glId);
        glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, input.width, input.height);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    @Override
    public boolean dispatch(float frameTimeDelta) {
        updateBuffer();
        inputSharedTexture.startWrite();
        copyTexture();
        inputSharedTexture.endWrite();

        try (MemoryStack stack = MemoryStack.stackPush()) {

            //INPUT
            VkDescriptorImageInfo.Buffer infoInWriteDescSet = VkDescriptorImageInfo.calloc(1, stack)
                    .imageView(inputSharedTexture.vkImageView)
                    .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

            VkWriteDescriptorSet inWriteDescSet = VkWriteDescriptorSet.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(shader.descriptorSet)
                    .dstBinding(IN_TEX_BINDING)
                    .descriptorCount(1)
                    .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
                    .pImageInfo(infoInWriteDescSet);
            //coef_scaler
            VkDescriptorImageInfo.Buffer infoCoefScalerWriteDescSet = VkDescriptorImageInfo.calloc(1, stack)
                    .imageView(inputSharedTexture.vkImageView)
                    .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

            VkWriteDescriptorSet coefScalerWriteDescSet = VkWriteDescriptorSet.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(shader.descriptorSet)
                    .dstBinding(COEF_SCALAR_BINDING)
                    .descriptorCount(1)
                    .descriptorType(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
                    .pImageInfo(infoCoefScalerWriteDescSet);
            //coef_usm
            VkDescriptorImageInfo.Buffer infoCoefUSMWriteDescSet = VkDescriptorImageInfo.calloc(1, stack)
                    .imageView(inputSharedTexture.vkImageView)
                    .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

            VkWriteDescriptorSet coefUSMWriteDescSet = VkWriteDescriptorSet.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(shader.descriptorSet)
                    .dstBinding(COEF_USM_BINDING)
                    .descriptorCount(1)
                    .descriptorType(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
                    .pImageInfo(infoCoefUSMWriteDescSet);
            //OUTPUT
            VkDescriptorImageInfo.Buffer infoOutWriteDescSet = VkDescriptorImageInfo.calloc(1, stack)
                    .imageView(outputSharedTexture.vkImageView)
                    .imageLayout(VK_IMAGE_LAYOUT_GENERAL);

            VkWriteDescriptorSet outWriteDescSet = VkWriteDescriptorSet.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(shader.descriptorSet)
                    .dstBinding(OUT_TEX_BINDING)
                    .descriptorCount(1)
                    .descriptorType(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
                    .pImageInfo(infoOutWriteDescSet);

            VkWriteDescriptorSet.Buffer writeDescSets = VkWriteDescriptorSet.calloc(4, stack)
                    .put(0, inWriteDescSet)
                    .put(1, outWriteDescSet)
                    .put(2, coefScalerWriteDescSet)
                    .put(3, coefUSMWriteDescSet);

            vkUpdateDescriptorSets(SuperResolution.interopManager.vulkanApp.deviceManager.device, writeDescSets, null);

            VkCommandBuffer cmdBuffer = SuperResolution.interopManager.vulkanApp.beginOneTimeSubmitCmd();
            vkCmdBindPipeline(cmdBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, shader.pipeline);

            LongBuffer descriptorSetPtr = stack.callocLong(1).put(shader.descriptorSet).flip();
            IntBuffer offset = stack.ints(256);
            vkCmdBindDescriptorSets(cmdBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, shader.pipelineLayout, 0, descriptorSetPtr, offset);

            int gridX = (int) Math.ceil((double) output.width / 32);
            int gridY = (int) Math.ceil((double) output.height / 24);
            vkCmdDispatch(cmdBuffer, gridX, gridY, 1);

            SuperResolution.interopManager.vulkanApp.endOneTimeSubmitCmd();
        }

        return true;
    }

    @Override
    public void blitToScreen(int width, int height) {
        outputSharedTexture.startRead();
        Texture.blitToScreen(outputSharedTexture.width, outputSharedTexture.height, width, height, outputSharedTexture.glId);
        outputSharedTexture.endRead();
    }

    @Override
    public void destroy() {
        inputSharedTexture.clean();
        outputSharedTexture.clean();
    }

    @Override
    public void resize(int width, int height) {
        inputSharedTexture.resize(MinecraftRenderingStates.getRenderWidth(), MinecraftRenderingStates.getRenderHeight());
        outputSharedTexture.resize(MinecraftRenderingStates.getScreenWidth(), MinecraftRenderingStates.getScreenHeight());
        NVIDIAImageScalingConfig.NVScalerUpdateConfig(config, 0.2f, 0, 0, input.width, input.height, input.width, input.height, 0, 0, width, height, width, height, NISHDRMode.None);
    }

    private void updateBuffer() {
        PointerBuffer bufferDataPointer = shader.getUniform(CB_BINDING).bufferDataPointer;
        ByteBuffer buffer = bufferDataPointer.getByteBuffer(256);

        buffer.putFloat(config.kDetectRatio)
                .putFloat(config.kDetectThres)
                .putFloat(config.kMinContrastRatio)
                .putFloat(config.kRatioNorm)
                .putFloat(config.kContrastBoost)
                .putFloat(config.kEps)
                .putFloat(config.kSharpStartY)
                .putFloat(config.kSharpScaleY)
                .putFloat(config.kSharpStrengthMin)
                .putFloat(config.kSharpStrengthScale)
                .putFloat(config.kSharpLimitMin)
                .putFloat(config.kSharpLimitScale)
                .putFloat(config.kScaleX)
                .putFloat(config.kScaleY)
                .putFloat(config.kDstNormX)
                .putFloat(config.kDstNormY)
                .putFloat(config.kSrcNormX)
                .putFloat(config.kSrcNormY)
                .putChar((char) config.kInputViewportOriginX)
                .putChar((char) config.kInputViewportOriginY)
                .putChar((char) config.kInputViewportWidth)
                .putChar((char) config.kInputViewportHeight)
                .putChar((char) config.kOutputViewportOriginX)
                .putChar((char) config.kOutputViewportOriginY)
                .putChar((char) config.kOutputViewportWidth)
                .putChar((char) config.kOutputViewportHeight)
                .putFloat(config.reserved0)
                .putFloat(config.reserved1);

        buffer.flip();
        bufferDataPointer.flip();
    }

    @Override
    public int getOutputTextureId() {
        return outputSharedTexture.glId;
    }

    @Override
    public int getInputTextureId() {
        return inputSharedTexture.glId;
    }
}