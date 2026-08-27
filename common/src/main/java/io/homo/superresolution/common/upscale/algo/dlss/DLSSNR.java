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

package io.homo.superresolution.common.upscale.algo.dlss;

import io.homo.superresolution.api.InitializationDescription;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.upscale.SRApiAlgorithm;
import io.homo.superresolution.core.NativeLibManager;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.SuperResolutionConstants;
import io.homo.superresolution.core.graphics.vulkan.VkReflectionHelper;
import io.homo.superresolution.core.graphics.vulkan.VulkanCommandBuffer;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import io.homo.superresolution.srapi.*;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.nio.file.Path;
import java.util.EnumSet;

public class DLSSNR extends SRApiAlgorithm {
    @Override
    protected void recreateSRApiContext(InitializationDescription desc) {
        if (NativeLibManager.LIB_SUPER_RESOLUTION_DLSSNR == null) {
            return;
        }
        Path lib = NativeLibManager.LIB_SUPER_RESOLUTION_DLSSNR
                .getTargetPath(SuperResolutionConstants.NATIVE_LIBRARIES_DIR.getPath());
        if (!(lib.toFile().isFile() && lib.toFile().canRead())) {
            return;
        }

        destroySRApiContext();
        SuperResolutionNativeAPI.srLoadUpscaleProvidersFromLibrary(
                lib.toAbsolutePath().toString(),
                "srGetDLSSNRUpscaleProviders",
                "srGetDLSSNRUpscaleProvidersCount");
        try (SRUpscaleProvider provider = new SRUpscaleProvider(0)) {
            SuperResolution.LOGGER.info("'srGetUpscaleProvider' return code: {}",
                    SuperResolutionNativeAPI.srGetUpscaleProvider(
                            provider,
                            0x8000007)
            );

            this.context = new SRUpscaleContext(0);
            VulkanDevice vulkanDevice = RenderSystems.vulkan().device();
            VulkanCommandBuffer commandBuffer = vulkanDevice.createCommandBuffer();
            EnumSet<SRUpscaleContextCreateFlags> flags = EnumSet.noneOf(SRUpscaleContextCreateFlags.class);
            if (desc.isAutoExposure()) {
                flags.add(
                        SRUpscaleContextCreateFlags.ENABLE_AUTO_EXPOSURE
                );
            }
            if (desc.isHdrInput()) {
                flags.add(
                        SRUpscaleContextCreateFlags.ENABLE_HDR
                );
            }
            if (desc.isMotionJittered()) {
                flags.add(
                        SRUpscaleContextCreateFlags.ENABLE_MOTION_VECTORS_JITTERED
                );
            }
            try (
                    SRCreateUpscaleContextDesc upscaleContextDesc = SRCreateUpscaleContextDesc.createVulkan(
                            new SRVulkanDeviceInfo(
                                    RenderSystems.vulkan().getVulkanInstance(),
                                    vulkanDevice.getPhysicalDevice(),
                                    vulkanDevice.getVkDevice(),
                                    commandBuffer.getNativeCommandBuffer(),
                                    vulkanDevice.getVkDevice().getCapabilities().vkGetDeviceProcAddr,
                                    VkReflectionHelper.getVkGetInstanceProcAddr()),
                            new Vector2i(RenderHandlerManager.getScreenWidth(),
                                    RenderHandlerManager.getScreenHeight()),
                            new Vector2i(RenderHandlerManager.getRenderWidth(),
                                    RenderHandlerManager.getRenderHeight()),
                            flags
                    );
                    SRContextExtraParams extraParams = new SRContextExtraParams()
            ) {
                upscaleContextDesc.setExtraParams(extraParams);
                extraParams.setString(
                        "DLSSNR_DLL_PATH",
                        SuperResolutionConstants.NATIVE_LIBRARIES_DIR.getPath().resolve("nvngx_dlssnr.dll").toAbsolutePath().toString()
                );
                extraParams.setFloat(
                        "DLSSNR_SCALING_RATIO",
                        RenderHandlerManager.getScreenWidth() / (float) RenderHandlerManager.getRenderWidth()
                );
                commandBuffer.begin();
                SRReturnCode createUpscaleContextCode = SuperResolutionNativeAPI.srCreateUpscaleContext(context, provider, upscaleContextDesc);
                SRReturnCode initUpscaleContextCode = createUpscaleContextCode == SRReturnCode.OK
                        ? SuperResolutionNativeAPI.srInitUpscaleContext(context)
                        : createUpscaleContextCode;
                commandBuffer.end();
                if (createUpscaleContextCode != SRReturnCode.OK) {
                    SuperResolution.LOGGER.error("Failed to create upscale context. Return code: {}", createUpscaleContextCode);
                    throw new RuntimeException("Failed to create upscale context");
                }
                if (initUpscaleContextCode != SRReturnCode.OK) {
                    SuperResolution.LOGGER.error("Failed to initialize upscale context. Return code: {}", initUpscaleContextCode);
                    throw new RuntimeException("Failed to initialize upscale context");
                }
                vulkanDevice.submitCommandBuffer(commandBuffer);
                commandBuffer.waitForFence();
            } finally {
                commandBuffer.destroy();
            }
        }
    }

