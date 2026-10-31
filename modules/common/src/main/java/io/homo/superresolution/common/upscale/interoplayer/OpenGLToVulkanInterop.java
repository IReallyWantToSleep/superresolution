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

package io.homo.superresolution.common.upscale.interoplayer;

import io.homo.superresolution.api.InitializationDescription;
import io.homo.superresolution.api.InputResourceSet;
import io.homo.superresolution.api.InputResourceType;
import io.homo.superresolution.api.interop.InteropResourceDescription;
import io.homo.superresolution.api.interop.InteropResourceRequirement;
import io.homo.superresolution.api.interop.InteropResourceType;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.framegeneration.FrameGeneration;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.perf.PerformanceTracker;
import io.homo.superresolution.common.presentation.PresentationBackendManager;
import io.homo.superresolution.common.presentation.capture.FrameCaptureManager;
import io.homo.superresolution.common.presentation.capture.FrameResources;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.InteropResourcesPreprocessor;
import io.homo.superresolution.common.workmode.SRWorkModeManager;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.command.CommandBufferState;
import io.homo.superresolution.core.graphics.impl.command.MemoryBarrierType;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsages;
import io.homo.superresolution.core.graphics.opengl.GlDevice;
import io.homo.superresolution.core.graphics.opengl.texture.GlImportableTexture2D;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.core.graphics.vulkan.*;
import io.homo.superresolution.srapi.SRRenderApiType;

import java.util.*;

import static io.homo.superresolution.api.interop.InteropResourceRequirement.Presence.Required;
import static io.homo.superresolution.api.interop.InteropResourceType.*;
import static org.lwjgl.opengl.EXTSemaphore.*;
import static org.lwjgl.opengl.GL11.glFinish;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_ALL_COMMANDS_BIT;

