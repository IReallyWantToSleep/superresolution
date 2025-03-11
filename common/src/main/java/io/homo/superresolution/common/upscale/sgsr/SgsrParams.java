package io.homo.superresolution.common.upscale.sgsr;

import io.homo.superresolution.common.render.impl.IUniformStruct;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Struct;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.system.MemoryUtil.nmemAllocChecked;

public class SgsrParams extends Struct implements IUniformStruct {

    public static final int SIZEOF;
    public static final int ALIGNOF;
    public static final int
            RENDERSIZE,
            DISPLAYSIZE,
            INVIEWPORTSIZEINVERSE,
            DISPLAYSIZERCPS,
            JITTEROFFSET,
            PADDING1,
            CLIPTOPREVCLIP,
            PREEXPOSURE,
            CAMERAFOVANGLEHOR,
            CAMERANEAR,
            MINLERPCONTRIBUTION,
            BSAMECAMERA,
            RESET;

    static {
        Layout layout = __struct(
                __member(8), // uvec2 renderSize
                __member(8), // uvec2 displaySize
                __member(8), // vec2 InViewportSizeInverse
                __member(8), // vec2 displaySizeRcp
                __member(8), // vec2 jitterOffset
                __member(8), // vec2 padding1
                __array(16, 4), // vec4 clipToPrevClip[4]
                __member(4), // float preExposure
                __member(4), // float cameraFovAngleHor
                __member(4), // float cameraNear
                __member(4), // float MinLerpContribution
                __member(4), // uint bSameCamera
                __member(4)  // uint reset
        );

        SIZEOF = layout.getSize();
        ALIGNOF = layout.getAlignment();

        RENDERSIZE = layout.offsetof(0);
        DISPLAYSIZE = layout.offsetof(1);
        INVIEWPORTSIZEINVERSE = layout.offsetof(2);
        DISPLAYSIZERCPS = layout.offsetof(3);
        JITTEROFFSET = layout.offsetof(4);
        PADDING1 = layout.offsetof(5);
        CLIPTOPREVCLIP = layout.offsetof(6);
        PREEXPOSURE = layout.offsetof(7);
        CAMERAFOVANGLEHOR = layout.offsetof(8);
        CAMERANEAR = layout.offsetof(9);
        MINLERPCONTRIBUTION = layout.offsetof(10);
        BSAMECAMERA = layout.offsetof(11);
        RESET = layout.offsetof(12);
    }

    protected SgsrParams(long address, @Nullable ByteBuffer container) {
        super(address, __checkContainer(container, SIZEOF));
    }

    // Native methods to set the values
    private static native void nrenderSize(long struct, int x, int y);

    private static native void ndisplaySize(long struct, int x, int y);

    private static native void nInViewportSizeInverse(long struct, float x, float y);

    private static native void ndisplaySizeRcp(long struct, float x, float y);

    private static native void njitterOffset(long struct, float x, float y);

    private static native void npadding1(long struct, float x, float y);

    private static native void nclipToPrevClip(long struct, FloatBuffer value);

    private static native void npreExposure(long struct, float value);

    private static native void ncameraFovAngleHor(long struct, float value);

    private static native void ncameraNear(long struct, float value);

    private static native void nMinLerpContribution(long struct, float value);

    private static native void nbSameCamera(long struct, int value);

    private static native void nreset(long struct, int value);

    public static SgsrParams malloc() {
        return new SgsrParams(nmemAllocChecked(SIZEOF), null);
    }

    public static SgsrParams calloc() {
        ByteBuffer buffer = MemoryStack.stackCalloc(SIZEOF);
        return new SgsrParams(MemoryUtil.memAddress(buffer), buffer);
    }

    public void renderSize(int x, int y) {
        nrenderSize(address(), x, y);
    }

    public void displaySize(int x, int y) {
        ndisplaySize(address(), x, y);
    }

    public void InViewportSizeInverse(float x, float y) {
        nInViewportSizeInverse(address(), x, y);
    }

    public void displaySizeRcp(float x, float y) {
        ndisplaySizeRcp(address(), x, y);
    }

    public void jitterOffset(float x, float y) {
        njitterOffset(address(), x, y);
    }

    public void padding1(float x, float y) {
        npadding1(address(), x, y);
    }

    public void clipToPrevClip(FloatBuffer value) {
        nclipToPrevClip(address(), value);
    }

    public void preExposure(float value) {
        npreExposure(address(), value);
    }

    public void cameraFovAngleHor(float value) {
        ncameraFovAngleHor(address(), value);
    }

    public void cameraNear(float value) {
        ncameraNear(address(), value);
    }

    public void MinLerpContribution(float value) {
        nMinLerpContribution(address(), value);
    }

    public void bSameCamera(int value) {
        nbSameCamera(address(), value);
    }

    public void reset(int value) {
        nreset(address(), value);
    }

    protected SgsrParams create(long address, @Nullable ByteBuffer container) {
        return new SgsrParams(address, container);
    }

    @Override
    public ByteBuffer container() {
        return container;
    }

    @Override
    public int sizeof() {
        return SIZEOF;
    }
}
