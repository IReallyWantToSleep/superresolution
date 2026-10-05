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
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.InteropResourcesPreprocessor;
import io.homo.superresolution.common.workmode.SRWorkModeManager;
import io.homo.superresolution.core.NativeLibManager;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.d3d12.*;
import io.homo.superresolution.core.graphics.impl.command.CommandBufferBehavior;
import io.homo.superresolution.core.graphics.impl.command.CommandPoolFlags;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsages;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBufferAttachment;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;

import java.util.Objects;

import static org.lwjgl.opengl.EXTSemaphore.GL_LAYOUT_GENERAL_EXT;
import static org.lwjgl.opengl.EXTSemaphore.GL_LAYOUT_SHADER_READ_ONLY_EXT;

public abstract class GlD3D12InteropAlgorithm<U> extends AbstractAlgorithm {
    private static final int[] GL_HANDOFF_LAYOUTS = {
            GL_LAYOUT_SHADER_READ_ONLY_EXT,
            GL_LAYOUT_SHADER_READ_ONLY_EXT,
            GL_LAYOUT_SHADER_READ_ONLY_EXT,
            GL_LAYOUT_SHADER_READ_ONLY_EXT,
            GL_LAYOUT_GENERAL_EXT
    };

    private D3D12InteropResources interopResources;
    private boolean initialized;
    private boolean destroyed;
    private int builtRenderWidth = -1;
    private int builtRenderHeight = -1;
    private int builtScreenWidth = -1;
    private int builtScreenHeight = -1;

    private static boolean hasRequiredInputs(InputResourceSet resources) {
        return resources != null
                && resources.has(InputResourceType.Color)
                && resources.has(InputResourceType.Depth)
                && resources.has(InputResourceType.MotionVectors);
    }

    protected abstract void createD3D12Upscaler(
            InitializationDescription desc,
            D3D12InteropResources resources);

    protected abstract void destroyD3D12Upscaler();

    protected abstract void dispatchUpscale(
            D3D12CommandBuffer commandBuffer,
            D3D12InteropResources resources,
            DispatchResource dispatchResource);

    @Override
    public final void initialize(InitializationDescription desc) {
        if (initialized || interopResources != null) {
            throw new IllegalStateException(
                    "The OpenGL/D3D12 interop algorithm still owns resources.");
        }
        if (!NativeLibManager.d3d12Available()) {
            throw new IllegalStateException(
                    "The optional D3D12 native library is unavailable.");
        }

        this.initDesc = Objects.requireNonNull(desc, "desc");
        destroyed = false;
        invalidateHistory();

        createResources(
                RenderHandlerManager.getRenderWidth(),
                RenderHandlerManager.getRenderHeight(),
                RenderHandlerManager.getScreenWidth(),
                RenderHandlerManager.getScreenHeight());
        createD3D12Upscaler(initDesc, interopResources);
        initialized = true;
    }

    @Override
    public final boolean dispatch(DispatchResource dispatchResource) {
        super.dispatch(Objects.requireNonNull(dispatchResource, "dispatchResource"));
        ensureInitialized();

        if (!hasRequiredInputs(dispatchResource.resources())) {
            return false;
        }
        if (!interopResources.matches(dispatchResource)) {
            rebuildResources(
                    dispatchResource.renderWidth(),
                    dispatchResource.renderHeight(),
                    dispatchResource.screenWidth(),
                    dispatchResource.screenHeight());
        }

        D3D12InteropResources resources = interopResources;
        InteropResourcesPreprocessor.processInputTextures(
                dispatchResource.resources().get(InputResourceType.Color),
                resources.inputColorGl(),
                dispatchResource.resources().get(InputResourceType.Depth),
                resources.inputDepthGl(),
                dispatchResource.resources().get(InputResourceType.MotionVectors),
                resources.inputMotionVectorsGl(),
                dispatchResource.resources().get(InputResourceType.Exposure),
                resources.inputExposureGl(),
                SRWorkModeManager.getCurrentState()
                        .motionVectorPreprocessingFunction());

        dispatchFrame(resources, dispatchResource);
        InteropResourcesPreprocessor.flipY(
                resources.outputColorGl(),
                resources.flippedOutput());
        return true;
    }

