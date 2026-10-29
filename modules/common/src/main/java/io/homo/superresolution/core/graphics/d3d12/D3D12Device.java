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

import io.homo.superresolution.core.graphics.impl.buffer.BufferDescription;
import io.homo.superresolution.core.graphics.impl.buffer.BufferUsage;
import io.homo.superresolution.core.graphics.impl.command.CommandBufferBehavior;
import io.homo.superresolution.core.graphics.impl.command.CommandBufferState;
import io.homo.superresolution.core.graphics.impl.command.CommandPoolFlags;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandDecoder;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.impl.framebuffer.FramebufferDescription;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.pipeline.ComputePipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.GraphicsPipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineDescriptorSet;
import io.homo.superresolution.core.graphics.impl.pipeline.RenderPass;
import io.homo.superresolution.core.graphics.impl.sampler.ISampler;
import io.homo.superresolution.core.graphics.impl.sampler.SamplerDescription;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.texture.ITextureView;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsage;
import io.homo.superresolution.core.graphics.impl.texture.TextureViewDescription;
import io.homo.superresolution.core.graphics.impl.validation.ValidatedCommandDecoder;
import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;
import io.homo.superresolution.core.graphics.impl.vertex.VertexBufferDescription;
import io.homo.superresolution.core.utils.ThrowableUtil;
import io.homo.superresolution.srapi.SRSurfaceFormat;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public final class D3D12Device implements IDevice, AutoCloseable {
    private static final int DXGI_FORMAT_R16G16B16A16_FLOAT = 10;
    private static final int DXGI_FORMAT_R11G11B10_FLOAT = 26;
    private static final int DXGI_FORMAT_R8G8B8A8_UNORM = 28;
    private static final int DXGI_FORMAT_R16G16_FLOAT = 34;
    private static final int DXGI_FORMAT_R32_FLOAT = 41;

    private final int debugFlags;
    private final long adapterLuid;
    private final long nativeDevice;
    private final D3D12Queue directQueue;
    private final D3D12CommandDecoder rawCommandDecoder;
    private final ValidatedCommandDecoder validatedCommandDecoder;
    private final ReentrantLock lifecycleLock = new ReentrantLock();
    private final ArrayList<D3D12CommandPool> commandPools = new ArrayList<>();
    private final ArrayList<D3D12Fence> fences = new ArrayList<>();
    private final ArrayList<D3D12Texture2D> textures = new ArrayList<>();
    private final ArrayList<D3D12Buffer> buffers = new ArrayList<>();
    private D3D12CommandPool defaultCommandPool;
    private long nativeHandle;
    private int externalBorrowCount;

    public D3D12Device(long adapterLuid) {
        this(adapterLuid, D3D12Native.DEBUG_NONE);
    }

    public D3D12Device(long adapterLuid, int debugFlags) {
        if (adapterLuid == 0) {
            throw new IllegalArgumentException("D3D12 adapter LUID must be nonzero");
        }
        this.debugFlags = debugFlags;
        nativeHandle = D3D12Exception.requireHandle(
                D3D12Native.nCreateDevice(adapterLuid, debugFlags),
                "Create D3D12 device");
        try {
            long queriedDevice = D3D12Native.nGetNativeDevice(nativeHandle);
            long queriedQueue = D3D12Native.nGetNativeQueue(nativeHandle);
            long queriedLuid = D3D12Native.nGetDeviceAdapterLuid(nativeHandle);
            if (queriedDevice == 0 || queriedQueue == 0 || queriedLuid == 0) {
                throw D3D12Exception.fromLastError(
                        "Query D3D12 device metadata", null);
            }

            this.nativeDevice = queriedDevice;
            this.adapterLuid = queriedLuid;
            this.directQueue = new D3D12Queue(this, queriedQueue);
            this.rawCommandDecoder = new D3D12CommandDecoder(this);
            this.validatedCommandDecoder = new ValidatedCommandDecoder(rawCommandDecoder);
            setDebugName(nativeHandle, "SuperResolution D3D12 Device");
            defaultCommandPool = createCommandPool(CommandPoolFlags.Reset);
        } catch (Throwable throwable) {
            if (defaultCommandPool != null) {
                try {
                    defaultCommandPool.destroy();
                } catch (Throwable destroyFailure) {
                    appendFailure(throwable, destroyFailure);
                }
            }
            try {
                D3D12Native.nDestroyDevice(nativeHandle);
                nativeHandle = 0;
            } catch (Throwable destroyFailure) {
                appendFailure(throwable, destroyFailure);
            }
            ThrowableUtil.rethrowError(throwable);
            throw throwable;
        }
    }

    <T> T withLifecycleLock(Supplier<T> action) {
        lifecycleLock.lock();
        try {
            return action.get();
        } finally {
            lifecycleLock.unlock();
        }
    }

    void withLifecycleLock(Runnable action) {
        lifecycleLock.lock();
        try {
            action.run();
        } finally {
            lifecycleLock.unlock();
        }
    }

    void assertLifecycleLockHeld() {
        if (!lifecycleLock.isHeldByCurrentThread()) {
            throw new IllegalStateException("D3D12 lifecycle lock is not held");
        }
    }

    public ExternalBorrowLease borrowExternal() {
        return withLifecycleLock(this::borrowExternalLocked);
    }

    ExternalBorrowLease borrowExternalLocked() {
        ExternalBorrowLease lease = new ExternalBorrowLease(this);
        retainExternalBorrowLocked();
        return lease;
    }

    void retainExternalBorrowLocked() {
        assertLifecycleLockHeld();
        ensureOpenLocked();
        if (externalBorrowCount == Integer.MAX_VALUE) {
            throw new IllegalStateException("Too many active D3D12 external borrows");
        }
        ++externalBorrowCount;
    }

    void releaseExternalBorrowLocked() {
        assertLifecycleLockHeld();
        if (externalBorrowCount <= 0) {
            throw new IllegalStateException("D3D12 external borrow count underflow");
        }
        --externalBorrowCount;
    }

    public long adapterLuid() {
        return withLifecycleLock(() -> {
            ensureOpenLocked();
            return adapterLuid;
        });
    }

    public int debugFlags() {
        return withLifecycleLock(() -> {
            ensureOpenLocked();
            return debugFlags;
        });
    }

    public long nativeDevice() {
        return withLifecycleLock(() -> {
            ensureOpenLocked();
            return nativeDevice;
        });
    }

    public D3D12Queue directQueue() {
        return withLifecycleLock(() -> {
            ensureOpenLocked();
            return directQueue;
        });
    }

    public D3D12Fence createFence(long initialValue) {
        return withLifecycleLock(() -> {
            ensureOpenLocked();
            if (initialValue < 0) {
                throw new IllegalArgumentException(
                        "D3D12 fence initial value cannot be negative");
            }
            fences.ensureCapacity(Math.addExact(fences.size(), 1));
            D3D12Fence fence = new D3D12Fence(this);
            fences.add(fence);
            try {
                fence.initializeLocked(initialValue);
                setDebugNameLocked(
                        fence.nativeHandleLocked(),
                        "SuperResolution D3D12 Shared Fence");
                return fence;
            } catch (Throwable throwable) {
                try {
                    fence.destroyLocked();
                } catch (Throwable destroyFailure) {
                    appendFailure(throwable, destroyFailure);
                }
                ThrowableUtil.rethrowError(throwable);
                throw throwable;
            }
        });
    }

    @Override
    public D3D12Texture2D createTexture(TextureDescription description) {
        return createTexture2D(description, D3D12ResourceState.COMMON, false);
    }

    public D3D12Texture2D createSharedTexture2D(
            TextureDescription description,
            D3D12ResourceState initialState) {
        return createTexture2D(description, initialState, true);
    }

    public D3D12Texture2D createTexture2D(
            TextureDescription description,
            D3D12ResourceState initialState,
            boolean shared) {
        return withLifecycleLock(() -> {
            ensureOpenLocked();
            validateTextureDescription(description, initialState);
            FormatMapping format = formatMapping(description.getFormat());
            int mipLevels = description.getMipmapSettings().resolveLevels(
                    description.getWidth(),
                    description.getHeight());
            int resourceFlags = textureResourceFlags(description);
            textures.ensureCapacity(Math.addExact(textures.size(), 1));
            D3D12Texture2D texture = new D3D12Texture2D(
                    this,
                    description,
                    format.surfaceFormat,
                    format.dxgiFormat,
                    resourceFlags,
                    shared);
            textures.add(texture);
            try {
                texture.initializeLocked(mipLevels, initialState);
                setDebugNameLocked(
                        texture.nativeHandleLocked(),
                        textureDebugName(description));
                rawCommandDecoder.trackTextureLocked(texture, initialState);
                return texture;
            } catch (Throwable throwable) {
                try {
                    texture.destroyLocked();
                } catch (Throwable destroyFailure) {
                    appendFailure(throwable, destroyFailure);
                }
                ThrowableUtil.rethrowError(throwable);
                throw throwable;
            }
        });
    }

    @Override
    public D3D12Buffer createBuffer(BufferDescription description) {
        return createBuffer(description, false);
    }

    public D3D12Buffer createSharedBuffer(BufferDescription description) {
        return createBuffer(description, true);
    }

    private D3D12Buffer createBuffer(BufferDescription description, boolean shared) {
        return withLifecycleLock(() -> {
            ensureOpenLocked();
            if (description == null) {
                throw new IllegalArgumentException("D3D12 buffer description cannot be null");
            }
            if (description.size() <= 0 || description.usage() == null ||
                    description.usage().isEmpty()) {
                throw new IllegalArgumentException("D3D12 buffer description is incomplete");
            }
            boolean transferSource = description.usage().has(BufferUsage.TransferSrc);
            if (transferSource && description.usage().has(BufferUsage.TransferDst)) {
                throw new IllegalArgumentException(
                        "A stage-1 D3D12 buffer cannot be both TransferSrc and TransferDst");
            }
            D3D12Buffer.Heap heap = transferSource
                    ? D3D12Buffer.Heap.UPLOAD
                    : D3D12Buffer.Heap.DEFAULT;
            D3D12ResourceState initialState = transferSource
                    ? D3D12ResourceState.COPY_SOURCE
                    : D3D12ResourceState.COPY_DESTINATION;
            if (shared && heap != D3D12Buffer.Heap.DEFAULT) {
                throw new IllegalArgumentException(
                        "Shared D3D12 buffers must use the DEFAULT heap");
            }
            buffers.ensureCapacity(Math.addExact(buffers.size(), 1));
            D3D12Buffer buffer = new D3D12Buffer(
                    this,
                    description,
                    heap,
                    initialState,
                    D3D12Native.RESOURCE_FLAG_NONE,
                    shared);
            buffers.add(buffer);
            try {
                buffer.initializeLocked();
                setDebugNameLocked(
                        buffer.nativeHandleLocked(),
                        "SuperResolution D3D12 Buffer (" + description.size() + " bytes)");
                return buffer;
            } catch (Throwable throwable) {
                try {
                    buffer.destroyLocked(false);
                } catch (Throwable destroyFailure) {
                    appendFailure(throwable, destroyFailure);
                }
                ThrowableUtil.rethrowError(throwable);
                throw throwable;
            }
        });
    }

    @Override
    public D3D12CommandPool createCommandPool(CommandPoolFlags... flags) {
        return withLifecycleLock(() -> {
            ensureOpenLocked();
            EnumSet<CommandPoolFlags> flagSet = EnumSet.noneOf(CommandPoolFlags.class);
            if (flags != null) {
                for (CommandPoolFlags flag : flags) {
                    if (flag == null) {
                        throw new IllegalArgumentException(
                                "D3D12 command pool flags cannot contain null");
                    }
                    flagSet.add(flag);
                }
            }
            return createCommandPoolLocked(flagSet);
        });
    }

    private D3D12CommandPool createCommandPoolLocked(
            EnumSet<CommandPoolFlags> flags) {
        assertLifecycleLockHeld();
        ensureOpenLocked();
        commandPools.ensureCapacity(Math.addExact(commandPools.size(), 1));
        D3D12CommandPool commandPool = new D3D12CommandPool(this, flags);
        try {
            commandPools.add(commandPool);
            return commandPool;
        } catch (Throwable throwable) {
            try {
                commandPool.destroyLocked();
            } catch (Throwable destroyFailure) {
                appendFailure(throwable, destroyFailure);
            }
            ThrowableUtil.rethrowError(throwable);
            throw throwable;
        }
    }

    private D3D12CommandPool defaultCommandPoolLocked() {
        assertLifecycleLockHeld();
        ensureOpenLocked();
        if (defaultCommandPool == null) {
            defaultCommandPool = createCommandPoolLocked(
                    EnumSet.of(CommandPoolFlags.Reset));
        }
        return defaultCommandPool;
    }

    @Override
    public D3D12CommandBuffer createCommandBuffer() {
        return createCommandBuffer(CommandBufferBehavior.OneTimeSubmit);
    }

    public D3D12CommandBuffer createCommandBuffer(CommandBufferBehavior behavior) {
        return withLifecycleLock(() ->
                defaultCommandPoolLocked().createCommandBufferLocked(behavior));
    }

    @Override
    public D3D12CommandPool defaultCommandPool() {
        return withLifecycleLock(this::defaultCommandPoolLocked);
    }

    @Override
    public ICommandDecoder commandDecoder() {
        return withLifecycleLock(() -> {
            ensureOpenLocked();
            return validatedCommandDecoder;
        });
    }

    public D3D12CommandDecoder rawCommandDecoder() {
        return withLifecycleLock(() -> {
            ensureOpenLocked();
            return rawCommandDecoder;
        });
    }

    @Override
    public void submitCommandBuffer(ICommandBuffer commandBuffer) {
        withLifecycleLock(() -> {
            ensureOpenLocked();
            if (!(commandBuffer instanceof D3D12CommandBuffer d3d12CommandBuffer)) {
                throw new IllegalArgumentException(
                        "Command buffer is not a D3D12 command buffer");
            }
            directQueue.submitLocked(d3d12CommandBuffer, null, 0, 0);
        });
    }

    public long completedSubmissionValue() {
        return withLifecycleLock(this::completedSubmissionValueLocked);
    }

    public long lastSubmittedValue() {
        return withLifecycleLock(this::lastSubmittedValueLocked);
    }

    public void waitIdle() {
        waitIdle(D3D12Native.WAIT_INFINITE);
    }

    public void waitIdle(int timeoutMilliseconds) {
        withLifecycleLock(() -> waitIdleLocked(timeoutMilliseconds));
    }

    void waitForSubmission(long completionValue) {
        withLifecycleLock(() -> waitForSubmissionLocked(completionValue));
    }

    void uploadMappedBuffer(
            D3D12Buffer destination,
            int destinationOffset,
            int size,
            ByteBuffer data) {
        withLifecycleLock(() -> uploadMappedBufferLocked(
                destination, destinationOffset, size, data));
    }

    void uploadMappedBufferLocked(
            D3D12Buffer destination,
            int destinationOffset,
            int size,
            ByteBuffer data) {
        assertLifecycleLockHeld();
        ensureOpenLocked();
        D3D12CommandBuffer commandBuffer = defaultCommandPoolLocked().createCommandBufferLocked(
                CommandBufferBehavior.OneTimeSubmit);
        try {
            commandBuffer.beginLocked();
            rawCommandDecoder.writeToBufferInternalLocked(
                    commandBuffer,
                    destination,
                    destinationOffset,
                    size,
                    data);
            commandBuffer.endLocked();
            directQueue.submitLocked(commandBuffer, null, 0, 0);
        } catch (Throwable throwable) {
            if (commandBuffer.stateLocked() != CommandBufferState.Destroyed) {
                try {
                    commandBuffer.destroyLocked();
                } catch (Throwable destroyFailure) {
                    appendFailure(throwable, destroyFailure);
                }
            }
            ThrowableUtil.rethrowError(throwable);
            throw throwable;
        }
    }

    void requireTexture(D3D12Texture2D texture, String operation) {
        withLifecycleLock(() -> requireTextureLocked(texture, operation));
    }

    void requireTextureLocked(D3D12Texture2D texture, String operation) {
        assertLifecycleLockHeld();
        ensureOpenLocked();
        if (texture == null || texture.device() != this) {
            throw new IllegalArgumentException(
                    operation + ": texture belongs to a different D3D12 device");
        }
        texture.nativeHandleLocked();
    }

    void onTextureStateChanged(D3D12Texture2D texture, D3D12ResourceState state) {
        withLifecycleLock(() -> rawCommandDecoder.trackTextureLocked(texture, state));
    }

    void onTextureStateCommittedLocked(
            D3D12Texture2D texture,
            D3D12ResourceState state) {
        assertLifecycleLockHeld();
        rawCommandDecoder.replaceTrackedTextureStateLocked(texture, state);
    }

    void validateTextureStateCommitLocked(D3D12Texture2D texture) {
        assertLifecycleLockHeld();
        if (!rawCommandDecoder.hasTrackedTextureLocked(texture)) {
            throw new IllegalStateException(
                    "D3D12 texture is missing from the resource state tracker before submit");
        }
    }

    void onFenceDestroyedLocked(D3D12Fence fence) {
        assertLifecycleLockHeld();
        fences.remove(fence);
    }

    void onTextureDestroyedLocked(D3D12Texture2D texture) {
        assertLifecycleLockHeld();
        rawCommandDecoder.removeTextureLocked(texture);
        textures.remove(texture);
    }

    void onBufferDestroyedLocked(D3D12Buffer buffer) {
        assertLifecycleLockHeld();
        buffers.remove(buffer);
    }

    void onCommandPoolDestroyedLocked(D3D12CommandPool commandPool) {
        assertLifecycleLockHeld();
        if (defaultCommandPool == commandPool) {
            defaultCommandPool = null;
        }
        commandPools.remove(commandPool);
    }

    long nativeHandle() {
        return withLifecycleLock(this::nativeHandleLocked);
    }

    void ensureOpen() {
        withLifecycleLock(this::ensureOpenLocked);
    }

    long nativeHandleLocked() {
        assertLifecycleLockHeld();
        ensureOpenLocked();
        return nativeHandle;
    }

    long completedSubmissionValueLocked() {
        assertLifecycleLockHeld();
        ensureOpenLocked();
        return D3D12Native.nGetCompletedSubmissionValue(nativeHandle);
    }

    long lastSubmittedValueLocked() {
        assertLifecycleLockHeld();
        ensureOpenLocked();
        return D3D12Native.nGetLastSubmittedValue(nativeHandle);
    }

    void waitIdleLocked(int timeoutMilliseconds) {
        assertLifecycleLockHeld();
        ensureOpenLocked();
        if (timeoutMilliseconds < 0 && timeoutMilliseconds != D3D12Native.WAIT_INFINITE) {
            throw new IllegalArgumentException("timeoutMilliseconds must be nonnegative or -1");
        }
        D3D12Exception.check(
                D3D12Native.nWaitIdle(nativeHandle, timeoutMilliseconds),
                "Wait for D3D12 device idle");
    }

    void waitForSubmissionLocked(long completionValue) {
        assertLifecycleLockHeld();
        ensureOpenLocked();
        if (completionValue <= 0 || completedSubmissionValueLocked() >= completionValue) {
            return;
        }
        waitIdleLocked(D3D12Native.WAIT_INFINITE);
        if (completedSubmissionValueLocked() < completionValue) {
            throw new D3D12Exception(
                    "D3D12 wait-idle completed before the requested submission value " +
                            completionValue);
        }
    }

    void ensureOpenLocked() {
        assertLifecycleLockHeld();
        if (nativeHandle == 0) {
            throw new IllegalStateException("D3D12 device is destroyed");
        }
    }

    void setDebugName(long nativeOwnerHandle, String name) {
        withLifecycleLock(() -> setDebugNameLocked(nativeOwnerHandle, name));
    }

    void setDebugNameLocked(long nativeOwnerHandle, String name) {
        assertLifecycleLockHeld();
        ensureOpenLocked();
        if (nativeOwnerHandle == 0 || name == null || name.isBlank()) {
            return;
        }
        D3D12Exception.check(
                D3D12Native.nSetDebugName(nativeOwnerHandle, name),
                "Set D3D12 debug name");
    }

    @Override
    public void close() {
        destroy();
    }

    public void destroy() {
        withLifecycleLock(this::destroyLocked);
    }

    private void destroyLocked() {
        assertLifecycleLockHeld();
        if (nativeHandle == 0) {
            return;
        }
        if (externalBorrowCount != 0) {
            throw new IllegalStateException(
                    "Cannot destroy the D3D12 device while " +
                            externalBorrowCount + " external borrow(s) are active");
        }
        waitIdleLocked(D3D12Native.WAIT_INFINITE);
        Throwable failure = null;

        List<D3D12CommandPool> pools = List.copyOf(commandPools);
        Throwable validationFailure = null;
        for (D3D12CommandPool commandPool : pools) {
            try {
                commandPool.validateDestroyAfterDeviceIdleLocked();
            } catch (Throwable throwable) {
                validationFailure = appendFailure(validationFailure, throwable);
            }
        }
        if (validationFailure != null) {
            failure = appendFailure(failure, validationFailure);
            rethrowDestroyFailure(failure);
        }

        for (D3D12CommandPool commandPool : pools) {
            try {
                commandPool.destroyAfterDeviceIdleLocked();
            } catch (Throwable throwable) {
                failure = appendFailure(failure, throwable);
            }
        }
        if (!commandPools.isEmpty()) {
            if (failure == null) {
                failure = new IllegalStateException(
                        "D3D12 command-pool destruction retained an owner without reporting a failure");
            }
            rethrowDestroyFailure(failure);
        }
        defaultCommandPool = null;

        for (D3D12Fence fence : List.copyOf(fences)) {
            try {
                fence.destroyLocked();
            } catch (Throwable throwable) {
                failure = appendFailure(failure, throwable);
            }
        }

        for (D3D12Texture2D texture : List.copyOf(textures)) {
            try {
                texture.destroyLocked();
            } catch (Throwable throwable) {
                failure = appendFailure(failure, throwable);
            }
        }

        for (D3D12Buffer buffer : List.copyOf(buffers)) {
            try {
                buffer.destroyLocked(true);
            } catch (Throwable throwable) {
                failure = appendFailure(failure, throwable);
            }
        }

        if (!fences.isEmpty() || !textures.isEmpty() || !buffers.isEmpty()) {
            if (failure == null) {
                failure = new IllegalStateException(
                        "D3D12 resource destruction retained an owner without reporting a failure");
            }
            rethrowDestroyFailure(failure);
        }

        long handleToDestroy = nativeHandle;
        try {
            D3D12Native.nDestroyDevice(handleToDestroy);
            nativeHandle = 0;
        } catch (Throwable throwable) {
            failure = appendFailure(failure, throwable);
        }
        rethrowDestroyFailure(failure);
    }

    boolean isDestroyed() {
        return withLifecycleLock(() -> nativeHandle == 0);
    }

    private static Throwable appendFailure(Throwable failure, Throwable addition) {
        if (failure == null) {
            return addition;
        }
        if (failure != addition) {
            failure.addSuppressed(addition);
        }
        return failure;
    }

    private static void rethrowDestroyFailure(Throwable failure) {
        if (failure == null) {
            return;
        }
        ThrowableUtil.rethrowError(failure);
        if (failure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new D3D12Exception("Failed to destroy D3D12 device: " + failure);
    }

    @Override
    public ISampler createSampler(SamplerDescription description) {
        ensureOpen();
        throw D3D12Exception.unsupported("samplers");
    }

    @Override
    public ITextureView createTextureView(TextureViewDescription description) {
        ensureOpen();
        throw D3D12Exception.unsupported("texture views");
    }

    @Override
    public IFrameBuffer createFramebuffer(FramebufferDescription description) {
        ensureOpen();
        throw D3D12Exception.unsupported("framebuffers");
    }

    @Override
    public IShaderProgram createShaderProgram(ShaderDescription description) {
        ensureOpen();
        throw D3D12Exception.unsupported("shader programs and DXIL compilation");
    }

    @Override
    public IVertexBuffer createVertexBuffer(VertexBufferDescription description) {
        ensureOpen();
        throw D3D12Exception.unsupported("vertex buffers");
    }

    @Override
    public RenderPass createRenderPass(RenderPass.Builder builder) {
        ensureOpen();
        throw D3D12Exception.unsupported("render passes");
    }

    @Override
    public PipelineDescriptorSet createDescriptorSet(IShaderProgram shader) {
        ensureOpen();
        throw D3D12Exception.unsupported("pipeline descriptor sets");
    }

    @Override
    public ComputePipeline createComputePipeline(ComputePipeline.Builder builder) {
        ensureOpen();
        throw D3D12Exception.unsupported("compute pipelines");
    }

    @Override
    public GraphicsPipeline createGraphicsPipeline(GraphicsPipeline.Builder builder) {
        ensureOpen();
        throw D3D12Exception.unsupported("graphics pipelines");
    }

    private static void validateTextureDescription(
            TextureDescription description,
            D3D12ResourceState initialState) {
        if (description == null) {
            throw new IllegalArgumentException("D3D12 texture description cannot be null");
        }
        if (initialState == null) {
            throw new IllegalArgumentException("D3D12 texture initial state cannot be null");
        }
        if (description.getType() != TextureType.Texture2D) {
            throw D3D12Exception.unsupported("texture type " + description.getType());
        }
        if (description.getWidth() <= 0 || description.getHeight() <= 0 ||
                description.getFormat() == null || description.getUsages() == null ||
                description.getUsages().isEmpty()) {
            throw new IllegalArgumentException("D3D12 Texture2D description is incomplete");
        }
        formatMapping(description.getFormat());
        if (description.getMipmapSettings() == null) {
            throw new IllegalArgumentException("D3D12 Texture2D mipmap settings cannot be null");
        }
        if (description.getMipmapSettings().isAutoGenerate()) {
            throw D3D12Exception.unsupported("automatic mipmap generation");
        }
        int mipLevels = description.getMipmapSettings().resolveLevels(
                description.getWidth(),
                description.getHeight());
        if (mipLevels != 1) {
            throw D3D12Exception.unsupported("multiple Texture2D mip levels");
        }
        int maximumMipLevels = 32 - Integer.numberOfLeadingZeros(
                Math.max(description.getWidth(), description.getHeight()));
        if (mipLevels <= 0 || mipLevels > maximumMipLevels) {
            throw new IllegalArgumentException(
                    "D3D12 Texture2D mip count exceeds its dimensions");
        }
        if (description.getUsages().getUsages().contains(TextureUsage.AttachmentDepth)) {
            throw D3D12Exception.unsupported("depth-stencil textures");
        }
        switch (initialState) {
            case UNORDERED_ACCESS -> requireTextureUsage(
                    description, TextureUsage.Storage, initialState);
            case COPY_SOURCE -> requireTextureUsage(
                    description, TextureUsage.TransferSource, initialState);
            case COPY_DESTINATION -> requireTextureUsage(
                    description, TextureUsage.TransferDestination, initialState);
            case RENDER_TARGET -> {
                if (!description.getUsages().getUsages().contains(TextureUsage.AttachmentColor) &&
                        !description.getUsages().getUsages().contains(TextureUsage.TransferDestination)) {
                    throw new IllegalArgumentException(
                            "D3D12 Texture2D RENDER_TARGET state requires color-attachment or transfer-destination usage");
                }
            }
            case DEPTH_WRITE -> throw D3D12Exception.unsupported("depth-write texture state");
            case PRESENT -> throw D3D12Exception.unsupported("presentation texture state");
            default -> {
            }
        }
    }

    private static void requireTextureUsage(
            TextureDescription description,
            TextureUsage usage,
            D3D12ResourceState state) {
        if (!description.getUsages().getUsages().contains(usage)) {
            throw new IllegalArgumentException(
                    "D3D12 Texture2D state " + state + " requires usage " + usage);
        }
    }

    private static int textureResourceFlags(TextureDescription description) {
        int flags = D3D12Native.RESOURCE_FLAG_NONE;
        if (description.getUsages().getUsages().contains(TextureUsage.Storage)) {
            flags |= D3D12Native.RESOURCE_FLAG_ALLOW_UNORDERED_ACCESS;
        }
        if (description.getUsages().getUsages().contains(TextureUsage.AttachmentColor) ||
                description.getUsages().getUsages().contains(TextureUsage.TransferDestination)) {
            flags |= D3D12Native.RESOURCE_FLAG_ALLOW_RENDER_TARGET;
        }
        return flags;
    }

    private static FormatMapping formatMapping(TextureFormat format) {
        return switch (format) {
            case RGBA8 -> new FormatMapping(
                    SRSurfaceFormat.R8G8B8A8_UNORM,
                    DXGI_FORMAT_R8G8B8A8_UNORM);
            case RGBA16F -> new FormatMapping(
                    SRSurfaceFormat.R16G16B16A16_FLOAT,
                    DXGI_FORMAT_R16G16B16A16_FLOAT);
            case R11G11B10F -> new FormatMapping(
                    SRSurfaceFormat.R11G11B10_FLOAT,
                    DXGI_FORMAT_R11G11B10_FLOAT);
            case R32F -> new FormatMapping(
                    SRSurfaceFormat.R32_FLOAT,
                    DXGI_FORMAT_R32_FLOAT);
            case RG16F -> new FormatMapping(
                    SRSurfaceFormat.R16G16_FLOAT,
                    DXGI_FORMAT_R16G16_FLOAT);
            default -> throw D3D12Exception.unsupported(
                    "Texture2D format " + format);
        };
    }

    private static String textureDebugName(TextureDescription description) {
        String label = description.getLabel();
        if (label != null && !label.isBlank()) {
            return label;
        }
        return "SuperResolution D3D12 Texture2D " + description.getFormat() +
                " " + description.getWidth() + "x" + description.getHeight();
    }

    public static final class ExternalBorrowLease implements AutoCloseable {
        private final D3D12Device owner;
        private boolean closed;

        private ExternalBorrowLease(D3D12Device owner) {
            this.owner = owner;
        }

        @Override
        public void close() {
            owner.withLifecycleLock(() -> {
                if (closed) {
                    return;
                }
                owner.releaseExternalBorrowLocked();
                closed = true;
            });
        }
    }

    private record FormatMapping(SRSurfaceFormat surfaceFormat, int dxgiFormat) {
    }
}
