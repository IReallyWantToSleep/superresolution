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

    public static native int NsrLoadUpscaleProvidersFromLibrary(
            String libPath,
            String getProvidersFuncName,
            String getProvidersCountFuncName
    );

    //==============参数管理API==============//

    /**
     * 创建新的参数集合
     */
    public static native long NsrCreateParams();

    /**
     * 销毁参数集合
     */
    public static native void NsrDestroyParams(long paramsPtr);

    /**
     * 设置布尔类型参数
     */
    public static native int NsrParamsSetBool(long paramsPtr, String name, boolean value);

    /**
     * 设置int32类型参数
     */
    public static native int NsrParamsSetInt32(long paramsPtr, String name, int value);

    /**
     * 设置uint32类型参数
     */
    public static native int NsrParamsSetUint32(long paramsPtr, String name, long value);

    /**
     * 设置float类型参数
     */
    public static native int NsrParamsSetFloat(long paramsPtr, String name, float value);

    /**
     * 设置double类型参数
     */
    public static native int NsrParamsSetDouble(long paramsPtr, String name, double value);

    /**
     * 设置字符串类型参数
     */
    public static native int NsrParamsSetString(long paramsPtr, String name, String value);

    /**
     * 设置指针类型参数
     */
    public static native int NsrParamsSetPointer(long paramsPtr, String name, long value);

    /**
     * 查找参数
     */
    public static native long NsrFindParam(long paramsPtr, String name);

    /**
     * 获取布尔类型参数值
     */
    public static native boolean NsrParamsGetBool(long paramsPtr, String name, boolean defaultValue);

    /**
     * 获取int32类型参数值
     */
    public static native int NsrParamsGetInt32(long paramsPtr, String name, int defaultValue);

    /**
     * 获取uint32类型参数值
     */
    public static native long NsrParamsGetUint32(long paramsPtr, String name, long defaultValue);

    /**
     * 获取float类型参数值
     */
    public static native float NsrParamsGetFloat(long paramsPtr, String name, float defaultValue);

    /**
     * 获取double类型参数值
     */
    public static native double NsrParamsGetDouble(long paramsPtr, String name, double defaultValue);

    /**
     * 获取字符串类型参数值
     */
    public static native String NsrParamsGetString(long paramsPtr, String name, String defaultValue);

    /**
     * 获取指针类型参数值
     */
    public static native long NsrParamsGetPointer(long paramsPtr, String name);

    //==============参数读取API (用于SRContextExtraParam)==============//

    /**
     * 获取参数名称
     */
    public static native String NsrParamGetName(long paramPtr);

    /**
     * 获取参数值类型
     */
    public static native int NsrParamGetValueType(long paramPtr);

    /**
     * 以布尔值形式获取参数值
     */
    public static native boolean NsrParamGetValueAsBool(long paramPtr);

    /**
     * 以int32形式获取参数值
     */
    public static native int NsrParamGetValueAsInt32(long paramPtr);

    /**
     * 以uint32形式获取参数值
     */
    public static native long NsrParamGetValueAsUint32(long paramPtr);

    /**
     * 以float形式获取参数值
     */
    public static native float NsrParamGetValueAsFloat(long paramPtr);

    /**
     * 以double形式获取参数值
     */
    public static native double NsrParamGetValueAsDouble(long paramPtr);

    /**
     * 以字符串形式获取参数值
     */
    public static native String NsrParamGetValueAsString(long paramPtr);

    /**
     * 以指针形式获取参数值
     */
    public static native long NsrParamGetValueAsPointer(long paramPtr);
}