    @Override
    public final void destroy() {
        if (destroyed && interopResources == null) {
            return;
        }

        initialized = false;
        awaitResourceUsers();
        destroyD3D12Upscaler();
        destroyResources();
        destroyed = true;
    }

    @Override
    public final void resize(int width, int height) {
        ensureInitialized();
        if (RenderHandlerManager.getRenderWidth() == builtRenderWidth
                && RenderHandlerManager.getRenderHeight() == builtRenderHeight
                && RenderHandlerManager.getScreenWidth() == builtScreenWidth
                && RenderHandlerManager.getScreenHeight() == builtScreenHeight) {
            return;
        }

        rebuildResources(
                RenderHandlerManager.getRenderWidth(),
                RenderHandlerManager.getRenderHeight(),
                RenderHandlerManager.getScreenWidth(),
                RenderHandlerManager.getScreenHeight());
    }

    @Override
    public final IFrameBuffer getOutputFrameBuffer() {
        return interopResources == null
                ? null
                : interopResources.outputFramebuffer();
    }

    @Override
    public final int getOutputTextureId() {
        return interopResources == null
                ? 0
                : Math.toIntExact(interopResources.flippedOutput().handle());
    }

    private void ensureInitialized() {
        if (!initialized || destroyed || interopResources == null) {
            throw new IllegalStateException(
                    "The OpenGL/D3D12 interop algorithm is not initialized.");
        }
    }

    private void rebuildResources(
            int renderWidth,
            int renderHeight,
            int screenWidth,
            int screenHeight) {
        initialized = false;
        awaitResourceUsers();
        destroyD3D12Upscaler();
        destroyResources();

        createResources(renderWidth, renderHeight, screenWidth, screenHeight);
        createD3D12Upscaler(initDesc, interopResources);
        invalidateHistory();
        initialized = true;
    }

    private void createResources(
            int renderWidth,
            int renderHeight,
            int screenWidth,
            int screenHeight) {
        builtRenderWidth = -1;
        builtRenderHeight = -1;
        builtScreenWidth = -1;
        builtScreenHeight = -1;

        D3D12InteropResources resources = new D3D12InteropResources(
                renderWidth,
                renderHeight,
                screenWidth,
                screenHeight);
        interopResources = resources;
        resources.initialize();

        builtRenderWidth = renderWidth;
        builtRenderHeight = renderHeight;
        builtScreenWidth = screenWidth;
        builtScreenHeight = screenHeight;
    }

    private void destroyResources() {
        if (interopResources == null) {
            return;
        }
        interopResources.destroy();
        interopResources = null;
        builtRenderWidth = -1;
        builtRenderHeight = -1;
        builtScreenWidth = -1;
        builtScreenHeight = -1;
    }

    private void awaitResourceUsers() {
        if (interopResources == null) {
            return;
        }
        RenderSystems.opengl().finish();
        if (interopResources.device() != null) {
            interopResources.device().waitIdle();
        }
    }

