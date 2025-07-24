package io.homo.superresolution.thirdparty.fsr2.common;

import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;

public class Fsr2DeviceCapabilities {
    private static boolean initialized = false;
    private static boolean fp16Supported = false;

    private static boolean detectFp16Support() {
        return GraphicsCapabilities.hasGLExtension("GL_NV_gpu_shader5") ||
                GraphicsCapabilities.hasGLExtension("GL_AMD_gpu_shader_half_float");
    }

    public static boolean isFp16Supported() {
        if (!initialized) {
            fp16Supported = detectFp16Support();
            initialized = true;
        }
        return fp16Supported && SuperResolutionConfig.SPECIAL.FSR2.FP16.get();
    }
}
