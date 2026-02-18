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

package io.homo.superresolution.common.upscale.ffxfsr;

import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.minecraft.handler.shadercompat.ShaderCompatHandler;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.InteropResourcesConverter;
import io.homo.superresolution.core.NativeLibManager;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.SuperResolutionConstants;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsages;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.opengl.texture.GlImportableTexture2D;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.core.graphics.vulkan.VkGlInteropSemaphore;
import io.homo.superresolution.core.graphics.vulkan.VulkanCommandBuffer;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import io.homo.superresolution.core.graphics.vulkan.VulkanTexture;
import io.homo.superresolution.core.graphics.vulkan.utils.VkReflectionHelper;
import io.homo.superresolution.core.graphics.vulkan.utils.VulkanCommandBufferRing;
import io.homo.superresolution.srapi.*;
import io.homo.superresolution.thirdparty.fsr2.common.Fsr2Utils;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.nio.file.Path;
import java.util.EnumSet;

import static org.lwjgl.opengl.EXTSemaphore.GL_LAYOUT_GENERAL_EXT;
import static org.lwjgl.opengl.EXTSemaphore.GL_LAYOUT_SHADER_READ_ONLY_EXT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_ALL_COMMANDS_BIT;
import static org.lwjgl.vulkan.VK10.vkQueueWaitIdle;

public class FfxFSR extends AbstractAlgorithm {

    private static final int INITIAL_COMMAND_BUFFER_RING_SIZE = 3;
    private final VulkanCommandBufferRing commandBufferRing = new VulkanCommandBufferRing(
            INITIAL_COMMAND_BUFFER_RING_SIZE
    );
    private SRUpscaleContext context;
    private GlImportableTexture2D inputColorGlTexture;
    private VulkanTexture inputColorVkTexture;
    private GlImportableTexture2D inputDepthGlTexture;
    private VulkanTexture inputDepthVkTexture;
    private GlImportableTexture2D inputMotionVectorsGlTexture;
    private VulkanTexture inputMotionVectorsVkTexture;
    private GlImportableTexture2D outputColorGlTexture;
    private GlTexture2D outputColorTexture;
    private VulkanTexture outputColorVkTexture;
    private GlFrameBuffer outputFrameBuffer;
    private VkGlInteropSemaphore syncSemaphore;
    private VkGlInteropSemaphore syncVkSemaphore;

    public void updateFsr() {
        if (NativeLibManager.LIB_SUPER_RESOLUTION_FSR == null) {
            return;
        }
        Path lib = NativeLibManager.LIB_SUPER_RESOLUTION_FSR.getTargetPath(SuperResolutionConstants.NATIVE_LIBRARIES_DIR.getPath());
        if (!(lib.toFile().isFile() && lib.toFile().canRead())) {
            return;
        }
        vkQueueWaitIdle(((VulkanDevice) RenderSystems.vulkan().device()).getMainQueue().getQueue());

        if (context != null) {
            if (context.nativePtr > 0) {
                SuperResolutionNativeAPI.srDestroyUpscaleContext(context);
            }
        }

        SuperResolutionNativeAPI.srLoadUpscaleProvidersFromLibrary(
                lib.toAbsolutePath().toString(),
                "srGetFfxFSRUpscaleProviders",
                "srGetFfxFSRUpscaleProvidersCount"
        );
        SRUpscaleProvider provider = new SRUpscaleProvider(0);
        SuperResolutionNativeAPI.srGetUpscaleProvider(provider, 0x8000003);
        this.context = new SRUpscaleContext(0);
        VulkanDevice vulkanDevice = (VulkanDevice) RenderSystems.vulkan().device();

        SRCreateUpscaleContextDesc upscaleContextDesc = SRCreateUpscaleContextDesc.createVulkan(
                new SRVulkanDeviceInfo(
                        RenderSystems.vulkan().getVulkanInstance(),
                        vulkanDevice.getPhysicalDevice(),
                        vulkanDevice.getVkDevice(),
                        null,
                        vulkanDevice.getVkDevice().getCapabilities().vkGetDeviceProcAddr,
                        VkReflectionHelper.getVkGetInstanceProcAddr()
                ),
                new Vector2i(RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight()),
                new Vector2i(RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight()),
                EnumSet.of(
                        SRUpscaleContextCreateFlags.ENABLE_AUTO_EXPOSURE,
                        SRUpscaleContextCreateFlags.ENABLE_MOTION_VECTORS_JITTERED,
                        SRUpscaleContextCreateFlags.ENABLE_DEBUG
                )
        );
        SRReturnCode code = SuperResolutionNativeAPI.srCreateUpscaleContext(
                context,
                provider,
                upscaleContextDesc
        );
        SRReturnCode code0 = SuperResolutionNativeAPI.srInitUpscaleContext(
                context
        );
        SuperResolution.LOGGER.info(String.valueOf(code.value));
        SuperResolution.LOGGER.info(String.valueOf(code0.value));
        SuperResolution.LOGGER.info(String.valueOf(context.nativePtr));
        SuperResolution.LOGGER.info(String.valueOf(provider.nativePtr));
    }

