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

package io.homo.superresolution.common.upscale.fsr1;

import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.api.InitializationDescription;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.InteropResourcesConverter;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import io.homo.superresolution.core.graphics.impl.buffer.*;
import io.homo.superresolution.core.graphics.impl.command.*;
import io.homo.superresolution.core.graphics.impl.framebuffer.*;
import io.homo.superresolution.core.graphics.impl.pipeline.*;
import io.homo.superresolution.core.graphics.impl.shader.*;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderResourceAccess;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.*;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.graphics.opengl.texture.*;
import io.homo.superresolution.core.graphics.vulkan.*;
import org.joml.Vector3i;

import static org.lwjgl.opengl.EXTSemaphore.GL_LAYOUT_GENERAL_EXT;
import static org.lwjgl.opengl.EXTSemaphore.GL_LAYOUT_SHADER_READ_ONLY_EXT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_ALL_COMMANDS_BIT;

public class FSR1 extends AbstractAlgorithm {
    private IShaderProgram fsr1EASUShader;
    private IShaderProgram fsr1RCASShader;
    private ComputePipeline fsr1EASUPipeline;
    private ComputePipeline fsr1RCASPipeline;
    private StructuredData fsr1UBOData;
    private IBuffer fsr1UBO;

    #if !(IS_VULKAN == 1)
    private ITexture fsr1TempTexture;
    private IFrameBuffer outputFbo;
    private ITexture output;
    #else
    private static final int COMMAND_BUFFER_RING_SIZE = 5;
    private final VulkanCommandBufferRing commandBufferRing = new VulkanCommandBufferRing(COMMAND_BUFFER_RING_SIZE);

    private GlImportableTexture2D inputColorGlTexture;
    private VulkanTexture inputColorVkTexture;

    private VulkanTexture fsr1TempVkTexture;

    private GlImportableTexture2D outputColorGlTexture;
    private VulkanTexture outputColorVkTexture;

    private GlTexture2D flippedOutputGlTexture;
    private IFrameBuffer outputFbo;

    private VkGlInteropSemaphore glFinishSemaphore;
    private VkGlInteropSemaphore upscaleVkFinish;
    #endif

    public static int checkFP16Support() {
        if (GraphicsCapabilities.hasGLExtension("GL_EXT_shader_16bit_storage") &&
                GraphicsCapabilities.hasGLExtension("GL_EXT_shader_explicit_arithmetic_types")) {
            return 1;
        }
        if (GraphicsCapabilities.hasGLExtension("GL_NV_gpu_shader5")) {
            return 2;
        }
        return 0;
    }

    @Override
    public void initialize(InitializationDescription desc) {
        this.initDesc = desc;
        fsr1UBOData = Std140StructBuilder.start()
                .vec2Entry("renderViewportSize")
                .vec2Entry("containerTextureSize")
                .vec2Entry("upscaledViewportSize")
                .floatEntry("sharpness")
                .build();
        #if !(IS_VULKAN == 1)
        fsr1UBO = RenderSystems.current().device().createBuffer(
                BufferDescription.create()
                        .size(fsr1UBOData.size())
                        .usages(BufferUsages.create().ubo().transferDst())
                        .build());
        #else
        fsr1UBO = RenderSystems.vulkan().device().createBuffer(
                BufferDescription.create()
                        .size(fsr1UBOData.size())
                        .usages(BufferUsages.create().ubo().transferDst())
                        .build());
        #endif
        initShader();
        #if !(IS_VULKAN == 1)
        fsr1TempTexture = RenderSystems.current().device().createTexture(
                TextureDescription.create()
                        .type(TextureType.Texture2D)
                        .width(RenderHandlerManager.getScreenWidth())
                        .height(RenderHandlerManager.getScreenHeight())
                        .format(SuperResolutionConfig.getInternalTextureFormat())
                        .usages(TextureUsages.create().sampler().storage())
                        .label("Fsr1TempTexture")
                        .build());
        output = RenderSystems.current().device().createTexture(
                TextureDescription.create()
                        .type(TextureType.Texture2D)
                        .width(RenderHandlerManager.getScreenWidth())
                        .height(RenderHandlerManager.getScreenHeight())
                        .format(SuperResolutionConfig.getInternalTextureFormat())
                        .usages(TextureUsages.create().sampler().storage())
                        .label("Fsr1OutputTexture")
                        .build());
        outputFbo = RenderSystems.current().device().createFramebuffer(
                FramebufferDescription.create()
                        .colorAttachment(output)
                        .label("Fsr1OutputFbo")
                        .build());
        #else
        createVkResources();
        #endif
    }

