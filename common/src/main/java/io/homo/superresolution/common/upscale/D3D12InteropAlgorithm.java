/*
 * Super Resolution
 * Copyright (c) 2026. Xiang Keshen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.upscale;

import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.api.InitializationDescription;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.workmode.SRWorkModeManager;
import io.homo.superresolution.core.NativeLibManager;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.d3d12.D3D12InteropContext;
import io.homo.superresolution.core.graphics.d3d12.D3D12InteropSemaphore;
import io.homo.superresolution.core.graphics.d3d12.GlD3D12ImportableTexture2D;
import io.homo.superresolution.core.graphics.impl.framebuffer.FramebufferDescription;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsages;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;

import java.util.Objects;

import static org.lwjgl.opengl.EXTSemaphore.GL_LAYOUT_GENERAL_EXT;
import static org.lwjgl.opengl.EXTSemaphore.GL_LAYOUT_SHADER_READ_ONLY_EXT;

/**
 * Low-latency OpenGL/Direct3D 12 interop path.
 *
 * <p>Every interop texture, semaphore, output framebuffer, and provider
 * context belongs to one immutable-size resource generation. Resize creates a
 * complete replacement generation before publishing it; dispatch therefore
 * cannot combine a new frame size with resources from an older size.</p>
 *
 * <p>D3D12 owns the shared committed resources and a shared timeline fence.
 * OpenGL imports those objects, writes the preprocessed inputs, signals the
 * fence, waits for the D3D12 dispatch, and then copies the vertically flipped
 * output into a regular OpenGL texture.</p>
 */
public abstract class D3D12InteropAlgorithm<U> extends AbstractAlgorithm {
    private enum LifecycleState {
        NEW,
        READY,
        RESIZE_PENDING,
        REBUILDING,
        DESTROYED
    }

    /**
     * The dimensions bound to one resource generation.
     *
     * <p>The screen dimensions are sampled once and the render dimensions are
     * derived from that same snapshot. This avoids constructing a generation
     * from values observed on opposite sides of a window resize.</p>
     */
    protected record InteropSize(
            int renderWidth,
            int renderHeight,
            int screenWidth,
            int screenHeight) {
        public InteropSize {
            if (renderWidth < 1 || renderHeight < 1 ||
                    screenWidth < 1 || screenHeight < 1) {
                throw new IllegalArgumentException(
                        "Interop dimensions must be positive");
            }
        }

        private static InteropSize capture() {
            return fromScreenSize(
                    RenderHandlerManager.getScreenWidth(),
                    RenderHandlerManager.getScreenHeight());
        }

        private static InteropSize fromScreenSize(int width, int height) {
            int screenWidth = Math.max(width, 32);
            int screenHeight = Math.max(height, 32);
            float scaleFactor = RenderHandlerManager.getScaleFactor();
            return new InteropSize(
                    (int) Math.max(screenWidth * scaleFactor, 32),
                    (int) Math.max(screenHeight * scaleFactor, 32),
                    screenWidth,
                    screenHeight);
        }

        private boolean matches(DispatchResource dispatchResource) {
            return dispatchResource.renderWidth() == renderWidth &&
                    dispatchResource.renderHeight() == renderHeight &&
                    dispatchResource.screenWidth() == screenWidth &&
                    dispatchResource.screenHeight() == screenHeight;
        }
    }

    private static final class InteropResources {
        private final InteropSize size;
        private final D3D12InteropContext context;
        private final GlD3D12ImportableTexture2D inputColor;
        private final GlD3D12ImportableTexture2D inputDepth;
        private final GlD3D12ImportableTexture2D inputMotionVectors;
        private final GlD3D12ImportableTexture2D inputExposure;
        private final GlD3D12ImportableTexture2D outputColor;
        private final D3D12InteropSemaphore semaphore;
        private final GlTexture2D flippedOutput;
        private final IFrameBuffer outputFramebuffer;
        private final int[] sharedTextureIds;

