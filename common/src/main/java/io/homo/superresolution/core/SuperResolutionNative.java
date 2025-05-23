package io.homo.superresolution.core;

import io.homo.superresolution.core.glslang.GlslangCompileShaderResult;
import io.netty.buffer.UnpooledUnsafeDirectByteBuf;

import java.nio.ByteBuffer;


public class SuperResolutionNative {
    public static native String getVersionInfo();

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
}
