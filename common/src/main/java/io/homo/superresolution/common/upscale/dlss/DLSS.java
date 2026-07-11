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

package io.homo.superresolution.common.upscale.dlss;

import io.homo.superresolution.api.InitializationDescription;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.config.enums.DLSSBackend;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.upscale.SRApiAlgorithm;
import io.homo.superresolution.core.NativeLibManager;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.SuperResolutionConstants;
import io.homo.superresolution.core.graphics.vulkan.VkReflectionHelper;
import io.homo.superresolution.core.graphics.vulkan.VulkanCommandBuffer;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import io.homo.superresolution.core.streamline.Streamline;
import io.homo.superresolution.core.utils.LargeStackExecutor;
import io.homo.superresolution.srapi.*;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.nio.file.Path;
import java.util.EnumSet;

public class DLSS extends SRApiAlgorithm {
    private static final long DLSS_PROVIDER_ID = 0x8000005L;
    private static String loadedProviderLibraryPath;
    private boolean usingStreamlineBackend;

    @Override
    protected void recreateSRApiContext(InitializationDescription desc) {
        NativeLibManager.NativeLib providerLibrary = selectProviderLibrary();
        if (providerLibrary == null) {
            return;
        }
        Path lib = providerLibrary.getTargetPath(SuperResolutionConstants.NATIVE_LIBRARIES_DIR.getPath());
        if (!(lib.toFile().isFile() && lib.toFile().canRead())) {
            return;
        }
        RenderSystems.vulkan().device().getMainQueue().waitIdle();
        String providerLibraryPath = lib.toAbsolutePath().toString();
        if (!providerLibraryPath.equals(loadedProviderLibraryPath)) {
            SRReturnCode unloadCode = SuperResolutionNativeAPI.srUnloadUpscaleProviders(DLSS_PROVIDER_ID);
            if (unloadCode != SRReturnCode.OK) {
                throw new RuntimeException("Failed to unload the previous DLSS provider: " + unloadCode);
            }
            SRReturnCode loadCode = SuperResolutionNativeAPI.srLoadUpscaleProvidersFromLibrary(
                    providerLibraryPath,
                    usingStreamlineBackend ? "srGetStreamlineUpscaleProviders" : "srGetDLSSUpscaleProviders",
                    usingStreamlineBackend ? "srGetStreamlineUpscaleProvidersCount" : "srGetDLSSUpscaleProvidersCount");
            if (loadCode != SRReturnCode.OK) {
                throw new RuntimeException("Failed to load the DLSS provider: " + loadCode);
            }
            loadedProviderLibraryPath = providerLibraryPath;
        }
        // NGX's NVSDK_NGX_VULKAN_Init reserves a ~1MB buffer on the stack; run the whole native
        // init on a large-stack thread so it can't overflow HotSpot's 1MB default render-thread
        // stack (which manifested as a bare SIGSEGV inside libnvidia-ngx with no hs_err).
        LargeStackExecutor.run("SR-DLSS-Init", () -> {
            try (SRUpscaleProvider provider = new SRUpscaleProvider(0)) {
                SuperResolution.LOGGER.info("'srGetUpscaleProvider' return code: {}",
                        SuperResolutionNativeAPI.srGetUpscaleProvider(
                                provider,
                                DLSS_PROVIDER_ID)
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
                        )
                ) {
                    String nativeLibraryDir = SuperResolutionConstants.NATIVE_LIBRARIES_DIR.getPath().toAbsolutePath().toString();
                    upscaleContextDesc.getExtraParams().setString("DLSS_BACKEND", usingStreamlineBackend ? "STREAMLINE" : "NGX");
                    if (usingStreamlineBackend) {
                        upscaleContextDesc.getExtraParams().setString("STREAMLINE_PLUGIN_PATH", nativeLibraryDir);
                        upscaleContextDesc.getExtraParams().setString("STREAMLINE_LOG_PATH",
                                SuperResolutionConstants.ERROR_DIR.getPath().toAbsolutePath().toString()
                        );
                    } else {
                        upscaleContextDesc.getExtraParams().setString("NGX_FEATURE_DLL_PATH", nativeLibraryDir);
                    }
                    upscaleContextDesc.getExtraParams().setInt32(
                            "DLSS_RENDER_PRESET",
                            SuperResolutionConfig.SPECIAL.DLSS.RENDER_PRESET.get().getCode()
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
        });
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
            if (!usingStreamlineBackend) {
                desc.setMotionVectorScale(new Vector2f(inFlightFrameResourcesSet.frameData.renderSize()));
            }else {
                desc.setMotionVectorScale(new Vector2f(inFlightFrameResourcesSet.frameData.renderSize()));
            }
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
            if (usingStreamlineBackend) {
                desc.getExtraParams().setUint32("STREAMLINE_FRAME_INDEX", inFlightFrameResourcesSet.frameData.frameCount());
            }
            SRReturnCode code = SuperResolutionNativeAPI.srDispatchUpscale(context, desc);
            if (code != SRReturnCode.OK) {
                SuperResolution.LOGGER.error("Failed to dispatch upscale context. Return code: {}", code);
            }
        }
    }

    private NativeLibManager.NativeLib selectProviderLibrary() {
        if (SuperResolutionConfig.getStartupDlssBackend() == DLSSBackend.STREAMLINE
                && Streamline.isSupportedOnCurrentVersion()
                && Streamline.isSupportedPlatform()
                && Streamline.isNativeAvailable()) {
            usingStreamlineBackend = true;
            return NativeLibManager.LIB_SUPER_RESOLUTION_STREAMLINE_DLSS;
        }
        usingStreamlineBackend = false;
        return NativeLibManager.LIB_SUPER_RESOLUTION_DLSS;
    }

}
