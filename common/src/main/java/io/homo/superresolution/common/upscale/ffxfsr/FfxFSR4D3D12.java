/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.upscale.ffxfsr;

import io.homo.superresolution.api.InitializationDescription;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.upscale.D3D12InteropAlgorithm;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.core.NativeLibManager;
import io.homo.superresolution.core.SuperResolutionConstants;
import io.homo.superresolution.core.graphics.d3d12.D3D12InteropContext;
import io.homo.superresolution.srapi.*;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

/**
 * AMD FSR 4.1 through the signed FFX API Direct3D 12 provider.
 */
public final class FfxFSR4D3D12 extends D3D12InteropAlgorithm {
    public static final String UPSCALER_DLL_NAME =
            "amd_fidelityfx_upscaler_dx12.dll";
    private static final long PROVIDER_ID = 0x8000006L;

    private SRUpscaleContext context;

    @Override
    protected void onD3D12InteropCreated(InitializationDescription desc) {
        Path providerLibrary = NativeLibManager.LIB_SUPER_RESOLUTION_FSR
                .getTargetPath(SuperResolutionConstants.NATIVE_LIBRARIES_DIR.getPath())
                .toAbsolutePath();
        Path upscalerDll = SuperResolutionConstants.NATIVE_LIBRARIES_DIR
                .getPath()
                .resolve(UPSCALER_DLL_NAME)
                .toAbsolutePath();
        if (!Files.isReadable(providerLibrary)) {
            throw new IllegalStateException(
                    "FSR provider library is missing: " + providerLibrary);
        }
        if (!Files.isReadable(upscalerDll)) {
            throw new IllegalStateException(
                    "AMD signed FFX upscaler DLL is missing: " + upscalerDll);
        }

        SRReturnCode loadCode =
                SuperResolutionNativeAPI.srLoadUpscaleProvidersFromLibrary(
                        providerLibrary.toString(),
                        "srGetFfxFSRUpscaleProviders",
                        "srGetFfxFSRUpscaleProvidersCount");
        if (loadCode != SRReturnCode.OK) {
            throw new IllegalStateException(
                    "Could not load FSR providers: " + loadCode);
        }

        try (SRUpscaleProvider provider = new SRUpscaleProvider(0)) {
            SRReturnCode providerCode =
                    SuperResolutionNativeAPI.srGetUpscaleProvider(
                            provider,
                            PROVIDER_ID);
            if (providerCode != SRReturnCode.OK) {
                throw new IllegalStateException(
                        "Could not acquire the D3D12 FFX API provider: " +
                                providerCode);
            }

            EnumSet<SRUpscaleContextCreateFlags> flags =
                    EnumSet.of(SRUpscaleContextCreateFlags.ENABLE_DEBUG);
            if (desc.isAutoExposure()) {
                flags.add(SRUpscaleContextCreateFlags.ENABLE_AUTO_EXPOSURE);
            }
            if (desc.isHdrInput()) {
                flags.add(SRUpscaleContextCreateFlags.ENABLE_HDR);
            }
            if (desc.isMotionJittered()) {
                flags.add(
                        SRUpscaleContextCreateFlags.ENABLE_MOTION_VECTORS_JITTERED);
            }

            context = new SRUpscaleContext(0);
            try (SRCreateUpscaleContextDesc createDesc =
                         SRCreateUpscaleContextDesc.createD3D12(
                                 new SRD3D12DeviceInfo(d3d12Interop.getDevice()),
                                 new Vector2i(
                                         RenderHandlerManager.getScreenWidth(),
                                         RenderHandlerManager.getScreenHeight()),
                                 new Vector2i(
                                         RenderHandlerManager.getRenderWidth(),
                                         RenderHandlerManager.getRenderHeight()),
                                 flags)) {
                SRReturnCode pathCode = createDesc
                        .getExtraParams()
                        .setString("ffxApiDllPath", upscalerDll.toString());
                if (pathCode != SRReturnCode.OK) {
                    throw new IllegalStateException(
                            "Could not configure the FFX API DLL path: " +
                                    pathCode);
                }

                SRReturnCode createCode =
                        SuperResolutionNativeAPI.srCreateUpscaleContext(
                                context,
                                provider,
                                createDesc);
                if (createCode != SRReturnCode.OK) {
                    context = null;
                    throw new IllegalStateException(
                            "Could not create the FSR 4.1 context: " +
                                    createCode);
                }
                SRReturnCode initCode =
                        SuperResolutionNativeAPI.srInitUpscaleContext(context);
                if (initCode != SRReturnCode.OK) {
                    context.destroy();
                    context = null;
                    throw new IllegalStateException(
                            "Could not initialize the FSR 4.1 context: " +
                                    initCode);
                }
            }
        }
    }

