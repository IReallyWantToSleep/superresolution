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

package io.homo.superresolution.srapi;

import io.homo.superresolution.core.SuperResolutionNative;

public class SuperResolutionNativeAPI {

    public static SRReturnCode srInitUpscaleContext(SRUpscaleContext context) {
        if (context.nativePtr < 1) {
            return SRReturnCode.NULL_POINTER;
        }
        int code = SuperResolutionNative.NsrInitUpscaleContext(context.nativePtr);
        return SRReturnCode.fromValue(code);
    }

    public static SRReturnCode srCreateUpscaleContext(
            SRUpscaleContext outContext,
            SRUpscaleProvider provider,
            SRCreateUpscaleContextDesc desc
    ) {
        if (provider.nativePtr < 1 || desc == null) {
            return SRReturnCode.NULL_POINTER;
        }

        long extraParamsPtr = desc.extraParams != null ? desc.extraParams.getNativePtr() : 0;
        int flags = 0;
        for (SRUpscaleContextCreateFlags contextCreateFlags : desc.getFlags()) {
            flags |= contextCreateFlags.value;
        }
        int code = SuperResolutionNative.NsrCreateUpscaleContext(
                outContext,
                provider.nativePtr,
                desc.renderApiType.value,
                desc.getOpenglDeviceInfo(),
                desc.getVulkanDeviceInfo(),
                desc.upscaledSize.x,
                desc.upscaledSize.y,
                desc.renderSize.x,
                desc.renderSize.y,
                desc.messageCallback,
                extraParamsPtr,
                flags
        );
        return SRReturnCode.fromValue(code);
    }

    public static SRReturnCode srDestroyUpscaleContext(SRUpscaleContext context) {
        if (context == null || context.nativePtr < 1) {
            return SRReturnCode.NULL_POINTER;
        }
        SRReturnCode code = SRReturnCode.fromValue(SuperResolutionNative.NsrDestroyUpscaleContext(context.nativePtr));
        if (code == SRReturnCode.OK) {
            context.nativePtr = 0;
        }
        return code;
    }

    public static SRReturnCode srDispatchUpscale(
            SRUpscaleContext context,
            SRDispatchUpscaleDesc desc
    ) {
        if (context.nativePtr < 1 || desc == null || desc.commandList == null) {
            return SRReturnCode.ERROR;
        }

        long extraParamsPtr = desc.extraParams != null ? desc.extraParams.getNativePtr() : 0;

        int code = SuperResolutionNative.NsrDispatchUpscale(
                context.nativePtr,
                desc.commandList.renderApiType.value,
                desc.commandList.getVulkanCommandBufferAddress(),
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
                extraParamsPtr,
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
            case AVAILABLE -> outResult instanceof SRUpscaleContextQueryAvailabilityResult;
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

    protected static long srCreateParams() {
        return SuperResolutionNative.NsrCreateParams();
    }

    protected static void srDestroyParams(long paramsPtr) {
        if (paramsPtr != 0) {
            SuperResolutionNative.NsrDestroyParams(paramsPtr);
        }
    }

    protected static int srParamsSetBool(long paramsPtr, String name, boolean value) {
        return SuperResolutionNative.NsrParamsSetBool(paramsPtr, name, value);
    }

    protected static int srParamsSetInt32(long paramsPtr, String name, int value) {
        return SuperResolutionNative.NsrParamsSetInt32(paramsPtr, name, value);
    }

    protected static int srParamsSetUint32(long paramsPtr, String name, long value) {
        return SuperResolutionNative.NsrParamsSetUint32(paramsPtr, name, value);
    }

    protected static int srParamsSetFloat(long paramsPtr, String name, float value) {
        return SuperResolutionNative.NsrParamsSetFloat(paramsPtr, name, value);
    }

    protected static int srParamsSetDouble(long paramsPtr, String name, double value) {
        return SuperResolutionNative.NsrParamsSetDouble(paramsPtr, name, value);
    }

    protected static int srParamsSetString(long paramsPtr, String name, String value) {
        return SuperResolutionNative.NsrParamsSetString(paramsPtr, name, value);
    }

    protected static int srParamsSetPointer(long paramsPtr, String name, long value) {
        return SuperResolutionNative.NsrParamsSetPointer(paramsPtr, name, value);
    }

    protected static SRContextExtraParam srFindParam(long paramsPtr, String name) {
        return new SRContextExtraParam(SuperResolutionNative.NsrFindParam(paramsPtr, name));
    }

    protected static boolean srParamsGetBool(long paramsPtr, String name, boolean defaultValue) {
        return SuperResolutionNative.NsrParamsGetBool(paramsPtr, name, defaultValue);
    }

    protected static int srParamsGetInt32(long paramsPtr, String name, int defaultValue) {
        return SuperResolutionNative.NsrParamsGetInt32(paramsPtr, name, defaultValue);
    }

    protected static long srParamsGetUint32(long paramsPtr, String name, long defaultValue) {
        return SuperResolutionNative.NsrParamsGetUint32(paramsPtr, name, defaultValue);
    }

    protected static float srParamsGetFloat(long paramsPtr, String name, float defaultValue) {
        return SuperResolutionNative.NsrParamsGetFloat(paramsPtr, name, defaultValue);
    }

    protected static double srParamsGetDouble(long paramsPtr, String name, double defaultValue) {
        return SuperResolutionNative.NsrParamsGetDouble(paramsPtr, name, defaultValue);
    }

    protected static String srParamsGetString(long paramsPtr, String name, String defaultValue) {
        return SuperResolutionNative.NsrParamsGetString(paramsPtr, name, defaultValue);
    }

    protected static long srParamsGetPointer(long paramsPtr, String name) {
        return SuperResolutionNative.NsrParamsGetPointer(paramsPtr, name);
    }

    protected static String srParamGetName(long paramPtr) {
        return SuperResolutionNative.NsrParamGetName(paramPtr);
    }

    protected static int srParamGetValueType(long paramPtr) {
        return SuperResolutionNative.NsrParamGetValueType(paramPtr);
    }

    protected static boolean srParamGetValueAsBool(long paramPtr) {
        return SuperResolutionNative.NsrParamGetValueAsBool(paramPtr);
    }

    protected static int srParamGetValueAsInt32(long paramPtr) {
        return SuperResolutionNative.NsrParamGetValueAsInt32(paramPtr);
    }

    protected static long srParamGetValueAsUint32(long paramPtr) {
        return SuperResolutionNative.NsrParamGetValueAsUint32(paramPtr);
    }

    protected static float srParamGetValueAsFloat(long paramPtr) {
        return SuperResolutionNative.NsrParamGetValueAsFloat(paramPtr);
    }

    protected static double srParamGetValueAsDouble(long paramPtr) {
        return SuperResolutionNative.NsrParamGetValueAsDouble(paramPtr);
    }

    protected static String srParamGetValueAsString(long paramPtr) {
        return SuperResolutionNative.NsrParamGetValueAsString(paramPtr);
    }

    protected static long srParamGetValueAsPointer(long paramPtr) {
        return SuperResolutionNative.NsrParamGetValueAsPointer(paramPtr);
    }

    public static SRReturnCode srShutdown() {
        return SRReturnCode.fromValue(SuperResolutionNative.NsrShutdown());
    }
}
