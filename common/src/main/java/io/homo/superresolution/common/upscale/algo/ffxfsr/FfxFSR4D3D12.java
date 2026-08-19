/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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

package io.homo.superresolution.common.upscale.algo.ffxfsr;

import io.homo.superresolution.api.InitializationDescription;
import io.homo.superresolution.api.InputResourceType;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.interoplayer.GlD3D12InteropAlgorithm;
import io.homo.superresolution.core.NativeLibManager;
import io.homo.superresolution.core.SuperResolutionConstants;
import io.homo.superresolution.core.graphics.d3d12.D3D12InteropContext;
import io.homo.superresolution.srapi.*;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Objects;

/**
 * AMD FSR 4.1 through the signed FFX API Direct3D 12 provider.
 */
public final class FfxFSR4D3D12 extends GlD3D12InteropAlgorithm<SRUpscaleContext> {
    public static final String UPSCALER_DLL_NAME = "amd_fidelityfx_upscaler_dx12.dll";
    private static final long PROVIDER_ID = 0x8000006L;

    private static SRTextureResource resource(D3D12InteropContext.Resource resource, SRResourceStates state) {
        Objects.requireNonNull(resource, "resource");
        Objects.requireNonNull(state, "state");
        SRTextureResourceDescription description = new SRTextureResourceDescription(
                resource.srFormat(),
                resource.textureDescription().getWidth(),
                resource.textureDescription().getHeight(),
                1,
                SRResourceUsage.UAV.value
        );
        return new SRTextureResource(resource.nativeResource(), description, EnumSet.of(state));
    }

    @Override
    protected SRUpscaleContext createD3D12Upscaler(InitializationDescription desc, D3D12InteropContext interop, InteropSize size) {
        Path providerLibrary = NativeLibManager.LIB_SUPER_RESOLUTION_FSR4
                .getTargetPath(SuperResolutionConstants.NATIVE_LIBRARIES_DIR.getPath())
                .toAbsolutePath();
        Path upscalerDll = SuperResolutionConstants.NATIVE_LIBRARIES_DIR
                .getPath()
                .resolve(UPSCALER_DLL_NAME)
                .toAbsolutePath();
        if (!Files.isReadable(providerLibrary)) {
            throw new IllegalStateException("FSR provider library is missing: " + providerLibrary);
        }
        if (!Files.isReadable(upscalerDll)) {
            throw new IllegalStateException("AMD signed FFX upscaler DLL is missing: " + upscalerDll);
        }

        SRReturnCode loadCode = SuperResolutionNativeAPI.srLoadUpscaleProvidersFromLibrary(
                providerLibrary.toString(),
                "srGetFfxFSR4UpscaleProviders",
                "srGetFfxFSR4UpscaleProvidersCount"
        );
        if (loadCode != SRReturnCode.OK) {
            throw new IllegalStateException("Could not load FSR providers: " + loadCode);
        }

        try (SRUpscaleProvider provider = new SRUpscaleProvider(0)) {
            SRReturnCode providerCode = SuperResolutionNativeAPI.srGetUpscaleProvider(provider, PROVIDER_ID);
            if (providerCode != SRReturnCode.OK) {
                throw new IllegalStateException("Could not acquire the D3D12 FFX API provider: " + providerCode);
            }

            EnumSet<SRUpscaleContextCreateFlags> flags = EnumSet.of(SRUpscaleContextCreateFlags.ENABLE_DEBUG);
            if (desc.isAutoExposure()) {
                flags.add(SRUpscaleContextCreateFlags.ENABLE_AUTO_EXPOSURE);
            }
            if (desc.isHdrInput()) {
                flags.add(SRUpscaleContextCreateFlags.ENABLE_HDR);
            }
            if (desc.isMotionJittered()) {
                flags.add(SRUpscaleContextCreateFlags.ENABLE_MOTION_VECTORS_JITTERED);
            }

            SRUpscaleContext context = new SRUpscaleContext(0);
            try (
                    SRCreateUpscaleContextDesc createDesc = SRCreateUpscaleContextDesc.createD3D12(
                            new SRD3D12DeviceInfo(interop.getDevice()),
                            new Vector2i(size.screenWidth(), size.screenHeight()),
                            new Vector2i(size.renderWidth(), size.renderHeight()),
                            flags
                    )
            ) {
                SRReturnCode pathCode = createDesc.getExtraParams().setString("ffxApiDllPath", upscalerDll.toString());
                if (pathCode != SRReturnCode.OK) {
                    throw new IllegalStateException("Could not configure the FFX API DLL path: " + pathCode);
                }

                SRReturnCode createCode = SuperResolutionNativeAPI.srCreateUpscaleContext(context, provider, createDesc);
                if (createCode != SRReturnCode.OK) {
                    throw new IllegalStateException("Could not create the FSR 4 context: " + createCode);
                }
                SRReturnCode initCode = SuperResolutionNativeAPI.srInitUpscaleContext(context);
                if (initCode != SRReturnCode.OK) {
                    context.destroy();
                    throw new IllegalStateException("Could not initialize the FSR 4 context: " + initCode);
                }
            }
            return context;
        }
    }

