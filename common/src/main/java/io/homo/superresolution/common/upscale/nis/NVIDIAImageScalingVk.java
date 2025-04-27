package io.homo.superresolution.common.upscale.nis;

/*
import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.render.MinecraftRenderHandle;
import io.homo.superresolution.common.render.gl.texture.GlTexture;
import io.homo.superresolution.common.render.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.common.render.impl.texture.TextureFormat;
import io.homo.superresolution.common.render.interop.texture.SharedTexture;
import io.homo.superresolution.common.render.vulkan.shader.VkComputeShader;
import io.homo.superresolution.common.render.vulkan.shader.VkShaderUniform;
import io.homo.superresolution.common.render.vulkan.shader.VkShaderUniformType;
import io.homo.superresolution.common.render.vulkan.texture.TextureUsage;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.nis.enums.NISHDRMode;
import io.homo.superresolution.common.upscale.nis.struct.NISConfig;
import io.homo.superresolution.common.utils.FileReadHelper;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static io.homo.superresolution.common.render.gl.Gl.*;
import static io.homo.superresolution.common.render.gl.GlConst.GL_FRAMEBUFFER;
import static io.homo.superresolution.common.render.gl.GlConst.GL_TEXTURE_2D;
import static io.homo.superresolution.common.upscale.nis.NVIDIAImageScalingConst.*;
import static org.lwjgl.vulkan.VK10.*;
*/
public class NVIDIAImageScalingVk {
    /*
    private VkComputeShader shader;
    private SharedTexture inputSharedTexture;
    private SharedTexture outputSharedTexture;
    private NISConfig config;
    private IFrameBuffer output;

    @Override
    protected boolean isSupport() {
        return AlgorithmDescriptions.NIS.getRequirement().check().support() && SuperResolution.interopManager.vulkanApp != null;
    }

    public void initShader() {
        shader = new VkComputeShader(SuperResolution.interopManager.vulkanApp.deviceManager);
        shader.addUniform(new VkShaderUniform()
                .binding(CB_BINDING)
                .type(VkShaderUniformType.buffer)
                .size(256)
                .name("const_buffer"));
        shader.addUniform(new VkShaderUniform()
                .binding(SAMPLER_BINDING)
                .type(VkShaderUniformType.sampler)
                .sampler(SuperResolution.interopManager.vulkanApp.deviceManager.textureSampler)
                .name("samplerLinearClamp"));
        shader.addUniform(new VkShaderUniform()
                .binding(IN_TEX_BINDING)
                .type(VkShaderUniformType.storageImage)
                .name("in_texture"));
        shader.addUniform(new VkShaderUniform()
                .binding(OUT_TEX_BINDING)
                .type(VkShaderUniformType.sampledImage)
                .name("out_texture"));
        shader.addUniform(new VkShaderUniform()
                .binding(COEF_SCALAR_BINDING)
                .type(VkShaderUniformType.sampledImage)
                .name("coef_scaler"));
        shader.addUniform(new VkShaderUniform()
                .binding(COEF_USM_BINDING)
                .type(VkShaderUniformType.sampledImage)
                .name("coef_usm"));

        shader.setShaderBin(FileReadHelper.readSpvFile("/shader/nis/nis_scaler_glsl.spv"));
        shader.build();
        shader.getShaderBin().clear();
    }

    @Override
    public void init() {
        config = new NISConfig();
        initShader();
        input = MinecraftRenderHandle.getRenderTarget();
        output = MinecraftRenderHandle.getOriginRenderTarget();
        inputSharedTexture = new SharedTexture(input.getWidth(), input.getHeight(),
                SuperResolution.interopManager.vulkanApp.deviceManager);
        inputSharedTexture
                .setFormat(TextureFormat.RGBA8)
                .setUsage(TextureUsage.storageImage)
                .create();
        outputSharedTexture = new SharedTexture(output.getWidth(), output.getHeight(),
                SuperResolution.interopManager.vulkanApp.deviceManager);
        outputSharedTexture
                .setFormat(TextureFormat.RGBA8)
                .setUsage(TextureUsage.sampledImage)
                .create();
    }

    private void copyTexture() {
        glBindFramebuffer(GL_FRAMEBUFFER, input.getFrameBufferId());
        glBindTexture(GL_TEXTURE_2D, inputSharedTexture.glId);
        glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, input.getWidth(), input.getHeight());
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        updateBuffer();
        inputSharedTexture.startWrite();
        copyTexture();
        inputSharedTexture.endWrite();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            // INPUT
            VkDescriptorImageInfo.Buffer infoInWriteDescSet = VkDescriptorImageInfo.calloc(1, stack)
                    .imageView(inputSharedTexture.vkImageView)
                    .imageLayout(VK_IMAGE_LAYOUT_GENERAL);

            VkWriteDescriptorSet inWriteDescSet = VkWriteDescriptorSet.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(shader.descriptorSet)
                    .dstBinding(IN_TEX_BINDING)
                    .descriptorCount(1)
                    .descriptorType(VkShaderUniformType.storageImage.getValue())
                    .pImageInfo(infoInWriteDescSet);
            // coef_scaler
            VkDescriptorImageInfo.Buffer infoCoefScalerWriteDescSet = VkDescriptorImageInfo.calloc(1, stack)
                    .imageView(outputSharedTexture.vkImageView)
                    .imageLayout(VK_IMAGE_LAYOUT_GENERAL);

            VkWriteDescriptorSet coefScalerWriteDescSet = VkWriteDescriptorSet.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(shader.descriptorSet)
                    .dstBinding(COEF_SCALAR_BINDING)
                    .descriptorCount(1)
                    .descriptorType(VkShaderUniformType.sampledImage.getValue())
                    .pImageInfo(infoCoefScalerWriteDescSet);
            // coef_usm
            VkDescriptorImageInfo.Buffer infoCoefUSMWriteDescSet = VkDescriptorImageInfo.calloc(1, stack)
                    .imageView(outputSharedTexture.vkImageView)
                    .imageLayout(VK_IMAGE_LAYOUT_GENERAL);

            VkWriteDescriptorSet coefUSMWriteDescSet = VkWriteDescriptorSet.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(shader.descriptorSet)
                    .dstBinding(COEF_USM_BINDING)
                    .descriptorCount(1)
                    .descriptorType(VkShaderUniformType.sampledImage.getValue())
                    .pImageInfo(infoCoefUSMWriteDescSet);
            // OUTPUT
            VkDescriptorImageInfo.Buffer infoOutWriteDescSet = VkDescriptorImageInfo.calloc(1, stack)
                    .imageView(outputSharedTexture.vkImageView)
                    .imageLayout(VK_IMAGE_LAYOUT_GENERAL);

            VkWriteDescriptorSet outWriteDescSet = VkWriteDescriptorSet.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(shader.descriptorSet)
                    .dstBinding(OUT_TEX_BINDING)
                    .descriptorCount(1)
                    .descriptorType(VkShaderUniformType.sampledImage.getValue())
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
            IntBuffer offset = stack.ints(0);
            vkCmdBindDescriptorSets(cmdBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, shader.pipelineLayout, 0,
                    descriptorSetPtr, offset);

            int gridX = (int) Math.ceil((double) output.getWidth() / 32);
            int gridY = (int) Math.ceil((double) output.getHeight() / 24);
            vkCmdDispatch(cmdBuffer, gridX, gridY, 1);
            VkImageMemoryBarrier.Buffer imageBarrier = VkImageMemoryBarrier.calloc(1, stack);
            imageBarrier.get(0)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .oldLayout(VK_IMAGE_LAYOUT_GENERAL)
                    .newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .image(outputSharedTexture.vkImage.image)
                    .subresourceRange(it -> it
                            .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                            .baseMipLevel(0)
                            .levelCount(1)
                            .baseArrayLayer(0)
                            .layerCount(1))
                    .srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT)
                    .dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
            vkCmdPipelineBarrier(
                    cmdBuffer,
                    VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                    VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                    0,
                    null,
                    null,
                    imageBarrier);
            vkQueueWaitIdle(SuperResolution.interopManager.vulkanApp.deviceManager.graphicsQueue);
            SuperResolution.interopManager.vulkanApp.endOneTimeSubmitCmd();
        }

        return true;
    }

    @Override
    public void blitToScreen(int width, int height) {
        // 修改为使用 outputSharedTexture 的 glId 进行屏幕输出
        GlTexture.blitToScreen(inputSharedTexture.width, inputSharedTexture.height, width, height,
                inputSharedTexture.glId);
    }

    @Override
    public void destroy() {
        inputSharedTexture.clean();
        outputSharedTexture.clean();
    }

    @Override
    public void resize(int width, int height) {
        inputSharedTexture.resize(MinecraftRenderHandle.getRenderWidth(), MinecraftRenderHandle.getRenderHeight());
        outputSharedTexture.resize(MinecraftRenderHandle.getScreenWidth(), MinecraftRenderHandle.getScreenHeight());
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
    public IFrameBuffer getOutputFrameBuffer() {
        return output;
    }

    @Override
    public int getOutputTextureId() {
        // 修改为返回 outputSharedTexture 的 glId
        return outputSharedTexture.glId;
    }

    @Override
    public int getInputTextureId() {
        return inputSharedTexture.glId;
    }*/
}
