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
import io.homo.superresolution.core.graphics.d3d12.D3D12CommandBuffer;
import io.homo.superresolution.core.graphics.d3d12.D3D12Device;
import io.homo.superresolution.core.graphics.d3d12.D3D12ResourceState;
import io.homo.superresolution.core.graphics.d3d12.D3D12Texture2D;
import io.homo.superresolution.core.utils.ThrowableUtil;
import io.homo.superresolution.srapi.*;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * AMD FSR 4.1 through the signed FFX API Direct3D 12 provider.
 */
public final class FfxFSR4D3D12 extends GlD3D12InteropAlgorithm<FfxFSR4D3D12.ContextOwner> {
    public static final String UPSCALER_DLL_NAME = "amd_fidelityfx_upscaler_dx12.dll";
    private static final long PROVIDER_ID = 0x8000006L;
    private final List<ContextOwner> retainedContextOwners = new ArrayList<>();

    private static SRTextureResource resource(
            D3D12Texture2D texture,
            SRResourceStates state) {
        Objects.requireNonNull(texture, "texture");
        Objects.requireNonNull(state, "state");
        SRTextureResource resource = new SRTextureResource(texture);
        resource.setStates(EnumSet.of(state));
        return resource;
    }

    @Override
    public void initialize(InitializationDescription desc) {
        throwRetainedFailure(retryRetainedContextOwners(null));
        super.initialize(desc);
    }

    @Override
    public void destroy() {
        super.destroy();
        throwRetainedFailure(retryRetainedContextOwners(null));
    }