    private void dispatchFrame(
            D3D12InteropResources resources,
            DispatchResource dispatchResource) {
        long openGlReadyValue = resources.reserveFenceValue();
        long d3d12DoneValue = resources.reserveFenceValue();
        boolean openGlReleased = false;
        boolean submitAttempted = false;

        try {
            resources.signalOpenGl(openGlReadyValue);
            openGlReleased = true;
            resources.assumeOpenGlHandoffStates();

            D3D12CommandBuffer commandBuffer = resources.beginCommandBuffer();
            dispatchUpscale(
                    commandBuffer,
                    resources,
                    dispatchResource);
            commandBuffer.end();

            submitAttempted = true;
            resources.submit(commandBuffer, openGlReadyValue, d3d12DoneValue);
        } catch (RuntimeException | Error failure) {
            if (openGlReleased) {
                boolean fenceRecovered = !submitAttempted;
                if (!submitAttempted) {
                    resources.recoverSharedFence(
                            openGlReadyValue,
                            d3d12DoneValue);
                } else {
                    fenceRecovered = resources.completedFenceValue() >= d3d12DoneValue;
                }
                if (fenceRecovered) {
                    resources.waitForOpenGlFenceOnly(d3d12DoneValue);
                }
            }
            throw failure;
        }

        resources.waitForOpenGl(d3d12DoneValue);
    }

    protected static final class D3D12InteropResources {
        private final int renderWidth;
        private final int renderHeight;
        private final int screenWidth;
        private final int screenHeight;

        private D3D12Device device;
        private D3D12Queue queue;
        private D3D12Device.ExternalBorrowLease deviceBorrow;
        private D3D12Fence fence;
        private D3D12CommandPool commandPool;
        private D3D12CommandBuffer commandBuffer;
        private D3D12InteropSemaphore semaphore;

        private D3D12Texture2D inputColor;
        private D3D12Texture2D inputDepth;
        private D3D12Texture2D inputMotionVectors;
        private D3D12Texture2D inputExposure;
        private D3D12Texture2D outputColor;

        private GlD3D12ImportableTexture2D inputColorGl;
        private GlD3D12ImportableTexture2D inputDepthGl;
        private GlD3D12ImportableTexture2D inputMotionVectorsGl;
        private GlD3D12ImportableTexture2D inputExposureGl;
        private GlD3D12ImportableTexture2D outputColorGl;

        private GlTexture2D flippedOutput;
        private IFrameBuffer outputFramebuffer;
        private int[] sharedTextureIds;

        private D3D12InteropResources(
                int renderWidth,
                int renderHeight,
                int screenWidth,
                int screenHeight) {
            if (renderWidth < 1 || renderHeight < 1
                    || screenWidth < 1 || screenHeight < 1) {
                throw new IllegalArgumentException(
                        "D3D12 interop dimensions must be positive.");
            }
            this.renderWidth = renderWidth;
            this.renderHeight = renderHeight;
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
        }

        private static TextureDescription sharedTextureDescription(
                int width,
                int height,
                TextureFormat format,
                String label) {
            return TextureDescription.create()
                    .type(TextureType.Texture2D)
                    .width(width)
                    .height(height)
                    .format(format)
                    .usages(TextureUsages.create().sampler().storage())
                    .label(label)
                    .build();
        }

        public int renderWidth() {
            return renderWidth;
        }

        public int renderHeight() {
            return renderHeight;
        }

        public int screenWidth() {
            return screenWidth;
        }

        public int screenHeight() {
            return screenHeight;
        }

        public D3D12Device device() {
            return device;
        }

        public D3D12Texture2D inputColor() {
            return inputColor;
        }

        public D3D12Texture2D inputDepth() {
            return inputDepth;
        }

        public D3D12Texture2D inputMotionVectors() {
            return inputMotionVectors;
        }

        public D3D12Texture2D inputExposure() {
            return inputExposure;
        }

        public D3D12Texture2D outputColor() {
            return outputColor;
        }

        public GlD3D12ImportableTexture2D inputColorGl() {
            return inputColorGl;
        }

        public GlD3D12ImportableTexture2D inputDepthGl() {
            return inputDepthGl;
        }

        public GlD3D12ImportableTexture2D inputMotionVectorsGl() {
            return inputMotionVectorsGl;
        }

        public GlD3D12ImportableTexture2D inputExposureGl() {
            return inputExposureGl;
        }

        public GlD3D12ImportableTexture2D outputColorGl() {
            return outputColorGl;
        }

        public GlTexture2D flippedOutput() {
            return flippedOutput;
        }

