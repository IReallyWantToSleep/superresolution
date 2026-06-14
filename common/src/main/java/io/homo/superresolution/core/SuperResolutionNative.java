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

package io.homo.superresolution.core;

import io.homo.superresolution.core.graphics.glslang.GlslangCompileShaderResult;
import io.homo.superresolution.srapi.*;

import java.nio.ByteBuffer;


public class SuperResolutionNative {
    public static native String getVersionInfo();

    //==============Glslang==============//
    public static native GlslangCompileShaderResult compileShaderToSpirv(
            String shaderSrc,
            int stage,
            int language,
            int client,
            int client_version,
            int target_language,
            int target_language_version,
            int default_version,
            int default_profile,
            boolean force_default_version_and_profile,
            boolean forward_compatible
    );

    public static native void freeDirectBuffer(ByteBuffer buffer);

    public static native int initGlslang();

    public static native int destroyGlslang();

    //==============SRApi==============//

    public static native int NsrCreateUpscaleContext(
            SRUpscaleContext outContext,
            long provider,
            int renderApiType,
            SROpenGLDeviceInfo openglDeviceInfo,
            SRVulkanDeviceInfo vulkanDeviceInfo,
            int upscaledSizeX,
            int upscaledSizeY,
            int renderSizeX,
            int renderSizeY,
            long messageCallback,
            long extraParamsPtr,
            int flags
    );

    public static native int NsrInitUpscaleContext(
            long Context
    );

    public static native int NsrDestroyUpscaleContext(long context);

    public static native int NsrDispatchUpscale(
            long context,
            int renderApiType,
            long vulkanCommandBuffer,
            SRTextureResource color,
            SRTextureResource depth,
            SRTextureResource motionVectors,
            SRTextureResource exposure,
            SRTextureResource reactive,
            SRTextureResource transparencyAndComposition,
            SRTextureResource output,
            float jitterOffsetX,
            float jitterOffsetY,
            float motionVectorScaleX,
            float motionVectorScaleY,
            int renderSizeX,
            int renderSizeY,
            int upscaleSizeX,
            int upscaleSizeY,
            float frameTimeDelta,
            boolean enableSharpening,
            float sharpness,
            float preExposure,
            float cameraNear,
            float cameraFar,
            float cameraFovAngleVertical,
            float viewSpaceToMetersFactor,
            boolean reset,
            long extraParamsPtr,
            int flags
    );

    public static native int NsrQueryUpscaleContext(
            long context,
            SRUpscaleContextQueryResult outResult,
            int queryType
    );

    public static native int NsrGetUpscaleProvider(
            SRUpscaleProvider provider,
            long providerId
    );

    public static native int NsrDestroyUpscaleProvider(long provider);

    public static native int NsrLoadUpscaleProvidersFromLibrary(
            String libPath,
            String getProvidersFuncName,
            String getProvidersCountFuncName
    );

    public static native int NsrShutdown();

    public static native long NsrCreateParams();

    public static native void NsrDestroyParams(long paramsPtr);

    public static native int NsrParamsSetBool(long paramsPtr, String name, boolean value);

    public static native int NsrParamsSetInt32(long paramsPtr, String name, int value);

    public static native int NsrParamsSetUint32(long paramsPtr, String name, long value);

    public static native int NsrParamsSetFloat(long paramsPtr, String name, float value);

    public static native int NsrParamsSetDouble(long paramsPtr, String name, double value);

    public static native int NsrParamsSetString(long paramsPtr, String name, String value);

    public static native int NsrParamsSetPointer(long paramsPtr, String name, long value);

    public static native long NsrFindParam(long paramsPtr, String name);

    public static native boolean NsrParamsGetBool(long paramsPtr, String name, boolean defaultValue);

    public static native int NsrParamsGetInt32(long paramsPtr, String name, int defaultValue);

    public static native long NsrParamsGetUint32(long paramsPtr, String name, long defaultValue);

    public static native float NsrParamsGetFloat(long paramsPtr, String name, float defaultValue);

    public static native double NsrParamsGetDouble(long paramsPtr, String name, double defaultValue);

    public static native String NsrParamsGetString(long paramsPtr, String name, String defaultValue);

    public static native long NsrParamsGetPointer(long paramsPtr, String name);

    public static native String NsrParamGetName(long paramPtr);

    public static native int NsrParamGetValueType(long paramPtr);

    public static native boolean NsrParamGetValueAsBool(long paramPtr);

    public static native int NsrParamGetValueAsInt32(long paramPtr);

    public static native long NsrParamGetValueAsUint32(long paramPtr);

    public static native float NsrParamGetValueAsFloat(long paramPtr);

    public static native double NsrParamGetValueAsDouble(long paramPtr);

    public static native String NsrParamGetValueAsString(long paramPtr);

    public static native long NsrParamGetValueAsPointer(long paramPtr);
}