    @Override
    protected void destroySRApiContext() {
        if (context != null) {
            SRReturnCode code = context.destroy();
            if (code != SRReturnCode.OK) {
                SuperResolution.LOGGER.error("Failed to destroy upscale context. Return code: {}", code);
                throw new RuntimeException("Failed to destroy upscale context");
            }
            context = null;
        }
    }

    @Override
    public void dispatchSRApiContext(
            VulkanCommandBuffer commandBuffer,
            InFlightFrameResourcesSet inFlightFrameResourcesSet

    ) {
        try (SRDispatchUpscaleDesc desc = new SRDispatchUpscaleDesc()) {
            desc.setCommandBuffer(SRDispatchCommandBufferInfo.createVulkan(commandBuffer.getNativeCommandBuffer()));
            desc.setColor(new SRTextureResource(inFlightFrameResourcesSet.inputColorVkTexture));
            desc.setDepth(new SRTextureResource(inFlightFrameResourcesSet.inputDepthVkTexture));
            desc.setMotionVectors(new SRTextureResource(inFlightFrameResourcesSet.inputMotionVectorsVkTexture));
            desc.setExposure(new SRTextureResource(inFlightFrameResourcesSet.inputExposureVkTexture));
            desc.setOutput(new SRTextureResource(inFlightFrameResourcesSet.outputColorVkTexture));
            desc.setJitterOffset(new Vector2f(inFlightFrameResourcesSet.frameData.jitterOffset()));
            desc.setMotionVectorScale(new Vector2f(inFlightFrameResourcesSet.frameData.renderSize()));
            desc.setRenderSize(new Vector2i(inFlightFrameResourcesSet.frameData.renderWidth(), inFlightFrameResourcesSet.frameData.renderHeight()));
            desc.setUpscaleSize(new Vector2i(inFlightFrameResourcesSet.frameData.screenWidth(), inFlightFrameResourcesSet.frameData.screenHeight()));
            desc.setFrameTimeDelta(inFlightFrameResourcesSet.frameData.frameTimeDelta());
            desc.setEnableSharpening(true);
            desc.setSharpness(SuperResolutionConfig.getSharpness());
            desc.setPreExposure(inFlightFrameResourcesSet.frameData.preExposure());
            desc.setCameraNear(inFlightFrameResourcesSet.frameData.cameraNear());
            desc.setCameraFar(inFlightFrameResourcesSet.frameData.cameraFar());
            desc.setCameraFovAngleVertical(inFlightFrameResourcesSet.frameData.verticalFov());
            desc.setViewSpaceToMetersFactor(1.0f);
            desc.setReset(consumeHistoryReset());
            desc.setFlags(0);
            SRContextExtraParams dispatchParams = desc.getExtraParams();
            dispatchParams.setFloat("DLSSNR_INTENSITY", SuperResolutionConfig.SPECIAL.DLSSNR.INTENSITY.get());
            dispatchParams.setFloat("DLSSNR_LOCAL_TONE_STRENGTH", SuperResolutionConfig.SPECIAL.DLSSNR.LOCAL_TONE_STRENGTH.get());
            dispatchParams.setFloat("DLSSNR_LOCAL_STRUCTURE_STRENGTH", SuperResolutionConfig.SPECIAL.DLSSNR.LOCAL_STRUCTURE_STRENGTH.get());
            dispatchParams.setFloat("DLSSNR_SKIN_STRUCTURE_STRENGTH", SuperResolutionConfig.SPECIAL.DLSSNR.SKIN_STRUCTURE_STRENGTH.get());
            dispatchParams.setUint32("DLSSNR_STYLE", Math.round(SuperResolutionConfig.SPECIAL.DLSSNR.STYLE.get()) & 0xFFFFFFFFL);
            dispatchParams.setInt32("DLSSNR_USE_AUTO_MASK", SuperResolutionConfig.SPECIAL.DLSSNR.USE_AUTO_MASK.get() ? 1 : 0);
            dispatchParams.setInt32("DLSSNR_UI_CORRECTION", SuperResolutionConfig.SPECIAL.DLSSNR.UI_CORRECTION.get() ? 1 : 0);
            dispatchParams.setInt32("DLSSNR_DEPTH_INVERTED", SuperResolutionConfig.SPECIAL.DLSSNR.DEPTH_INVERTED.get() ? 1 : 0);
            SRReturnCode code = SuperResolutionNativeAPI.srDispatchUpscale(context, desc);
            if (code != SRReturnCode.OK) {
                SuperResolution.LOGGER.error("Failed to dispatch upscale context. Return code: {}", code);
            }
        }
    }
}