    #if (IS_VULKAN == 1)
    private void createVkResources() {
        VulkanDevice vkDevice = RenderSystems.vulkan().device();
        GlDevice glDevice = RenderSystems.opengl().device();
        vkDevice.getMainQueue().waitIdle();

        inputColorVkTexture = vkDevice.createTextureExportable(
                TextureDescription.create()
                        .usages(TextureUsages.create().sampler().storage())
                        .format(SuperResolutionConfig.getInternalTextureFormat())
                        .type(TextureType.Texture2D)
                        .width(RenderHandlerManager.getRenderWidth())
                        .height(RenderHandlerManager.getRenderHeight())
                        .label("FSR1InputColorVkTexture")
                        .build()
        );
        inputColorGlTexture = glDevice.createTextureImportable(inputColorVkTexture);

        fsr1TempVkTexture = (VulkanTexture) vkDevice.createTexture(
                TextureDescription.create()
                        .type(TextureType.Texture2D)
                        .usages(TextureUsages.create().sampler().storage())
                        .format(SuperResolutionConfig.getInternalTextureFormat())
                        .width(RenderHandlerManager.getScreenWidth())
                        .height(RenderHandlerManager.getScreenHeight())
                        .label("FSR1TempVkTexture")
                        .build()
        );

        outputColorVkTexture = vkDevice.createTextureExportable(
                TextureDescription.create()
                        .type(TextureType.Texture2D)
                        .usages(TextureUsages.create().sampler().storage().transferDestination())
                        .format(SuperResolutionConfig.getInternalTextureFormat())
                        .width(RenderHandlerManager.getScreenWidth())
                        .height(RenderHandlerManager.getScreenHeight())
                        .label("FSR1OutputColorVkTexture")
                        .build()
        );
        outputColorGlTexture = glDevice.createTextureImportable(outputColorVkTexture);

        flippedOutputGlTexture = (GlTexture2D) glDevice.createTexture(
                TextureDescription.create()
                        .type(TextureType.Texture2D)
                        .usages(TextureUsages.create().sampler().storage().transferDestination())
                        .format(SuperResolutionConfig.getInternalTextureFormat())
                        .width(RenderHandlerManager.getScreenWidth())
                        .height(RenderHandlerManager.getScreenHeight())
                        .label("FSR1FlippedOutputGlTexture")
                        .build()
        );

        outputFbo = RenderSystems.current().device().createFramebuffer(
                FramebufferDescription.create()
                        .colorAttachment(flippedOutputGlTexture)
                        .build()
        );

        glFinishSemaphore = VkGlInteropSemaphore.create(vkDevice);
        upscaleVkFinish = VkGlInteropSemaphore.create(vkDevice);
    }

    private void destroyVkResources() {
        if (inputColorGlTexture != null) { inputColorGlTexture.destroy(); inputColorGlTexture = null; }
        if (inputColorVkTexture != null) { inputColorVkTexture.destroy(); inputColorVkTexture = null; }
        if (fsr1TempVkTexture != null) { fsr1TempVkTexture.destroy(); fsr1TempVkTexture = null; }
        if (outputColorGlTexture != null) { outputColorGlTexture.destroy(); outputColorGlTexture = null; }
        if (outputColorVkTexture != null) { outputColorVkTexture.destroy(); outputColorVkTexture = null; }
        if (flippedOutputGlTexture != null) { flippedOutputGlTexture.destroy(); flippedOutputGlTexture = null; }
        if (outputFbo != null) { outputFbo.destroy(); outputFbo = null; }
        if (glFinishSemaphore != null) { glFinishSemaphore.destroy(); glFinishSemaphore = null; }
        if (upscaleVkFinish != null) { upscaleVkFinish.destroy(); upscaleVkFinish = null; }
    }
    #endif

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        super.dispatch(dispatchResource);

