/*
 * Super Resolution
 * Copyright (c) 2026. Xiang Keshen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.core.graphics.d3d12;

import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsages;
import io.homo.superresolution.srapi.SRSurfaceFormat;
import org.lwjgl.opengl.EXTMemoryObject;
import org.lwjgl.opengl.EXTMemoryObjectWin32;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Native owner for the D3D12 device, queue, command list, shared resources,
 * and shared timeline fence used by OpenGL/Direct3D interop.
 */
public final class D3D12InteropContext implements AutoCloseable {
    private final Resource inputColor;
    private final Resource inputDepth;
    private final Resource inputMotionVectors;
    private final Resource inputExposure;
    private final Resource outputColor;
    private long nativePtr;
    private long nextFenceValue = 1;

    private D3D12InteropContext(
            long nativePtr,
            int renderWidth,
            int renderHeight,
            int outputWidth,
            int outputHeight,
            TextureFormat colorFormat) {
        this.nativePtr = nativePtr;
        this.inputColor = readResource(
                D3D12InteropNative.RESOURCE_INPUT_COLOR,
                textureDescription(renderWidth, renderHeight, colorFormat, "D3D12InputColor"),
                toSrFormat(colorFormat));
        this.inputDepth = readResource(
                D3D12InteropNative.RESOURCE_INPUT_DEPTH,
                textureDescription(renderWidth, renderHeight, TextureFormat.R32F, "D3D12InputDepth"),
                SRSurfaceFormat.R32_FLOAT);
        this.inputMotionVectors = readResource(
                D3D12InteropNative.RESOURCE_INPUT_MOTION_VECTORS,
                textureDescription(renderWidth, renderHeight, TextureFormat.RG16F, "D3D12InputMotionVectors"),
                SRSurfaceFormat.R16G16_FLOAT);
        this.inputExposure = readResource(
                D3D12InteropNative.RESOURCE_INPUT_EXPOSURE,
                textureDescription(1, 1, TextureFormat.R32F, "D3D12InputExposure"),
                SRSurfaceFormat.R32_FLOAT);
        this.outputColor = readResource(
                D3D12InteropNative.RESOURCE_OUTPUT_COLOR,
                textureDescription(outputWidth, outputHeight, colorFormat, "D3D12OutputColor"),
                toSrFormat(colorFormat));
    }

    public static D3D12InteropContext create(
            int renderWidth,
            int renderHeight,
            int outputWidth,
            int outputHeight,
            TextureFormat colorFormat) {
        if (!GL.getCapabilities().GL_EXT_memory_object_win32 ||
                !GL.getCapabilities().GL_EXT_semaphore_win32) {
            throw new UnsupportedOperationException(
                    "D3D12 interop requires GL_EXT_memory_object_win32 and GL_EXT_semaphore_win32.");
        }

        long adapterLuid = queryOpenGlAdapterLuid();
        long nativePtr = D3D12InteropNative.Nd3d12CreateContext(
                adapterLuid,
                renderWidth,
                renderHeight,
                outputWidth,
                outputHeight,
                toSrFormat(colorFormat).value);
        if (nativePtr == 0) {
            throw new IllegalStateException(
                    "Could not create D3D12 interop context: " +
                            D3D12InteropNative.Nd3d12GetLastError());
        }
        try {
            return new D3D12InteropContext(
                    nativePtr,
                    renderWidth,
                    renderHeight,
                    outputWidth,
                    outputHeight,
                    colorFormat);
        } catch (Throwable throwable) {
            D3D12InteropNative.Nd3d12DestroyContext(nativePtr);
            throw throwable;
        }
    }

