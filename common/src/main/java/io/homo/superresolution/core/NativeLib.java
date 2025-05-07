package io.homo.superresolution.core;

public class NativeLib {
    public native int initVulkan();

    public native int resizeVulkan(int width, int height);

    public native int cleanupVulkan();
}
