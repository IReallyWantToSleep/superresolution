package io.homo.superresolution.common.upscale.interoplayer;

import io.homo.superresolution.api.interop.InteropResourceType;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.vulkan.VulkanCommandBuffer;
import io.homo.superresolution.srapi.SRRenderApiType;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static org.lwjgl.vulkan.VK10.*;

public final class VulkanToVulkanInterop extends VulkanInputInterop<VulkanCommandContext> {
    private PendingDispatch pending;

    @Override
    public SRRenderApiType executionApi() {
        return SRRenderApiType.VULKAN;
    }

    @Override
    public InteropDispatchResource<VulkanCommandContext> beginDispatch(DispatchResource resource) {
        requireReady();
        if (pending != null || !resolveLayout(resource).equals(builtLayout)) {
            throw new IllegalStateException("Vulkan dispatch is pending or requires resource rebuild");
        }
        VulkanCommandBuffer commands = getCurrentCommandBuffer();
        VulkanInteropState states = new VulkanInteropState();
        VulkanCommandContext context = new VulkanCommandContext(commands);
        try {
            rememberExecution(states);
            prepareInputs(commands, states, resource.resources());
            for (Map.Entry<InteropResourceType, ITexture> entry : vulkanTextures.entrySet()) {
                states.transition(commands, entry.getValue(),
                        entry.getKey().isInput() ? VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL : VK_IMAGE_LAYOUT_GENERAL,
                        entry.getKey().isInput() ? VK_ACCESS_SHADER_READ_BIT : VK_ACCESS_SHADER_READ_BIT | VK_ACCESS_SHADER_WRITE_BIT,
                        VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT);
            }
            InteropDispatchResource<VulkanCommandContext> prepared = new InteropDispatchResource<>(
                    InteropFrameData.from(resource), new InteropResourceSet(executionTextures), context);
            pending = new PendingDispatch(prepared, commands, states);
            return prepared;
        } catch (RuntimeException | Error failure) {
            context.close();
            throw failure;
        }
    }

    @Override
    public void completeDispatch(InteropDispatchResource<VulkanCommandContext> resource, boolean success) {
        PendingDispatch recording = pending;
        if (recording == null || recording.resource() != resource) {
            throw new IllegalStateException("Vulkan dispatch context mismatch");
        }
        pending = null;
        try {
            resource.commandContext().close();
            if (!success) {
                return;
            }
            restoreGeneral(recording.commands(), recording.states());
            int output = recordOutput(recording.commands(), recording.states());
            restoreGeneral(recording.commands(), recording.states());
            recording.commands().end();
            publishOutput(output);
        } catch (RuntimeException | Error failure) {
            throw failure;
        }
    }

    @Override
    public void executeCommands(Consumer<VulkanCommandContext> recorder) {
        Objects.requireNonNull(recorder, "recorder");
        if (vulkanDevice == null || unusable || pending != null) {
            throw new IllegalStateException("Vulkan setup recording is unavailable");
        }
        VulkanCommandBuffer commands = getCurrentCommandBuffer();
        VulkanInteropState states = new VulkanInteropState();
        VulkanCommandContext context = new VulkanCommandContext(commands);
        try {
            rememberExecution(states);
            recorder.accept(context);
            context.close();
            restoreGeneral(commands, states);
        } catch (RuntimeException | Error failure) {
            throw failure;
        } finally {
            context.close();
        }
    }

    @Override
    protected void initializeExecution() {
    }

    @Override
    protected ITexture allocateExecution(InteropResourceType type, TextureDescription description) {
        ITexture texture = vulkanDevice.createTexture(description);
        vulkanTextures.put(type, texture);
        return texture;
    }

    @Override
    protected Object executionDevice() {
        return vulkanDevice;
    }

    @Override
    protected void destroyExecution() {
        for (InteropResourceType type : java.util.List.copyOf(executionTextures.keySet())) {
            executionTextures.get(type).destroy();
            executionTextures.remove(type);
            vulkanTextures.remove(type);
        }
    }

    @Override
    protected boolean hasPendingDispatch() {
        return pending != null;
    }

    private void restoreGeneral(VulkanCommandBuffer commands, VulkanInteropState states) {
        for (ITexture texture : vulkanTextures.values()) {
            states.transition(commands, texture, VK_IMAGE_LAYOUT_GENERAL,
                    VK_ACCESS_MEMORY_READ_BIT | VK_ACCESS_MEMORY_WRITE_BIT,
                    VK_PIPELINE_STAGE_ALL_COMMANDS_BIT);
        }
    }

    private record PendingDispatch(InteropDispatchResource<VulkanCommandContext> resource,

                                   VulkanCommandBuffer commands,

                                   VulkanInteropState states) {
    }
}