        private InteropResources(
                InteropSize size,
                D3D12InteropContext context,
                GlD3D12ImportableTexture2D inputColor,
                GlD3D12ImportableTexture2D inputDepth,
                GlD3D12ImportableTexture2D inputMotionVectors,
                GlD3D12ImportableTexture2D inputExposure,
                GlD3D12ImportableTexture2D outputColor,
                D3D12InteropSemaphore semaphore,
                GlTexture2D flippedOutput,
                IFrameBuffer outputFramebuffer) {
            this.size = size;
            this.context = context;
            this.inputColor = inputColor;
            this.inputDepth = inputDepth;
            this.inputMotionVectors = inputMotionVectors;
            this.inputExposure = inputExposure;
            this.outputColor = outputColor;
            this.semaphore = semaphore;
            this.flippedOutput = flippedOutput;
            this.outputFramebuffer = outputFramebuffer;
            this.sharedTextureIds = new int[]{
                    Math.toIntExact(inputColor.handle()),
                    Math.toIntExact(inputDepth.handle()),
                    Math.toIntExact(inputMotionVectors.handle()),
                    Math.toIntExact(inputExposure.handle()),
                    Math.toIntExact(outputColor.handle())
            };
        }
    }

    private record Generation<U>(InteropResources resources, U upscaler) {
    }

    private Generation<U> activeGeneration;
    private LifecycleState lifecycleState = LifecycleState.NEW;
    private boolean resizeMismatchLogged;

    protected abstract U createD3D12Upscaler(
            InitializationDescription desc,
            D3D12InteropContext interop,
            InteropSize size);

    protected abstract void destroyD3D12Upscaler(U upscaler);

    protected abstract boolean dispatchD3D12Upscale(
            U upscaler,
            D3D12InteropContext interop,
            long commandList,
            DispatchResource dispatchResource);

    protected boolean isD3D12UpscalerReady(U upscaler) {
        return true;
    }

    @Override
    public void initialize(InitializationDescription desc) {
        if (!NativeLibManager.d3d12InteropAvailable()) {
            throw new IllegalStateException(
                    "The optional D3D12 interop native library is unavailable.");
        }
        this.initDesc = desc;
        lifecycleState = LifecycleState.REBUILDING;
        try {
            activeGeneration = createGeneration(InteropSize.capture());
            lifecycleState = LifecycleState.READY;
        } catch (Throwable throwable) {
            lifecycleState = LifecycleState.DESTROYED;
            throw throwable;
        }
    }

    private Generation<U> createGeneration(InteropSize size) {
        InteropResources resources = createInteropResources(size);
        U upscaler = null;
        try {
            upscaler = Objects.requireNonNull(
                    createD3D12Upscaler(initDesc, resources.context, size),
                    "D3D12 upscaler creation returned null");
            return new Generation<>(resources, upscaler);
        } catch (Throwable throwable) {
            if (upscaler != null) {
                try {
                    destroyD3D12Upscaler(upscaler);
                } catch (Throwable cleanupFailure) {
                    throwable.addSuppressed(cleanupFailure);
                }
            }
            try {
                destroyInteropResources(resources);
            } catch (Throwable cleanupFailure) {
                throwable.addSuppressed(cleanupFailure);
            }
            throw throwable;
        }
    }

