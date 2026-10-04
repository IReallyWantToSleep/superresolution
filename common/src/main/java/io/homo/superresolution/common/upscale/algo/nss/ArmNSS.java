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

package io.homo.superresolution.common.upscale.algo.nss;

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

public class ArmNSS extends SRApiAlgorithm {

    private static boolean providerLoaded = false;

    @Override
    protected void recreateSRApiContext(InitializationDescription desc) {
        if (NativeLibManager.LIB_SUPER_RESOLUTION_NSS == null) {
            SuperResolution.LOGGER.warn("NSS native library not available");
            return;
        }
        Path lib = NativeLibManager.LIB_SUPER_RESOLUTION_NSS.getTargetPath(SuperResolutionConstants.NATIVE_LIBRARIES_DIR.getPath());
        if (!(lib.toFile().isFile() && lib.toFile().canRead())) {
            SuperResolution.LOGGER.warn("NSS native library file not found or unreadable: {}", lib);
            return;
        }

        RenderSystems.vulkan().device().getMainQueue().waitIdle();

        if (!providerLoaded) {
            SuperResolutionNativeAPI.srLoadUpscaleProvidersFromLibrary(
                    lib.toAbsolutePath().toString(),
                    "srGetArmNSSUpscaleProviders",
                    "srGetArmNSSUpscaleProvidersCount"
            );
            providerLoaded = true;
        }

        try (SRUpscaleProvider provider = new SRUpscaleProvider(0)) {
            int providerId = 0x8000007;
            SRReturnCode providerCode = SuperResolutionNativeAPI.srGetUpscaleProvider(provider, providerId);
            SuperResolution.LOGGER.info("NSS provider (0x{}) acquisition: {}", Integer.toHexString(providerId), providerCode);

            this.context = new SRUpscaleContext(0);
            VulkanDevice vulkanDevice = RenderSystems.vulkan().device();
            EnumSet<SRUpscaleContextCreateFlags> flags = EnumSet.noneOf(SRUpscaleContextCreateFlags.class);
            flags.add(SRUpscaleContextCreateFlags.ENABLE_DEBUG);
            //if (desc.isAutoExposure()) {
            //    flags.add(SRUpscaleContextCreateFlags.ENABLE_AUTO_EXPOSURE);
            //}
            if (desc.isHdrInput()) {
                flags.add(SRUpscaleContextCreateFlags.ENABLE_HDR);
            }
            //if (desc.isMotionJittered()) {
            //    flags.add(SRUpscaleContextCreateFlags.ENABLE_MOTION_VECTORS_JITTERED);
            //}
            if (desc.isDepthInverted()) {
                flags.add(SRUpscaleContextCreateFlags.ENABLE_DEPTH_INVERTED);
            }
            try (
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
                            flags
                    );
                    SRContextExtraParams extraParams = new SRContextExtraParams()
            ) {
                upscaleContextDesc.setExtraParams(extraParams);
                extraParams.setString(
                        "NSS_DLL_PATH",
                        SuperResolutionConstants.NATIVE_LIBRARIES_DIR.getPath().resolve("ngsdk_windows_x64d.dll").toAbsolutePath().toString()
                );
                extraParams.setPointer(
                        "NSS_VK_QUEUE",
                        vulkanDevice.getMainQueue().getQueue().address()
                );
                extraParams.setUint32(
                        "NSS_VK_QUEUE_FAMILY_INDEX",
                        vulkanDevice.getMainQueue().getQueueFamilyIndex()
                );
                SRReturnCode createUpscaleContextCode = SuperResolutionNativeAPI.srCreateUpscaleContext(context, provider, upscaleContextDesc);
                if (createUpscaleContextCode != SRReturnCode.OK) {
                    SuperResolution.LOGGER.error("Failed to create upscale context. Return code: {}", createUpscaleContextCode);
                    context = null;
                    throw new RuntimeException("Failed to create upscale context");
                }
                SRReturnCode initUpscaleContextCode = SuperResolutionNativeAPI.srInitUpscaleContext(context);
                if (initUpscaleContextCode != SRReturnCode.OK) {
                    SuperResolution.LOGGER.error("Failed to initialize upscale context. Return code: {}", initUpscaleContextCode);
                    RuntimeException failure = new RuntimeException("Failed to initialize upscale context");
                    try {
                        destroySRApiContext();
                    } catch (Throwable cleanupFailure) {
                        failure.addSuppressed(cleanupFailure);
                    }
                    throw failure;
                }
            }
        }
        RenderSystems.vulkan().device().getMainQueue().waitIdle();
    }

    @Override
    protected void destroySRApiContext() {
        if (context == null) {
            return;
        }

        SRReturnCode code = context.destroy();
        if (code != SRReturnCode.OK) {
            SuperResolution.LOGGER.error("Failed to destroy NSS context: {}", code);
            throw new RuntimeException("Failed to destroy NSS context: " + code);
        }
        context = null;
    }

    @Override
    protected void dispatchSRApiContext(
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
            desc.setJitterOffset(
                    new Vector2f(
                            frameResourcesSet.frameData.jitterOffset().x * -1,
                            frameResourcesSet.frameData.jitterOffset().y * -1
                    )
            );
            desc.setMotionVectorScale(
                    new Vector2f(
                            frameResourcesSet.frameData.renderWidth(),
                            frameResourcesSet.frameData.renderHeight()
                    )
            );
            desc.setRenderSize(new Vector2i(
                    frameResourcesSet.frameData.renderWidth(),
                    frameResourcesSet.frameData.renderHeight()
            ));
            desc.setUpscaleSize(new Vector2i(
                    frameResourcesSet.frameData.screenWidth(),
                    frameResourcesSet.frameData.screenHeight()
            ));
            desc.setFrameTimeDelta(frameResourcesSet.frameData.frameTimeDelta());
            desc.setEnableSharpening(true);
            desc.setSharpness(SuperResolutionConfig.getSharpness());
            desc.setPreExposure(frameResourcesSet.frameData.preExposure());
            desc.setCameraNear(frameResourcesSet.frameData.cameraNear());
            desc.setCameraFar(frameResourcesSet.frameData.cameraFar());
            desc.setCameraFovAngleVertical((float) Math.toRadians(frameResourcesSet.frameData.verticalFov()));
            desc.setViewSpaceToMetersFactor(1.0f);
            desc.setReset(consumeHistoryReset());
            // Enable NSS runtime validation so malformed resources or motion-vector
            // ranges are reported by the SDK instead of silently producing a soft frame.
            desc.setFlags(1);

            SRReturnCode code = SuperResolutionNativeAPI.srDispatchUpscale(context, desc);
            if (code != SRReturnCode.OK) {
                SuperResolution.LOGGER.error("NSS dispatch failed with code: {}", code);
            }
        }
    }

}
