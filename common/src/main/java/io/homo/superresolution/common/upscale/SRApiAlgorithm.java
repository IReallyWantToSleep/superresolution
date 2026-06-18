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
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.config.enums.InteropSyncMode;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
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
import io.homo.superresolution.shadercompat.IrisShaderCompatUtils;
import io.homo.superresolution.srapi.SRUpscaleContext;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import static org.lwjgl.opengl.EXTSemaphore.GL_LAYOUT_GENERAL_EXT;
import static org.lwjgl.opengl.EXTSemaphore.GL_LAYOUT_SHADER_READ_ONLY_EXT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_ALL_COMMANDS_BIT;

public abstract class SRApiAlgorithm extends AbstractAlgorithm {
    public static final int INITIAL_COMMAND_BUFFER_RING_SIZE = 5;
    public static final int MAX_IN_FLIGHT_FRAME = 3;
    private final VulkanCommandBufferRing commandBufferRing = new VulkanCommandBufferRing(
            INITIAL_COMMAND_BUFFER_RING_SIZE);
    protected SRUpscaleContext context;
    protected InFlightFrameResourcesSet[] inFlightFrames = new InFlightFrameResourcesSet[MAX_IN_FLIGHT_FRAME];

    protected boolean syncSerialMode;

    //部分模组会暴力ci.cancel掉世界渲染
    //这时SR的frameCount依然增加
    //造成在三缓模式下的同步全部乱套
    //所以我们需要自己维护一个frameCount
    protected int frameCount = 0;

    // Resolution the resources/context were last built at, to skip redundant resize() rebuilds
    // (Iris/forceResize call resize() on every pipeline reload even when nothing changed).
    private int builtRenderWidth = -1;
    private int builtRenderHeight = -1;
    private int builtScreenWidth = -1;
    private int builtScreenHeight = -1;

    protected abstract void recreateSRApiContext(InitializationDescription desc);

    protected abstract void destroySRApiContext();

    protected abstract void dispatchSRApiContext(
            VulkanCommandBuffer commandBuffer,
            InFlightFrameResourcesSet inFlightFrameResourcesSet
    );

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
        recreateSRApiContext(desc);
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        super.dispatch(dispatchResource);

