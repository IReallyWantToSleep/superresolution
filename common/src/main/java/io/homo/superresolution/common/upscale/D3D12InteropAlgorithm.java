/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.upscale;

import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.api.InitializationDescription;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.workmode.SRWorkModeManager;
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

import static org.lwjgl.opengl.EXTSemaphore.GL_LAYOUT_GENERAL_EXT;
import static org.lwjgl.opengl.EXTSemaphore.GL_LAYOUT_SHADER_READ_ONLY_EXT;

/**
 * Low-latency OpenGL/Direct3D 12 interop path.
 *
 * <p>D3D12 owns the shared committed resources and a shared timeline fence.
 * OpenGL imports those objects, writes the preprocessed inputs, signals the
 * fence, waits for the D3D12 dispatch, and then copies the vertically flipped
 * output into a regular OpenGL texture.</p>
 */
public abstract class D3D12InteropAlgorithm extends AbstractAlgorithm {
    protected D3D12InteropContext d3d12Interop;
    protected GlD3D12ImportableTexture2D inputColor;
    protected GlD3D12ImportableTexture2D inputDepth;
    protected GlD3D12ImportableTexture2D inputMotionVectors;
    protected GlD3D12ImportableTexture2D inputExposure;
    protected GlD3D12ImportableTexture2D outputColor;

    private D3D12InteropSemaphore semaphore;
    private GlTexture2D flippedOutput;
    private IFrameBuffer outputFramebuffer;
    private int builtRenderWidth = -1;
    private int builtRenderHeight = -1;
    private int builtScreenWidth = -1;
    private int builtScreenHeight = -1;

    protected abstract void onD3D12InteropCreated(InitializationDescription desc);

    protected abstract void onBeforeD3D12InteropDestroyed();

    protected abstract boolean dispatchD3D12Upscale(
            long commandList,
            DispatchResource dispatchResource);

    protected boolean isD3D12UpscalerReady() {
        return true;
    }

    @Override
    public void initialize(InitializationDescription desc) {
        this.initDesc = desc;
        try {
            createResources();
            onD3D12InteropCreated(desc);
        } catch (Throwable throwable) {
            try {
                destroyResources();
            } catch (Throwable cleanupFailure) {
                throwable.addSuppressed(cleanupFailure);
            }
            throw throwable;
        }
    }