    private InteropResources createInteropResources(InteropSize size) {
        D3D12InteropContext context = null;
        GlD3D12ImportableTexture2D inputColor = null;
        GlD3D12ImportableTexture2D inputDepth = null;
        GlD3D12ImportableTexture2D inputMotionVectors = null;
        GlD3D12ImportableTexture2D inputExposure = null;
        GlD3D12ImportableTexture2D outputColor = null;
        D3D12InteropSemaphore semaphore = null;
        GlTexture2D flippedOutput = null;
        IFrameBuffer outputFramebuffer = null;
        try {
            context = D3D12InteropContext.create(
                    size.renderWidth(),
                    size.renderHeight(),
                    size.screenWidth(),
                    size.screenHeight(),
                    SuperResolutionConfig.getInternalTextureFormat());

            inputColor = new GlD3D12ImportableTexture2D(context.inputColor());
            inputDepth = new GlD3D12ImportableTexture2D(context.inputDepth());
            inputMotionVectors =
                    new GlD3D12ImportableTexture2D(context.inputMotionVectors());
            inputExposure =
                    new GlD3D12ImportableTexture2D(context.inputExposure());
            outputColor = new GlD3D12ImportableTexture2D(context.outputColor());
            semaphore =
                    new D3D12InteropSemaphore(context.getFenceSharedHandle());

            flippedOutput =
                    (GlTexture2D) RenderSystems.opengl().device().createTexture(
                            TextureDescription.create()
                                    .type(TextureType.Texture2D)
                                    .usages(TextureUsages.create()
                                            .sampler()
                                            .storage())
                                    .format(SuperResolutionConfig
                                            .getInternalTextureFormat())
                                    .width(size.screenWidth())
                                    .height(size.screenHeight())
                                    .label("D3D12UpscaleFlippedOutput")
                                    .build());
            outputFramebuffer =
                    RenderSystems.opengl().device().createFramebuffer(
                            FramebufferDescription.create()
                                    .colorAttachment(flippedOutput)
                                    .label("D3D12UpscaleOutputFramebuffer")
                                    .build());
            return new InteropResources(
                    size,
                    context,
                    inputColor,
                    inputDepth,
                    inputMotionVectors,
                    inputExposure,
                    outputColor,
                    semaphore,
                    flippedOutput,
                    outputFramebuffer);
        } catch (Throwable throwable) {
            try {
                destroyPartialInteropResources(
                        context,
                        inputColor,
                        inputDepth,
                        inputMotionVectors,
                        inputExposure,
                        outputColor,
                        semaphore,
                        flippedOutput,
                        outputFramebuffer);
            } catch (Throwable cleanupFailure) {
                throwable.addSuppressed(cleanupFailure);
            }
            throw throwable;
        }
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        super.dispatch(dispatchResource);
        Generation<U> generation = activeGeneration;
        if (lifecycleState == LifecycleState.DESTROYED ||
                lifecycleState == LifecycleState.REBUILDING ||
                generation == null ||
                !isD3D12UpscalerReady(generation.upscaler())) {
            return false;
        }

        InteropResources resources = generation.resources();
        if (!resources.size.matches(dispatchResource)) {
            lifecycleState = LifecycleState.RESIZE_PENDING;
            if (!resizeMismatchLogged) {
                SuperResolution.LOGGER.warn(
                        "Retaining the previous D3D12 output while resize is " +
                                "pending: dispatch render={}x{}, screen={}x{}; " +
                                "active generation render={}x{}, screen={}x{}",
                        dispatchResource.renderWidth(),
                        dispatchResource.renderHeight(),
                        dispatchResource.screenWidth(),
                        dispatchResource.screenHeight(),
                        resources.size.renderWidth(),
                        resources.size.renderHeight(),
                        resources.size.screenWidth(),
                        resources.size.screenHeight());
                resizeMismatchLogged = true;
            }
            needsHistoryReset = true;
            return false;
        }
        lifecycleState = LifecycleState.READY;

        InteropResourcesConverter.processInputTextures(
                dispatchResource.resources().colorTexture(),
                resources.inputColor,
                dispatchResource.resources().depthTexture(),
                resources.inputDepth,
                dispatchResource.resources().motionVectorsTexture(),
                resources.inputMotionVectors,
                dispatchResource.resources().exposureTexture(),
                resources.inputExposure,
                SRWorkModeManager.getCurrentState()
                        .motionVectorPreprocessingFunction());

        long openGlReadyValue = resources.context.nextFenceValue();
        resources.semaphore.signal(
                openGlReadyValue,
                resources.sharedTextureIds,
                new int[]{
                        GL_LAYOUT_SHADER_READ_ONLY_EXT,
                        GL_LAYOUT_SHADER_READ_ONLY_EXT,
                        GL_LAYOUT_SHADER_READ_ONLY_EXT,
                        GL_LAYOUT_SHADER_READ_ONLY_EXT,
                        GL_LAYOUT_GENERAL_EXT
                });

        resources.context.beginFrame(openGlReadyValue);
        boolean dispatched;
        long d3d12DoneValue = resources.context.nextFenceValue();
        try {
            dispatched = dispatchD3D12Upscale(
                    generation.upscaler(),
                    resources.context,
                    resources.context.getCommandList(),
                    dispatchResource);
        } finally {
            // Always close and submit the command list, then reacquire every
            // resource in OpenGL. This keeps the allocator and cross-API
            // ownership usable even if a provider throws after recording part
            // of a dispatch.
            resources.context.executeFrame(d3d12DoneValue);
            resources.semaphore.waitFor(
                    d3d12DoneValue,
                    resources.sharedTextureIds,
                    new int[]{
                            GL_LAYOUT_SHADER_READ_ONLY_EXT,
                            GL_LAYOUT_SHADER_READ_ONLY_EXT,
                            GL_LAYOUT_SHADER_READ_ONLY_EXT,
                            GL_LAYOUT_SHADER_READ_ONLY_EXT,
                            GL_LAYOUT_GENERAL_EXT
                    });
        }

        InteropResourcesConverter.flipY(
                resources.outputColor,
                resources.flippedOutput);
        return dispatched;
    }

