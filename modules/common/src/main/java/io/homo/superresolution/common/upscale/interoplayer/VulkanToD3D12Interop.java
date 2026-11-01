package io.homo.superresolution.common.upscale.interoplayer;

import io.homo.superresolution.api.interop.InteropResourceType;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.NativeLibManager;
import io.homo.superresolution.core.graphics.d3d12.*;
import io.homo.superresolution.core.graphics.impl.command.CommandBufferBehavior;
import io.homo.superresolution.core.graphics.impl.command.CommandPoolFlags;
import io.homo.superresolution.core.graphics.vulkan.VulkanCommandBuffer;
import io.homo.superresolution.srapi.SRRenderApiType;

import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static org.lwjgl.vulkan.VK11.*;

public final class VulkanToD3D12Interop extends VulkanInputInterop<D3D12CommandContext> {
    private D3D12Device device;
    private D3D12Device.ExternalBorrowLease deviceBorrow;
    private D3D12Fence fence;
    private VulkanD3D12Fence timeline;
    private D3D12CommandPool commandPool;
    private D3D12CommandBuffer commands;
    private PendingDispatch pending;
    private boolean externalOwned;

    @Override
    public SRRenderApiType executionApi() {
        return SRRenderApiType.D3D12;
    }

    @Override
    protected void initializeExecution() {
        if (!NativeLibManager.d3d12Available()) {
            throw new IllegalStateException("The optional D3D12 native library is unavailable");
        }
        device = new D3D12Device(VulkanD3D12Fence.adapterLuid(vulkanDevice));
        deviceBorrow = device.borrowExternal();
        fence = device.createFence(0);
        timeline = new VulkanD3D12Fence(vulkanDevice, fence);
        commandPool = device.createCommandPool(CommandPoolFlags.Reset);
        commands = commandPool.createCommandBuffer(CommandBufferBehavior.ReusableSequential);
        externalOwned = false;
    }

    @Override
    protected ITexture allocateExecution(InteropResourceType type, TextureDescription description) {
        D3D12Texture2D texture = device.createSharedTexture2D(description, D3D12ResourceState.COMMON);
        // Retain native ownership before the fallible Vulkan import.
        executionTextures.put(type, texture);
        vulkanTextures.put(type, new VulkanD3D12Texture(vulkanDevice, texture));
        return texture;
    }

    @Override
    protected Object executionDevice() {
        return device;
    }

    @Override
    protected boolean hasPendingDispatch() {
        return pending != null;
    }

    @Override
    protected void awaitExecution() {
        if (device != null) {
            device.waitIdle();
        }
    }

    @Override
    protected void destroyExecution() {
        for (InteropResourceType type : List.copyOf(vulkanTextures.keySet())) {
            vulkanTextures.get(type).destroy();
            vulkanTextures.remove(type);
        }
        for (InteropResourceType type : List.copyOf(executionTextures.keySet())) {
            executionTextures.get(type).destroy();
            executionTextures.remove(type);
        }
        if (commands != null) {
            commands.destroy();
            commands = null;
        }
        if (commandPool != null) {
            commandPool.destroy();
            commandPool = null;
        }
        if (timeline != null) {
            timeline.close();
            timeline = null;
        }
        if (fence != null) {
            fence.destroy();
            fence = null;
        }
        if (deviceBorrow != null) {
            deviceBorrow.close();
            deviceBorrow = null;
        }
        if (device != null) {
            device.destroy();
            device = null;
        }
    }

    @Override
    public InteropDispatchResource<D3D12CommandContext> beginDispatch(DispatchResource resource) {
        requireReady();
        if (pending != null || !resolveLayout(resource).equals(builtLayout)) {
            throw new IllegalStateException("D3D12 dispatch is pending or requires resource rebuild");
        }
        long ready = fence.reserveValue();
        long done = fence.reserveValue();
        VulkanCommandBuffer inputCopy = getCurrentCommandBuffer();
        VulkanInteropState states = new VulkanInteropState();
        boolean submissionAttempted = false;
        boolean released = false;
        D3D12CommandContext context = null;
        try {
            rememberExecution(states);
            acquireShared(inputCopy, states);
            prepareInputs(inputCopy, states, resource.resources());
            releaseShared(inputCopy, states);
            submissionAttempted = true;
            externalOwned = true;
            timeline.signal(ready);
            released = true;
            commands.waitForFence();
            commands.reset();
            commands.begin();
            for (Map.Entry<InteropResourceType, ITexture> entry : executionTextures.entrySet()) {
                device.rawCommandDecoder().transitionTexture(commands, (D3D12Texture2D) entry.getValue(),
                        entry.getKey().isInput() ? D3D12ResourceState.COMPUTE_READ : D3D12ResourceState.COMMON);
            }
            context = new D3D12CommandContext(commands);
            InteropDispatchResource<D3D12CommandContext> prepared = new InteropDispatchResource<>(
                    InteropFrameData.from(resource), new InteropResourceSet(executionTextures), context);
            pending = new PendingDispatch(prepared, ready, done);
            return prepared;
        } catch (RuntimeException | Error failure) {
            if (context != null) {
                context.close();
            }
            if (released) {
                recoverUnsubmitted(ready, done, failure);
            } else if (submissionAttempted) {
                unusable = true;
            }
            throw failure;
        }
    }

