/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

package io.homo.superresolution.srapi;

import io.homo.superresolution.core.SuperResolutionNative;

public class SuperResolutionNativeAPI {
    /*
    SR_API SRReturnCode srCreateUpscaleContext(
        SRUpscaleContext *outContext,
        SRUpscaleProvider *provider,
        const SRCreateUpscaleContextDesc *desc);

    SR_API SRReturnCode srDestroyUpscaleContext(SRUpscaleContext *context);

    SR_API SRReturnCode srQueryUpscaleContext(
        SRUpscaleContext *context,
        SRUpscaleContextQueryResult *outResult,
        SRUpscaleContextQueryType queryType);

    SR_API SRReturnCode srDispatchUpscale(
        SRUpscaleContext *context,
        const SRDispatchUpscaleDesc *desc);
    * */
    public static SRReturnCode srCreateUpscaleContext(
            SRUpscaleContext outContext,
            SRUpscaleProvider provider,
            SRCreateUpscaleContextDesc desc
    ) {
        if (provider.nativePtr < 1) {
            return SRReturnCode.ERROR;
        }
        int code = SuperResolutionNative.NsrCreateUpscaleContext(
                outContext,
                provider.nativePtr,
                desc.device,
                desc.phyDevice,
                desc.upscaledSize.x,
                desc.upscaledSize.y,
                desc.renderSize.x,
                desc.renderSize.y,
                desc.flags
        );
        return SRReturnCode.fromValue(code);
    }

    public static SRReturnCode srDestroyUpscaleContext(
            SRUpscaleContext context
    ) {
        if (context.nativePtr < 1) {
            return SRReturnCode.ERROR;
        }
        return SRReturnCode.fromValue(SuperResolutionNative.NsrDestroyUpscaleContext(context.nativePtr));

    }

    public static SRReturnCode srDispatchUpscale(
            SRUpscaleContext context,
            SRDispatchUpscaleDesc desc
    ) {
        if (context.nativePtr < 1) {
            return SRReturnCode.ERROR;
        }
        int code = SuperResolutionNative.NsrDispatchUpscale(
                context.nativePtr,
                desc.commandList,
                desc.color,
                desc.depth,
                desc.motionVectors,
                desc.exposure,
                desc.reactive,
                desc.transparencyAndComposition,
                desc.output,
                desc.jitterOffset.x,
                desc.jitterOffset.y,
                desc.motionVectorScale.x,
                desc.motionVectorScale.y,
                desc.renderSize.x,
                desc.renderSize.y,
                desc.upscaleSize.x,
                desc.upscaleSize.y,
                desc.frameTimeDelta,
                desc.enableSharpening,
                desc.sharpness,
                desc.preExposure,
                desc.cameraNear,
                desc.cameraFar,
                desc.cameraFovAngleVertical,
                desc.viewSpaceToMetersFactor,
                desc.reset,
                desc.flags
        );
        return SRReturnCode.fromValue(code);
    }

    public static SRReturnCode srQueryUpscaleContext(
            SRUpscaleContext context,
            SRUpscaleContextQueryResult outResult,
            SRUpscaleContextQueryType queryType
    ) {
        if (context.nativePtr < 1) {
            return SRReturnCode.ERROR;
        }
        boolean typeTrue = switch (queryType) {
            case VERSION_INFO -> outResult instanceof SRUpscaleContextQueryVersionInfoResult;
            case GPU_MEMORY_INFO -> outResult instanceof SRUpscaleContextQueryGpuMemoryInfoResult;
        };
        if (!typeTrue) {
            return SRReturnCode.ERROR;
        }

        return SRReturnCode.fromValue(SuperResolutionNative.NsrQueryUpscaleContext(context.nativePtr, outResult, queryType.value));
    }

    public static SRReturnCode srGetUpscaleProvider(
            SRUpscaleProvider provider,
            long providerId
    ) {
        return SRReturnCode.fromValue(SuperResolutionNative.NsrGetUpscaleProvider(provider, providerId));
    }

    public static SRReturnCode srLoadUpscaleProvidersFromLibrary(
            String libPath,
            String getProvidersFuncName,
            String getProvidersCountFuncName
    ) {
        return SRReturnCode.fromValue(SuperResolutionNative.NsrLoadUpscaleProvidersFromLibrary(
                libPath,
                getProvidersFuncName,
                getProvidersCountFuncName
        ));

    }
}