    private void createResources() {
        d3d12Interop = D3D12InteropContext.create(
                RenderHandlerManager.getRenderWidth(),
                RenderHandlerManager.getRenderHeight(),
                RenderHandlerManager.getScreenWidth(),
                RenderHandlerManager.getScreenHeight(),
                SuperResolutionConfig.getInternalTextureFormat());

        inputColor = new GlD3D12ImportableTexture2D(d3d12Interop.inputColor());
        inputDepth = new GlD3D12ImportableTexture2D(d3d12Interop.inputDepth());
        inputMotionVectors = new GlD3D12ImportableTexture2D(d3d12Interop.inputMotionVectors());
        inputExposure = new GlD3D12ImportableTexture2D(d3d12Interop.inputExposure());
        outputColor = new GlD3D12ImportableTexture2D(d3d12Interop.outputColor());
        semaphore = new D3D12InteropSemaphore(d3d12Interop.getFenceSharedHandle());

        flippedOutput = (GlTexture2D) RenderSystems.opengl().device().createTexture(
                TextureDescription.create()
                        .type(TextureType.Texture2D)
                        .usages(TextureUsages.create().sampler().storage())
                        .format(SuperResolutionConfig.getInternalTextureFormat())
                        .width(RenderHandlerManager.getScreenWidth())
                        .height(RenderHandlerManager.getScreenHeight())
                        .label("D3D12UpscaleFlippedOutput")
                        .build());
        outputFramebuffer = RenderSystems.opengl().device().createFramebuffer(
                FramebufferDescription.create()
                        .colorAttachment(flippedOutput)
                        .label("D3D12UpscaleOutputFramebuffer")
                        .build());
        builtRenderWidth = RenderHandlerManager.getRenderWidth();
        builtRenderHeight = RenderHandlerManager.getRenderHeight();
        builtScreenWidth = RenderHandlerManager.getScreenWidth();
        builtScreenHeight = RenderHandlerManager.getScreenHeight();
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        super.dispatch(dispatchResource);
        if (d3d12Interop == null || !isD3D12UpscalerReady()) {
            return false;
        }

        InteropResourcesConverter.processInputTextures(
                dispatchResource.resources().colorTexture(),
                inputColor,
                dispatchResource.resources().depthTexture(),
                inputDepth,
                dispatchResource.resources().motionVectorsTexture(),
                inputMotionVectors,
                dispatchResource.resources().exposureTexture(),
                inputExposure,
                SRWorkModeManager.getCurrentState().motionVectorPreprocessingFunction());

        int[] sharedTextures = sharedTextureIds();
        long openGlReadyValue = d3d12Interop.nextFenceValue();
        semaphore.signal(
                openGlReadyValue,
                sharedTextures,
                new int[]{
                        GL_LAYOUT_SHADER_READ_ONLY_EXT,
                        GL_LAYOUT_SHADER_READ_ONLY_EXT,
                        GL_LAYOUT_SHADER_READ_ONLY_EXT,
                        GL_LAYOUT_SHADER_READ_ONLY_EXT,
                        GL_LAYOUT_GENERAL_EXT
                });

        d3d12Interop.beginFrame(openGlReadyValue);
        boolean dispatched;
        long d3d12DoneValue = d3d12Interop.nextFenceValue();
        try {
            dispatched = dispatchD3D12Upscale(
                    d3d12Interop.getCommandList(),
                    dispatchResource);
        } finally {
            // Always close and submit the command list, then reacquire every
            // resource in OpenGL. This keeps the allocator and cross-API
            // ownership usable even if a provider throws after recording part
            // of a dispatch.
            d3d12Interop.executeFrame(d3d12DoneValue);
            semaphore.waitFor(
                    d3d12DoneValue,
                    sharedTextures,
                    new int[]{
                            GL_LAYOUT_GENERAL_EXT,
                            GL_LAYOUT_GENERAL_EXT,
                            GL_LAYOUT_GENERAL_EXT,
                            GL_LAYOUT_GENERAL_EXT,
                            GL_LAYOUT_GENERAL_EXT
                    });
        }

        InteropResourcesConverter.flipY(outputColor, flippedOutput);
        return dispatched;
    }

    private int[] sharedTextureIds() {
        return new int[]{
                Math.toIntExact(inputColor.handle()),
                Math.toIntExact(inputDepth.handle()),
                Math.toIntExact(inputMotionVectors.handle()),
                Math.toIntExact(inputExposure.handle()),
                Math.toIntExact(outputColor.handle())
        };
    }

    @Override
    public void resize(int width, int height) {
        if (isD3D12UpscalerReady() &&
                RenderHandlerManager.getRenderWidth() == builtRenderWidth &&
                RenderHandlerManager.getRenderHeight() == builtRenderHeight &&
                RenderHandlerManager.getScreenWidth() == builtScreenWidth &&
                RenderHandlerManager.getScreenHeight() == builtScreenHeight) {
            return;
        }
        destroyResources();
        needsHistoryReset = true;
        try {
            createResources();
            onD3D12InteropCreated(initDesc);
        } catch (Throwable throwable) {
            try {
                destroyResources();
            } catch (Throwable cleanupFailure) {
                throwable.addSuppressed(cleanupFailure);
            }
            throw throwable;
        }
    }

    @Override
    public void destroy() {
        destroyResources();
    }

    private void destroyResources() {
        if (d3d12Interop != null) {
            d3d12Interop.waitIdle();
        }
        onBeforeD3D12InteropDestroyed();

        if (outputFramebuffer != null) {
            outputFramebuffer.destroy();
            outputFramebuffer = null;
        }
        if (flippedOutput != null) {
            flippedOutput.destroy();
            flippedOutput = null;
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
        if (semaphore != null) {
            semaphore.close();
            semaphore = null;
        }
        if (d3d12Interop != null) {
            d3d12Interop.close();
            d3d12Interop = null;
        }
        builtRenderWidth = -1;
        builtRenderHeight = -1;
        builtScreenWidth = -1;
        builtScreenHeight = -1;
    }

    @Override
    public IFrameBuffer getOutputFrameBuffer() {
        return outputFramebuffer;
    }

    @Override
    public int getOutputTextureId() {
        return flippedOutput == null
                ? 0
                : Math.toIntExact(flippedOutput.handle());
    }
}