    private static long queryOpenGlAdapterLuid() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer luid = stack.calloc(EXTMemoryObjectWin32.GL_LUID_SIZE_EXT)
                    .order(ByteOrder.nativeOrder());
            EXTMemoryObject.glGetUnsignedBytevEXT(
                    EXTMemoryObjectWin32.GL_DEVICE_LUID_EXT,
                    luid);
            long lowPart = Integer.toUnsignedLong(luid.getInt(0));
            long highPart = Integer.toUnsignedLong(luid.getInt(4));
            long value = lowPart | (highPart << 32);
            if (value == 0) {
                throw new IllegalStateException("OpenGL reported a null device LUID.");
            }
            return value;
        }
    }

    private static TextureDescription textureDescription(
            int width,
            int height,
            TextureFormat format,
            String label) {
        return TextureDescription.create()
                .type(TextureType.Texture2D)
                .width(width)
                .height(height)
                .format(format)
                .usages(TextureUsages.create().sampler().storage())
                .label(label)
                .build();
    }

    private static SRSurfaceFormat toSrFormat(TextureFormat format) {
        return switch (format) {
            case RGBA8 -> SRSurfaceFormat.R8G8B8A8_UNORM;
            case RGBA16F -> SRSurfaceFormat.R16G16B16A16_FLOAT;
            case R11G11B10F -> SRSurfaceFormat.R11G11B10_FLOAT;
            default -> throw new IllegalArgumentException(
                    "Unsupported D3D12 interop color format: " + format);
        };
    }

    private static void check(int code, String operation) {
        if (code != 0) {
            throw new IllegalStateException(
                    "Could not " + operation + " (0x" +
                            Integer.toHexString(code) + "): " +
                            D3D12InteropNative.Nd3d12GetLastError());
        }
    }

    private Resource readResource(
            int index,
            TextureDescription description,
            SRSurfaceFormat srFormat) {
        long resource = D3D12InteropNative.Nd3d12GetResource(nativePtr, index);
        long sharedHandle = D3D12InteropNative.Nd3d12GetResourceSharedHandle(nativePtr, index);
        long allocationSize = D3D12InteropNative.Nd3d12GetResourceAllocationSize(nativePtr, index);
        if (resource == 0 || sharedHandle == 0 || allocationSize <= 0) {
            throw new IllegalStateException(
                    "Native D3D12 resource " + index + " is incomplete: " +
                            D3D12InteropNative.Nd3d12GetLastError());
        }
        return new Resource(
                index,
                resource,
                sharedHandle,
                allocationSize,
                description,
                srFormat);
    }

    public long getDevice() {
        ensureOpen();
        return D3D12InteropNative.Nd3d12GetDevice(nativePtr);
    }

    public long getCommandList() {
        ensureOpen();
        return D3D12InteropNative.Nd3d12GetCommandList(nativePtr);
    }

    public long getFenceSharedHandle() {
        ensureOpen();
        return D3D12InteropNative.Nd3d12GetFenceSharedHandle(nativePtr);
    }

    public long nextFenceValue() {
        ensureOpen();
        return nextFenceValue++;
    }

    public void beginFrame(long openGlReadyFenceValue) {
        ensureOpen();
        int code = D3D12InteropNative.Nd3d12BeginFrame(
                nativePtr,
                openGlReadyFenceValue);
        check(code, "begin D3D12 interop frame");
    }

    public void executeFrame(long d3d12DoneFenceValue) {
        ensureOpen();
        int code = D3D12InteropNative.Nd3d12ExecuteFrame(
                nativePtr,
                d3d12DoneFenceValue);
        check(code, "execute D3D12 interop frame");
    }

    public void waitIdle() {
        if (nativePtr == 0) {
            return;
        }
        check(
                D3D12InteropNative.Nd3d12WaitIdle(nativePtr),
                "wait for D3D12 interop");
    }

    private void ensureOpen() {
        if (nativePtr == 0) {
            throw new IllegalStateException("D3D12 interop context is closed.");
        }
    }

    public Resource inputColor() {
        return inputColor;
    }

    public Resource inputDepth() {
        return inputDepth;
    }

    public Resource inputMotionVectors() {
        return inputMotionVectors;
    }

    public Resource inputExposure() {
        return inputExposure;
    }

    public Resource outputColor() {
        return outputColor;
    }

    @Override
    public void close() {
        if (nativePtr != 0) {
            D3D12InteropNative.Nd3d12DestroyContext(nativePtr);
            nativePtr = 0;
        }
    }

    public record Resource(
            int index,

            long nativeResource,

            long sharedHandle,

            long allocationSize,

            TextureDescription textureDescription,

            SRSurfaceFormat srFormat) {
    }
}
