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
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.config.enums.InteropSyncMode;
import io.homo.superresolution.common.framegeneration.FrameGeneration;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.perf.PerformanceTracker;
import io.homo.superresolution.common.presentation.capture.FrameCaptureManager;
import io.homo.superresolution.common.presentation.capture.FrameResources;
import io.homo.superresolution.common.presentation.PresentationBackendManager;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.InteropResourcesPreprocessor;
import io.homo.superresolution.common.workmode.SRWorkModeManager;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.framebuffer.FramebufferDescription;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsages;
import io.homo.superresolution.core.graphics.opengl.GlDevice;
import io.homo.superresolution.core.graphics.opengl.texture.GlImportableTexture2D;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.core.graphics.vulkan.*;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.util.*;
import java.util.function.Consumer;

import static org.lwjgl.opengl.EXTSemaphore.*;
import static org.lwjgl.opengl.GL11.glFinish;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_ALL_COMMANDS_BIT;
import static io.homo.superresolution.api.interop.InteropResourceType.*;
import static io.homo.superresolution.api.interop.InteropResourceRequirement.FormatSource.*;
import static io.homo.superresolution.api.interop.InteropResourceRequirement.SizeSource.*;
import static io.homo.superresolution.api.interop.InteropResourceRequirement.Presence.*;

public abstract class GlVulkanInteropAlgorithm extends AbstractAlgorithm implements InteropInputDispatch {
    public static final int INITIAL_COMMAND_BUFFER_RING_SIZE = 5;
    public static final int MAX_IN_FLIGHT_FRAME = 3;
    private final VulkanCommandBufferRing commandBufferRing = new VulkanCommandBufferRing(
            INITIAL_COMMAND_BUFFER_RING_SIZE);
    protected InFlightFrameResourcesSet[] inFlightFrames = new InFlightFrameResourcesSet[MAX_IN_FLIGHT_FRAME];

    protected boolean syncSerialMode;
    private boolean flipInteropResourcesY;
    private InteropResourceLayout builtLayout;
    private boolean dispatchActive;
    private boolean destroyed;
    private boolean initialized;

    // 部分模组会跳过世界渲染，但全局 GameFrameIndex 仍会推进。
    // interop 流水线只按实际 dispatch 推进，避免三缓冲资源错位。
    protected int interopFrameSequence = 0;

    // Resolution the interop resources were last built at, to skip redundant resize() rebuilds.
    // (Iris/forceResize call resize() on every pipeline reload even when nothing changed).
    private int builtRenderWidth = -1;
    private int builtRenderHeight = -1;
    private int builtScreenWidth = -1;
    private int builtScreenHeight = -1;