        public IFrameBuffer outputFramebuffer() {
            return outputFramebuffer;
        }

        private boolean matches(DispatchResource dispatchResource) {
            return renderWidth == dispatchResource.renderWidth()
                    && renderHeight == dispatchResource.renderHeight()
                    && screenWidth == dispatchResource.screenWidth()
                    && screenHeight == dispatchResource.screenHeight();
        }

        private void initialize() {
            D3D12OpenGlInterop.requireExtensions();
            device = RenderSystems.d3d12().device();
            queue = device.directQueue();
            deviceBorrow = device.borrowExternal();

            fence = device.createFence(0);
            commandPool = device.createCommandPool(CommandPoolFlags.Reset);
            commandBuffer = commandPool.createCommandBuffer(
                    CommandBufferBehavior.ReusableSequential);
            semaphore = new D3D12InteropSemaphore(fence);
            semaphore.initializeImport();

            TextureFormat colorFormat =
                    SuperResolutionConfig.getInternalTextureFormat();
            inputColor = device.createSharedTexture2D(
                    sharedTextureDescription(
                            renderWidth,
                            renderHeight,
                            colorFormat,
                            "D3D12InputColor"),
                    D3D12ResourceState.COMMON);
            inputDepth = device.createSharedTexture2D(
                    sharedTextureDescription(
                            renderWidth,
                            renderHeight,
                            TextureFormat.R32F,
                            "D3D12InputDepth"),
                    D3D12ResourceState.COMMON);
            inputMotionVectors = device.createSharedTexture2D(
                    sharedTextureDescription(
                            renderWidth,
                            renderHeight,
                            TextureFormat.RG16F,
                            "D3D12InputMotionVectors"),
                    D3D12ResourceState.COMMON);
            inputExposure = device.createSharedTexture2D(
                    sharedTextureDescription(
                            1,
                            1,
                            TextureFormat.R32F,
                            "D3D12InputExposure"),
                    D3D12ResourceState.COMMON);
            outputColor = device.createSharedTexture2D(
                    sharedTextureDescription(
                            screenWidth,
                            screenHeight,
                            colorFormat,
                            "D3D12OutputColor"),
                    D3D12ResourceState.COMMON);

            inputColorGl = new GlD3D12ImportableTexture2D(inputColor);
            inputColorGl.initializeImport();
            inputDepthGl = new GlD3D12ImportableTexture2D(inputDepth);
            inputDepthGl.initializeImport();
            inputMotionVectorsGl = new GlD3D12ImportableTexture2D(inputMotionVectors);
            inputMotionVectorsGl.initializeImport();
            inputExposureGl = new GlD3D12ImportableTexture2D(inputExposure);
            inputExposureGl.initializeImport();
            outputColorGl = new GlD3D12ImportableTexture2D(outputColor);
            outputColorGl.initializeImport();

            flippedOutput = new OwnedGlTexture2D(
                    TextureDescription.create()
                            .type(TextureType.Texture2D)
                            .usages(TextureUsages.create()
                                    .sampler()
                                    .storage())
                            .format(colorFormat)
                            .width(screenWidth)
                            .height(screenHeight)
                            .label("D3D12UpscaleFlippedOutput")
                            .build());
            ((OwnedGlTexture2D) flippedOutput).initializeOwned();

            GlFrameBuffer framebuffer = new GlFrameBuffer();
            outputFramebuffer = framebuffer;
            framebuffer.addAttachment(new GlFrameBufferAttachment(
                    GlFrameBufferAttachment.FrameBufferAttachmentType.COLOR,
                    flippedOutput));
            framebuffer.validate();
            framebuffer.label("D3D12UpscaleOutputFramebuffer");

            sharedTextureIds = new int[]{
                    Math.toIntExact(inputColorGl.handle()),
                    Math.toIntExact(inputDepthGl.handle()),
                    Math.toIntExact(inputMotionVectorsGl.handle()),
                    Math.toIntExact(inputExposureGl.handle()),
                    Math.toIntExact(outputColorGl.handle())
            };
        }

