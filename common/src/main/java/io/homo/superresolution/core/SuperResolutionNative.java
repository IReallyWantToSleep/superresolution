package io.homo.superresolution.core;

import io.homo.superresolution.core.graphics.glslang.GlslangCompileShaderResult;
import io.homo.superresolution.core.math.Vector2f;
import io.homo.superresolution.core.math.Vector2i;
import io.homo.superresolution.srapi.*;

import java.nio.ByteBuffer;
import java.util.function.Supplier;


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
            long device,
            long phyDevice,
            int upscaledSizeX,
            int upscaledSizeY,
            int renderSizeX,
            int renderSizeY,
            int flags
    );

    public static native int NsrDestroyUpscaleContext(long context);

    public static native int NsrDispatchUpscale(
            long context,
            long commandList,
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
}