public final class OpenGLToVulkanInterop implements InteropImplementation<VulkanCommandContext>,
        VulkanInputCaptureInterop {
    public static final int INITIAL_COMMAND_BUFFER_RING_SIZE = 4;
    private final VulkanCommandBufferRing commandBufferRing = new VulkanCommandBufferRing(
            INITIAL_COMMAND_BUFFER_RING_SIZE);
    private FrameResourcesSet frameResourcesSet;
    private InteropDispatchResource<VulkanCommandContext> pendingDispatch;
    private int pendingTimestampSlot = -1;
    private boolean failed;

    private boolean flipInteropResourcesY;
    private InteropResourceLayout builtLayout;
    private boolean destroyed;
    private boolean initialized;
    private List<InteropResourceRequirement> requirements = List.of();
    private InteropResourceAccess resourceAccess;
    // Resolution the interop resources were last built at, to skip redundant resize() rebuilds.
    // (Iris/forceResize call resize() on every pipeline reload even when nothing changed).
    private int builtRenderWidth = -1;
    private int builtRenderHeight = -1;
    private int builtScreenWidth = -1;
    private int builtScreenHeight = -1;

    public OpenGLToVulkanInterop() {
    }

    private InteropResourceLayout resolveLayout(InputResourceSet inputs) {
        return InteropResourceLayout.resolve(requirements,
                InteropResourceContextManager.getCurrentContext(), inputs,
                SuperResolutionConfig.getInternalTextureFormat(),
                RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight(),
                RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight());
    }

    private InteropResourceLayout resolveLayout(DispatchResource resource) {
        return InteropResourceLayout.resolve(requirements,
                InteropResourceContextManager.getCurrentContext(), resource.resources(),
                SuperResolutionConfig.getInternalTextureFormat(),
                resource.renderWidth(), resource.renderHeight(),
                resource.screenWidth(), resource.screenHeight());
    }

    private void createResources(InteropResourceLayout layout) {
        VulkanDevice vkDevice = RenderSystems.vulkan().device();
        vkDevice.getMainQueue().waitIdle();
        frameResourcesSet = new FrameResourcesSet();
        frameResourcesSet.initialize(layout);
        builtLayout = layout;
        builtRenderWidth = layout.resources().get(Color).width();
        builtRenderHeight = layout.resources().get(Color).height();
        builtScreenWidth = layout.resources().get(OutputColor).width();
        builtScreenHeight = layout.resources().get(OutputColor).height();
    }

    private void destroyResources() {
        RenderSystems.vulkan().device().getMainQueue().waitIdle();
        if (frameResourcesSet != null) {
            frameResourcesSet.destroy();
            frameResourcesSet = null;
        }
        builtLayout = null;
    }

    @Override
    public SRRenderApiType inputApi() {
        return SRRenderApiType.OPENGL;
    }

    @Override
    public SRRenderApiType executionApi() {
        return SRRenderApiType.VULKAN;
    }

    @Override
    public void initialize(
            InitializationDescription desc,
            List<InteropResourceRequirement> requirements) {
        if (initialized || frameResourcesSet != null) {
            throw new IllegalStateException("Interop algorithm still owns resources");
        }
        this.requirements = List.copyOf(requirements);
        flipInteropResourcesY = SuperResolutionConfig.isFlipVkGlInteropResourcesY();
        destroyed = false;
        createResources(resolveLayout((InputResourceSet) null));
        refreshResourceAccess();
        initialized = true;
    }

    @Override
    public InteropResourceAccess resourceAccess() {
        return Objects.requireNonNull(resourceAccess, "Interop resources are not initialized");
    }

    @Override
    public void executeCommands(java.util.function.Consumer<VulkanCommandContext> recorder) {
        VulkanDevice device = RenderSystems.vulkan().device();
        VulkanCommandBuffer commands = device.createCommandBuffer();
        try (VulkanCommandContext context = new VulkanCommandContext(commands)) {
            commands.begin();
            recorder.accept(context);
            context.close();
            commands.end();
            device.submitCommandBuffer(commands);
            commands.waitForFence();
        } finally {
            commands.destroy();
        }
    }

    @Override
    public boolean validateInputResources(DispatchResource resource) {
        return resource != null
                && InteropResourceLayout.hasRequiredInputs(requirements, resource.resources());
    }

    @Override
    public Map<InteropResourceType, InteropResourceDescription> resourceDescriptions() {
        return builtLayout == null ? Map.of() : builtLayout.resources();
    }

    @Override
    public boolean resourcesNeedRebuild(DispatchResource resource) {
        return failed || !resolveLayout(resource).equals(builtLayout);
    }

    @Override
    public void rebuildForDispatch(DispatchResource resource) {
        rebuildResources(resolveLayout(resource));
    }

    @Override
    public InteropDispatchResource<VulkanCommandContext> beginDispatch(
            DispatchResource dispatchResource) {
        if (!initialized || destroyed) {
            throw new IllegalStateException("Interop implementation is not initialized");
        }
        Objects.requireNonNull(dispatchResource, "dispatchResource");
        if (pendingDispatch != null || failed) {
            throw new IllegalStateException("A previous Vulkan dispatch is pending or unrecoverable");
        }
        InteropResourceLayout layout = resolveLayout(dispatchResource);
        if (!layout.equals(builtLayout)) {
            throw new IllegalStateException("Interop resources require rebuild before dispatch");
        }
        InteropFrameData frame = InteropFrameData.from(dispatchResource);
        processInputResources(frameResourcesSet, dispatchResource);
        VulkanDevice vulkanDevice = RenderSystems.vulkan().device();
        VulkanCommandBuffer commandBuffer = commandBufferRing.acquire(vulkanDevice);
        commandBuffer.begin();
        EnumMap<InteropResourceType, ITexture> textures = new EnumMap<>(InteropResourceType.class);
        for (InteropResourceType type : frameResourcesSet.resourceTypes()) {
            textures.put(type, frameResourcesSet.vulkan(type));
        }
        InteropDispatchResource<VulkanCommandContext> prepared = new InteropDispatchResource<>(
                frame,
                new InteropResourceSet(textures),
                new VulkanCommandContext(commandBuffer));
        pendingDispatch = prepared;
        VulkanTimestampProfiler profiler = vulkanDevice.timestampProfiler();
        pendingTimestampSlot = profiler == null ? -1
                : profiler.beginRegion(commandBuffer.getNativeCommandBuffer(), PerformanceTracker.VK_UPSCALE);
        try {
            signalInputTexturesReady(frameResourcesSet);
        } catch (RuntimeException | Error failure) {
            failed = true;
            pendingDispatch = null;
            prepared.commandContext().close();
            if (profiler != null) {
                profiler.cancelRegion(pendingTimestampSlot);
            }
            pendingTimestampSlot = -1;
            commandBuffer.reset();
            throw failure;
        }
        try {
            publishCaptureInputs(frameResourcesSet, dispatchResource);
        } catch (RuntimeException | Error failure) {
            try {
                completeDispatch(prepared, false);
            } catch (RuntimeException | Error recoveryFailure) {
                failure.addSuppressed(recoveryFailure);
            }
            throw failure;
        }
        return prepared;
    }

    @Override
    public void completeDispatch(
            InteropDispatchResource<VulkanCommandContext> resource,
            boolean success) {
        if (pendingDispatch != resource) {
            throw new IllegalStateException("Vulkan dispatch context mismatch");
        }
        VulkanCommandBuffer commandBuffer = resource.commandContext().commandBuffer();
        VulkanTimestampProfiler profiler = RenderSystems.vulkan().device().timestampProfiler();
        try {
            resource.commandContext().close();
            if (!success) {
                commandBuffer.reset();
                commandBuffer.begin();
                if (profiler != null) {
                    profiler.cancelRegion(pendingTimestampSlot);
                }
            } else if (profiler != null && pendingTimestampSlot >= 0) {
                profiler.endRegion(commandBuffer.getNativeCommandBuffer(), pendingTimestampSlot);
            }
            if (commandBuffer.state() == CommandBufferState.Recording) {
                commandBuffer.end();
            }
            VulkanDevice vulkanDevice = RenderSystems.vulkan().device();
            frameResourcesSet.fence = vulkanDevice.submitCommandBuffer(
                    commandBuffer,
                    new long[]{frameResourcesSet.glFinish.getVkSemaphoreHandle()},
                    new int[]{VK_PIPELINE_STAGE_ALL_COMMANDS_BIT},
                    new long[]{frameResourcesSet.upscaleVkFinish.getVkSemaphoreHandle()});
            frameResourcesSet.commandBuffer = commandBuffer;
            int[] handles = sharedTextureHandles(frameResourcesSet);
            frameResourcesSet.upscaleVkFinish.waitVulkanSignal(
                    handles, new int[0], sharedTextureLayouts(frameResourcesSet));
            if (success) {
                publishOutput(frameResourcesSet);
            }
        } catch (RuntimeException | Error failure) {
            failed = true;
            if (profiler != null) {
                profiler.cancelRegion(pendingTimestampSlot);
            }
            throw failure;
        } finally {
            pendingTimestampSlot = -1;
            pendingDispatch = null;
        }
    }

    @Override
    public ITexture outputTexture() {
        return frameResourcesSet == null ? null : frameResourcesSet.sourceOutputGlTexture;
    }

    @Override
    public void resize(int width, int height) {
        if (!initialized || destroyed) {
            throw new IllegalStateException("Interop implementation is not initialized");
        }
        if (!failed && RenderHandlerManager.getRenderWidth() == builtRenderWidth
                && RenderHandlerManager.getRenderHeight() == builtRenderHeight
                && width == builtScreenWidth && height == builtScreenHeight) {
            return;
        }
        rebuildResources(InteropResourceLayout.resolve(requirements,
                InteropResourceContextManager.getCurrentContext(), null,
                SuperResolutionConfig.getInternalTextureFormat(),
                RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight(), width, height));
    }

    @Override
    public void awaitUsers() {
        PresentationBackendManager.flushCapturedFrame();
        if (frameResourcesSet != null) {
            frameResourcesSet.awaitCaptureRelease();
        }
        glFinish();
        RenderSystems.vulkan().device().getMainQueue().waitIdle();
    }

    @Override
    public void destroy() {
        if (destroyed) {
            return;
        }
        awaitUsers();
        commandBufferRing.destroy();
        destroyResources();
        resourceAccess = null;
        destroyed = true;
        initialized = false;
    }

    private void refreshResourceAccess() {
        EnumMap<InteropResourceType, ITexture> textures = new EnumMap<>(InteropResourceType.class);
        for (InteropResourceType type : frameResourcesSet.resourceTypes()) {
            textures.put(type, frameResourcesSet.vulkan(type));
        }
        resourceAccess = new InteropResourceAccess(
                inputApi(),
                executionApi(),
                textures,
                resourceDescriptions(),
                outputTexture(),
                RenderSystems.vulkan().device());
    }

    private void publishOutput(FrameResourcesSet inFlight) {
        PerformanceTracker.push(PerformanceTracker.GL_INTEROP_FLIP);
        GlTexture2D destination = inFlight.unpublishedSourceOutput;
        ICommandBuffer copy = RenderSystems.opengl().device().defaultCommandPool().createCommandBuffer();
        try {
            copy.begin();
            if (flipInteropResourcesY) {
                InteropResourcesPreprocessor.flipY(
                        copy, inFlight.openGl(OutputColor), destination);
            } else {
                InteropResourcesPreprocessor.copyTexture(copy, inFlight.openGl(OutputColor), destination);
            }
            copy.memoryBarrier(MemoryBarrierType.TEXTURE_FETCH, MemoryBarrierType.STORAGE_IMAGE_WRITE);
            copy.end();
            RenderSystems.opengl().device().submitCommandBuffer(copy);
            GlTexture2D previous = inFlight.sourceOutputGlTexture;
            inFlight.sourceOutputGlTexture = destination;
            inFlight.unpublishedSourceOutput = previous;
        } finally {
            copy.destroy();
            PerformanceTracker.pop(PerformanceTracker.GL_INTEROP_FLIP);
        }
    }

    private void rebuildResources(InteropResourceLayout layout) {
        awaitUsers();
        commandBufferRing.destroy();
        destroyResources();
        try {
            createResources(layout);
            refreshResourceAccess();
            failed = false;
        } catch (RuntimeException | Error error) {
            builtLayout = null;
            throw error;
        }
        FrameGeneration.invalidateHistory();
    }

    private void processInputResources(FrameResourcesSet inFlight, DispatchResource dispatchResource) {
        inFlight.awaitCaptureRelease();
        String motionVectorPreprocessingFunction =
                SRWorkModeManager.getCurrentState().motionVectorPreprocessingFunction();
        PerformanceTracker.push(PerformanceTracker.GL_INPUT_CONVERT);
        try {
            InputResourceSet inputs = dispatchResource.resources();
            InteropResourcesPreprocessor.processInputTextures(
                    inputs.get(InputResourceType.Color), inFlight.openGl(Color),
                    inputs.get(InputResourceType.Depth), inFlight.openGl(Depth),
                    inputs.get(InputResourceType.MotionVectors), inFlight.openGl(MotionVectors),
                    inputs.get(InputResourceType.Exposure), inFlight.openGl(Exposure),
                    motionVectorPreprocessingFunction,
                    flipInteropResourcesY
            );
            ICommandBuffer supplemental = null;
            try {
                for (InteropResourceType type : inFlight.resourceTypes()) {
                    if (!type.isInput() || type == Color || type == Depth
                            || type == MotionVectors || type == Exposure) {
                        continue;
                    }
                    ITexture source = inputs.get(type.inputType());
                    if (source != null) {
                        if (supplemental == null) {
                            supplemental = RenderSystems.opengl().device().defaultCommandPool().createCommandBuffer();
                            supplemental.begin();
                        }
                        recordSupplementalTransfer(supplemental, type, source, inFlight.openGl(type));
                    }
                }
                if (supplemental != null) {
                    supplemental.end();
                    RenderSystems.opengl().device().submitCommandBuffer(supplemental);
                }
            } finally {
                if (supplemental != null) {
                    supplemental.destroy();
                }
            }
        } finally {
            PerformanceTracker.pop(PerformanceTracker.GL_INPUT_CONVERT);
        }
    }

    private void signalInputTexturesReady(FrameResourcesSet inFlight) {
        inFlight.glFinish.signalVulkan(
                sharedTextureHandles(inFlight), new int[0], sharedTextureLayouts(inFlight));
    }

    private static int[] sharedTextureHandles(FrameResourcesSet resources) {
        return resources.resourceTypes().stream()
                .mapToInt(type -> Math.toIntExact(resources.openGl(type).handle())).toArray();
    }

    private static int[] sharedTextureLayouts(FrameResourcesSet resources) {
        return resources.resourceTypes().stream()
                .mapToInt(type -> type.isInput() ? GL_LAYOUT_SHADER_READ_ONLY_EXT : GL_LAYOUT_GENERAL_EXT)
                .toArray();
    }

    private void publishCaptureInputs(
            FrameResourcesSet inFlight,
            DispatchResource dispatchResource
    ) {
        if (!PresentationBackendManager.isVulkanPresentationRequested()
                || !FrameCaptureManager.isInitialized()
                // Real-only presentation does not consume these borrowed textures.
                || !FrameGeneration.isFrameGenerationEnabled()
                || dispatchResource.resources() == null) {
            return;
        }

        boolean hasDepth = inFlight.has(Depth) && dispatchResource.resources().has(InputResourceType.Depth);
        boolean hasMotionVectors = inFlight.has(MotionVectors)
                && dispatchResource.resources().has(InputResourceType.MotionVectors);
        if (!hasDepth && !hasMotionVectors) {
            return;
        }

        FrameResources captureFrame = FrameCaptureManager.captureVulkanInputs(
                dispatchResource.frameCount(),
                hasDepth ? inFlight.vulkan(Depth) : null,
                hasDepth ? inFlight.openGl(Depth) : null,
                hasDepth ? inFlight.captureDepthReady : null,
                hasDepth ? inFlight.captureDepthRelease : null,
                hasMotionVectors ? inFlight.vulkan(MotionVectors) : null,
                hasMotionVectors ? inFlight.openGl(MotionVectors) : null,
                hasMotionVectors ? inFlight.captureMotionReady : null,
                hasMotionVectors ? inFlight.captureMotionRelease : null
        );
        if (captureFrame == null) {
            return;
        }

        boolean borrowedDepth = hasDepth && captureFrame.hasDepth();
        boolean borrowedMotionVectors = hasMotionVectors && captureFrame.hasMotionVector();
        if (borrowedDepth || borrowedMotionVectors) {
            inFlight.captureInputsFrame = captureFrame;
        }
        if (borrowedDepth) {
            inFlight.captureDepthReady.signalVulkan(
                    new int[]{Math.toIntExact(inFlight.openGl(Depth).handle())},
                    new int[0],
                    new int[]{GL_LAYOUT_SHADER_READ_ONLY_EXT}
            );
            inFlight.captureDepthPending = true;
        }
        if (borrowedMotionVectors) {
            inFlight.captureMotionReady.signalVulkan(
                    new int[]{Math.toIntExact(inFlight.openGl(MotionVectors).handle())},
                    new int[0],
                    new int[]{GL_LAYOUT_SHADER_READ_ONLY_EXT}
            );
            inFlight.captureMotionPending = true;
        }

    }

    private void recordSupplementalTransfer(ICommandBuffer commandBuffer, InteropResourceType type,
                                            ITexture source, ITexture destination) {
        if (!flipInteropResourcesY) {
            InteropResourcesPreprocessor.copyTexture(commandBuffer, source, destination);
        } else if (type == SpecularMotionVectors) {
            InteropResourcesPreprocessor.flipMotionVectorY(commandBuffer, source, destination);
        } else {
            InteropResourcesPreprocessor.flipY(commandBuffer, source, destination);
        }
    }

    private static final class FrameResourcesSet {
        private final EnumMap<InteropResourceType, VulkanTexture> vulkanTextures =
                new EnumMap<>(InteropResourceType.class);
        private final EnumMap<InteropResourceType, GlImportableTexture2D> glTextures =
                new EnumMap<>(InteropResourceType.class);

        private GlTexture2D sourceOutputGlTexture;
        private GlTexture2D unpublishedSourceOutput;

        public VkGlInteropSemaphore glFinish;
        public VkGlInteropSemaphore upscaleVkFinish;
        public VkGlInteropSemaphore captureDepthReady;
        public VkGlInteropSemaphore captureDepthRelease;
        public VkGlInteropSemaphore captureMotionReady;
        public VkGlInteropSemaphore captureMotionRelease;
        public VulkanCommandBuffer commandBuffer;
        public long fence;

        private boolean captureDepthPending;
        private boolean captureMotionPending;
        private FrameResources captureInputsFrame;

        public boolean has(InteropResourceType type) {
            return vulkanTextures.containsKey(type);
        }

        public VulkanTexture vulkan(InteropResourceType type) {
            return vulkanTextures.get(type);
        }

        public GlImportableTexture2D openGl(InteropResourceType type) {
            return glTextures.get(type);
        }

        public Set<InteropResourceType> resourceTypes() {
            return Collections.unmodifiableSet(vulkanTextures.keySet());
        }

        public void destroy() {
            awaitCaptureRelease();
            if (sourceOutputGlTexture != null) {
                sourceOutputGlTexture.destroy();
                sourceOutputGlTexture = null;
            }
            if (unpublishedSourceOutput != null) {
                unpublishedSourceOutput.destroy();
                unpublishedSourceOutput = null;
            }
            for (Iterator<GlImportableTexture2D> it = glTextures.values().iterator(); it.hasNext(); ) {
                it.next().destroy();
                it.remove();
            }
            for (Iterator<VulkanTexture> it = vulkanTextures.values().iterator(); it.hasNext(); ) {
                it.next().destroy();
                it.remove();
            }

            if (glFinish != null) {
                glFinish.destroy();
                glFinish = null;
            }

            if (upscaleVkFinish != null) {
                upscaleVkFinish.destroy();
                upscaleVkFinish = null;
            }
            if (captureDepthReady != null) {
                captureDepthReady.destroy();
                captureDepthReady = null;
            }
            if (captureDepthRelease != null) {
                captureDepthRelease.destroy();
                captureDepthRelease = null;
            }
            if (captureMotionReady != null) {
                captureMotionReady.destroy();
                captureMotionReady = null;
            }
            if (captureMotionRelease != null) {
                captureMotionRelease.destroy();
                captureMotionRelease = null;
            }
        }

        private void initialize(InteropResourceLayout layout) {
            VulkanDevice vkDevice = RenderSystems.vulkan().device();
            GlDevice glDevice = RenderSystems.opengl().device();
            vkDevice.getMainQueue().waitIdle();
            for (Map.Entry<InteropResourceType, InteropResourceDescription> entry : layout.resources().entrySet()) {
                InteropResourceDescription description = entry.getValue();
                TextureUsages usages = TextureUsages.create();
                usages.getUsages().addAll(description.usages());
                VulkanTexture texture = vkDevice.createTextureExportable(
                        TextureDescription.create()
                                .type(TextureType.Texture2D)
                                .usages(usages)
                                .format(description.format())
                                .width(description.width())
                                .height(description.height())
                                .label("SRInterop-%s".formatted(entry.getKey()))
                                .build());
                vulkanTextures.put(entry.getKey(), texture);
                glTextures.put(entry.getKey(), glDevice.createTextureImportable(texture));
            }

            InteropResourceDescription output = layout.resources().get(OutputColor);
            this.sourceOutputGlTexture = (GlTexture2D) glDevice.createTexture(
                        TextureDescription.create()
                                .type(TextureType.Texture2D)
                                .usages(TextureUsages.create().sampler().storage().transferDestination())
                                .format(output.format())
                                .width(output.width())
                                .height(output.height())
                                .label("SRUpscaleSourceOutputGlTexture")
                                .build()
                );
            this.unpublishedSourceOutput = (GlTexture2D) glDevice.createTexture(
                    TextureDescription.create()
                            .type(TextureType.Texture2D)
                            .usages(TextureUsages.create().sampler().storage().transferDestination())
                            .format(output.format())
                            .width(output.width())
                            .height(output.height())
                            .label("SRUpscaleUnpublishedOutputGlTexture")
                            .build());

            this.glFinish = VkGlInteropSemaphore.create(vkDevice);
            this.upscaleVkFinish = VkGlInteropSemaphore.create(vkDevice);
            if (PresentationBackendManager.isVulkanPresentationRequested()) {
                this.captureDepthReady = VkGlInteropSemaphore.create(vkDevice);
                this.captureDepthRelease = VkGlInteropSemaphore.create(vkDevice);
                this.captureMotionReady = VkGlInteropSemaphore.create(vkDevice);
                this.captureMotionRelease = VkGlInteropSemaphore.create(vkDevice);
            }
        }

        private void awaitCaptureRelease() {
            if ((captureDepthPending || captureMotionPending) && captureInputsFrame == null) {
                throw new IllegalStateException(
                        "Borrowed interop inputs are pending without a capture frame"
                );
            }
            if (captureInputsFrame != null
                    && (captureDepthPending || captureMotionPending)) {
                captureInputsFrame.awaitBorrowedInputReleaseSubmission();
            }
            if (captureDepthPending && captureDepthRelease != null && openGl(Depth) != null) {
                captureDepthRelease.waitVulkanSignal(
                        new int[]{Math.toIntExact(openGl(Depth).handle())},
                        new int[0],
                        new int[]{GL_LAYOUT_TRANSFER_DST_EXT}
                );
                captureDepthPending = false;
            }
            if (captureMotionPending && captureMotionRelease != null && openGl(MotionVectors) != null) {
                captureMotionRelease.waitVulkanSignal(
                        new int[]{Math.toIntExact(openGl(MotionVectors).handle())},
                        new int[0],
                        new int[]{GL_LAYOUT_TRANSFER_DST_EXT}
                );
                captureMotionPending = false;
            }
            if (!captureDepthPending && !captureMotionPending) {
                captureInputsFrame = null;
            }
        }
    }
}