    protected abstract void dispatchVulkanUpscale(
            VulkanCommandBuffer commandBuffer,
            InFlightFrameResourcesSet inFlightFrameResourcesSet
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

    /** Override to declare a different resource contract; the base class owns every allocation. */
    protected List<InteropResourceRequirement> getInteropResourceRequirements() {
        return List.of(
                InteropResourceRequirement.input(Color, Required, RenderSize, Context,
                        SuperResolutionConfig.getInternalTextureFormat()),
                InteropResourceRequirement.input(Depth, Required, RenderSize, Fixed, TextureFormat.R32F),
                InteropResourceRequirement.input(MotionVectors, Required, RenderSize, Context, TextureFormat.RG16F),
                InteropResourceRequirement.input(Exposure, Optional, OneByOne, Fixed, TextureFormat.R32F),
                InteropResourceRequirement.output(OutputColor, OutputSize, InternalColorConfig, null));
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
        for (int i = 0; i < (syncSerialMode ? 1 : MAX_IN_FLIGHT_FRAME); i++) {
            inFlightFrames[i] = new InFlightFrameResourcesSet(flipInteropResourcesY);
            inFlightFrames[i].index = i;
            inFlightFrames[i].initialize(layout);
        }
        builtLayout = layout;
        builtRenderWidth = RenderHandlerManager.getRenderWidth();
        builtRenderHeight = RenderHandlerManager.getRenderHeight();
        builtScreenWidth = RenderHandlerManager.getScreenWidth();
        builtScreenHeight = RenderHandlerManager.getScreenHeight();
    }

    private void destroyResources() {
        RenderSystems.vulkan().device().getMainQueue().waitIdle();
        for (int i = 0; i < (syncSerialMode ? 1 : MAX_IN_FLIGHT_FRAME); i++) {
            if (inFlightFrames[i] != null) {
                inFlightFrames[i].destroy();
                inFlightFrames[i] = null;
            }
        }
        builtLayout = null;
    }

    @Override
    public final void initialize(InitializationDescription desc) {
        checkLifecycleAccess();
        if (initialized || Arrays.stream(inFlightFrames).anyMatch(Objects::nonNull)) {
            throw new IllegalStateException("Interop algorithm still owns resources");
        }
        syncSerialMode = SuperResolutionConfig.getInteropSyncMode() == InteropSyncMode.LowLatency;
        flipInteropResourcesY = SuperResolutionConfig.isFlipVkGlInteropResourcesY();
        this.initDesc = Objects.requireNonNull(desc);
        destroyed = false;
        interopFrameSequence = 0;
        invalidateHistory();
        createResources(resolveLayout(null));
        onInteropResourcesCreated();
        initialized = true;
    }

    @Override
    public final boolean dispatch(DispatchResource dispatchResource) {
        return dispatchWithInputWriter(dispatchResource, null);
    }

    /**
     * Synchronous render-thread input access, currently supported only in LowLatency mode.
     * DispatchResource must still describe the inputs (including optional inputs and RR combinations).
     * The writer runs after reuse waits and before the shared input semaphore is signalled.
     */
    @Override
    public final boolean dispatchWithInputWriter(
            DispatchResource dispatchResource, Consumer<InteropInputWriter> writer) {
        checkRenderThread();
        if (!initialized || destroyed || dispatchActive) {
            throw new IllegalStateException("Interop algorithm is uninitialized, destroyed or already dispatching");
        }
        if (writer != null && !syncSerialMode) {
            throw new IllegalStateException("External interop input writers require LowLatency mode");
        }
        dispatchActive = true;
        try {
            return dispatchInternal(Objects.requireNonNull(dispatchResource), writer);
        } finally {
            dispatchActive = false;
        }
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
        if (syncSerialMode) {
            int currentFrameIndex = 0;
            InFlightFrameResourcesSet inFlight;
            VkGlInteropSemaphore upscaleFinishSemaphore;
            VkGlInteropSemaphore glFinishSemaphore;
            inFlight = inFlightFrames[currentFrameIndex];
            upscaleFinishSemaphore = inFlight.upscaleVkFinish;
            glFinishSemaphore = inFlight.glFinish;
            // commandBufferRing的acquire会帮我们waitForFence
            //if (inFlight.commandBuffer != null) {
            //    inFlight.commandBuffer.waitForFence();
            //}
            processInputResources(inFlight, dispatchResource, writer);
            interopFrameSequence++;
            signalInputTexturesReady(inFlight);
            publishCaptureInputs(inFlight, dispatchResource);

            VulkanDevice vulkanDevice = RenderSystems.vulkan().device();
            inFlight.frameData = FrameData.from(dispatchResource, flipInteropResourcesY);

            VulkanCommandBuffer commandBuffer = commandBufferRing.acquire(vulkanDevice);
            // 构建第N-1帧的Cmdbuf
            commandBuffer.begin();
            dispatchVulkanUpscale(
                    commandBuffer,
                    inFlight
            );
            commandBuffer.end();

            // 提交第N-1帧的Cmdbuf
            // 在第N-1帧的GL渲染结果准备好后（ginishSemaphore）
            // 执行Upscale
            // 并在Upscale完成后（upscaleFinishSemaphore）通知GL Queue
            inFlight.fence = vulkanDevice.submitCommandBuffer(
                    commandBuffer,
                    new long[]{glFinishSemaphore.getVkSemaphoreHandle()},
                    new int[]{VK_PIPELINE_STAGE_ALL_COMMANDS_BIT},
                    new long[]{upscaleFinishSemaphore.getVkSemaphoreHandle()}
            );

            // 存一下第N-1帧的Cmdbuf
            inFlight.commandBuffer = commandBuffer;

            upscaleFinishSemaphore.waitVulkanSignal(
                    new int[]{Math.toIntExact(inFlight.openGl(OutputColor).handle())},
                    new int[]{},
                    new int[]{GL_LAYOUT_GENERAL_EXT}
            );
            flipOutputIfEnabled(inFlight);
        } else {
            int currentFrameIndex = interopFrameSequence + 1;
            {
                InFlightFrameResourcesSet inFlight;
                VkGlInteropSemaphore glFinishSemaphore;
                // =============== 处理第N帧还未完成的GL渲染结果 ================
                inFlight = inFlightFrames[currentFrameIndex % MAX_IN_FLIGHT_FRAME];
                glFinishSemaphore = inFlight.glFinish;
                inFlight.frameData = FrameData.from(dispatchResource, flipInteropResourcesY);
                // Do NOT wait on upscaleVkFinish here. This slot's upscale output is consumed -- with
                // its own waitOpenGL -- in the third stage below, so an extra GL wait on the same binary
                // semaphore makes it two waits per one signal each cycle, and on the first cycle a wait
                // before any signal. On Linux the opaque-FD semaphore wait blocks the GL queue hard and
                // deadlocks (Windows happens to tolerate the illegal wait). Reuse safety for this slot's
                // inputs comes from the fence wait below plus the command-buffer ring.
                if (inFlight.commandBuffer != null) {
                    inFlight.commandBuffer.waitForFence();
                }
                processInputResources(inFlight, dispatchResource, null);
                interopFrameSequence++;

                signalInputTexturesReady(inFlight);
                publishCaptureInputs(inFlight, dispatchResource);
            }
            if (currentFrameIndex > 1) {
                InFlightFrameResourcesSet inFlight;
                VkGlInteropSemaphore upscaleFinishSemaphore;
                VkGlInteropSemaphore glFinishSemaphore;
                int finishedGlIndex = 0;
                // =============== 处理第N-1帧已经预期完成的GL渲染结果 ================
                finishedGlIndex = (((currentFrameIndex - 1) % MAX_IN_FLIGHT_FRAME) + MAX_IN_FLIGHT_FRAME) % MAX_IN_FLIGHT_FRAME;
                // 获取第N-1帧的资源集合
                inFlight = inFlightFrames[finishedGlIndex];

                upscaleFinishSemaphore = inFlight.upscaleVkFinish;
                glFinishSemaphore = inFlight.glFinish;
                VulkanDevice vulkanDevice = RenderSystems.vulkan().device();
                VulkanCommandBuffer commandBuffer = commandBufferRing.acquire(vulkanDevice);

                if (inFlight.frameData != null) {
                    // 构建第N-1帧的Cmdbuf
                    commandBuffer.begin();
                    dispatchVulkanUpscale(
                            commandBuffer,
                            inFlight
                    );
                    commandBuffer.end();

                    // 提交第N-1帧的Cmdbuf
                    // 在第N-1帧的GL渲染结果准备好后（glFinishSemaphore）
                    // 执行Upscale
                    // 并在Upscale完成后（upscaleFinishSemaphore）通知GL Queue
                    inFlight.fence = vulkanDevice.submitCommandBuffer(
                            commandBuffer,
                            new long[]{glFinishSemaphore.getVkSemaphoreHandle()},
                            new int[]{VK_PIPELINE_STAGE_ALL_COMMANDS_BIT},
                            new long[]{upscaleFinishSemaphore.getVkSemaphoreHandle()}
                    );

                    // 存一下第N-1帧的Cmdbuf
                    inFlight.commandBuffer = commandBuffer;

                }
                // =================================================================
            }
            if (currentFrameIndex > 2) {
                InFlightFrameResourcesSet inFlight;
                VkGlInteropSemaphore upscaleFinishSemaphore;
                VkGlInteropSemaphore glFinishSemaphore;
                int finishedGlIndex = 0;
                int finishedIndex = 0;
                // =============== 渲染第N-2帧已经预期完成的Upscale结果 ================
                // 获取第N-2帧的资源集合Index
                finishedIndex = (((currentFrameIndex - 2) % MAX_IN_FLIGHT_FRAME) + MAX_IN_FLIGHT_FRAME) % MAX_IN_FLIGHT_FRAME;

                // 获取第N-2帧的资源集合
                inFlight = inFlightFrames[finishedIndex];
                upscaleFinishSemaphore = inFlight.upscaleVkFinish;
                glFinishSemaphore = inFlight.glFinish;

                // Only consume this slot's upscale output if its upscale was actually submitted since the
                // last resource (re)creation. A non-null commandBuffer means upscaleVkFinish has been
                // signaled at least once. resize() rebuilds the slots (fresh, UNSIGNALED semaphores) but
                // does NOT reset interopFrameSequence, so for the first frames after a resize this stage's index is
                // still > 2 while the slot was never upscaled; waiting on its never-signaled binary
                // semaphore blocks the GL queue and deadlocks (the HighPerformance freeze during world
                // load, where resize() fires repeatedly). A later recreateAlgorithm (new instance,
                // interopFrameSequence = 0) re-primes and briefly unblocks it -- hence the freeze/render/freeze cycle.
                if (inFlight.commandBuffer != null) {
                    // No CPU fence wait here. waitVulkanSignal below is a GL-queue-side
                    // GPU wait that already orders the flip after the upscale, and the
                    // matching signal was submitted during the previous dispatch, so the
                    // glWaitSemaphoreEXT is legal. Blocking on the fence as well stalled
                    // the render thread on GPU work submitted one frame earlier and capped
                    // the pipeline at a single frame of overlap. The commandBuffer != null
                    // guard is what keeps a never-signaled semaphore from being waited on
                    // after a resize.

                    //GL Queue等待第N-2帧的Upscale结果
                    upscaleFinishSemaphore.waitVulkanSignal(
                            new int[]{Math.toIntExact(inFlight.openGl(OutputColor).handle())},
                            new int[]{},
                            new int[]{GL_LAYOUT_GENERAL_EXT}
                    );

                    //把第N-2帧的Upscale结果交给最终输出纹理
                    flipOutputIfEnabled(inFlight);
                }
                // =================================================================
            }
        }
        return true;
    }

    private void flipOutputIfEnabled(InFlightFrameResourcesSet inFlight) {
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

    @Override
    public final void destroy() {
        checkLifecycleAccess();
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
        checkLifecycleAccess();
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

    private void rebuildResources(InteropResourceLayout layout) {
        awaitResourceUsers();
        commandBufferRing.destroy();
        onBeforeInteropResourcesDestroyed();
        destroyResources();
        interopFrameSequence = 0;
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
        for (InFlightFrameResourcesSet frame : inFlightFrames) {
            if (frame != null) {
                frame.awaitCaptureRelease();
            }
        }
        // Vulkan idle alone does not retire OpenGL readers of an unflipped shared output.
        glFinish();
        RenderSystems.vulkan().device().getMainQueue().waitIdle();
    }

    private static void checkRenderThread() {
        if (SuperResolution.renderThread != null && Thread.currentThread() != SuperResolution.renderThread) {
            throw new IllegalStateException("Interop resources may only be used on the render thread");
        }
    }

    private void checkLifecycleAccess() {
        checkRenderThread();
        if (dispatchActive) {
            throw new IllegalStateException("Cannot resize or destroy interop resources during dispatch");
        }
    }

    @Override
    public IFrameBuffer getOutputFrameBuffer() {
        if (syncSerialMode) {
            return inFlightFrames[0].outputFrameBuffer;
        }
        int currentFrameIndex = interopFrameSequence;
        int finishedIndex = (((currentFrameIndex - 2) % MAX_IN_FLIGHT_FRAME) + MAX_IN_FLIGHT_FRAME) % MAX_IN_FLIGHT_FRAME;
        return inFlightFrames[finishedIndex].outputFrameBuffer;
    }

    @Override
    public int getOutputTextureId() {
        if (syncSerialMode) {
            return Math.toIntExact(inFlightFrames[0].openGl(OutputColor).handle());
        }
        int currentFrameIndex = interopFrameSequence;
        int finishedIndex = (((currentFrameIndex - 2) % MAX_IN_FLIGHT_FRAME) + MAX_IN_FLIGHT_FRAME) % MAX_IN_FLIGHT_FRAME;
        GlImportableTexture2D outputColorGlTexture = inFlightFrames[finishedIndex].openGl(OutputColor);
        return Math.toIntExact(outputColorGlTexture.handle());
    }

    private void processInputResources(InFlightFrameResourcesSet inFlight, DispatchResource dispatchResource,
                                       Consumer<InteropInputWriter> writer) {
        inFlight.awaitCaptureRelease();
        String motionVectorPreprocessingFunction =
                SRWorkModeManager.getCurrentState().motionVectorPreprocessingFunction();
        InteropInputWriteSession session = new InteropInputWriteSession(inFlight.glTextures,
                (type, source) -> transferInput(
                        type, source, inFlight.openGl(type), motionVectorPreprocessingFunction));
        PerformanceTracker.push(PerformanceTracker.GL_INPUT_CONVERT);
        try {
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
            session.close();
            PerformanceTracker.pop(PerformanceTracker.GL_INPUT_CONVERT);
        }
    }

    private void signalInputTexturesReady(InFlightFrameResourcesSet inFlight) {
        int[] handles = inFlight.resourceTypes().stream().filter(InteropResourceType::isInput)
                .mapToInt(type -> Math.toIntExact(inFlight.openGl(type).handle())).toArray();
        int[] layouts = new int[handles.length];
        Arrays.fill(layouts, GL_LAYOUT_SHADER_READ_ONLY_EXT);
        inFlight.glFinish.signalVulkan(handles, new int[]{}, layouts);
    }

    private void publishCaptureInputs(
            InFlightFrameResourcesSet inFlight,
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
            if (!flipY) {
                jitterOffset.y *= -1.0f;
            }
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

    public static class InFlightFrameResourcesSet {
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

        protected int index;
        private boolean captureDepthPending;
        private boolean captureMotionPending;
        private FrameResources captureInputsFrame;

        public InFlightFrameResourcesSet(boolean flipInteropResourcesY) {
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
            for (Iterator<GlImportableTexture2D> it = glTextures.values().iterator(); it.hasNext();) {
                it.next().destroy();
                it.remove();
            }
            for (Iterator<VulkanTexture> it = vulkanTextures.values().iterator(); it.hasNext();) {
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
                                .label("SRInterop-%s-%s".formatted(entry.getKey(), index))
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
                                .label("SRUpscaleFlippedOutputGlTexture-%s".formatted(index))
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