    @Override
    protected void destroyD3D12Upscaler(SRUpscaleContext context) {
        if (context.nativePtr > 0) {
            SRReturnCode code = context.destroy();
            if (code != SRReturnCode.OK) {
                SuperResolution.LOGGER.error("Failed to destroy FSR 4 context: {}", code);
            }
        }
    }

    @Override
    protected boolean dispatchD3D12Upscale(SRUpscaleContext context, D3D12InteropContext interop, long commandList, DispatchResource dispatchResource) {
        try (SRDispatchUpscaleDesc desc = new SRDispatchUpscaleDesc()) {
            desc.setCommandBuffer(SRDispatchCommandBufferInfo.createD3D12(commandList));
            desc.setColor(resource(interop.inputColor(), SRResourceStates.COMPUTE_READ));
            desc.setDepth(resource(interop.inputDepth(), SRResourceStates.COMPUTE_READ));
            desc.setMotionVectors(resource(interop.inputMotionVectors(), SRResourceStates.COMPUTE_READ));
            if (dispatchResource.resources().has(InputResourceType.Exposure)) {
                desc.setExposure(resource(interop.inputExposure(), SRResourceStates.COMPUTE_READ));
            }
            desc.setOutput(resource(interop.outputColor(), SRResourceStates.COMMON));

            desc.setJitterOffset(new Vector2f(dispatchResource.jitterOffset()));
            desc.setMotionVectorScale(new Vector2f(dispatchResource.renderWidth(), dispatchResource.renderHeight()));
            desc.setRenderSize(new Vector2i(dispatchResource.renderWidth(), dispatchResource.renderHeight()));
            desc.setUpscaleSize(new Vector2i(dispatchResource.screenWidth(), dispatchResource.screenHeight()));
            desc.setFrameTimeDelta(dispatchResource.frameTimeDelta());
            desc.setEnableSharpening(true);
            desc.setSharpness(SuperResolutionConfig.getSharpness());
            desc.setPreExposure(dispatchResource.preExposure());
            desc.setCameraNear(dispatchResource.cameraNear());
            desc.setCameraFar(dispatchResource.cameraFar());
            desc.setCameraFovAngleVertical((float) Math.toRadians(dispatchResource.verticalFov()));
            desc.setViewSpaceToMetersFactor(1.0f);
            desc.setReset(consumeHistoryReset());
            desc.setFlags(0);

            SRReturnCode code = SuperResolutionNativeAPI.srDispatchUpscale(context, desc);
            if (code != SRReturnCode.OK) {
                SuperResolution.LOGGER.error("FSR 4 dispatch failed: {}", code);
                return false;
            }
            return true;
        }
    }

    @Override
    protected boolean isD3D12UpscalerReady(SRUpscaleContext context) {
        return context.nativePtr > 0;
    }
}
