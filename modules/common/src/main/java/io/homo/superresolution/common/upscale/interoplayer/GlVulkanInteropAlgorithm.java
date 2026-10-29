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

import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.api.InitializationDescription;
import io.homo.superresolution.api.InputResourceSet;
import io.homo.superresolution.api.InputResourceType;
import io.homo.superresolution.api.interop.*;
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
import io.homo.superresolution.core.graphics.impl.framebuffer.FramebufferDescription;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.GlDevice;
import io.homo.superresolution.core.graphics.opengl.texture.GlImportableTexture2D;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.core.graphics.vulkan.*;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.util.*;
import java.util.function.Consumer;

import static io.homo.superresolution.api.interop.InteropResourceRequirement.FormatSource.*;
import static io.homo.superresolution.api.interop.InteropResourceRequirement.Presence.Optional;
import static io.homo.superresolution.api.interop.InteropResourceRequirement.Presence.Required;
import static io.homo.superresolution.api.interop.InteropResourceRequirement.SizeSource.*;
import static io.homo.superresolution.api.interop.InteropResourceType.*;
import static org.lwjgl.opengl.EXTSemaphore.*;
import static org.lwjgl.opengl.GL11.glFinish;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_ALL_COMMANDS_BIT;

public abstract class GlVulkanInteropAlgorithm extends AbstractAlgorithm implements InteropInputDispatch {
    public static final int INITIAL_COMMAND_BUFFER_RING_SIZE = 4;
    private final VulkanCommandBufferRing commandBufferRing = new VulkanCommandBufferRing(
            INITIAL_COMMAND_BUFFER_RING_SIZE);
    protected FrameResourcesSet frameResourcesSet = null;

    private boolean flipInteropResourcesY;
    private InteropResourceLayout builtLayout;
    private boolean destroyed;
    private boolean initialized;
    // Resolution the interop resources were last built at, to skip redundant resize() rebuilds.
    // (Iris/forceResize call resize() on every pipeline reload even when nothing changed).
    private int builtRenderWidth = -1;
    private int builtRenderHeight = -1;
    private int builtScreenWidth = -1;
    private int builtScreenHeight = -1;

    protected abstract void dispatchVulkanUpscale(
            VulkanCommandBuffer commandBuffer,
            FrameResourcesSet frameResourcesSet
    );

    protected boolean isVulkanInteropReady() {
        return true;
    }

    protected final boolean shouldFlipInteropResourcesY() {
        return flipInteropResourcesY;
    }

    protected void onInteropResourcesCreated() {
    }

    protected void onBeforeInteropResourcesDestroyed() {
    }

    protected List<InteropResourceRequirement> getInteropResourceRequirements() {
        return List.of(
                InteropResourceRequirement.input(
                        Color,
                        Required, RenderSize, InternalColorConfigOrContext,
                        SuperResolutionConfig.getInternalTextureFormat()
                ),
                InteropResourceRequirement.input(
                        Depth,
                        Required, RenderSize, Fixed,
                        TextureFormat.R32F
                ),
                InteropResourceRequirement.input(
                        MotionVectors,
                        Required, RenderSize, Fixed,
                        TextureFormat.RG16F
                ),
                InteropResourceRequirement.input(
                        Exposure,
                        Optional, OneByOne, Fixed,
                        TextureFormat.R32F
                ),
                InteropResourceRequirement.output(
                        OutputColor,
                        OutputSize, InternalColorConfig, null
                )
        );
    }

    protected boolean validateInputResources(InputResourceSet inputs) {
        if (inputs == null) {
            return false;
        }
        for (InteropResourceRequirement requirement : getInteropResourceRequirements()) {
            if (requirement.type().isInput() && requirement.presence() == Required
                    && !inputs.has(requirement.type().inputType())) {
                return false;
            }
        }
        return true;
    }

    /** Called after layout preparation, also for dispatches using an external input writer. */
    protected boolean prepareVulkanDispatch(DispatchResource resource) {
        return isVulkanInteropReady();
    }

    @Override
    public final Map<InteropResourceType, InteropResourceDescription> getInteropResourceDescriptions() {
        return builtLayout == null ? Map.of() : builtLayout.resources();
    }

    @Override
    public final boolean dispatchWithInputWriter(
            DispatchResource dispatchResource, Consumer<InteropInputWriter> writer) {
        if (!initialized || destroyed) {
            throw new IllegalStateException("Interop algorithm is uninitialized or destroyed");
        }
        return dispatchInternal(Objects.requireNonNull(dispatchResource), writer);
    }

