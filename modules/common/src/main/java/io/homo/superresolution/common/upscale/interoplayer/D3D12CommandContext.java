package io.homo.superresolution.common.upscale.interoplayer;

import io.homo.superresolution.core.graphics.d3d12.D3D12CommandBuffer;
import io.homo.superresolution.core.graphics.d3d12.D3D12ResourceState;
import io.homo.superresolution.core.graphics.d3d12.D3D12Texture2D;
import io.homo.superresolution.srapi.SRRenderApiType;

import java.util.Objects;

public final class D3D12CommandContext implements InteropCommandContext {
    private final D3D12CommandBuffer commandBuffer;
    private final boolean borrowed;
    private D3D12CommandBuffer.NativeCommandListLease nativeLease;
    private boolean closed;

    public D3D12CommandContext(D3D12CommandBuffer commandBuffer) {
        this(commandBuffer, false);
    }

    public D3D12CommandContext(D3D12CommandBuffer commandBuffer, boolean borrowed) {
        this.commandBuffer = Objects.requireNonNull(commandBuffer, "commandBuffer");
        this.borrowed = borrowed;
    }

    @Override
    public SRRenderApiType api() {
        return SRRenderApiType.D3D12;
    }

    @Override
    public D3D12CommandBuffer commandBuffer() {
        requireOpen();
        return commandBuffer;
    }

    /** Borrowed by provider dispatch descriptions; the interop implementation closes it. */
    public D3D12CommandBuffer.NativeCommandListLease leaseNativeCommandList() {
        requireOpen();
        if (nativeLease == null) {
            nativeLease = commandBuffer.leaseNativeCommandList();
        }
        return nativeLease;
    }

    @Override
    public long nativeHandle() {
        return leaseNativeCommandList().handle();
    }

    public void setTextureState(D3D12Texture2D texture, D3D12ResourceState state) {
        leaseNativeCommandList().setTextureState(texture, state);
    }

    @Override
    public boolean borrowed() {
        return borrowed;
    }

    @Override
    public void close() {
        if (!closed) {
            if (nativeLease != null) {
                nativeLease.close();
            }
            closed = true;
        }
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("D3D12 command context is closed");
        }
    }
}
