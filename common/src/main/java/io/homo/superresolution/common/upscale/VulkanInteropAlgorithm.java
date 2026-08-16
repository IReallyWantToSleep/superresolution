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

package io.homo.superresolution.common.upscale;

import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.api.InitializationDescription;
import io.homo.superresolution.api.InputResourceType;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.config.enums.InteropSyncMode;
import io.homo.superresolution.common.framegeneration.FrameGeneration;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.presentation.capture.FrameCaptureManager;
import io.homo.superresolution.common.presentation.capture.FrameResources;
import io.homo.superresolution.common.presentation.vulkan.VulkanPresentationFeature;
import io.homo.superresolution.common.workmode.SRWorkModeManager;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.framebuffer.FramebufferDescription;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
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

import static org.lwjgl.opengl.EXTSemaphore.GL_LAYOUT_GENERAL_EXT;
import static org.lwjgl.opengl.EXTSemaphore.GL_LAYOUT_SHADER_READ_ONLY_EXT;
import static org.lwjgl.opengl.EXTSemaphore.GL_LAYOUT_TRANSFER_DST_EXT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_ALL_COMMANDS_BIT;

public abstract class VulkanInteropAlgorithm extends AbstractAlgorithm {
    public static final int INITIAL_COMMAND_BUFFER_RING_SIZE = 5;
    public static final int MAX_IN_FLIGHT_FRAME = 3;
    private final VulkanCommandBufferRing commandBufferRing = new VulkanCommandBufferRing(
            INITIAL_COMMAND_BUFFER_RING_SIZE);
    protected InFlightFrameResourcesSet[] inFlightFrames = new InFlightFrameResourcesSet[MAX_IN_FLIGHT_FRAME];

    protected boolean syncSerialMode;

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

    protected void onInteropResourcesCreated() {
    }

    protected void onBeforeInteropResourcesDestroyed() {
    }

    protected void createResources() {
        VulkanDevice vkDevice = RenderSystems.vulkan().device();
        vkDevice.getMainQueue().waitIdle();
        for (int i = 0; i < (syncSerialMode ? 1 : MAX_IN_FLIGHT_FRAME); i++) {
            inFlightFrames[i] = new InFlightFrameResourcesSet();
            inFlightFrames[i].index = i;
            inFlightFrames[i].initialize();
        }
        builtRenderWidth = RenderHandlerManager.getRenderWidth();
        builtRenderHeight = RenderHandlerManager.getRenderHeight();
        builtScreenWidth = RenderHandlerManager.getScreenWidth();
        builtScreenHeight = RenderHandlerManager.getScreenHeight();
    }

    protected void destroyResources() {
        RenderSystems.vulkan().device().getMainQueue().waitIdle();
        for (int i = 0; i < (syncSerialMode ? 1 : MAX_IN_FLIGHT_FRAME); i++) {
            if (inFlightFrames[i] != null) {
                inFlightFrames[i].destroy();
            }
        }
    }