    protected void destroySharedTexture() {
        vkQueueWaitIdle(((VulkanDevice) RenderSystems.vulkan().device()).getMainQueue().getQueue());

        if (this.inputColorVkTexture != null) {
            this.inputColorVkTexture.destroy();
        }
        if (this.inputColorGlTexture != null) {
            this.inputColorGlTexture.destroy();
        }
        if (this.inputDepthVkTexture != null) {
            this.inputDepthVkTexture.destroy();
        }
        if (this.inputDepthGlTexture != null) {
            this.inputDepthGlTexture.destroy();
        }
        if (this.outputColorVkTexture != null) {
            this.outputColorVkTexture.destroy();
        }
        if (this.outputColorGlTexture != null) {
            this.outputColorGlTexture.destroy();
        }
        if (this.inputMotionVectorsVkTexture != null) {
            this.inputMotionVectorsVkTexture.destroy();
        }
        if (this.inputMotionVectorsGlTexture != null) {
            this.inputMotionVectorsGlTexture.destroy();
        }
        if (this.outputFrameBuffer != null) {
            this.outputFrameBuffer.destroy();
        }
        if (this.outputColorTexture != null) {
            this.outputColorTexture.destroy();
        }
    }

    protected void createSharedTexture() {
        this.inputColorVkTexture = new VulkanTexture(
                (VulkanDevice) RenderSystems.vulkan().device(),
                TextureDescription.create()
                        .usages(TextureUsages.create().sampler().storage())
                        .format(SuperResolutionConfig.getInternalTextureFormat())
                        .type(TextureType.Texture2D)
                        .width(RenderHandlerManager.getRenderWidth())
                        .height(RenderHandlerManager.getRenderHeight())
                        .label("SRUpscaleInputColorVkTexture")
                        .build(),
                false,
                0,
                true
        );
        this.inputColorGlTexture = new GlImportableTexture2D(this.inputColorVkTexture);

        this.inputDepthVkTexture = new VulkanTexture(
                (VulkanDevice) RenderSystems.vulkan().device(),
                TextureDescription.create()
                        .usages(TextureUsages.create().sampler().storage())
                        .format(TextureFormat.DEPTH32F)
                        .type(TextureType.Texture2D)
                        .width(RenderHandlerManager.getRenderWidth())
                        .height(RenderHandlerManager.getRenderHeight())
                        .label("SRUpscaleInputDepthVkTexture")
                        .build(),
                false,
                0,
                true
        );
        this.inputDepthGlTexture = new GlImportableTexture2D(this.inputDepthVkTexture);

        this.inputMotionVectorsVkTexture = new VulkanTexture(
                (VulkanDevice) RenderSystems.vulkan().device(),
                TextureDescription.create()
                        .usages(TextureUsages.create().sampler().storage())
                        .format(TextureFormat.RG16F)
                        .type(TextureType.Texture2D)
                        .width(RenderHandlerManager.getRenderWidth())
                        .height(RenderHandlerManager.getRenderHeight())
                        .label("SRUpscaleInputMotionVectorsVkTexture")
                        .build(),
                false,
                0,
                true
        );
        this.inputMotionVectorsGlTexture = new GlImportableTexture2D(this.inputMotionVectorsVkTexture);

        this.outputColorVkTexture = new VulkanTexture(
                (VulkanDevice) RenderSystems.vulkan().device(),
                TextureDescription.create()
                        .type(TextureType.Texture2D)
                        .usages(TextureUsages.create().sampler().storage())
                        .format(SuperResolutionConfig.getInternalTextureFormat())
                        .width(RenderHandlerManager.getScreenWidth())
                        .height(RenderHandlerManager.getScreenHeight())
                        .label("SRUpscaleOutputColorVkTexture")
                        .build(),
                false,
                0,
                true
        );
        this.outputColorGlTexture = new GlImportableTexture2D(this.outputColorVkTexture);
        this.outputColorTexture = GlTexture2D.create(
                TextureDescription.create()
                        .type(TextureType.Texture2D)
                        .usages(TextureUsages.create().sampler().storage())
                        .format(SuperResolutionConfig.getInternalTextureFormat())
                        .width(RenderHandlerManager.getScreenWidth())
                        .height(RenderHandlerManager.getScreenHeight())
                        .label("SRUpscaleOutputColorGlTexture_FfxFsr")
                        .build()
        );
        this.outputFrameBuffer = GlFrameBuffer.create(this.outputColorTexture, null);
    }

