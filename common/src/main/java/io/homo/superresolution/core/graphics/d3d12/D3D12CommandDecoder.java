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

import io.homo.superresolution.core.graphics.impl.buffer.BufferUsage;
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandDecoder;
import io.homo.superresolution.core.graphics.impl.command.MemoryBarrierType;
import io.homo.superresolution.core.graphics.impl.command.ResourceAccessType;
import io.homo.superresolution.core.graphics.impl.command.ResourceStateTracker;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.impl.pipeline.ComputePipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.GraphicsPipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.RenderPass;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

public final class D3D12CommandDecoder implements ICommandDecoder {
    private final D3D12Device device;
    private final ResourceStateTracker stateTracker = new ResourceStateTracker();

    D3D12CommandDecoder(D3D12Device device) {
        this.device = Objects.requireNonNull(device, "device");
    }

    @Override
    public ResourceStateTracker getStateTracker() {
        return device.withLifecycleLock(() -> {
            device.ensureOpenLocked();
            return stateTracker.snapshot();
        });
    }

    @Override
    public void declareExternalResource(ITexture texture, ResourceAccessType currentState) {
        device.withLifecycleLock(() -> {
            D3D12Texture2D d3d12Texture = requireTextureLocked(
                    texture, "declareExternalResource");
            D3D12ResourceState nativeState = D3D12ResourceState.fromAccessType(
                    Objects.requireNonNull(currentState, "currentState"));
            d3d12Texture.assumeCommittedStateLocked(nativeState);
        });
    }

    @Override
    public void restoreExternalResource(
            ICommandBuffer commandBuffer,
            ITexture texture,
            ResourceAccessType targetState) {
        device.withLifecycleLock(() -> transitionTextureLocked(
                requireCommandBufferLocked(commandBuffer, "restoreExternalResource"),
                requireTextureLocked(texture, "restoreExternalResource"),
                D3D12ResourceState.fromAccessType(
                        Objects.requireNonNull(targetState, "targetState"))));
    }

    public void transitionTexture(
            D3D12CommandBuffer commandBuffer,
            D3D12Texture2D texture,
            D3D12ResourceState targetState) {
        device.withLifecycleLock(() ->
                transitionTextureLocked(commandBuffer, texture, targetState));
    }

    private void transitionTextureLocked(
            D3D12CommandBuffer commandBuffer,
            D3D12Texture2D texture,
            D3D12ResourceState targetState) {
        device.assertLifecycleLockHeld();
        Objects.requireNonNull(targetState, "targetState");
        long commandList = commandBuffer.nativeHandleForDecoderLocked("transitionTexture");
        device.requireTextureLocked(texture, "transitionTexture");
        D3D12ResourceState currentState = D3D12ResourceState.fromNativeCode(
                D3D12Native.nGetCommandTextureState(
                        commandList,
                        texture.nativeHandleLocked()));
        if (currentState == targetState) {
            commandBuffer.recordTextureStateLocked(texture, targetState);
            return;
        }
        D3D12ResourceState previousState = commandBuffer.recordTextureStateLocked(
                texture,
                targetState);
        try {
            D3D12Exception.check(
                    D3D12Native.nCmdTransitionTexture(
                            commandList,
                            texture.nativeHandleLocked(),
                            currentState.nativeCode(),
                            targetState.nativeCode()),
                    "Record D3D12 Texture2D transition");
        } catch (Throwable throwable) {
            commandBuffer.restoreRecordedTextureStateLocked(texture, previousState);
            throw throwable;
        }
    }

    @Override
    public void clearTextureRGBA(
            ICommandBuffer commandBuffer,
            ITexture texture,
            float[] color) {
        device.withLifecycleLock(() -> {
            D3D12CommandBuffer d3d12CommandBuffer = requireCommandBufferLocked(
                    commandBuffer, "clearTextureRGBA");
            D3D12Texture2D d3d12Texture = requireTextureLocked(
                    texture, "clearTextureRGBA");
            if ((d3d12Texture.resourceFlags() &
                    D3D12Native.RESOURCE_FLAG_ALLOW_RENDER_TARGET) == 0) {
                throw new IllegalArgumentException(
                        "clearTextureRGBA: D3D12 texture was not created with render-target clear support");
            }
            transitionTextureLocked(
                    d3d12CommandBuffer,
                    d3d12Texture,
                    D3D12ResourceState.RENDER_TARGET);

            float red = color.length > 0 ? color[0] : 0.0f;
            float green = color.length > 1 ? color[1] : 0.0f;
            float blue = color.length > 2 ? color[2] : 0.0f;
            float alpha = color.length > 3 ? color[3] : 1.0f;
            D3D12Exception.check(
                    D3D12Native.nCmdClearTextureRgba(
                            d3d12CommandBuffer.nativeHandleForDecoderLocked(
                                    "clearTextureRGBA"),
                            d3d12Texture.nativeHandleLocked(),
                            red,
                            green,
                            blue,
                            alpha),
                    "Record D3D12 Texture2D RGBA clear");
        });
    }