        if (context == null || context.nativePtr < 1) {
            return false;
        }
        frameCount++;
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
            glFinishSemaphore.signalOpenGL(
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

            VulkanDevice vulkanDevice = RenderSystems.vulkan().device();
            inFlight.frameData = FrameData.from(dispatchResource);

            VulkanCommandBuffer commandBuffer = commandBufferRing.acquire(vulkanDevice);
            // 构建第N-1帧的Cmdbuf
            commandBuffer.begin();
            dispatchSRApiContext(
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

            upscaleFinishSemaphore.waitOpenGL(
                    new int[]{Math.toIntExact(inFlight.outputColorGlTexture.handle())},
                    new int[]{},
                    new int[]{GL_LAYOUT_GENERAL_EXT}
            );
            InteropResourcesConverter.flipY(
                    inFlight.outputColorGlTexture,
                    inFlight.flippedOutputGlTexture);
        } else {
            int currentFrameIndex = frameCount;
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

                glFinishSemaphore.signalOpenGL(
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
                    dispatchSRApiContext(
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
                // does NOT reset frameCount, so for the first frames after a resize this stage's index is
                // still > 2 while the slot was never upscaled; waiting on its never-signaled binary
                // semaphore blocks the GL queue and deadlocks (the HighPerformance freeze during world
                // load, where resize() fires repeatedly). A later recreateAlgorithm (new instance,
                // frameCount = 0) re-primes and briefly unblocks it -- hence the freeze/render/freeze cycle.
                if (inFlight.commandBuffer != null) {
                    inFlight.commandBuffer.waitForFence();

                    //GL Queue等待第N-2帧的Upscale结果
                    upscaleFinishSemaphore.waitOpenGL(
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
        destroyResources();
        destroySRApiContext();
        context = null;
    }

    @Override
    public void resize(int width, int height) {
        // Skip the expensive teardown+rebuild (DLSS/NGX context + interop resources) when the
        // resolution is unchanged. Iris/forceResize fire resize() on every pipeline reload during world
        // load even at constant resolution; rebuilding each time is a multi-second recreate-storm, worst
        // in HighPerformance mode (its waitIdle() also blocks on the async pipeline).
        if (context != null
                && RenderHandlerManager.getRenderWidth() == builtRenderWidth
                && RenderHandlerManager.getRenderHeight() == builtRenderHeight
                && RenderHandlerManager.getScreenWidth() == builtScreenWidth
                && RenderHandlerManager.getScreenHeight() == builtScreenHeight) {
            return;
        }
        RenderSystems.vulkan().device().getMainQueue().waitIdle();
        commandBufferRing.destroy();

        destroyResources();
        destroySRApiContext();

        recreateSRApiContext(this.initDesc);
        createResources();
    }

    @Override
    public IFrameBuffer getOutputFrameBuffer() {
        if (syncSerialMode) {
            return inFlightFrames[0].outputFrameBuffer;
        }
        int currentFrameIndex = frameCount;
        int finishedIndex = (((currentFrameIndex - 2) % MAX_IN_FLIGHT_FRAME) + MAX_IN_FLIGHT_FRAME) % MAX_IN_FLIGHT_FRAME;
        return inFlightFrames[finishedIndex].outputFrameBuffer;
    }

    @Override
    public int getOutputTextureId() {
        if (syncSerialMode) {
            return Math.toIntExact(inFlightFrames[0].outputColorGlTexture.handle());
        }
        int currentFrameIndex = frameCount;
        int finishedIndex = (((currentFrameIndex - 2) % MAX_IN_FLIGHT_FRAME) + MAX_IN_FLIGHT_FRAME) % MAX_IN_FLIGHT_FRAME;
        GlImportableTexture2D outputColorGlTexture = inFlightFrames[finishedIndex].outputColorGlTexture;
        return Math.toIntExact(outputColorGlTexture.handle());
    }

    private void processInputResources(InFlightFrameResourcesSet inFlight, DispatchResource dispatchResource) {

        String motionVectorPreprocessingFunction = null;
        if (IrisShaderCompatUtils.shouldApplySuperResolutionChanges()) {
            var config = IrisShaderCompatUtils.getCurrentConfig();
            if (config.isPresent()) {
                var customs = config.get().upscale.customs;
                if (customs != null) {
                    motionVectorPreprocessingFunction = customs.motionVectorPreprocessingFunction;
                }
            }
        }

        InteropResourcesConverter.processInputTextures(
                dispatchResource.resources().colorTexture(), inFlight.inputColorGlTexture,
                dispatchResource.resources().depthTexture(), inFlight.inputDepthGlTexture,
                dispatchResource.resources().motionVectorsTexture(), inFlight.inputMotionVectorsGlTexture,
                dispatchResource.resources().exposureTexture(), inFlight.inputExposureGlTexture,
                motionVectorPreprocessingFunction
        );
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
        public FrameData frameData;
        public VulkanCommandBuffer commandBuffer;
        public long fence;

        protected int index;

        public void destroy() {
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
        }

        public void initialize() {
            VulkanDevice vkDevice = RenderSystems.vulkan().device();
            GlDevice glDevice = RenderSystems.opengl().device();
            vkDevice.getMainQueue().waitIdle();
            this.inputColorVkTexture = vkDevice.createTextureExportable(
                    TextureDescription.create()
                            .usages(TextureUsages.create().sampler().storage())
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
                            .usages(TextureUsages.create().sampler().storage())
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
                            .usages(TextureUsages.create().sampler().storage())
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
                            .usages(TextureUsages.create().sampler().storage())
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
        }
    }
}
