/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.core.graphics.d3d12;

final class D3D12InteropNative {
    static final int RESOURCE_INPUT_COLOR = 0;
    static final int RESOURCE_INPUT_DEPTH = 1;
    static final int RESOURCE_INPUT_MOTION_VECTORS = 2;
    static final int RESOURCE_INPUT_EXPOSURE = 3;
    static final int RESOURCE_OUTPUT_COLOR = 4;

    private D3D12InteropNative() {
    }

    static native long Nd3d12CreateContext(
            long adapterLuid,
            int renderWidth,
            int renderHeight,
            int outputWidth,
            int outputHeight,
            int colorFormat
    );

    static native void Nd3d12DestroyContext(long context);

    static native long Nd3d12GetDevice(long context);

    static native long Nd3d12GetCommandList(long context);

    static native long Nd3d12GetResource(long context, int index);

    static native long Nd3d12GetResourceSharedHandle(long context, int index);

    static native long Nd3d12GetResourceAllocationSize(long context, int index);

    static native long Nd3d12GetFenceSharedHandle(long context);

    static native int Nd3d12BeginFrame(long context, long waitFenceValue);

    static native int Nd3d12ExecuteFrame(long context, long signalFenceValue);

    static native int Nd3d12WaitIdle(long context);

    static native String Nd3d12GetLastError();
}