    @Override
    public void completeDispatch(InteropDispatchResource<D3D12CommandContext> resource, boolean success) {
        PendingDispatch recording = pending;
        if (recording == null || recording.resource() != resource) {
            throw new IllegalStateException("D3D12 dispatch context mismatch");
        }
        pending = null;
        if (!success) {
            try {
                resource.commandContext().close();
            } catch (RuntimeException | Error failure) {
                recoverUnsubmitted(recording.ready(), recording.done(), failure);
                throw failure;
            }
            recoverUnsubmitted(recording.ready(), recording.done(), null);
            return;
        }
        boolean submissionAttempted = false;
        try {
            D3D12CommandBuffer.NativeCommandListLease lease = resource.commandContext().leaseNativeCommandList();
            for (Map.Entry<InteropResourceType, ITexture> entry : executionTextures.entrySet()) {
                D3D12Texture2D texture = (D3D12Texture2D) entry.getValue();
                // Provider contract: read-only inputs and COMMON output on return.
                lease.setTextureState(texture, entry.getKey().isInput()
                        ? D3D12ResourceState.COMPUTE_READ : D3D12ResourceState.COMMON);
                lease.transitionTexture(texture, D3D12ResourceState.COMMON);
            }
            resource.commandContext().close();
            commands.end();
            submissionAttempted = true;
            device.directQueue().submit(commands, fence, recording.ready(), recording.done());
            timeline.await(recording.done());
            copyOutput();
        } catch (RuntimeException | Error failure) {
            try {
                resource.commandContext().close();
            } catch (RuntimeException | Error cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            if (!submissionAttempted) {
                recoverUnsubmitted(recording.ready(), recording.done(), failure);
            } else {
                // Native submission may have executed without a recoverable completion.
                unusable = true;
            }
            throw failure;
        }
    }

    private void acquireShared(VulkanCommandBuffer commands, VulkanInteropState states) {
        for (ITexture texture : vulkanTextures.values()) {
            states.transition(commands, texture, VK_IMAGE_LAYOUT_GENERAL,
                    VK_ACCESS_MEMORY_READ_BIT | VK_ACCESS_MEMORY_WRITE_BIT, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT,
                    externalOwned ? VK_QUEUE_FAMILY_EXTERNAL : VK_QUEUE_FAMILY_IGNORED,
                    externalOwned ? vulkanDevice.getMainQueue().getQueueFamilyIndex() : VK_QUEUE_FAMILY_IGNORED);
        }
    }

    private void releaseShared(VulkanCommandBuffer commands, VulkanInteropState states) {
        for (ITexture texture : vulkanTextures.values()) {
            states.transition(commands, texture, VK_IMAGE_LAYOUT_GENERAL, 0, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                    vulkanDevice.getMainQueue().getQueueFamilyIndex(), VK_QUEUE_FAMILY_EXTERNAL);
        }
    }

    private void copyOutput() {
        VulkanCommandBuffer copy = getCurrentCommandBuffer();
        VulkanInteropState states = new VulkanInteropState();
        try {
            rememberExecution(states);
            acquireShared(copy, states);
            int output = recordOutput(copy, states);
            releaseShared(copy, states);
            publishOutput(output);
        } catch (RuntimeException | Error failure) {
            throw failure;
        }
    }

    private void recoverUnsubmitted(long ready, long done, Throwable original) {
        try {
            commands.reset();
            device.directQueue().recoverSharedFence(fence, ready, done);
            timeline.await(done);
        } catch (RuntimeException | Error recoveryFailure) {
            unusable = true;
            if (original != null) {
                original.addSuppressed(recoveryFailure);
            } else {
                throw recoveryFailure;
            }
        }
    }

    @Override
    public void executeCommands(Consumer<D3D12CommandContext> recorder) {
        Objects.requireNonNull(recorder, "recorder");
        if (device == null || unusable || pending != null) {
            throw new IllegalStateException("D3D12 setup recording is unavailable");
        }
        D3D12CommandBuffer setup = commandPool.createCommandBuffer(CommandBufferBehavior.ReusableSequential);
        D3D12CommandContext context = new D3D12CommandContext(setup);
        boolean submissionAttempted = false;
        try {
            setup.begin();
            recorder.accept(context);
            context.close();
            setup.end();
            submissionAttempted = true;
            device.directQueue().submit(setup);
            setup.waitForFence();
        } catch (RuntimeException | Error failure) {
            if (submissionAttempted) {
                unusable = true;
                device.waitIdle();
            }
            throw failure;
        } finally {
            context.close();
            setup.destroy();
        }
    }

    private record PendingDispatch(InteropDispatchResource<D3D12CommandContext> resource,
                                   long ready, long done) {
    }
}