    @Override
    public void init() {
        createSharedTexture();
        syncSemaphore = VkGlInteropSemaphore.create((VulkanDevice) RenderSystems.vulkan().device());
        syncVkSemaphore = VkGlInteropSemaphore.create((VulkanDevice) RenderSystems.vulkan().device());
        resize(RenderHandlerManager.getScreenWidth(),
                RenderHandlerManager.getScreenHeight()
        );
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        super.dispatch(dispatchResource);

        if (context == null || context.nativePtr < 1) {
            return false;
        }
        InteropResourcesConverter.flipY(
                dispatchResource.resources().colorTexture(),
                this.inputColorGlTexture);
        InteropResourcesConverter.preprocessDepth(
                dispatchResource.resources().depthTexture(),
                this.inputDepthGlTexture);

        if (dispatchResource.resources().motionVectorsTexture() != null) {
            InteropResourcesConverter.flipMotionVectorY(
                    dispatchResource.resources().motionVectorsTexture(),
                    this.inputMotionVectorsGlTexture);
        }
        syncSemaphore.signalOpenGL(
                new int[]{
                        Math.toIntExact(this.inputColorGlTexture.handle()),
                        Math.toIntExact(this.inputDepthGlTexture.handle()),
                        Math.toIntExact(this.inputMotionVectorsGlTexture.handle())
                },
                null,
                new int[]{
                        GL_LAYOUT_SHADER_READ_ONLY_EXT,
                        GL_LAYOUT_SHADER_READ_ONLY_EXT,
                        GL_LAYOUT_SHADER_READ_ONLY_EXT
                }
        );
        VulkanDevice vulkanDevice = (VulkanDevice) RenderSystems.vulkan().device();
        VulkanCommandBuffer commandBuffer = commandBufferRing.acquire(vulkanDevice);
        commandBuffer.begin();
        SRDispatchUpscaleDesc desc = new SRDispatchUpscaleDesc();
        desc.setCommandList(SRDispatchCommandBufferInfo.createVulkan(
                commandBuffer.getNativeCommandBuffer()
        ));
        desc.setColor(new SRTextureResource(this.inputColorVkTexture));
        desc.setDepth(new SRTextureResource(this.inputDepthVkTexture));
        desc.setMotionVectors(new SRTextureResource(this.inputMotionVectorsVkTexture));
        desc.setOutput(new SRTextureResource(this.outputColorVkTexture));
        desc.setJitterOffset(
                getOriginJitterOffset(
                        dispatchResource.frameCount(),
                        dispatchResource.renderSize(),
                        dispatchResource.screenSize())
                        .mul(new Vector2f(1, -1))
        );
        desc.setMotionVectorScale(new Vector2f(1));
        desc.setRenderSize(
                new Vector2i(
                        dispatchResource.renderWidth(),
                        dispatchResource.renderHeight()
                )
        );
        desc.setUpscaleSize(
                new Vector2i(
                        dispatchResource.screenWidth(),
                        dispatchResource.screenHeight()
                )
        );
        desc.setFrameTimeDelta(dispatchResource.frameTimeDelta());
        desc.setEnableSharpening(true);
        desc.setSharpness(SuperResolutionConfig.getSharpness());
        desc.setPreExposure(1.0f);
        desc.setCameraNear(dispatchResource.cameraNear());
        desc.setCameraFar(dispatchResource.cameraFar());
        desc.setCameraFovAngleVertical(dispatchResource.verticalFov());
        desc.setViewSpaceToMetersFactor(1.0f);
        desc.setReset(false);
        desc.setFlags(0);

        SRReturnCode code = SuperResolutionNativeAPI.srDispatchUpscale(
                context,
                desc
        );

        commandBuffer.end();
        if (code != SRReturnCode.OK) {
            return false;
        }
        vulkanDevice.submitCommandBuffer(
                commandBuffer,
                new long[]{syncSemaphore.getVkSemaphoreHandle()},
                new int[]{VK_PIPELINE_STAGE_ALL_COMMANDS_BIT},
                new long[]{syncVkSemaphore.getVkSemaphoreHandle()}
        );
        syncVkSemaphore.waitOpenGL(
                new int[]{Math.toIntExact(this.outputColorGlTexture.handle())},
                new int[]{},
                new int[]{GL_LAYOUT_GENERAL_EXT});
        InteropResourcesConverter.flipY(
                this.outputColorGlTexture,
                this.outputColorTexture);
        return true;
    }