    @Override
    public void clearTextureDepth(ICommandBuffer commandBuffer, ITexture texture, float depth) {
        device.ensureOpen();
        throw D3D12Exception.unsupported("texture depth clear");
    }

    @Override
    public void clearTextureStencil(ICommandBuffer commandBuffer, ITexture texture, int stencil) {
        device.ensureOpen();
        throw D3D12Exception.unsupported("texture stencil clear");
    }

    @Override
    public void copyTexture(
            ICommandBuffer commandBuffer,
            ITexture src,
            ITexture dst,
            int srcX0,
            int srcY0,
            int srcX1,
            int srcY1,
            int srcLevel,
            int dstX0,
            int dstY0,
            int dstX1,
            int dstY1,
            int dstLevel) {
        device.withLifecycleLock(() -> {
            D3D12CommandBuffer d3d12CommandBuffer = requireCommandBufferLocked(
                    commandBuffer, "copyTexture");
            D3D12Texture2D source = requireTextureLocked(
                    src, "copyTexture(source)");
            D3D12Texture2D destination = requireTextureLocked(
                    dst, "copyTexture(destination)");
            int sourceWidth = srcX1 - srcX0;
            int sourceHeight = srcY1 - srcY0;
            if (sourceWidth != dstX1 - dstX0 || sourceHeight != dstY1 - dstY0) {
                throw new IllegalArgumentException(
                        "copyTexture: D3D12 copy regions must have identical extents");
            }
            transitionTextureLocked(
                    d3d12CommandBuffer, source, D3D12ResourceState.COPY_SOURCE);
            transitionTextureLocked(
                    d3d12CommandBuffer,
                    destination,
                    D3D12ResourceState.COPY_DESTINATION);
            D3D12Exception.check(
                    D3D12Native.nCmdCopyTexture(
                            d3d12CommandBuffer.nativeHandleForDecoderLocked(
                                    "copyTexture"),
                            source.nativeHandleLocked(),
                            destination.nativeHandleLocked(),
                            srcX0,
                            srcY0,
                            sourceWidth,
                            sourceHeight,
                            srcLevel,
                            dstX0,
                            dstY0,
                            dstLevel),
                    "Record D3D12 Texture2D copy");
        });
    }

    @Override
    public void copyBuffer(
            ICommandBuffer commandBuffer,
            IBuffer src,
            IBuffer dst,
            long srcOffset,
            long dstOffset,
            long size) {
        device.withLifecycleLock(() -> {
            D3D12CommandBuffer d3d12CommandBuffer = requireCommandBufferLocked(
                    commandBuffer, "copyBuffer");
            D3D12Buffer source = requireBufferLocked(src, "copyBuffer(source)");
            D3D12Buffer destination = requireBufferLocked(
                    dst, "copyBuffer(destination)");
            requireBufferRangeLocked(
                    source, srcOffset, size, "copyBuffer(source)");
            requireBufferRangeLocked(
                    destination, dstOffset, size, "copyBuffer(destination)");
            if (source.heap() != D3D12Buffer.Heap.UPLOAD ||
                    source.initialState() != D3D12ResourceState.COPY_SOURCE ||
                    !source.getUsages().has(BufferUsage.TransferSrc)) {
                throw new IllegalArgumentException(
                        "copyBuffer: source must be an UPLOAD/COPY_SOURCE TransferSrc buffer");
            }
            if (destination.heap() != D3D12Buffer.Heap.DEFAULT ||
                    destination.initialState() != D3D12ResourceState.COPY_DESTINATION ||
                    !destination.getUsages().has(BufferUsage.TransferDst)) {
                throw new IllegalArgumentException(
                        "copyBuffer: destination must be a DEFAULT/COPY_DESTINATION TransferDst buffer");
            }
            D3D12Exception.check(
                    D3D12Native.nCmdCopyBuffer(
                            d3d12CommandBuffer.nativeHandleForDecoderLocked(
                                    "copyBuffer"),
                            source.nativeHandleLocked(),
                            destination.nativeHandleLocked(),
                            srcOffset,
                            dstOffset,
                            size),
                    "Record D3D12 buffer copy");
        });
    }

    @Override
    public void writeToBuffer(
            ICommandBuffer commandBuffer,
            IBuffer dst,
            long dstOffset,
            long size,
            ByteBuffer data) {
        device.withLifecycleLock(() -> writeToBufferInternalLocked(
                requireCommandBufferLocked(commandBuffer, "writeToBuffer"),
                requireBufferLocked(dst, "writeToBuffer"),
                dstOffset,
                size,
                data));
    }

