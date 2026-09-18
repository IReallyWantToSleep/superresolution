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

package io.homo.superresolution.common.upscale.algo.xess;

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
import static io.homo.superresolution.api.interop.InteropResourceType.*;

public class XeSS extends SRApiAlgorithm {

    @Override
    protected void recreateSRApiContext(InitializationDescription desc) {
        if (NativeLibManager.LIB_SUPER_RESOLUTION_XESS == null) {
            return;
        }
        Path lib = NativeLibManager.LIB_SUPER_RESOLUTION_XESS
                .getTargetPath(SuperResolutionConstants.NATIVE_LIBRARIES_DIR.getPath());
        if (!(lib.toFile().isFile() && lib.toFile().canRead())) {
            return;
        }

        destroySRApiContext();
        SuperResolutionNativeAPI.srLoadUpscaleProvidersFromLibrary(
                lib.toAbsolutePath().toString(),
                "srGetXeSSUpscaleProviders",
                "srGetXeSSUpscaleProvidersCount");
        try (SRUpscaleProvider provider = new SRUpscaleProvider(0)) {
            SuperResolution.LOGGER.info("'srGetUpscaleProvider' return code: {}",
                    SuperResolutionNativeAPI.srGetUpscaleProvider(
                            provider,
                            0x8000004)
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
                        "XESS_DLL_PATH",
                        SuperResolutionConstants.NATIVE_LIBRARIES_DIR.getPath().resolve("libxess.dll").toAbsolutePath().toString()
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
            FrameResourcesSet frameResourcesSet

    ) {
        try (SRDispatchUpscaleDesc desc = new SRDispatchUpscaleDesc()) {
            desc.setCommandBuffer(SRDispatchCommandBufferInfo.createVulkan(commandBuffer.getNativeCommandBuffer()));
            desc.setColor(new SRTextureResource(frameResourcesSet.vulkan(Color)));
            desc.setDepth(new SRTextureResource(frameResourcesSet.vulkan(Depth)));
            desc.setMotionVectors(new SRTextureResource(frameResourcesSet.vulkan(MotionVectors)));
            if (frameResourcesSet.has(Exposure)) {
                desc.setExposure(new SRTextureResource(frameResourcesSet.vulkan(Exposure)));
            }
            desc.setOutput(new SRTextureResource(frameResourcesSet.vulkan(OutputColor)));
            desc.setJitterOffset(new Vector2f(frameResourcesSet.frameData.jitterOffset()));
            desc.setMotionVectorScale(new Vector2f(frameResourcesSet.frameData.renderSize()));
            desc.setRenderSize(new Vector2i(frameResourcesSet.frameData.renderWidth(), frameResourcesSet.frameData.renderHeight()));
            desc.setUpscaleSize(new Vector2i(frameResourcesSet.frameData.screenWidth(), frameResourcesSet.frameData.screenHeight()));
            desc.setFrameTimeDelta(frameResourcesSet.frameData.frameTimeDelta());
            desc.setEnableSharpening(true);
            desc.setSharpness(SuperResolutionConfig.getSharpness());
            desc.setPreExposure(frameResourcesSet.frameData.preExposure());
            desc.setCameraNear(frameResourcesSet.frameData.cameraNear());
            desc.setCameraFar(frameResourcesSet.frameData.cameraFar());
            desc.setCameraFovAngleVertical(frameResourcesSet.frameData.verticalFov());
            desc.setViewSpaceToMetersFactor(1.0f);
            desc.setReset(consumeHistoryReset());
            desc.setFlags(0);
            SRReturnCode code = SuperResolutionNativeAPI.srDispatchUpscale(context, desc);
            if (code != SRReturnCode.OK) {
                SuperResolution.LOGGER.error("Failed to dispatch upscale context. Return code: {}", code);
            }
        }
    }
}
