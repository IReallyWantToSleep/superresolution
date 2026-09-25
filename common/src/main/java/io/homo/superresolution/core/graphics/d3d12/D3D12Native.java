/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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

package io.homo.superresolution.core.graphics.d3d12;

import java.nio.ByteBuffer;

final class D3D12Native {
    static final int DEBUG_NONE = 0;
    static final int DEBUG_LAYER = 1;
    static final int DEBUG_GPU_VALIDATION = 1 << 1;
    static final int DEBUG_DRED = 1 << 2;

    static final int RESOURCE_FLAG_NONE = 0;
    static final int RESOURCE_FLAG_ALLOW_RENDER_TARGET = 1;
    static final int RESOURCE_FLAG_ALLOW_DEPTH_STENCIL = 1 << 1;
    static final int RESOURCE_FLAG_ALLOW_UNORDERED_ACCESS = 1 << 2;

    static final int BUFFER_HEAP_DEFAULT = 0;
    static final int BUFFER_HEAP_UPLOAD = 1;
    static final int BUFFER_HEAP_READBACK = 2;

    static final int WAIT_INFINITE = -1;

    static final int SUBMIT_NOT_EXECUTED = 0;
    static final int SUBMIT_SUBMITTED = 1;
    static final int SUBMIT_EXECUTED_UNTRACKED = 2;

    private D3D12Native() {
    }

    static native String nGetLastError();

    static native long nCreateDevice(long adapterLuid, int debugFlags);

    static native void nDestroyDevice(long device);

    static native long nGetNativeDevice(long device);

    static native long nGetNativeQueue(long device);

    static native long nGetDeviceAdapterLuid(long device);

    static native long nGetCompletedSubmissionValue(long device);

    static native long nGetLastSubmittedValue(long device);

    static native int nWaitIdle(long device, int timeoutMilliseconds);

    static native int nSetDebugName(long object, String name);

    static native long nCreateSharedFence(long device, long initialValue);

    static native void nDestroySharedFence(long fence);

    static native long nGetNativeFence(long fence);

    static native long nGetSharedFenceHandle(long fence);

    static native long nReserveSharedFenceValue(long fence);

    static native long nGetCompletedSharedFenceValue(long fence);

    static native int nSignalSharedFence(long fence, long value);

    static native int nWaitSharedFence(long fence, long value, int timeoutMilliseconds);

    static native int nRecoverSharedFence(
            long device,
            long sharedFence,
            long waitValue,
            long signalValue);

    static native int nRecoverExecutedSharedFence(
            long device,
            long sharedFence,
            long waitValue,
            long signalValue);

    static native long nCreateTexture2D(
            long device,
            int width,
            int height,
            int mipLevels,
            int surfaceFormat,
            int resourceFlags,
            int initialState,
            boolean shared);

    static native void nDestroyTexture2D(long texture);

    static native long nGetNativeTextureResource(long texture);

    static native long nGetTextureSharedHandle(long texture);

    static native long nGetTextureAllocationSize(long texture);

    static native int nGetTextureWidth(long texture);

    static native int nGetTextureHeight(long texture);

    static native int nGetTextureMipLevels(long texture);

    static native int nGetTextureSurfaceFormat(long texture);

    static native int nGetTextureResourceFlags(long texture);

    static native int nGetTextureInitialState(long texture);

    static native int nGetTextureCommittedState(long texture);

    static native int nSetTextureCommittedState(long texture, int state);

    static native long nCreateBuffer(
            long device,
            long size,
            int heap,
            int resourceFlags,
            int initialState,
            boolean shared);

    static native void nDestroyBuffer(long buffer);

    static native long nGetNativeBufferResource(long buffer);

    static native long nGetBufferSharedHandle(long buffer);

    static native long nGetBufferSize(long buffer);

    static native int nGetBufferHeapType(long buffer);

    static native int nGetBufferResourceFlags(long buffer);

    static native int nGetBufferInitialState(long buffer);

    static native ByteBuffer nMapBuffer(long buffer, long offset, long size);

    static native int nUnmapBuffer(long buffer, long offset, long size);

    static native long nCreateCommandAllocator(long device);

    static native void nDestroyCommandAllocator(long allocator);

    static native long nGetNativeCommandAllocator(long allocator);

    static native long nCreateCommandList(long device, long allocator);

    static native int nDestroyCommandList(long commandList);

    static native int nBeginCommandList(long commandList);

    static native int nEndCommandList(long commandList);

    static native int nAbortCommandList(long commandList);

    static native long nGetNativeCommandList(long commandList);

    static native int nGetCommandTextureState(long commandList, long texture);

    static native int nSetCommandTextureState(long commandList, long texture, int state);

    static native int nCmdTransitionTexture(
            long commandList,
            long texture,
            int before,
            int after);

    static native int nCmdCopyTexture(
            long commandList,
            long source,
            long destination,
            int sourceX,
            int sourceY,
            int width,
            int height,
            int sourceMip,
            int destinationX,
            int destinationY,
            int destinationMip);

    static native int nCmdCopyBuffer(
            long commandList,
            long source,
            long destination,
            long sourceOffset,
            long destinationOffset,
            long size);

    static native int nCmdWriteBuffer(
            long commandList,
            long destination,
            long destinationOffset,
            ByteBuffer data,
            int dataOffset,
            int size);

    static native int nCmdWriteTexture2D(
            long commandList,
            long destination,
            int x,
            int y,
            int width,
            int height,
            int mip,
            int sourceRowPitch,
            ByteBuffer data,
            int dataOffset,
            int size);

    static native int nCmdClearTextureRgba(
            long commandList,
            long texture,
            float red,
            float green,
            float blue,
            float alpha);

    static native int nCmdUavBarrier(long commandList, long resourceOrZero);

    static native long nSubmit(
            long device,
            long commandList,
            long sharedFenceOrZero,
            long waitValue,
            long signalValue);
}