        fsr1UBOData.setVec2("renderViewportSize", RenderHandlerManager.getRenderWidth(),
                RenderHandlerManager.getRenderHeight());
        fsr1UBOData.setVec2("containerTextureSize", RenderHandlerManager.getRenderWidth(),
                RenderHandlerManager.getRenderHeight());
        fsr1UBOData.setVec2("upscaledViewportSize", RenderHandlerManager.getScreenWidth(),
                RenderHandlerManager.getScreenHeight());
        fsr1UBOData.setFloat("sharpness", SuperResolutionConfig.getSharpness());
        fsr1UBOData.fillBuffer();

        Vector3i workGroupSize = getWorkGroupSize();

        #if !(IS_VULKAN == 1)
        // EASU pass
        fsr1EASUPipeline.descriptorSet().samplerTexture("inImage", getResources().colorTexture());
        fsr1EASUPipeline.descriptorSet().storageImage("outImage", fsr1TempTexture);
        fsr1EASUPipeline.descriptorSet().uniformBuffer("fsr1_data", fsr1UBO);
        fsr1EASUPipeline.descriptorSet().update();

        // RCAS pass
        fsr1RCASPipeline.descriptorSet().storageImage("inImage", fsr1TempTexture);
        fsr1RCASPipeline.descriptorSet().storageImage("outImage", output);
        fsr1RCASPipeline.descriptorSet().uniformBuffer("fsr1_data", fsr1UBO);
        fsr1RCASPipeline.descriptorSet().update();

        ICommandBuffer commandBuffer = RenderSystems.current().device().defaultCommandPool().createCommandBuffer();
        commandBuffer.begin();
        commandBuffer.writeToBuffer(fsr1UBO,0, fsr1UBOData);
        commandBuffer.bindPipeline(fsr1EASUPipeline);
        commandBuffer.dispatch(workGroupSize.x, workGroupSize.y, workGroupSize.z);
        commandBuffer.bindPipeline(fsr1RCASPipeline);
        commandBuffer.dispatch(workGroupSize.x, workGroupSize.y, workGroupSize.z);
        commandBuffer.end();
        RenderSystems.current().device().submitCommandBuffer(commandBuffer);
        #else

        InteropResourcesConverter.flipY(dispatchResource.resources().colorTexture(), inputColorGlTexture);

        glFinishSemaphore.signalOpenGL(
                new int[]{Math.toIntExact(inputColorGlTexture.handle())},
                new int[]{},
                new int[]{GL_LAYOUT_SHADER_READ_ONLY_EXT}
        );
        inputColorVkTexture.setCurrentLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

        VulkanDevice vulkanDevice = RenderSystems.vulkan().device();
        VulkanCommandBuffer vkCommandBuffer = commandBufferRing.acquire(vulkanDevice);

        fsr1EASUPipeline.descriptorSet().samplerTexture("inImage", inputColorVkTexture);
        fsr1EASUPipeline.descriptorSet().storageImage("outImage", fsr1TempVkTexture);
        fsr1EASUPipeline.descriptorSet().uniformBuffer("fsr1_data", fsr1UBO);
        fsr1EASUPipeline.descriptorSet().update();

        fsr1RCASPipeline.descriptorSet().storageImage("inImage", fsr1TempVkTexture);
        fsr1RCASPipeline.descriptorSet().storageImage("outImage", outputColorVkTexture);
        fsr1RCASPipeline.descriptorSet().uniformBuffer("fsr1_data", fsr1UBO);
        fsr1RCASPipeline.descriptorSet().update();

