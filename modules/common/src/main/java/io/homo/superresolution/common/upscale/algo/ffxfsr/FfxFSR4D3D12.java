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
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.upscale.interoplayer.D3D12CommandContext;
import io.homo.superresolution.common.upscale.interoplayer.D3D12InteropAlgorithm;
import io.homo.superresolution.common.upscale.interoplayer.InteropDispatchResource;
import io.homo.superresolution.common.upscale.interoplayer.InteropFrameData;
import io.homo.superresolution.common.upscale.interoplayer.InteropResourceAccess;
import io.homo.superresolution.core.NativeLibManager;
import io.homo.superresolution.core.SuperResolutionConstants;
import io.homo.superresolution.core.graphics.d3d12.D3D12Device;
import io.homo.superresolution.core.graphics.d3d12.D3D12Texture2D;
import io.homo.superresolution.srapi.*;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Objects;

import static io.homo.superresolution.api.interop.InteropResourceType.*;

public final class FfxFSR4D3D12
        extends D3D12InteropAlgorithm {
    public static final String UPSCALER_DLL_NAME =
            "amd_fidelityfx_upscaler_dx12.dll";
    private static final long FSR4_PROVIDER_ID = 0x8000006L;
    private SRUpscaleContext context;

    private static SRTextureResource textureResource(
            D3D12Texture2D texture,
            SRResourceStates state) {
        Objects.requireNonNull(texture, "texture");
        Objects.requireNonNull(state, "state");
        SRTextureResource resource = new SRTextureResource(texture);
        resource.setStates(EnumSet.of(state));
        return resource;
    }

    private static EnumSet<SRUpscaleContextCreateFlags> createContextFlags(
            InitializationDescription desc) {
        EnumSet<SRUpscaleContextCreateFlags> flags =
                EnumSet.of(SRUpscaleContextCreateFlags.ENABLE_DEBUG);
        if (desc.isAutoExposure()) {
            flags.add(SRUpscaleContextCreateFlags.ENABLE_AUTO_EXPOSURE);
        }
        if (desc.isHdrInput()) {
            flags.add(SRUpscaleContextCreateFlags.ENABLE_HDR);
        }
        if (desc.isMotionJittered()) {
            flags.add(SRUpscaleContextCreateFlags.ENABLE_MOTION_VECTORS_JITTERED);
        }
        if (desc.isDepthInverted()) {
            flags.add(SRUpscaleContextCreateFlags.ENABLE_DEPTH_INVERTED);
        }
        return flags;
    }

    private static void requireReadable(Path path, String description) {
        if (!Files.isReadable(path)) {
            throw new IllegalStateException(description + " is missing: " + path);
        }
    }

    private static void requireSuccess(
            SRReturnCode code,
            String operation) {
        if (code != SRReturnCode.OK) {
            throw new IllegalStateException(
                    "Could not " + operation + ": " + code);
        }
    }

    @Override
    protected void createAlgorithmContext(InteropResourceAccess resources) {
        if (NativeLibManager.LIB_SUPER_RESOLUTION_FSR4 == null) {
            throw new IllegalStateException("FSR 4 provider library is unavailable");
        }
        Path providerLibrary = NativeLibManager.LIB_SUPER_RESOLUTION_FSR4
                .getTargetPath(SuperResolutionConstants.NATIVE_LIBRARIES_DIR.getPath())
                .toAbsolutePath();
        Path upscalerDll = SuperResolutionConstants.NATIVE_LIBRARIES_DIR
                .getPath()
                .resolve(UPSCALER_DLL_NAME)
                .toAbsolutePath();
        requireReadable(providerLibrary, "FSR provider library");
        requireReadable(upscalerDll, "AMD signed FFX upscaler DLL");

        SRUpscaleContext context = new SRUpscaleContext(0);
        this.context = context;
        try {
            SRReturnCode loadCode =
                    SuperResolutionNativeAPI.srLoadUpscaleProvidersFromLibrary(
                            providerLibrary.toString(),
                            "srGetFfxFSR4UpscaleProviders",
                            "srGetFfxFSR4UpscaleProvidersCount");
            requireSuccess(loadCode, "load FSR providers");

            try (SRUpscaleProvider provider = new SRUpscaleProvider(0)) {
                SRReturnCode providerCode =
                        SuperResolutionNativeAPI.srGetUpscaleProvider(
                                provider,
                                FSR4_PROVIDER_ID);
                requireSuccess(
                        providerCode,
                        "acquire the D3D12 FFX API provider");

                try (
                        SRCreateUpscaleContextDesc createDesc =
                                SRCreateUpscaleContextDesc.createD3D12(
                                        new SRD3D12DeviceInfo(
                                                ((D3D12Device) resources.device()).nativeDevice()),
                                        new Vector2i(
                                                resources.outputTexture().getWidth(),
                                                resources.outputTexture().getHeight()),
                                        new Vector2i(
                                                resources.executionTexture(Color).getWidth(),
                                                resources.executionTexture(Color).getHeight()),
                                        createContextFlags(initDesc))
                ) {
                    SRReturnCode pathCode =
                            createDesc.getExtraParams().setString(
                                    "ffxApiDllPath",
                                    upscalerDll.toString());
                    requireSuccess(pathCode, "configure the FFX API DLL path");

                    SRReturnCode createCode =
                            SuperResolutionNativeAPI.srCreateUpscaleContext(
                                    context,
                                    provider,
                                    createDesc);
                    requireSuccess(createCode, "create the FSR 4 context");

                    SRReturnCode initCode =
                            SuperResolutionNativeAPI.srInitUpscaleContext(context);
                    requireSuccess(initCode, "initialize the FSR 4 context");
                }
            }
        } catch (RuntimeException | Error failure) {
            if (context.nativePtr > 0) {
                try {
                    requireSuccess(context.destroy(), "destroy the incomplete FSR 4 context");
                    this.context = null;
                } catch (RuntimeException | Error cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            throw failure;
        }
    }

    @Override
    protected void destroyAlgorithmContext() {
        if (context == null || context.nativePtr <= 0) {
            return;
        }
        SRReturnCode code = context.destroy();
        requireSuccess(code, "destroy the FSR 4 context");
        context = null;
    }

    @Override
    protected boolean dispatchAlgorithm(InteropDispatchResource<D3D12CommandContext> resource) {
        if (context == null || context.nativePtr <= 0) {
            return false;
        }
        InteropFrameData frame = resource.frameData();
        try (SRDispatchUpscaleDesc desc = new SRDispatchUpscaleDesc()) {
            desc.setCommandBuffer(
                    SRDispatchCommandBufferInfo.createD3D12(resource.commandContext().leaseNativeCommandList()));
            desc.setColor(textureResource(
                    (D3D12Texture2D) resource.texture(Color),
                    SRResourceStates.COMPUTE_READ));
            desc.setDepth(textureResource(
                    (D3D12Texture2D) resource.texture(Depth),
                    SRResourceStates.COMPUTE_READ));
            desc.setMotionVectors(textureResource(
                    (D3D12Texture2D) resource.texture(MotionVectors),
                    SRResourceStates.COMPUTE_READ));
            if (resource.has(Exposure)) {
                desc.setExposure(textureResource(
                        (D3D12Texture2D) resource.texture(Exposure),
                        SRResourceStates.COMPUTE_READ));
            }
            desc.setOutput(textureResource(
                    (D3D12Texture2D) resource.outputTexture(),
                    SRResourceStates.COMMON));

            desc.setJitterOffset(new Vector2f(frame.jitterOffset()));
            desc.setMotionVectorScale(new Vector2f(
                    frame.renderWidth(),
                    frame.renderHeight()));
            desc.setRenderSize(new Vector2i(
                    frame.renderWidth(),
                    frame.renderHeight()));
            desc.setUpscaleSize(new Vector2i(
                    frame.screenWidth(),
                    frame.screenHeight()));
            desc.setFrameTimeDelta(frame.frameTimeDelta());
            desc.setEnableSharpening(true);
            desc.setSharpness(SuperResolutionConfig.getSharpness());
            desc.setPreExposure(frame.preExposure());
            desc.setCameraNear(frame.cameraNear());
            desc.setCameraFar(frame.cameraFar());
            desc.setCameraFovAngleVertical(
                    (float) Math.toRadians(frame.verticalFov()));
            desc.setViewSpaceToMetersFactor(1.0f);
            desc.setReset(consumeHistoryReset());
            desc.setFlags(0);

            SRReturnCode code =
                    SuperResolutionNativeAPI.srDispatchUpscale(context, desc);
            requireSuccess(code, "dispatch FSR 4");
        }
        return true;
    }
}