    @Override
    public void destroy() {
        vkQueueWaitIdle(((VulkanDevice) RenderSystems.vulkan().device()).getMainQueue().getQueue());

        commandBufferRing.destroy();
        destroySharedTexture();
        syncSemaphore.destroy();
        syncVkSemaphore.destroy();

        if (context != null && context.nativePtr > 0) {
            SuperResolutionNativeAPI.srDestroyUpscaleContext(context);
        }
    }

    @Override
    public void resize(int width, int height) {
        vkQueueWaitIdle(((VulkanDevice) RenderSystems.vulkan().device()).getMainQueue().getQueue());

        commandBufferRing.destroy();
        updateFsr();
        destroySharedTexture();
        createSharedTexture();
    }

    @Override
    public IFrameBuffer getOutputFrameBuffer() {
        return outputFrameBuffer;
    }

    @Override
    public int getOutputTextureId() {
        return Math.toIntExact(outputColorGlTexture.handle());
    }

    @Override
    public Vector2f getJitterOffset(int frameCount, Vector2f renderSize, Vector2f screenSize) {
        //return new Vector2f(0);
        Vector2f originJitter = getOriginJitterOffset(frameCount, renderSize, screenSize);
        return new Vector2f(
                originJitter.x,
                originJitter.y
        );
    }

    @Override
    public boolean isSupportJitter() {
        return true;
    }

    private Vector2f getOriginJitterOffset(int frameCount, Vector2f renderSize, Vector2f screenSize) {
        if (!ShaderCompatHandler.dontHackMinecraftRenderingPipeline()) {
            return new Vector2f(0);
        }
        //halton
        int jitterPhaseCount = Fsr2Utils.ffxFsr2GetJitterPhaseCount(renderSize.x, screenSize.x);
        return Fsr2Utils.ffxFsr2GetJitterOffset(frameCount, jitterPhaseCount);
        //R2 参考PhotonShader
        /*
        return new Vector2f(
                (float) (Mth.frac(1.3247179572 * frameCount + 0.5) * 2.0 - 1.0),
                (float) (Mth.frac(1.7548776662 * frameCount + 0.5) * 2.0 - 1.0)
        );
        */
    }
}