    @Override
    public void initialize(InitializationDescription desc) {
        syncSerialMode = SuperResolutionConfig.getInteropSyncMode() == InteropSyncMode.LowLatency;
        this.initDesc = desc;
        createResources();
        onInteropResourcesCreated();
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        super.dispatch(dispatchResource);

        if (!isVulkanInteropReady()) {
            return false;
        }
        interopFrameSequence++;
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
            processInputResources(inFlight, dispatchResource);
            glFinishSemaphore.signalVulkan(
                    new int[]{Math.toIntExact(inFlight.inputColorGlTexture.handle()),
                            Math.toIntExact(inFlight.inputDepthGlTexture.handle()),
                            Math.toIntExact(inFlight.inputMotionVectorsGlTexture.handle()),
                            Math.toIntExact(inFlight.inputExposureGlTexture.handle())

                    },
                    new int[]{},
                    new int[]{
                            GL_LAYOUT_SHADER_READ_ONLY_EXT,
                            GL_LAYOUT_SHADER_READ_ONLY_EXT,
                            GL_LAYOUT_SHADER_READ_ONLY_EXT,
                            GL_LAYOUT_SHADER_READ_ONLY_EXT
                    }
            );
            publishCaptureInputs(inFlight, dispatchResource);

            VulkanDevice vulkanDevice = RenderSystems.vulkan().device();
            inFlight.frameData = FrameData.from(dispatchResource);

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
                    new int[]{Math.toIntExact(inFlight.outputColorGlTexture.handle())},
                    new int[]{},
                    new int[]{GL_LAYOUT_GENERAL_EXT}
            );
            InteropResourcesConverter.flipY(
                    inFlight.outputColorGlTexture,
                    inFlight.flippedOutputGlTexture);
        } else {
            int currentFrameIndex = interopFrameSequence;
            {
                InFlightFrameResourcesSet inFlight;
                VkGlInteropSemaphore glFinishSemaphore;
                // =============== 处理第N帧还未完成的GL渲染结果 ================
                inFlight = inFlightFrames[currentFrameIndex % MAX_IN_FLIGHT_FRAME];
                glFinishSemaphore = inFlight.glFinish;
                inFlight.frameData = FrameData.from(dispatchResource);
                // Do NOT wait on upscaleVkFinish here. This slot's upscale output is consumed -- with
                // its own waitOpenGL -- in the third stage below, so an extra GL wait on the same binary
                // semaphore makes it two waits per one signal each cycle, and on the first cycle a wait
                // before any signal. On Linux the opaque-FD semaphore wait blocks the GL queue hard and
                // deadlocks (Windows happens to tolerate the illegal wait). Reuse safety for this slot's
                // inputs comes from the fence wait below plus the command-buffer ring.
                if (inFlight.commandBuffer != null) {
                    inFlight.commandBuffer.waitForFence();
                }
                processInputResources(inFlight, dispatchResource);

                glFinishSemaphore.signalVulkan(
                        new int[]{Math.toIntExact(inFlight.inputColorGlTexture.handle()),
                                Math.toIntExact(inFlight.inputDepthGlTexture.handle()),
                                Math.toIntExact(inFlight.inputMotionVectorsGlTexture.handle()),
                                Math.toIntExact(inFlight.inputExposureGlTexture.handle())

                        },
                        new int[]{},
                        new int[]{
                                GL_LAYOUT_SHADER_READ_ONLY_EXT,
                                GL_LAYOUT_SHADER_READ_ONLY_EXT,
                                GL_LAYOUT_SHADER_READ_ONLY_EXT,
                                GL_LAYOUT_SHADER_READ_ONLY_EXT
                        }
                );
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
                    inFlight.commandBuffer.waitForFence();

                    //GL Queue等待第N-2帧的Upscale结果
                    upscaleFinishSemaphore.waitVulkanSignal(
                            new int[]{Math.toIntExact(inFlight.outputColorGlTexture.handle())},
                            new int[]{},
                            new int[]{GL_LAYOUT_GENERAL_EXT}
                    );

                    //把第N-2帧的Upscale结果从OpenGL共享纹理翻转到最终输出纹理
                    InteropResourcesConverter.flipY(
                            inFlight.outputColorGlTexture,
                            inFlight.flippedOutputGlTexture);
                }
                // =================================================================
            }
        }
        return true;
    }

    @Override
    public void destroy() {
        RenderSystems.vulkan().device().getMainQueue().waitIdle();
        commandBufferRing.destroy();
        onBeforeInteropResourcesDestroyed();
        destroyResources();
    }

    @Override
    public void resize(int width, int height) {
        // Skip rebuilding interop resources when the resolution is unchanged. Iris/forceResize call
        // resize() on every pipeline reload during world load even at constant resolution.
        if (isVulkanInteropReady()
                && RenderHandlerManager.getRenderWidth() == builtRenderWidth
                && RenderHandlerManager.getRenderHeight() == builtRenderHeight
                && RenderHandlerManager.getScreenWidth() == builtScreenWidth
                && RenderHandlerManager.getScreenHeight() == builtScreenHeight) {
            return;
        }
        RenderSystems.vulkan().device().getMainQueue().waitIdle();
        commandBufferRing.destroy();

        onBeforeInteropResourcesDestroyed();
        destroyResources();

        createResources();
        onInteropResourcesCreated();
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
            return Math.toIntExact(inFlightFrames[0].outputColorGlTexture.handle());
        }
        int currentFrameIndex = interopFrameSequence;
        int finishedIndex = (((currentFrameIndex - 2) % MAX_IN_FLIGHT_FRAME) + MAX_IN_FLIGHT_FRAME) % MAX_IN_FLIGHT_FRAME;
        GlImportableTexture2D outputColorGlTexture = inFlightFrames[finishedIndex].outputColorGlTexture;
        return Math.toIntExact(outputColorGlTexture.handle());
    }

    private void processInputResources(InFlightFrameResourcesSet inFlight, DispatchResource dispatchResource) {
        inFlight.awaitCaptureRelease();

        String motionVectorPreprocessingFunction =
                SRWorkModeManager.getCurrentState().motionVectorPreprocessingFunction();

        InteropResourcesConverter.processInputTextures(
                dispatchResource.resources().get(InputResourceType.Color), inFlight.inputColorGlTexture,
                dispatchResource.resources().get(InputResourceType.Depth), inFlight.inputDepthGlTexture,
                dispatchResource.resources().get(InputResourceType.MotionVectors), inFlight.inputMotionVectorsGlTexture,
                dispatchResource.resources().get(InputResourceType.Exposure), inFlight.inputExposureGlTexture,
                motionVectorPreprocessingFunction
        );
    }

    private void publishCaptureInputs(
            InFlightFrameResourcesSet inFlight,
            DispatchResource dispatchResource
    ) {
        if (!VulkanPresentationFeature.isRequested()
                || !FrameCaptureManager.isInitialized()
                // Real-only presentation does not consume these borrowed textures.
                || !FrameGeneration.isFrameGenerationEnabled()
                || dispatchResource.resources() == null) {
            return;
        }

        boolean hasDepth = dispatchResource.resources().has(InputResourceType.Depth);
        boolean hasMotionVectors = dispatchResource.resources().has(InputResourceType.MotionVectors);
        if (!hasDepth && !hasMotionVectors) {
            return;
        }

        FrameResources captureFrame = FrameCaptureManager.captureVulkanInputs(
                dispatchResource.frameCount(),
                hasDepth ? inFlight.inputDepthVkTexture : null,
                hasDepth ? inFlight.inputDepthGlTexture : null,
                hasDepth ? inFlight.captureDepthReady : null,
                hasDepth ? inFlight.captureDepthRelease : null,
                hasMotionVectors ? inFlight.inputMotionVectorsVkTexture : null,
                hasMotionVectors ? inFlight.inputMotionVectorsGlTexture : null,
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
                    new int[]{Math.toIntExact(inFlight.inputDepthGlTexture.handle())},
                    new int[0],
                    new int[]{GL_LAYOUT_SHADER_READ_ONLY_EXT}
            );
            inFlight.captureDepthPending = true;
        }
        if (borrowedMotionVectors) {
            inFlight.captureMotionReady.signalVulkan(
                    new int[]{Math.toIntExact(inFlight.inputMotionVectorsGlTexture.handle())},
                    new int[0],
                    new int[]{GL_LAYOUT_SHADER_READ_ONLY_EXT}
            );
            inFlight.captureMotionPending = true;
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
        public static FrameData from(DispatchResource dispatchResource) {
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
                    dispatchResource.jitterOffset(),
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
        public GlImportableTexture2D inputColorGlTexture;
        public VulkanTexture inputColorVkTexture;

        public GlImportableTexture2D inputDepthGlTexture;
        public VulkanTexture inputDepthVkTexture;

        public GlImportableTexture2D inputMotionVectorsGlTexture;
        public VulkanTexture inputMotionVectorsVkTexture;

        public GlImportableTexture2D inputExposureGlTexture;
        public VulkanTexture inputExposureVkTexture;

        public GlImportableTexture2D outputColorGlTexture;
        public VulkanTexture outputColorVkTexture;

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

        public void destroy() {
            awaitCaptureRelease();
            if (inputColorGlTexture != null) {
                inputColorGlTexture.destroy();
            }
            if (inputColorVkTexture != null) {
                inputColorVkTexture.destroy();
            }
            if (inputDepthGlTexture != null) {
                inputDepthGlTexture.destroy();
            }
            if (inputDepthVkTexture != null) {
                inputDepthVkTexture.destroy();
            }
            if (outputColorGlTexture != null) {
                outputColorGlTexture.destroy();
            }
            if (outputColorVkTexture != null) {
                outputColorVkTexture.destroy();
            }
            if (inputMotionVectorsGlTexture != null) {
                inputMotionVectorsGlTexture.destroy();
            }
            if (inputMotionVectorsVkTexture != null) {
                inputMotionVectorsVkTexture.destroy();
            }

            if (inputExposureGlTexture != null) {
                inputExposureGlTexture.destroy();
            }
            if (inputExposureVkTexture != null) {
                inputExposureVkTexture.destroy();
            }

            if (flippedOutputGlTexture != null) {
                flippedOutputGlTexture.destroy();
            }

            if (outputFrameBuffer != null) {
                outputFrameBuffer.destroy();
            }

            if (glFinish != null) {
                glFinish.destroy();
            }

            if (upscaleVkFinish != null) {
                upscaleVkFinish.destroy();
            }
            if (captureDepthReady != null) {
                captureDepthReady.destroy();
            }
            if (captureDepthRelease != null) {
                captureDepthRelease.destroy();
            }
            if (captureMotionReady != null) {
                captureMotionReady.destroy();
            }
            if (captureMotionRelease != null) {
                captureMotionRelease.destroy();
            }
        }

        public void initialize() {
            VulkanDevice vkDevice = RenderSystems.vulkan().device();
            GlDevice glDevice = RenderSystems.opengl().device();
            vkDevice.getMainQueue().waitIdle();
            this.inputColorVkTexture = vkDevice.createTextureExportable(
                    TextureDescription.create()
                            .usages(TextureUsages.create().sampler().storage().transferSource())
                            .format(SuperResolutionConfig.getInternalTextureFormat())
                            .type(TextureType.Texture2D)
                            .width(RenderHandlerManager.getRenderWidth())
                            .height(RenderHandlerManager.getRenderHeight())
                            .label("SRUpscaleInputColorVkTexture-%s".formatted(index))
                            .build()
            );
            this.inputColorGlTexture = glDevice.createTextureImportable(this.inputColorVkTexture);

            this.inputDepthVkTexture = vkDevice.createTextureExportable(
                    TextureDescription.create()
                            .usages(TextureUsages.create().sampler().storage().transferSource())
                            .format(TextureFormat.R32F)
                            .type(TextureType.Texture2D)
                            .width(RenderHandlerManager.getRenderWidth())
                            .height(RenderHandlerManager.getRenderHeight())
                            .label("SRUpscaleInputDepthVkTexture-%s".formatted(index))
                            .build()
            );
            this.inputDepthGlTexture = glDevice.createTextureImportable(this.inputDepthVkTexture);

            this.inputMotionVectorsVkTexture = vkDevice.createTextureExportable(
                    TextureDescription.create()
                            .usages(TextureUsages.create().sampler().storage().transferSource())
                            .format(TextureFormat.RG16F)
                            .type(TextureType.Texture2D)
                            .width(RenderHandlerManager.getRenderWidth())
                            .height(RenderHandlerManager.getRenderHeight())
                            .label("SRUpscaleInputMotionVectorsVkTexture-%s".formatted(index))
                            .build()
            );
            this.inputMotionVectorsGlTexture = glDevice.createTextureImportable(this.inputMotionVectorsVkTexture);

            this.inputExposureVkTexture = vkDevice.createTextureExportable(
                    TextureDescription.create()
                            .usages(TextureUsages.create().sampler().storage().transferSource())
                            .format(TextureFormat.R16F)
                            .type(TextureType.Texture2D)
                            .width(1)
                            .height(1)
                            .label("SRUpscaleInputExposureVkTexture-%s".formatted(index))
                            .build()
            );
            this.inputExposureGlTexture = glDevice.createTextureImportable(this.inputExposureVkTexture);

            this.outputColorVkTexture = vkDevice.createTextureExportable(
                    TextureDescription.create()
                            .type(TextureType.Texture2D)
                            .usages(TextureUsages.create().sampler().storage().transferDestination())
                            .format(SuperResolutionConfig.getInternalTextureFormat())
                            .width(RenderHandlerManager.getScreenWidth())
                            .height(RenderHandlerManager.getScreenHeight())
                            .label("SRUpscaleOutputColorVkTexture-%s".formatted(index))
                            .build()
            );
            this.outputColorGlTexture = glDevice.createTextureImportable(this.outputColorVkTexture);

            this.flippedOutputGlTexture = (GlTexture2D) glDevice.createTexture(
                    TextureDescription.create()
                            .type(TextureType.Texture2D)
                            .usages(TextureUsages.create().sampler().storage().transferDestination())
                            .format(SuperResolutionConfig.getInternalTextureFormat())
                            .width(RenderHandlerManager.getScreenWidth())
                            .height(RenderHandlerManager.getScreenHeight())
                            .label("SRUpscaleFlippedOutputGlTexture-%s".formatted(index))
                            .build()
            );

            this.outputFrameBuffer = RenderSystems.current().device().createFramebuffer(
                    FramebufferDescription.create()
                            .colorAttachment(this.flippedOutputGlTexture)
                            .build());

            this.glFinish = VkGlInteropSemaphore.create(vkDevice);
            this.upscaleVkFinish = VkGlInteropSemaphore.create(vkDevice);
            if (VulkanPresentationFeature.isRequested()) {
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
            if (captureDepthPending && captureDepthRelease != null && inputDepthGlTexture != null) {
                captureDepthRelease.waitVulkanSignal(
                        new int[]{Math.toIntExact(inputDepthGlTexture.handle())},
                        new int[0],
                        new int[]{GL_LAYOUT_TRANSFER_DST_EXT}
                );
                captureDepthPending = false;
            }
            if (captureMotionPending && captureMotionRelease != null && inputMotionVectorsGlTexture != null) {
                captureMotionRelease.waitVulkanSignal(
                        new int[]{Math.toIntExact(inputMotionVectorsGlTexture.handle())},
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