    @Override
    protected ContextOwner createD3D12Upscaler(
            InitializationDescription desc,
            D3D12Device device,
            InteropSize size) {
        throwRetainedFailure(retryRetainedContextOwners(null));

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

        SRUpscaleContext context = new SRUpscaleContext(0);
        ContextOwner owner = new ContextOwner(context);
        int retainedOwnerSlot = retainedContextOwners.size();
        retainedContextOwners.add(null);
        try {
            owner.pinDevice(device);
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

                try (
                        SRCreateUpscaleContextDesc createDesc = SRCreateUpscaleContextDesc.createD3D12(
                                new SRD3D12DeviceInfo(device.nativeDevice()),
                                new Vector2i(size.screenWidth(), size.screenHeight()),
                                new Vector2i(size.renderWidth(), size.renderHeight()),
                                flags
                        )
                ) {
                    SRReturnCode pathCode = createDesc.getExtraParams().setString(
                            "ffxApiDllPath",
                            upscalerDll.toString());
                    if (pathCode != SRReturnCode.OK) {
                        throw new IllegalStateException(
                                "Could not configure the FFX API DLL path: " + pathCode);
                    }

                    SRReturnCode createCode = SuperResolutionNativeAPI.srCreateUpscaleContext(
                            context,
                            provider,
                            createDesc);
                    if (createCode != SRReturnCode.OK) {
                        throw new IllegalStateException(
                                "Could not create the FSR 4 context: " + createCode);
                    }
                    SRReturnCode initCode = SuperResolutionNativeAPI.srInitUpscaleContext(context);
                    if (initCode != SRReturnCode.OK) {
                        throw new IllegalStateException(
                                "Could not initialize the FSR 4 context: " + initCode);
                    }
                }
            }
            retainedContextOwners.remove(retainedOwnerSlot);
            return owner;
        } catch (Throwable failure) {
            try {
                destroyD3D12Upscaler(owner);
                retainedContextOwners.remove(retainedOwnerSlot);
            } catch (Throwable destroyFailure) {
                retainedContextOwners.set(retainedOwnerSlot, owner);
                if (failure != destroyFailure) {
                    failure.addSuppressed(destroyFailure);
                }
            }
            throwRetainedFailure(failure);
            throw new AssertionError("unreachable");
        }
    }

    @Override
    protected void destroyD3D12Upscaler(ContextOwner owner) {
        if (owner.context.nativePtr > 0) {
            SRReturnCode code = owner.context.destroy();
            if (code != SRReturnCode.OK) {
                throw new IllegalStateException(
                        "Failed to destroy FSR 4 context: " + code);
            }
        }
        if (owner.deviceBorrow != null) {
            owner.deviceBorrow.close();
            owner.deviceBorrow = null;
        }
    }

    @Override
    protected boolean dispatchD3D12Upscale(
            ContextOwner owner,
            D3D12CommandBuffer commandBuffer,
            D3D12Resources resources,
            DispatchResource dispatchResource) {
        try (
                D3D12CommandBuffer.NativeCommandListLease commandList =
                        commandBuffer.leaseNativeCommandList();
                SRDispatchUpscaleDesc desc = new SRDispatchUpscaleDesc()
        ) {
            desc.setCommandBuffer(
                    SRDispatchCommandBufferInfo.createD3D12(commandList));
            desc.setColor(resource(
                    resources.inputColor(),
                    SRResourceStates.COMPUTE_READ));
            desc.setDepth(resource(
                    resources.inputDepth(),
                    SRResourceStates.COMPUTE_READ));
            desc.setMotionVectors(resource(
                    resources.inputMotionVectors(),
                    SRResourceStates.COMPUTE_READ));
            if (dispatchResource.resources().has(InputResourceType.Exposure)) {
                desc.setExposure(resource(
                        resources.inputExposure(),
                        SRResourceStates.COMPUTE_READ));
            }
            desc.setOutput(resource(
                    resources.outputColor(),
                    SRResourceStates.COMMON));

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

            SRReturnCode code = SuperResolutionNativeAPI.srDispatchUpscale(
                    owner.context,
                    desc);
            if (code == SRReturnCode.OK) {
                commandList.setTextureState(
                        resources.inputColor(),
                        D3D12ResourceState.COMPUTE_READ);
                commandList.setTextureState(
                        resources.inputDepth(),
                        D3D12ResourceState.COMPUTE_READ);
                commandList.setTextureState(
                        resources.inputMotionVectors(),
                        D3D12ResourceState.COMPUTE_READ);
                if (dispatchResource.resources().has(InputResourceType.Exposure)) {
                    commandList.setTextureState(
                            resources.inputExposure(),
                            D3D12ResourceState.COMPUTE_READ);
                }
                commandList.setTextureState(
                        resources.outputColor(),
                        D3D12ResourceState.COMMON);
                return true;
            }
            SuperResolution.LOGGER.error("FSR 4 dispatch failed: {}", code);
            return false;
        }
    }

    @Override
    protected boolean isD3D12UpscalerReady(ContextOwner owner) {
        return owner.context.nativePtr > 0;
    }

    private Throwable retryRetainedContextOwners(Throwable failure) {
        Iterator<ContextOwner> iterator = retainedContextOwners.iterator();
        while (iterator.hasNext()) {
            ContextOwner owner = iterator.next();
            try {
                destroyD3D12Upscaler(owner);
                iterator.remove();
            } catch (Throwable destroyFailure) {
                if (failure == null) {
                    failure = destroyFailure;
                } else if (failure != destroyFailure) {
                    failure.addSuppressed(destroyFailure);
                }
            }
        }
        return failure;
    }

    private static void throwRetainedFailure(Throwable failure) {
        if (failure == null) {
            return;
        }
        ThrowableUtil.rethrowError(failure);
        if (failure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new RuntimeException(failure);
    }

    public static final class ContextOwner {
        private final SRUpscaleContext context;
        private D3D12Device.ExternalBorrowLease deviceBorrow;

        private ContextOwner(SRUpscaleContext context) {
            this.context = Objects.requireNonNull(context, "context");
        }

        private void pinDevice(D3D12Device device) {
            if (deviceBorrow != null) {
                throw new IllegalStateException(
                        "The FSR 4 context already pins a D3D12 device");
            }
            deviceBorrow = Objects.requireNonNull(device, "device").borrowExternal();
        }
    }
}