        vkCommandBuffer.begin();
        vulkanDevice.commandDecoder().bindPipeline(vkCommandBuffer, fsr1EASUPipeline);
        vulkanDevice.commandDecoder().dispatch(vkCommandBuffer, workGroupSize.x, workGroupSize.y, workGroupSize.z);
        vulkanDevice.commandDecoder().bindPipeline(vkCommandBuffer, fsr1RCASPipeline);
        vulkanDevice.commandDecoder().dispatch(vkCommandBuffer, workGroupSize.x, workGroupSize.y, workGroupSize.z);
        vkCommandBuffer.end();

        vulkanDevice.submitCommandBuffer(
                vkCommandBuffer,
                new long[]{glFinishSemaphore.getVkSemaphoreHandle()},
                new int[]{VK_PIPELINE_STAGE_ALL_COMMANDS_BIT},
                new long[]{upscaleVkFinish.getVkSemaphoreHandle()}
        );

        upscaleVkFinish.waitOpenGL(
                new int[]{Math.toIntExact(outputColorGlTexture.handle())},
                new int[]{},
                new int[]{GL_LAYOUT_GENERAL_EXT}
        );

        InteropResourcesConverter.flipY(outputColorGlTexture, flippedOutputGlTexture);
        #endif
        return true;
    }

    @Override
    public void destroy() {
        #if (IS_VULKAN == 1)
        RenderSystems.vulkan().device().getMainQueue().waitIdle();
        commandBufferRing.destroy();
        destroyVkResources();
        #else
        if (output != null) { output.destroy(); output = null; }
        if (fsr1TempTexture != null) { fsr1TempTexture.destroy(); fsr1TempTexture = null; }
        if (outputFbo != null) { outputFbo.destroy(); outputFbo = null; }
        #endif
        if (fsr1EASUShader != null) { fsr1EASUShader.destroy(); fsr1EASUShader = null; }
        if (fsr1RCASShader != null) { fsr1RCASShader.destroy(); fsr1RCASShader = null; }
        if (fsr1UBOData != null) { fsr1UBOData.free(); fsr1UBOData = null; }
        if (fsr1UBO != null) { fsr1UBO.destroy(); fsr1UBO = null; }
        if (fsr1EASUPipeline != null) { fsr1EASUPipeline.destroy(); fsr1EASUPipeline = null; }
        if (fsr1RCASPipeline != null) { fsr1RCASPipeline.destroy(); fsr1RCASPipeline = null; }
    }

    @Override
    public void resize(int width, int height) {
        destroy();
        initialize(initDesc);
    }

    @Override
    public IFrameBuffer getOutputFrameBuffer() {
        return outputFbo;
    }

    @Override
    public int getOutputTextureId() {
        #if (IS_VULKAN == 1)
        return Math.toIntExact(flippedOutputGlTexture.handle());
        #else
        return Math.toIntExact(output.handle());
        #endif
    }

    public void initShader() {
        int fp16 = SuperResolutionConfig.SPECIAL.FSR1.FP16.get() ? checkFP16Support() : 0;
        #if (IS_VULKAN == 1)
        fsr1EASUShader = RenderSystems.vulkan().device().createShaderProgram(
                ShaderDescription
                        .compute(ShaderSource.file(ShaderType.Compute,
                                "/shader/fsr1/fsr1_main.comp.glsl"))
                        .name("FSR1_EASU")
                        .addDefine("FSR_FP16_CRITERIA", String.valueOf(fp16))
                        .addDefine("FSR_HALF", String.valueOf(fp16 == 0 ? 0 : 1))
                        .addDefine("FSR_EASU", String.valueOf(1))
                        .addDefine("SR_INTERNAL_TEXTURE_FORMAT", SuperResolutionConfig.getInternalTextureFormatGlslFormatQualifier())
                        .uniformBuffer("fsr1_data", 2, (int) fsr1UBOData.size())
                        .uniformSamplerTexture("inImage", 0)
                        .uniformStorageTexture("outImage", ShaderResourceAccess.Write, 1)
                        .build());
        fsr1RCASShader = RenderSystems.vulkan().device().createShaderProgram(
                ShaderDescription
                        .compute(ShaderSource.file(ShaderType.Compute,
                                "/shader/fsr1/fsr1_main.comp.glsl"))
                        .name("FSR1_RCAS")
                        .addDefine("FSR_FP16_CRITERIA", String.valueOf(fp16))
                        .addDefine("FSR_HALF", String.valueOf(fp16 == 0 ? 0 : 1))
                        .addDefine("FSR_RCAS", String.valueOf(1))
                        .addDefine("SR_INTERNAL_TEXTURE_FORMAT", SuperResolutionConfig.getInternalTextureFormatGlslFormatQualifier())
                        .uniformBuffer("fsr1_data", 2, (int) fsr1UBOData.size())
                        .uniformStorageTexture("inImage", ShaderResourceAccess.Read, 0)
                        .uniformStorageTexture("outImage", ShaderResourceAccess.Write, 1)
                        .build());
        fsr1EASUPipeline = ComputePipeline.builder().shader(fsr1EASUShader).build(RenderSystems.vulkan().device());
        fsr1RCASPipeline = ComputePipeline.builder().shader(fsr1RCASShader).build(RenderSystems.vulkan().device());
        #else
        fsr1EASUShader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription
                        .compute(ShaderSource.file(ShaderType.Compute,
                                "/shader/fsr1/fsr1_main.comp.glsl"))
                        .name("FSR1_EASU")
                        .addDefine("FSR_FP16_CRITERIA", String.valueOf(fp16))
                        .addDefine("FSR_HALF", String.valueOf(fp16 == 0 ? 0 : 1))
                        .addDefine("FSR_EASU", String.valueOf(1))
                        .addDefine("SR_INTERNAL_TEXTURE_FORMAT", SuperResolutionConfig.getInternalTextureFormatGlslFormatQualifier())
                        .uniformBuffer("fsr1_data", 2, (int) fsr1UBOData.size())
                        .uniformSamplerTexture("inImage", 0)
                        .uniformStorageTexture("outImage", ShaderResourceAccess.Write, 1)
                        .build());
        fsr1RCASShader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription
                        .compute(ShaderSource.file(ShaderType.Compute,
                                "/shader/fsr1/fsr1_main.comp.glsl"))
                        .name("FSR1_RCAS")
                        .addDefine("FSR_FP16_CRITERIA", String.valueOf(fp16))
                        .addDefine("FSR_HALF", String.valueOf(fp16 == 0 ? 0 : 1))
                        .addDefine("FSR_RCAS", String.valueOf(1))
                        .addDefine("SR_INTERNAL_TEXTURE_FORMAT", SuperResolutionConfig.getInternalTextureFormatGlslFormatQualifier())
                        .uniformBuffer("fsr1_data", 2, (int) fsr1UBOData.size())
                        .uniformStorageTexture("inImage", ShaderResourceAccess.Read, 0)
                        .uniformStorageTexture("outImage", ShaderResourceAccess.Write, 1)
                        .build());
        if (fp16 == 2) {
            if (fsr1EASUShader instanceof GlShaderProgram) {
                ((GlShaderProgram) fsr1EASUShader).compile(true);
            }
            if (fsr1RCASShader instanceof GlShaderProgram) {
                ((GlShaderProgram) fsr1RCASShader).compile(true);
            }
        } else {
            fsr1EASUShader.compile();
            fsr1RCASShader.compile();
        }
        fsr1EASUPipeline = RenderSystems.current().device().createComputePipeline(ComputePipeline.builder().shader(fsr1EASUShader));
        fsr1RCASPipeline = RenderSystems.current().device().createComputePipeline(ComputePipeline.builder().shader(fsr1RCASShader));
        #endif
    }

    private Vector3i getWorkGroupSize() {
        int workRegionDim = 16;
        int dispatchX = (RenderHandlerManager.getScreenWidth() + (workRegionDim - 1)) / workRegionDim;
        int dispatchY = (RenderHandlerManager.getScreenHeight() + (workRegionDim - 1)) / workRegionDim;
        return new Vector3i(
                dispatchX,
                dispatchY,
                1);
    }
}