        private void destroy() {
            if (outputFramebuffer != null) {
                outputFramebuffer.destroy();
                outputFramebuffer = null;
            }
            if (flippedOutput != null) {
                flippedOutput.destroy();
                flippedOutput = null;
            }
            if (outputColorGl != null) {
                outputColorGl.destroy();
                outputColorGl = null;
            }
            if (inputExposureGl != null) {
                inputExposureGl.destroy();
                inputExposureGl = null;
            }
            if (inputMotionVectorsGl != null) {
                inputMotionVectorsGl.destroy();
                inputMotionVectorsGl = null;
            }
            if (inputDepthGl != null) {
                inputDepthGl.destroy();
                inputDepthGl = null;
            }
            if (inputColorGl != null) {
                inputColorGl.destroy();
                inputColorGl = null;
            }
            if (outputColor != null) {
                outputColor.destroy();
                outputColor = null;
            }
            if (inputExposure != null) {
                inputExposure.destroy();
                inputExposure = null;
            }
            if (inputMotionVectors != null) {
                inputMotionVectors.destroy();
                inputMotionVectors = null;
            }
            if (inputDepth != null) {
                inputDepth.destroy();
                inputDepth = null;
            }
            if (inputColor != null) {
                inputColor.destroy();
                inputColor = null;
            }
            sharedTextureIds = null;

            if (semaphore != null) {
                semaphore.close();
                semaphore = null;
            }
            if (commandBuffer != null) {
                commandBuffer.destroy();
                commandBuffer = null;
            }
            if (commandPool != null) {
                commandPool.destroy();
                commandPool = null;
            }
            if (fence != null) {
                fence.destroy();
                fence = null;
            }
            if (deviceBorrow != null) {
                deviceBorrow.close();
                deviceBorrow = null;
            }
            device = null;
            queue = null;
        }

        private long reserveFenceValue() {
            return fence.reserveValue();
        }

        private long completedFenceValue() {
            return fence.completedValue();
        }

        private void signalOpenGl(long fenceValue) {
            semaphore.signal(fenceValue, sharedTextureIds, GL_HANDOFF_LAYOUTS);
        }

        private void waitForOpenGl(long fenceValue) {
            semaphore.waitFor(fenceValue, sharedTextureIds, GL_HANDOFF_LAYOUTS);
        }

        private void waitForOpenGlFenceOnly(long fenceValue) {
            semaphore.waitForFenceOnly(fenceValue);
        }

        private void recoverSharedFence(long waitValue, long signalValue) {
            queue.recoverSharedFence(fence, waitValue, signalValue);
        }

        private void submit(
                D3D12CommandBuffer commandBuffer,
                long waitValue,
                long signalValue) {
            queue.submit(commandBuffer, fence, waitValue, signalValue);
        }

        private D3D12CommandBuffer beginCommandBuffer() {
            commandBuffer.waitForFence();
            commandBuffer.begin();
            return commandBuffer;
        }

        private void assumeOpenGlHandoffStates() {
            inputColor.assumeCommittedState(D3D12ResourceState.COMPUTE_READ);
            inputDepth.assumeCommittedState(D3D12ResourceState.COMPUTE_READ);
            inputMotionVectors.assumeCommittedState(
                    D3D12ResourceState.COMPUTE_READ);
            inputExposure.assumeCommittedState(
                    D3D12ResourceState.COMPUTE_READ);
            outputColor.assumeCommittedState(D3D12ResourceState.COMMON);
        }
    }

    private static final class OwnedGlTexture2D extends GlTexture2D {
        private OwnedGlTexture2D(TextureDescription description) {
            super(description);
        }

        private void initializeOwned() {
            configureMipmap();
            initializeTexture();
        }
    }
}