    void writeToBufferInternalLocked(
            D3D12CommandBuffer commandBuffer,
            D3D12Buffer destination,
            long destinationOffset,
            long size,
            ByteBuffer data) {
        device.assertLifecycleLockHeld();
        if (size <= 0) {
            return;
        }
        if (destination.heap() == D3D12Buffer.Heap.READBACK) {
            throw new IllegalArgumentException(
                    "writeToBuffer: destination cannot use the READBACK heap");
        }
        if (destination.heap() == D3D12Buffer.Heap.DEFAULT &&
                destination.initialState() != D3D12ResourceState.COPY_DESTINATION) {
            throw new IllegalArgumentException(
                    "writeToBuffer: DEFAULT destination must use COPY_DESTINATION state");
        }
        DirectData directData = directData(data, size, "writeToBuffer");
        D3D12Exception.check(
                D3D12Native.nCmdWriteBuffer(
                        commandBuffer.nativeHandleForDecoderLocked("writeToBuffer"),
                        destination.nativeHandleLocked(),
                        destinationOffset,
                        directData.buffer,
                        directData.offset,
                        Math.toIntExact(size)),
                "Record D3D12 buffer upload");
    }

    @Override
    public void writeToTexture(
            ICommandBuffer commandBuffer,
            ITexture texture,
            ByteBuffer data,
            int x,
            int y,
            int width,
            int height,
            int mipLevel) {
        device.withLifecycleLock(() -> {
            D3D12CommandBuffer d3d12CommandBuffer = requireCommandBufferLocked(
                    commandBuffer, "writeToTexture");
            D3D12Texture2D destination = requireTextureLocked(
                    texture, "writeToTexture");
            int rowPitch = Math.multiplyExact(
                    width,
                    destination.getTextureFormat().getBytesPerPixel());
            long dataSize = Math.multiplyExact((long) rowPitch, height);
            DirectData directData = directData(data, dataSize, "writeToTexture");
            transitionTextureLocked(
                    d3d12CommandBuffer,
                    destination,
                    D3D12ResourceState.COPY_DESTINATION);
            D3D12Exception.check(
                    D3D12Native.nCmdWriteTexture2D(
                            d3d12CommandBuffer.nativeHandleForDecoderLocked(
                                    "writeToTexture"),
                            destination.nativeHandleLocked(),
                            x,
                            y,
                            width,
                            height,
                            mipLevel,
                            rowPitch,
                            directData.buffer,
                            directData.offset,
                            Math.toIntExact(dataSize)),
                    "Record D3D12 Texture2D upload");
        });
    }

    @Override
    public void setViewport(
            ICommandBuffer commandBuffer,
            float x,
            float y,
            float width,
            float height) {
        device.ensureOpen();
        throw D3D12Exception.unsupported("viewport state");
    }

    @Override
    public void setScissor(
            ICommandBuffer commandBuffer,
            int x,
            int y,
            int width,
            int height) {
        device.ensureOpen();
        throw D3D12Exception.unsupported("scissor state");
    }

    @Override
    public void setLineWidth(ICommandBuffer commandBuffer, float width) {
        device.ensureOpen();
        throw D3D12Exception.unsupported("line width state");
    }

    @Override
    public void setBlendConstants(
            ICommandBuffer commandBuffer,
            float r,
            float g,
            float b,
            float a) {
        device.ensureOpen();
        throw D3D12Exception.unsupported("blend constants");
    }

    @Override
    public void beginRenderPass(ICommandBuffer commandBuffer, RenderPass renderPass) {
        device.ensureOpen();
        throw D3D12Exception.unsupported("render pass recording");
    }

    @Override
    public void endRenderPass(ICommandBuffer commandBuffer) {
        device.ensureOpen();
        throw D3D12Exception.unsupported("render pass recording");
    }

    @Override
    public void bindPipeline(ICommandBuffer commandBuffer, GraphicsPipeline pipeline) {
        device.ensureOpen();
        throw D3D12Exception.unsupported("graphics pipelines");
    }

    @Override
    public void bindPipeline(ICommandBuffer commandBuffer, ComputePipeline pipeline) {
        device.ensureOpen();
        throw D3D12Exception.unsupported("compute pipelines");
    }

    @Override
    public void draw(
            ICommandBuffer commandBuffer,
            IVertexBuffer vertexBuffer,
            int vertexCount,
            int firstVertex) {
        device.ensureOpen();
        throw D3D12Exception.unsupported("draw commands");
    }