    @Override
    protected void onBeforeD3D12InteropDestroyed() {
        if (context != null) {
            if (context.nativePtr > 0) {
                SRReturnCode code = context.destroy();
                if (code != SRReturnCode.OK) {
                    SuperResolution.LOGGER.error(
                            "Failed to destroy FSR 4.1 context: {}",
                            code);
                }
            }
            context = null;
        }
    }

    @Override
    protected boolean isD3D12UpscalerReady() {
        return context != null && context.nativePtr > 0;
    }

    @Override
    protected boolean dispatchD3D12Upscale(
            long commandList,
            DispatchResource dispatchResource) {
        try (SRDispatchUpscaleDesc desc = new SRDispatchUpscaleDesc()) {
            desc.setCommandBuffer(
                    SRDispatchCommandBufferInfo.createD3D12(commandList));
            desc.setColor(resource(
                    d3d12Interop.inputColor(),
                    SRResourceStates.COMPUTE_READ));
            desc.setDepth(resource(
                    d3d12Interop.inputDepth(),
                    SRResourceStates.COMPUTE_READ));
            desc.setMotionVectors(resource(
                    d3d12Interop.inputMotionVectors(),
                    SRResourceStates.COMPUTE_READ));
            if (!initDesc.isAutoExposure() &&
                    dispatchResource.resources().exposureTexture() != null) {
                desc.setExposure(resource(
                        d3d12Interop.inputExposure(),
                        SRResourceStates.COMPUTE_READ));
            }
            desc.setOutput(resource(
                    d3d12Interop.outputColor(),
                    SRResourceStates.UNORDERED_ACCESS));

            desc.setJitterOffset(new Vector2f(dispatchResource.jitterOffset()));
            desc.setMotionVectorScale(new Vector2f(
                    dispatchResource.renderWidth(),
                    dispatchResource.renderHeight()));
            desc.setRenderSize(new Vector2i(
                    dispatchResource.renderWidth(),
                    dispatchResource.renderHeight()));
            desc.setUpscaleSize(new Vector2i(
                    dispatchResource.screenWidth(),
                    dispatchResource.screenHeight()));
            desc.setFrameTimeDelta(dispatchResource.frameTimeDelta());
            desc.setEnableSharpening(true);
            desc.setSharpness(SuperResolutionConfig.getSharpness());
            desc.setPreExposure(dispatchResource.preExposure());
            desc.setCameraNear(dispatchResource.cameraNear());
            desc.setCameraFar(dispatchResource.cameraFar());
            desc.setCameraFovAngleVertical(
                    (float) Math.toRadians(dispatchResource.verticalFov()));
            desc.setViewSpaceToMetersFactor(1.0f);
            desc.setReset(consumeHistoryReset());
            desc.setFlags(0);

            SRReturnCode code =
                    SuperResolutionNativeAPI.srDispatchUpscale(context, desc);
            if (code != SRReturnCode.OK) {
                SuperResolution.LOGGER.error(
                        "FSR 4.1 D3D12 dispatch failed: {}",
                        code);
                return false;
            }
            return true;
        }
    }

    private static SRTextureResource resource(
            D3D12InteropContext.Resource resource,
            SRResourceStates state) {
        SRTextureResourceDescription description =
                new SRTextureResourceDescription(
                        resource.srFormat(),
                        resource.textureDescription().getWidth(),
                        resource.textureDescription().getHeight(),
                        1,
                        SRResourceUsage.UAV.value);
        return new SRTextureResource(
                resource.nativeResource(),
                description,
                EnumSet.of(state));
    }
}