    private InteropResourceLayout resolveLayout(InputResourceSet inputs) {
        return InteropResourceLayout.resolve(getInteropResourceRequirements(),
                InteropResourceContextManager.getCurrentContext(), inputs,
                SuperResolutionConfig.getInternalTextureFormat(),
                RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight(),
                RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight());
    }

    private void createResources(InteropResourceLayout layout) {
        VulkanDevice vkDevice = RenderSystems.vulkan().device();
        vkDevice.getMainQueue().waitIdle();
        frameResourcesSet = new FrameResourcesSet(flipInteropResourcesY);
        frameResourcesSet.initialize(layout);
        builtLayout = layout;
        builtRenderWidth = RenderHandlerManager.getRenderWidth();
        builtRenderHeight = RenderHandlerManager.getRenderHeight();
        builtScreenWidth = RenderHandlerManager.getScreenWidth();
        builtScreenHeight = RenderHandlerManager.getScreenHeight();
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
    public final void initialize(InitializationDescription desc) {
        if (initialized || frameResourcesSet != null) {
            throw new IllegalStateException("Interop algorithm still owns resources");
        }
        flipInteropResourcesY = SuperResolutionConfig.isFlipVkGlInteropResourcesY();
        this.initDesc = Objects.requireNonNull(desc);
        destroyed = false;
        invalidateHistory();
        createResources(resolveLayout(null));
        onInteropResourcesCreated();
        initialized = true;
    }

    @Override
    public final boolean dispatch(DispatchResource dispatchResource) {
        return dispatchWithInputWriter(dispatchResource, null);
    }

    @Override
    public final void destroy() {
        if (destroyed) {
            return;
        }
        awaitResourceUsers();
        commandBufferRing.destroy();
        onBeforeInteropResourcesDestroyed();
        destroyResources();
        destroyed = true;
        initialized = false;
    }

    @Override
    public final void resize(int width, int height) {
        if (!initialized || destroyed) {
            throw new IllegalStateException("Cannot resize an uninitialized or destroyed interop algorithm");
        }
        // Skip rebuilding interop resources when the resolution is unchanged. Iris/forceResize call
        // resize() on every pipeline reload during world load even at constant resolution.
        if (isVulkanInteropReady()
                && RenderHandlerManager.getRenderWidth() == builtRenderWidth
                && RenderHandlerManager.getRenderHeight() == builtRenderHeight
                && RenderHandlerManager.getScreenWidth() == builtScreenWidth
                && RenderHandlerManager.getScreenHeight() == builtScreenHeight) {
            return;
        }
        rebuildResources(resolveLayout(null));
    }

    @Override
    public IFrameBuffer getOutputFrameBuffer() {
        return frameResourcesSet.outputFrameBuffer;
    }

    @Override
    public int getOutputTextureId() {
        return Math.toIntExact(frameResourcesSet.openGl(OutputColor).handle());
    }

    private boolean dispatchInternal(DispatchResource dispatchResource, Consumer<InteropInputWriter> writer) {
        super.dispatch(dispatchResource);
        if (!validateInputResources(dispatchResource.resources())) {
            return false;
        }
        InteropResourceLayout layout = resolveLayout(dispatchResource.resources());
        if (!layout.equals(builtLayout)) {
            rebuildResources(layout);
        }
        if (!prepareVulkanDispatch(dispatchResource)) {
            return false;
        }
        VkGlInteropSemaphore upscaleFinishSemaphore;
        VkGlInteropSemaphore glFinishSemaphore;
        upscaleFinishSemaphore = frameResourcesSet.upscaleVkFinish;
        glFinishSemaphore = frameResourcesSet.glFinish;
        // commandBufferRing的acquire会帮我们waitForFence
        //if (frameResourcesSet.commandBuffer != null) {
        //    frameResourcesSet.commandBuffer.waitForFence();
        //}
        processInputResources(frameResourcesSet, dispatchResource, writer);
        signalInputTexturesReady(frameResourcesSet);
        publishCaptureInputs(frameResourcesSet, dispatchResource);

        VulkanDevice vulkanDevice = RenderSystems.vulkan().device();
        frameResourcesSet.frameData = FrameData.from(dispatchResource, flipInteropResourcesY);

        VulkanCommandBuffer commandBuffer = commandBufferRing.acquire(vulkanDevice);
        // 构建第N-1帧的Cmdbuf

        commandBuffer.begin();
        dispatchVulkanUpscale(
                commandBuffer,
                frameResourcesSet
        );
        commandBuffer.end();

        // 提交第N-1帧的Cmdbuf
        // 在第N-1帧的GL渲染结果准备好后（ginishSemaphore）
        // 执行Upscale
        // 并在Upscale完成后（upscaleFinishSemaphore）通知GL Queue
        frameResourcesSet.fence = vulkanDevice.submitCommandBuffer(
                commandBuffer,
                new long[]{glFinishSemaphore.getVkSemaphoreHandle()},
                new int[]{VK_PIPELINE_STAGE_ALL_COMMANDS_BIT},
                new long[]{upscaleFinishSemaphore.getVkSemaphoreHandle()}
        );

        // 存一下第N-1帧的Cmdbuf
        frameResourcesSet.commandBuffer = commandBuffer;

        upscaleFinishSemaphore.waitVulkanSignal(
                new int[]{Math.toIntExact(frameResourcesSet.openGl(OutputColor).handle())},
                new int[]{},
                new int[]{GL_LAYOUT_GENERAL_EXT}
        );
        flipOutputIfEnabled(frameResourcesSet);
        return true;
    }

    private void flipOutputIfEnabled(FrameResourcesSet inFlight) {
        if (!flipInteropResourcesY) {
            return;
        }
        PerformanceTracker.push(PerformanceTracker.GL_INTEROP_FLIP);
        try {
            InteropResourcesPreprocessor.flipY(
                    inFlight.openGl(OutputColor),
                    inFlight.flippedOutputGlTexture);
        } finally {
            PerformanceTracker.pop(PerformanceTracker.GL_INTEROP_FLIP);
        }
    }

    private void rebuildResources(InteropResourceLayout layout) {
        awaitResourceUsers();
        commandBufferRing.destroy();
        onBeforeInteropResourcesDestroyed();
        destroyResources();
        try {
            createResources(layout);
            onInteropResourcesCreated();
        } catch (RuntimeException | Error error) {
            // Keep partial owners reachable for destruction, but never accept this layout as ready.
            builtLayout = null;
            throw error;
        }
        invalidateHistory();
        FrameGeneration.invalidateHistory();
    }

    private void awaitResourceUsers() {
        PresentationBackendManager.flushCapturedFrame();
        if (frameResourcesSet != null) {
            frameResourcesSet.awaitCaptureRelease();
        }
        // Vulkan idle alone does not retire OpenGL readers of an unflipped shared output.
        glFinish();
        RenderSystems.vulkan().device().getMainQueue().waitIdle();
    }

    private void processInputResources(FrameResourcesSet inFlight, DispatchResource dispatchResource,
                                       Consumer<InteropInputWriter> writer) {
        inFlight.awaitCaptureRelease();
        String motionVectorPreprocessingFunction =
                SRWorkModeManager.getCurrentState().motionVectorPreprocessingFunction();
        try (
                InteropInputWriteSession session = new InteropInputWriteSession(inFlight.glTextures,
                        (type, source) -> transferInput(
                                type, source, inFlight.openGl(type), motionVectorPreprocessingFunction))
        ) {
            PerformanceTracker.push(PerformanceTracker.GL_INPUT_CONVERT);
            if (writer != null) {
                writer.accept(session);
                session.requireHealthy();
            }
            InputResourceSet inputs = dispatchResource.resources();
            InteropResourcesPreprocessor.processInputTextures(
                    session.pendingSource(Color, inputs), inFlight.openGl(Color),
                    session.pendingSource(Depth, inputs), inFlight.openGl(Depth),
                    session.pendingSource(MotionVectors, inputs), inFlight.openGl(MotionVectors),
                    session.pendingSource(Exposure, inputs), inFlight.openGl(Exposure),
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
                    ITexture source = session.pendingSource(type, inputs);
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
        int[] handles = inFlight.resourceTypes().stream().filter(InteropResourceType::isInput)
                .mapToInt(type -> Math.toIntExact(inFlight.openGl(type).handle())).toArray();
        int[] layouts = new int[handles.length];
        Arrays.fill(layouts, GL_LAYOUT_SHADER_READ_ONLY_EXT);
        inFlight.glFinish.signalVulkan(handles, new int[]{}, layouts);
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

    private void transferInput(InteropResourceType type, ITexture source, ITexture destination, String mvFunction) {
        if (source.getTextureType() != TextureType.Texture2D
                || source.getWidth() < destination.getWidth() || source.getHeight() < destination.getHeight()) {
            throw new IllegalArgumentException("External source does not cover the interop input: " + type);
        }
        if (type == Color || type == Depth || type == MotionVectors || type == Exposure) {
            InteropResourcesPreprocessor.processInputTextures(
                    type == Color ? source : null, type == Color ? destination : null,
                    type == Depth ? source : null, type == Depth ? destination : null,
                    type == MotionVectors ? source : null, type == MotionVectors ? destination : null,
                    type == Exposure ? source : null, type == Exposure ? destination : null,
                    mvFunction, flipInteropResourcesY);
            return;
        }
        ICommandBuffer commandBuffer = RenderSystems.opengl().device().defaultCommandPool().createCommandBuffer();
        try {
            commandBuffer.begin();
            recordSupplementalTransfer(commandBuffer, type, source, destination);
            commandBuffer.end();
            RenderSystems.opengl().device().submitCommandBuffer(commandBuffer);
        } finally {
            commandBuffer.destroy();
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

    public record FrameData(
            int renderWidth,

            int renderHeight,

            Vector2f renderSize,

            int screenWidth,

            int screenHeight,

            Vector2f screenSize,

            int frameCount,

            float frameTimeDelta,

            float verticalFov,

            float horizontalFov,

            float cameraNear,

            float cameraFar,

            Vector2f jitterOffset,

            int jitterSeq,

            Matrix4f modelViewMatrix,

            Matrix4f projectionMatrix,

            Matrix4f modelViewProjectionMatrix,

            Matrix4f viewMatrix,

            Matrix4f lastModelViewMatrix,

            Matrix4f lastProjectionMatrix,

            Matrix4f lastModelViewProjectionMatrix,

            Matrix4f lastViewMatrix,

            float preExposure

    ) {
        public static FrameData from(DispatchResource dispatchResource, boolean flipY) {
            Vector2f jitterOffset = new Vector2f(dispatchResource.jitterOffset());
            return new FrameData(
                    dispatchResource.renderWidth(),
                    dispatchResource.renderHeight(),
                    dispatchResource.renderSize(),
                    dispatchResource.screenWidth(),
                    dispatchResource.screenHeight(),
                    dispatchResource.screenSize(),
                    dispatchResource.frameCount(),
                    dispatchResource.frameTimeDelta(),
                    dispatchResource.verticalFov(),
                    dispatchResource.horizontalFov(),
                    dispatchResource.cameraNear(),
                    dispatchResource.cameraFar(),
                    jitterOffset,
                    dispatchResource.jitterSequenceLength(),
                    new Matrix4f(dispatchResource.modelViewMatrix()),
                    new Matrix4f(dispatchResource.projectionMatrix()),
                    new Matrix4f(dispatchResource.modelViewProjectionMatrix()),
                    new Matrix4f(dispatchResource.viewMatrix()),
                    new Matrix4f(dispatchResource.lastModelViewMatrix()),
                    new Matrix4f(dispatchResource.lastProjectionMatrix()),
                    new Matrix4f(dispatchResource.lastModelViewProjectionMatrix()),
                    new Matrix4f(dispatchResource.lastViewMatrix()),
                    dispatchResource.preExposure()
            );
        }
    }

    public static class FrameResourcesSet {
        private final boolean flipInteropResourcesY;
        private final EnumMap<InteropResourceType, VulkanTexture> vulkanTextures =
                new EnumMap<>(InteropResourceType.class);
        private final EnumMap<InteropResourceType, GlImportableTexture2D> glTextures =
                new EnumMap<>(InteropResourceType.class);

        public GlTexture2D flippedOutputGlTexture;
        public IFrameBuffer outputFrameBuffer;

        public VkGlInteropSemaphore glFinish;
        public VkGlInteropSemaphore upscaleVkFinish;
        public VkGlInteropSemaphore captureDepthReady;
        public VkGlInteropSemaphore captureDepthRelease;
        public VkGlInteropSemaphore captureMotionReady;
        public VkGlInteropSemaphore captureMotionRelease;
        public FrameData frameData;
        public VulkanCommandBuffer commandBuffer;
        public long fence;

        private boolean captureDepthPending;
        private boolean captureMotionPending;
        private FrameResources captureInputsFrame;

        public FrameResourcesSet(boolean flipInteropResourcesY) {
            this.flipInteropResourcesY = flipInteropResourcesY;
        }

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
            if (outputFrameBuffer != null) {
                outputFrameBuffer.destroy();
                outputFrameBuffer = null;
            }
            if (flippedOutputGlTexture != null) {
                flippedOutputGlTexture.destroy();
                flippedOutputGlTexture = null;
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
            if (flipInteropResourcesY) {
                this.flippedOutputGlTexture = (GlTexture2D) glDevice.createTexture(
                        TextureDescription.create()
                                .type(TextureType.Texture2D)
                                .usages(TextureUsages.create().sampler().storage().transferDestination())
                                .format(output.format())
                                .width(output.width())
                                .height(output.height())
                                .label("SRUpscaleFlippedOutputGlTexture")
                                .build()
                );
            }

            this.outputFrameBuffer = RenderSystems.current().device().createFramebuffer(
                    FramebufferDescription.create()
                            .colorAttachment(flipInteropResourcesY
                                    ? this.flippedOutputGlTexture
                                    : openGl(OutputColor))
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