    @Override
    public void dispatch(
            ICommandBuffer commandBuffer,
            int groupCountX,
            int groupCountY,
            int groupCountZ) {
        device.ensureOpen();
        throw D3D12Exception.unsupported("compute dispatch");
    }

    @Override
    public void memoryBarrier(
            ICommandBuffer commandBuffer,
            MemoryBarrierType... barriers) {
        device.withLifecycleLock(() -> {
            D3D12CommandBuffer d3d12CommandBuffer = requireCommandBufferLocked(
                    commandBuffer, "memoryBarrier");
            for (MemoryBarrierType barrier : barriers) {
                if (barrier != MemoryBarrierType.STORAGE_IMAGE_WRITE &&
                        barrier != MemoryBarrierType.SHADER_STORAGE &&
                        barrier != MemoryBarrierType.ALL) {
                    throw D3D12Exception.unsupported(
                            "memory barrier " + barrier);
                }
            }
            D3D12Exception.check(
                    D3D12Native.nCmdUavBarrier(
                            d3d12CommandBuffer.nativeHandleForDecoderLocked(
                                    "memoryBarrier"),
                            0),
                    "Record global D3D12 UAV barrier");
        });
    }

    @Override
    public IDevice getDevice() {
        device.ensureOpen();
        return device;
    }

    void trackTextureLocked(D3D12Texture2D texture, D3D12ResourceState state) {
        device.assertLifecycleLockHeld();
        stateTracker.setState(texture, state.trackerState());
    }

    boolean hasTrackedTextureLocked(D3D12Texture2D texture) {
        device.assertLifecycleLockHeld();
        return stateTracker.hasState(texture);
    }

    boolean replaceTrackedTextureStateLocked(
            D3D12Texture2D texture,
            D3D12ResourceState state) {
        device.assertLifecycleLockHeld();
        return stateTracker.replaceState(texture, state.trackerState());
    }

    void removeTextureLocked(D3D12Texture2D texture) {
        device.assertLifecycleLockHeld();
        stateTracker.remove(texture);
    }

    private D3D12CommandBuffer requireCommandBufferLocked(
            ICommandBuffer commandBuffer,
            String operation) {
        device.assertLifecycleLockHeld();
        device.ensureOpenLocked();
        if (!(commandBuffer instanceof D3D12CommandBuffer d3d12CommandBuffer)) {
            throw new IllegalArgumentException(
                    operation + ": command buffer is not a D3D12 command buffer");
        }
        if (d3d12CommandBuffer.getDevice() != device) {
            throw new IllegalArgumentException(
                    operation + ": command buffer belongs to a different device");
        }
        d3d12CommandBuffer.requireDecoderRecordingLocked(operation);
        return d3d12CommandBuffer;
    }

    private D3D12Texture2D requireTextureLocked(ITexture texture, String operation) {
        device.assertLifecycleLockHeld();
        if (!(texture instanceof D3D12Texture2D d3d12Texture)) {
            throw new IllegalArgumentException(
                    operation + ": texture is not a D3D12 Texture2D");
        }
        device.requireTextureLocked(d3d12Texture, operation);
        return d3d12Texture;
    }

    private D3D12Buffer requireBufferLocked(IBuffer buffer, String operation) {
        device.assertLifecycleLockHeld();
        device.ensureOpenLocked();
        if (!(buffer instanceof D3D12Buffer d3d12Buffer)) {
            throw new IllegalArgumentException(
                    operation + ": buffer is not a D3D12 buffer");
        }
        if (d3d12Buffer.device() != device) {
            throw new IllegalArgumentException(
                    operation + ": buffer belongs to a different device");
        }
        d3d12Buffer.nativeHandleLocked();
        return d3d12Buffer;
    }

    private static void requireBufferRangeLocked(
            D3D12Buffer buffer,
            long offset,
            long size,
            String operation) {
        long capacity = buffer.sizeLocked();
        if (offset < 0 || size <= 0 || offset > capacity ||
                size > capacity - offset) {
            throw new IllegalArgumentException(
                    operation + ": copy range is out of bounds");
        }
    }

    private static DirectData directData(ByteBuffer data, long size, String operation) {
        Objects.requireNonNull(data, "data");
        if (size < 0 || size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    operation + ": data size cannot be represented by a Java ByteBuffer");
        }
        int byteCount = (int) size;
        if (data.remaining() < byteCount) {
            throw new IllegalArgumentException(
                    operation + ": source data is smaller than the requested upload size");
        }
        if (data.isDirect()) {
            return new DirectData(data, data.position());
        }
        ByteBuffer source = data.duplicate();
        source.limit(source.position() + byteCount);
        ByteBuffer direct = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.nativeOrder());
        direct.put(source).flip();
        return new DirectData(direct, 0);
    }

    private record DirectData(ByteBuffer buffer, int offset) {
    }
}
