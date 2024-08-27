package io.homo.superresolution.fsr2.nativelib;

import io.homo.superresolution.fsr2.types.impl.FfxFsr2Interface;
import io.homo.superresolution.fsr2.types.impl.FfxResource;

import java.util.function.Function;

public class ffx_fsr2_api_gl {
    public native long ffxFsr2GetScratchMemorySizeGL();
    public native int ffxFsr2GetInterfaceGL(
            FfxFsr2Interface outInterface,
            char[] scratchBuffer,
            long scratchBufferSize,
            Function<String,Object> getProcAddress);
    public native FfxResource ffxGetTextureResourceGL(
            long textureGL,
            long width,
            long height,
            long imgFormat,
            String name);
    public native FfxResource ffxGetBufferResourceGL(
            long bufferGL,
            long size,
            String name);
    public native int ffxGetGLImage(long context, long resId);

    public ffx_fsr2_api_gl(String path){
        System.load(path);
    }

    public static void main(String[] args) {
        ffx_fsr2_api_gl a = new ffx_fsr2_api_gl("I:/superresolution/common/src/main/resources/lib/ffx_fsr2_api_gl_x64d.dll");
        a.ffxFsr2GetScratchMemorySizeGL();
    }
}