    @Override
    public void resize(int width, int height) {
        InteropSize targetSize = InteropSize.fromScreenSize(width, height);
        Generation<U> previous = activeGeneration;
        if (previous != null &&
                previous.resources().size.equals(targetSize) &&
                isD3D12UpscalerReady(previous.upscaler())) {
            lifecycleState = LifecycleState.READY;
            resizeMismatchLogged = false;
            return;
        }

        lifecycleState = LifecycleState.REBUILDING;
        if (previous != null) {
            drainGeneration(previous);
        }

        Generation<U> replacement;
        try {
            replacement = createGeneration(targetSize);
        } catch (Throwable throwable) {
            lifecycleState = previous == null
                    ? LifecycleState.DESTROYED
                    : LifecycleState.RESIZE_PENDING;
            throw throwable;
        }

        // Publish only after both the interop resources and provider context
        // are ready. If construction fails, the previous generation remains
        // intact and can still provide its last completed output.
        activeGeneration = replacement;
        lifecycleState = LifecycleState.READY;
        resizeMismatchLogged = false;
        needsHistoryReset = true;

        if (previous != null) {
            destroyGeneration(previous, false);
        }
    }

    @Override
    public void destroy() {
        Generation<U> generation = activeGeneration;
        activeGeneration = null;
        lifecycleState = LifecycleState.DESTROYED;
        resizeMismatchLogged = false;
        if (generation != null) {
            destroyGeneration(generation, true);
        }
    }

    private void drainGeneration(Generation<U> generation) {
        // OpenGL command buffers are submitted asynchronously and their
        // waitForFence() implementation is a no-op. Drain the GL queue before
        // deleting imported memory objects or releasing their owning D3D12
        // resources during resize.
        RenderSystems.opengl().finish();
        generation.resources().context.waitIdle();
    }

    private void destroyGeneration(
            Generation<U> generation,
            boolean drain) {
        if (drain) {
            drainGeneration(generation);
        }
        try {
            destroyD3D12Upscaler(generation.upscaler());
        } finally {
            destroyInteropResources(generation.resources());
        }
    }

    private static void destroyInteropResources(
            InteropResources resources) {
        destroyPartialInteropResources(
                resources.context,
                resources.inputColor,
                resources.inputDepth,
                resources.inputMotionVectors,
                resources.inputExposure,
                resources.outputColor,
                resources.semaphore,
                resources.flippedOutput,
                resources.outputFramebuffer);
    }

    private static void destroyPartialInteropResources(
            D3D12InteropContext context,
            GlD3D12ImportableTexture2D inputColor,
            GlD3D12ImportableTexture2D inputDepth,
            GlD3D12ImportableTexture2D inputMotionVectors,
            GlD3D12ImportableTexture2D inputExposure,
            GlD3D12ImportableTexture2D outputColor,
            D3D12InteropSemaphore semaphore,
            GlTexture2D flippedOutput,
            IFrameBuffer outputFramebuffer) {
        if (outputFramebuffer != null) {
            outputFramebuffer.destroy();
        }
        if (flippedOutput != null) {
            flippedOutput.destroy();
        }
        if (outputColor != null) {
            outputColor.destroy();
        }
        if (inputExposure != null) {
            inputExposure.destroy();
        }
        if (inputMotionVectors != null) {
            inputMotionVectors.destroy();
        }
        if (inputDepth != null) {
            inputDepth.destroy();
        }
        if (inputColor != null) {
            inputColor.destroy();
        }
        if (semaphore != null) {
            semaphore.close();
        }
        if (context != null) {
            context.close();
        }
    }

    @Override
    public IFrameBuffer getOutputFrameBuffer() {
        Generation<U> generation = activeGeneration;
        return generation == null
                ? null
                : generation.resources().outputFramebuffer;
    }

    @Override
    public int getOutputTextureId() {
        Generation<U> generation = activeGeneration;
        return generation == null
                ? 0
                : Math.toIntExact(
                        generation.resources().flippedOutput.handle());
    }
}
