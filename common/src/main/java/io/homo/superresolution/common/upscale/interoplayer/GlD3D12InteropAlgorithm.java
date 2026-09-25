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
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.InteropResourcesPreprocessor;
import io.homo.superresolution.common.workmode.SRWorkModeManager;
import io.homo.superresolution.core.NativeLibManager;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.d3d12.D3D12CommandBuffer;
import io.homo.superresolution.core.graphics.d3d12.D3D12CommandPool;
import io.homo.superresolution.core.graphics.d3d12.D3D12Device;
import io.homo.superresolution.core.graphics.d3d12.D3D12Fence;
import io.homo.superresolution.core.graphics.d3d12.D3D12InteropSemaphore;
import io.homo.superresolution.core.graphics.d3d12.D3D12OpenGlInterop;
import io.homo.superresolution.core.graphics.d3d12.D3D12Queue;
import io.homo.superresolution.core.graphics.d3d12.D3D12ResourceState;
import io.homo.superresolution.core.graphics.d3d12.D3D12Texture2D;
import io.homo.superresolution.core.graphics.d3d12.GlD3D12ImportableTexture2D;
import io.homo.superresolution.core.graphics.impl.command.CommandBufferBehavior;
import io.homo.superresolution.core.graphics.impl.command.CommandPoolFlags;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsages;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBufferAttachment;
import io.homo.superresolution.core.utils.ThrowableUtil;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

import static org.lwjgl.opengl.EXTSemaphore.GL_LAYOUT_GENERAL_EXT;
import static org.lwjgl.opengl.EXTSemaphore.GL_LAYOUT_SHADER_READ_ONLY_EXT;

/**
 * Low-latency OpenGL/Direct3D 12 interop path.
 *
 * <p>The session owns the stable D3D12 fence, command resources, and imported
 * OpenGL semaphore. A size generation owns only the five shared textures,
 * their OpenGL imports, the upscaler context, and the OpenGL output objects.
 * Resize can therefore replace a generation without rebuilding the device,
 * queue, or synchronization timeline.</p>
 */
public abstract class GlD3D12InteropAlgorithm<U> extends AbstractAlgorithm {
    private static final int[] GL_HANDOFF_LAYOUTS = {
            GL_LAYOUT_SHADER_READ_ONLY_EXT,
            GL_LAYOUT_SHADER_READ_ONLY_EXT,
            GL_LAYOUT_SHADER_READ_ONLY_EXT,
            GL_LAYOUT_SHADER_READ_ONLY_EXT,
            GL_LAYOUT_GENERAL_EXT
    };

    private InteropSession session;
    private Generation<U> activeGeneration;
    private final GenerationReuseState<Generation<U>> generationReuseState =
            new GenerationReuseState<>();
    private final ArrayList<Generation<U>> retiredGenerations = new ArrayList<>(1);
    private final ArrayList<InteropResources> retainedInteropResources = new ArrayList<>(1);
    private LifecycleState lifecycleState = LifecycleState.NEW;
    private boolean resizeMismatchLogged;
    private boolean missingRequiredInputsLogged;
    private boolean recoveryFailureLogged;

    protected abstract U createD3D12Upscaler(
            InitializationDescription desc,
            D3D12Device device,
            InteropSize size);

    protected abstract void destroyD3D12Upscaler(U upscaler);

    protected abstract boolean dispatchD3D12Upscale(
            U upscaler,
            D3D12CommandBuffer commandBuffer,
            D3D12Resources resources,
            DispatchResource dispatchResource);

    protected boolean isD3D12UpscalerReady(U upscaler) {
        return true;
    }

    @Override
    public void initialize(InitializationDescription desc) {
        if (!NativeLibManager.d3d12Available()) {
            throw new IllegalStateException(
                    "The optional D3D12 native library is unavailable.");
        }
        if (session != null || activeGeneration != null ||
                !retiredGenerations.isEmpty() ||
                !retainedInteropResources.isEmpty()) {
            throw new IllegalStateException(
                    "The OpenGL/D3D12 interop algorithm still owns resources.");
        }

        this.initDesc = Objects.requireNonNull(desc, "desc");
        generationReuseState.reset();
        retiredGenerations.clear();
        needsHistoryReset = true;
        resizeMismatchLogged = false;
        missingRequiredInputsLogged = false;
        recoveryFailureLogged = false;
        lifecycleState = LifecycleState.REBUILDING;
        InteropSession replacementSession = null;
        try {
            replacementSession = InteropSession.beginCreate();
            replacementSession.initialize();
            Generation<U> replacementGeneration = createGeneration(
                    replacementSession,
                    InteropSize.capture());
            session = replacementSession;
            activeGeneration = replacementGeneration;
            lifecycleState = LifecycleState.READY;
        } catch (Throwable throwable) {
            if (replacementSession != null) {
                try {
                    replacementSession.close();
                } catch (Throwable closeFailure) {
                    session = replacementSession;
                    lifecycleState = LifecycleState.DESTROY_FAILED;
                    throwFailure(appendFailure(throwable, closeFailure));
                    return;
                }
            }
            lifecycleState = retainedInteropResources.isEmpty() &&
                    retiredGenerations.isEmpty()
                    ? LifecycleState.DESTROYED
                    : LifecycleState.DESTROY_FAILED;
            throwFailure(throwable);
        }
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        super.dispatch(dispatchResource);
        Generation<U> generation = activeGeneration;
        InteropSession currentSession = session;
        if (lifecycleState == LifecycleState.DESTROYED ||
                lifecycleState == LifecycleState.DESTROYING ||
                lifecycleState == LifecycleState.DESTROY_FAILED ||
                lifecycleState == LifecycleState.REBUILDING ||
                generation == null ||
                currentSession == null) {
            return false;
        }

        if (!hasRequiredInputs(dispatchResource.resources())) {
            if (!missingRequiredInputsLogged) {
                SuperResolution.LOGGER.warn(
                        "FSR4 D3D12 requires color, depth, and motion-vector " +
                                "inputs; retaining the previous output.");
                missingRequiredInputsLogged = true;
            }
            needsHistoryReset = true;
            return false;
        }
        missingRequiredInputsLogged = false;

        if (generationReuseState.requiresRebuild(generation)) {
            Generation<U> recovered = recoverInvalidGeneration(
                    currentSession,
                    generation,
                    InteropSize.fromDispatch(dispatchResource));
            if (recovered == null) {
                needsHistoryReset = true;
                return false;
            }
            generation = recovered;
        }
        if (!isD3D12UpscalerReady(generation.upscaler())) {
            markGenerationInvalid(generation);
            needsHistoryReset = true;
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

        try {
            InteropResourcesPreprocessor.processInputTextures(
                    dispatchResource.resources().get(InputResourceType.Color),
                    resources.inputColor,
                    dispatchResource.resources().get(InputResourceType.Depth),
                    resources.inputDepth,
                    dispatchResource.resources().get(InputResourceType.MotionVectors),
                    resources.inputMotionVectors,
                    dispatchResource.resources().get(InputResourceType.Exposure),
                    resources.inputExposure,
                    SRWorkModeManager.getCurrentState()
                            .motionVectorPreprocessingFunction());

            boolean dispatched = dispatchFrame(
                    currentSession,
                    generation,
                    dispatchResource);
            if (!dispatched) {
                needsHistoryReset = true;
                return false;
            }
            InteropResourcesPreprocessor.flipY(
                    resources.outputColor,
                    resources.flippedOutput);
            return true;
        } catch (Throwable throwable) {
            needsHistoryReset = true;
            throw throwable;
        }
    }

    private boolean dispatchFrame(
            InteropSession currentSession,
            Generation<U> generation,
            DispatchResource dispatchResource) {
        InteropResources resources = generation.resources();
        long openGlReadyValue = currentSession.fence.reserveValue();
        long d3d12DoneValue = currentSession.fence.reserveValue();
        boolean openGlReleased = false;
        boolean handoffRestored = false;
        boolean submitInvoked = false;
        boolean discardCommandBuffer = false;
        boolean dispatched = false;
        boolean generationReusable = true;
        D3D12CommandBuffer commandBuffer = null;
        Throwable failure = null;

        try {
            currentSession.semaphore.signal(
                    openGlReadyValue,
                    resources.sharedTextureIds,
                    GL_HANDOFF_LAYOUTS);
            openGlReleased = true;
            resources.d3d12.assumeGlHandoffStates();

            commandBuffer = currentSession.beginCommandBuffer();
            try {
                dispatched = dispatchD3D12Upscale(
                        generation.upscaler(),
                        commandBuffer,
                        resources.d3d12,
                        dispatchResource);
                if (!dispatched) {
                    generationReusable = false;
                }
            } catch (Throwable throwable) {
                generationReusable = false;
                failure = appendFailure(failure, throwable);
            }

            try {
                commandBuffer.end();
            } catch (Throwable throwable) {
                generationReusable = false;
                discardCommandBuffer = true;
                failure = appendFailure(failure, throwable);
            }

            if (!discardCommandBuffer) {
                try {
                    submitInvoked = true;
                    currentSession.queue.submit(
                            commandBuffer,
                            currentSession.fence,
                            openGlReadyValue,
                            d3d12DoneValue);
                    handoffRestored = true;
                } catch (Throwable throwable) {
                    generationReusable = false;
                    discardCommandBuffer = true;
                    failure = appendFailure(failure, throwable);
                    handoffRestored = currentSession.fence.completedValue() >=
                            d3d12DoneValue;
                }
            }
        } catch (Throwable throwable) {
            generationReusable = false;
            failure = appendFailure(failure, throwable);
            discardCommandBuffer = commandBuffer != null;
        } finally {
            // Queue.submit owns recovery once invoked because only it knows
            // whether ExecuteCommandLists ran. Empty recovery is valid solely
            // for failures that happened before submit was called.
            if (openGlReleased && !handoffRestored && !submitInvoked) {
                try {
                    currentSession.queue.recoverSharedFence(
                            currentSession.fence,
                            openGlReadyValue,
                            d3d12DoneValue);
                    handoffRestored = true;
                } catch (Throwable throwable) {
                    generationReusable = false;
                    failure = appendFailure(failure, throwable);
                }
            }
            if (openGlReleased && handoffRestored) {
                try {
                    if (generationReusable) {
                        currentSession.semaphore.waitFor(
                                d3d12DoneValue,
                                resources.sharedTextureIds,
                                GL_HANDOFF_LAYOUTS);
                    } else {
                        currentSession.semaphore.waitForFenceOnly(d3d12DoneValue);
                    }
                } catch (Throwable throwable) {
                    generationReusable = false;
                    failure = appendFailure(failure, throwable);
                }
            }
            if (discardCommandBuffer && commandBuffer != null) {
                failure = currentSession.replaceCommandBuffer(
                        commandBuffer,
                        failure);
            }
        }

        if (generationReuseState.recordDispatchOutcome(
                activeGeneration,
                generation,
                generationReusable)) {
            lifecycleState = LifecycleState.RECOVERY_PENDING;
        }
        throwFailure(failure);
        return dispatched;
    }

    private void markGenerationInvalid(Generation<U> generation) {
        if (generationReuseState.invalidateActive(activeGeneration, generation)) {
            lifecycleState = LifecycleState.RECOVERY_PENDING;
        }
    }

    private Generation<U> recoverInvalidGeneration(
            InteropSession currentSession,
            Generation<U> failedGeneration,
            InteropSize targetSize) {
        if (!generationReuseState.requiresRebuild(failedGeneration)) {
            return activeGeneration;
        }

        lifecycleState = LifecycleState.REBUILDING;
        Generation<U> replacement;
        try {
            drainSession(currentSession);
            replacement = createGeneration(
                    currentSession,
                    targetSize);
        } catch (Throwable throwable) {
            lifecycleState = LifecycleState.RECOVERY_PENDING;
            if (!recoveryFailureLogged) {
                SuperResolution.LOGGER.error(
                        "Failed to rebuild an invalid D3D12 interop generation; " +
                                "the last completed output remains available",
                        throwable);
                recoveryFailureLogged = true;
            }
            ThrowableUtil.rethrowError(throwable);
            return null;
        }

        activeGeneration = replacement;
        generationReuseState.clearIfInvalid(failedGeneration);
        lifecycleState = LifecycleState.READY;
        resizeMismatchLogged = false;
        recoveryFailureLogged = false;
        needsHistoryReset = true;

        Throwable retirementFailure = null;
        try {
            retirementFailure = retryRetiredGenerations(
                    currentSession,
                    retirementFailure);
        } catch (Throwable throwable) {
            retirementFailure = appendFailure(retirementFailure, throwable);
        }
        try {
            retirementFailure = retireGeneration(
                    currentSession,
                    failedGeneration,
                    retirementFailure);
        } catch (Throwable throwable) {
            retainRetiredGeneration(failedGeneration);
            retirementFailure = appendFailure(retirementFailure, throwable);
        }
        if (retirementFailure != null) {
            SuperResolution.LOGGER.error(
                    "Failed to retire an invalid D3D12 interop generation; " +
                            "ownership was retained for a later retry",
                    retirementFailure);
            ThrowableUtil.rethrowError(retirementFailure);
        }
        return replacement;
    }

    private Throwable retireGeneration(
            InteropSession currentSession,
            Generation<U> generation,
            Throwable failure) {
        try {
            destroyGeneration(currentSession, generation, false);
        } catch (Throwable throwable) {
            retainRetiredGeneration(generation);
            failure = appendFailure(failure, throwable);
        }
        return failure;
    }

    private Throwable retryRetiredGenerations(
            InteropSession currentSession,
            Throwable failure) {
        Iterator<Generation<U>> iterator = retiredGenerations.iterator();
        while (iterator.hasNext()) {
            Generation<U> retired = iterator.next();
            try {
                destroyGeneration(currentSession, retired, false);
                iterator.remove();
            } catch (Throwable throwable) {
                failure = appendFailure(failure, throwable);
            }
        }
        return failure;
    }

    private void retainRetiredGeneration(Generation<U> generation) {
        for (int index = 0; index < retiredGenerations.size(); index++) {
            if (retiredGenerations.get(index) == generation) {
                return;
            }
        }
        retiredGenerations.add(generation);
    }

    private static boolean hasRequiredInputs(InputResourceSet resources) {
        return resources != null &&
                resources.has(InputResourceType.Color) &&
                resources.has(InputResourceType.Depth) &&
                resources.has(InputResourceType.MotionVectors);
    }

    @Override
    public void destroy() {
        InteropSession currentSession = session;
        if (lifecycleState == LifecycleState.DESTROYED) {
            return;
        }
        lifecycleState = LifecycleState.DESTROYING;

        if (currentSession != null) {
            try {
                drainSession(currentSession);
            } catch (Throwable throwable) {
                lifecycleState = LifecycleState.DESTROY_FAILED;
                throwFailure(throwable);
                return;
            }
        }

        Throwable failure = retryRetainedInteropResources(null);
        failure = retryRetiredGenerations(currentSession, failure);
        Generation<U> generation = activeGeneration;
        if (generation != null) {
            try {
                destroyGeneration(currentSession, generation, false);
                activeGeneration = null;
                generationReuseState.clearIfInvalid(generation);
            } catch (Throwable throwable) {
                failure = appendFailure(failure, throwable);
            }
        }
        if (failure != null) {
            lifecycleState = LifecycleState.DESTROY_FAILED;
            throwFailure(failure);
            return;
        }

        if (currentSession != null) {
            try {
                currentSession.close();
                session = null;
            } catch (Throwable throwable) {
                lifecycleState = LifecycleState.DESTROY_FAILED;
                throwFailure(throwable);
                return;
            }
        }

        retiredGenerations.clear();
        retainedInteropResources.clear();
        lifecycleState = LifecycleState.DESTROYED;
        resizeMismatchLogged = false;
        missingRequiredInputsLogged = false;
        recoveryFailureLogged = false;
    }

    @Override
    public void resize(int width, int height) {
        InteropSession currentSession = session;
        if (currentSession == null ||
                lifecycleState == LifecycleState.NEW ||
                lifecycleState == LifecycleState.REBUILDING ||
                lifecycleState == LifecycleState.DESTROYING ||
                lifecycleState == LifecycleState.DESTROY_FAILED ||
                lifecycleState == LifecycleState.DESTROYED) {
            throw new IllegalStateException(
                    "The OpenGL/D3D12 interop algorithm is not initialized.");
        }

        InteropSize targetSize = InteropSize.fromScreenSize(width, height);
        Generation<U> previous = activeGeneration;
        if (previous != null &&
                !generationReuseState.requiresRebuild(previous) &&
                previous.resources().size.equals(targetSize) &&
                isD3D12UpscalerReady(previous.upscaler())) {
            lifecycleState = LifecycleState.READY;
            resizeMismatchLogged = false;
            return;
        }

        lifecycleState = LifecycleState.REBUILDING;
        if (previous != null) {
            try {
                drainSession(currentSession);
            } catch (Throwable throwable) {
                lifecycleState = LifecycleState.RESIZE_PENDING;
                throwFailure(throwable);
            }
        }

        Generation<U> replacement;
        try {
            replacement = createGeneration(currentSession, targetSize);
        } catch (Throwable throwable) {
            lifecycleState = LifecycleState.RESIZE_PENDING;
            throwFailure(throwable);
            throw new AssertionError("unreachable");
        }

        activeGeneration = replacement;
        generationReuseState.clearIfInvalid(previous);
        lifecycleState = LifecycleState.READY;
        resizeMismatchLogged = false;
        recoveryFailureLogged = false;
        needsHistoryReset = true;

        Throwable retirementFailure = null;
        try {
            retirementFailure = retryRetiredGenerations(
                    currentSession,
                    retirementFailure);
        } catch (Throwable throwable) {
            retirementFailure = appendFailure(retirementFailure, throwable);
        }
        if (previous != null) {
            try {
                retirementFailure = retireGeneration(
                        currentSession,
                        previous,
                        retirementFailure);
            } catch (Throwable throwable) {
                retainRetiredGeneration(previous);
                retirementFailure = appendFailure(
                        retirementFailure,
                        throwable);
            }
        }
        throwFailure(retirementFailure);
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
        GlTexture2D output = generation == null
                ? null
                : generation.resources().flippedOutput;
        return output == null ? 0 : Math.toIntExact(output.handle());
    }

    private Generation<U> createGeneration(
            InteropSession currentSession,
            InteropSize size) {
        Throwable retryFailure = retryRetiredGenerations(currentSession, null);
        retryFailure = retryRetainedInteropResources(retryFailure);
        throwFailure(retryFailure);
        retiredGenerations.ensureCapacity(Math.addExact(
                retiredGenerations.size(),
                1));

        Generation<U> generation = new Generation<>();
        try {
            generation.setResources(createInteropResources(
                    currentSession.device,
                    size));
            generation.setUpscaler(Objects.requireNonNull(
                    createD3D12Upscaler(initDesc, currentSession.device, size),
                    "D3D12 upscaler creation returned null"));
            return generation;
        } catch (Throwable throwable) {
            try {
                destroyGeneration(currentSession, generation, false);
            } catch (Throwable cleanupFailure) {
                retainRetiredGeneration(generation);
                throwable = appendFailure(throwable, cleanupFailure);
            }
            throwFailure(throwable);
            throw new AssertionError("unreachable");
        }
    }

    private InteropResources createInteropResources(
            D3D12Device device,
            InteropSize size) {
        retainedInteropResources.ensureCapacity(Math.addExact(
                retainedInteropResources.size(),
                1));
        InteropResources resources = new InteropResources(size);
        try {
            resources.pinDevice(device);
            TextureFormat colorFormat =
                    SuperResolutionConfig.getInternalTextureFormat();
            resources.inputColorD3D12 = device.createSharedTexture2D(
                    sharedTextureDescription(
                            size.renderWidth(),
                            size.renderHeight(),
                            colorFormat,
                            "D3D12InputColor"),
                    D3D12ResourceState.COMMON);
            resources.inputDepthD3D12 = device.createSharedTexture2D(
                    sharedTextureDescription(
                            size.renderWidth(),
                            size.renderHeight(),
                            TextureFormat.R32F,
                            "D3D12InputDepth"),
                    D3D12ResourceState.COMMON);
            resources.inputMotionVectorsD3D12 = device.createSharedTexture2D(
                    sharedTextureDescription(
                            size.renderWidth(),
                            size.renderHeight(),
                            TextureFormat.RG16F,
                            "D3D12InputMotionVectors"),
                    D3D12ResourceState.COMMON);
            resources.inputExposureD3D12 = device.createSharedTexture2D(
                    sharedTextureDescription(
                            1,
                            1,
                            TextureFormat.R32F,
                            "D3D12InputExposure"),
                    D3D12ResourceState.COMMON);
            resources.outputColorD3D12 = device.createSharedTexture2D(
                    sharedTextureDescription(
                            size.screenWidth(),
                            size.screenHeight(),
                            colorFormat,
                            "D3D12OutputColor"),
                    D3D12ResourceState.COMMON);

            resources.inputColor = new GlD3D12ImportableTexture2D(
                    resources.inputColorD3D12);
            resources.inputColor.initializeImport();
            resources.inputDepth = new GlD3D12ImportableTexture2D(
                    resources.inputDepthD3D12);
            resources.inputDepth.initializeImport();
            resources.inputMotionVectors = new GlD3D12ImportableTexture2D(
                    resources.inputMotionVectorsD3D12);
            resources.inputMotionVectors.initializeImport();
            resources.inputExposure = new GlD3D12ImportableTexture2D(
                    resources.inputExposureD3D12);
            resources.inputExposure.initializeImport();
            resources.outputColor = new GlD3D12ImportableTexture2D(
                    resources.outputColorD3D12);
            resources.outputColor.initializeImport();

            OwnedGlTexture2D flippedOutput = new OwnedGlTexture2D(
                    TextureDescription.create()
                            .type(TextureType.Texture2D)
                            .usages(TextureUsages.create()
                                    .sampler()
                                    .storage())
                            .format(colorFormat)
                            .width(size.screenWidth())
                            .height(size.screenHeight())
                            .label("D3D12UpscaleFlippedOutput")
                            .build());
            resources.flippedOutput = flippedOutput;
            flippedOutput.initializeOwned();
            GlFrameBuffer outputFramebuffer = new GlFrameBuffer();
            resources.outputFramebuffer = outputFramebuffer;
            outputFramebuffer.addAttachment(new GlFrameBufferAttachment(
                    GlFrameBufferAttachment.FrameBufferAttachmentType.COLOR,
                    resources.flippedOutput));
            outputFramebuffer.validate();
            outputFramebuffer.label("D3D12UpscaleOutputFramebuffer");
            resources.complete();
            return resources;
        } catch (Throwable throwable) {
            throwable = cleanupAndRetainInteropResources(
                    resources,
                    throwable);
            throwFailure(throwable);
            throw new AssertionError("unreachable");
        }
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

    private void destroyGeneration(
            InteropSession currentSession,
            Generation<U> generation,
            boolean drain) {
        if (drain) {
            drainSession(currentSession);
        }
        // The provider borrows the generation resources. Do not release any
        // imported or D3D12 resource until provider destruction has succeeded.
        U upscaler = generation.upscaler();
        if (upscaler != null) {
            destroyD3D12Upscaler(upscaler);
            generation.clearUpscaler();
        }
        InteropResources resources = generation.resources();
        if (resources != null) {
            destroyInteropResources(resources);
            generation.clearResources();
        }
    }

    private static void drainSession(InteropSession currentSession) {
        // OpenGL command buffers are asynchronous and their public fence wait
        // is a no-op. Drain GL before releasing imported memory, then drain the
        // D3D12 queue before releasing the resources or provider context.
        RenderSystems.opengl().finish();
        currentSession.device.waitIdle();
    }

    private static void destroyInteropResources(InteropResources resources) {
        resources.destroy();
    }

    private Throwable cleanupAndRetainInteropResources(
            InteropResources resources,
            Throwable failure) {
        try {
            destroyInteropResources(resources);
        } catch (Throwable cleanupFailure) {
            retainInteropResources(resources);
            failure = appendFailure(failure, cleanupFailure);
        }
        return failure;
    }

    private void retainInteropResources(InteropResources resources) {
        for (int index = 0; index < retainedInteropResources.size(); index++) {
            if (retainedInteropResources.get(index) == resources) {
                return;
            }
        }
        retainedInteropResources.add(resources);
    }

    private Throwable retryRetainedInteropResources(Throwable failure) {
        Iterator<InteropResources> iterator = retainedInteropResources.iterator();
        while (iterator.hasNext()) {
            InteropResources retained = iterator.next();
            try {
                destroyInteropResources(retained);
                iterator.remove();
            } catch (Throwable cleanupFailure) {
                failure = appendFailure(failure, cleanupFailure);
            }
        }
        return failure;
    }

    private static Throwable cleanup(
            Throwable failure,
            CleanupAction cleanupAction) {
        try {
            cleanupAction.run();
        } catch (Throwable cleanupFailure) {
            failure = appendFailure(failure, cleanupFailure);
        }
        return failure;
    }

    private static Throwable appendFailure(
            Throwable failure,
            Throwable nextFailure) {
        if (failure == null) {
            return nextFailure;
        }
        if (failure != nextFailure) {
            failure.addSuppressed(nextFailure);
        }
        return failure;
    }

    private static void throwFailure(Throwable failure) {
        if (failure == null) {
            return;
        }
        ThrowableUtil.rethrowError(failure);
        if (failure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new RuntimeException(failure);
    }

    @FunctionalInterface
    private interface CleanupAction {
        void run();
    }

    private enum LifecycleState {
        NEW,
        READY,
        RESIZE_PENDING,
        RECOVERY_PENDING,
        REBUILDING,
        DESTROYING,
        DESTROY_FAILED,
        DESTROYED
    }

    /**
     * The dimensions bound to one resource generation.
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

        private static InteropSize fromDispatch(
                DispatchResource dispatchResource) {
            return new InteropSize(
                    dispatchResource.renderWidth(),
                    dispatchResource.renderHeight(),
                    dispatchResource.screenWidth(),
                    dispatchResource.screenHeight());
        }

        private boolean matches(DispatchResource dispatchResource) {
            return dispatchResource.renderWidth() == renderWidth &&
                    dispatchResource.renderHeight() == renderHeight &&
                    dispatchResource.screenWidth() == screenWidth &&
                    dispatchResource.screenHeight() == screenHeight;
        }
    }

    protected record D3D12Resources(
            D3D12Texture2D inputColor,
            D3D12Texture2D inputDepth,
            D3D12Texture2D inputMotionVectors,
            D3D12Texture2D inputExposure,
            D3D12Texture2D outputColor) {
        public D3D12Resources {
            Objects.requireNonNull(inputColor, "inputColor");
            Objects.requireNonNull(inputDepth, "inputDepth");
            Objects.requireNonNull(inputMotionVectors, "inputMotionVectors");
            Objects.requireNonNull(inputExposure, "inputExposure");
            Objects.requireNonNull(outputColor, "outputColor");
        }

        private void assumeGlHandoffStates() {
            inputColor.assumeCommittedState(D3D12ResourceState.COMPUTE_READ);
            inputDepth.assumeCommittedState(D3D12ResourceState.COMPUTE_READ);
            inputMotionVectors.assumeCommittedState(
                    D3D12ResourceState.COMPUTE_READ);
            inputExposure.assumeCommittedState(D3D12ResourceState.COMPUTE_READ);
            outputColor.assumeCommittedState(D3D12ResourceState.COMMON);
        }
    }

    private static final class InteropSession implements AutoCloseable {
        private final D3D12Device device;
        private final D3D12Queue queue;
        private D3D12Device.ExternalBorrowLease deviceBorrow;
        private D3D12Fence fence;
        private D3D12CommandPool commandPool;
        private D3D12InteropSemaphore semaphore;
        private D3D12CommandBuffer commandBuffer;

        private InteropSession(
                D3D12Device device,
                D3D12Queue queue) {
            this.device = device;
            this.queue = queue;
        }

        private static InteropSession beginCreate() {
            D3D12OpenGlInterop.requireExtensions();
            D3D12Device device = RenderSystems.d3d12().device();
            InteropSession session = new InteropSession(
                    device,
                    device.directQueue());
            session.deviceBorrow = device.borrowExternal();
            return session;
        }

        private void initialize() {
            fence = device.createFence(0);
            commandPool = device.createCommandPool(CommandPoolFlags.Reset);
            commandBuffer = commandPool.createCommandBuffer(
                    CommandBufferBehavior.ReusableSequential);
            semaphore = new D3D12InteropSemaphore(fence);
            semaphore.initializeImport();
        }

        private D3D12CommandBuffer beginCommandBuffer() {
            D3D12CommandBuffer current = commandBuffer;
            if (current == null) {
                current = commandPool.createCommandBuffer(
                        CommandBufferBehavior.ReusableSequential);
                commandBuffer = current;
            }
            try {
                current.waitForFence();
                current.begin();
                return current;
            } catch (Throwable throwable) {
                throwable = replaceCommandBuffer(current, throwable);
                throwFailure(throwable);
                throw new AssertionError("unreachable");
            }
        }

        private Throwable replaceCommandBuffer(
                D3D12CommandBuffer failedCommandBuffer,
                Throwable failure) {
            if (commandBuffer != failedCommandBuffer) {
                return failure;
            }
            try {
                failedCommandBuffer.destroy();
                commandBuffer = null;
            } catch (Throwable destroyFailure) {
                return appendFailure(failure, destroyFailure);
            }
            try {
                commandBuffer = commandPool.createCommandBuffer(
                        CommandBufferBehavior.ReusableSequential);
            } catch (Throwable replacementFailure) {
                failure = appendFailure(failure, replacementFailure);
            }
            return failure;
        }

        @Override
        public void close() {
            // Stop at the first failure and retain every downstream owner so a
            // later destroy() call can resume without breaking dependencies.
            D3D12InteropSemaphore currentSemaphore = semaphore;
            if (currentSemaphore != null) {
                currentSemaphore.close();
                semaphore = null;
            }
            D3D12CommandBuffer current = commandBuffer;
            if (current != null) {
                current.destroy();
                commandBuffer = null;
            }
            D3D12CommandPool currentPool = commandPool;
            if (currentPool != null) {
                currentPool.destroy();
                commandPool = null;
            }
            D3D12Fence currentFence = fence;
            if (currentFence != null) {
                currentFence.destroy();
                fence = null;
            }
            D3D12Device.ExternalBorrowLease currentDeviceBorrow = deviceBorrow;
            if (currentDeviceBorrow != null) {
                currentDeviceBorrow.close();
                deviceBorrow = null;
            }
        }
    }

    private static final class InteropResources {
        private final InteropSize size;
        private D3D12Device.ExternalBorrowLease deviceBorrow;
        private D3D12Texture2D inputColorD3D12;
        private D3D12Texture2D inputDepthD3D12;
        private D3D12Texture2D inputMotionVectorsD3D12;
        private D3D12Texture2D inputExposureD3D12;
        private D3D12Texture2D outputColorD3D12;
        private D3D12Resources d3d12;
        private GlD3D12ImportableTexture2D inputColor;
        private GlD3D12ImportableTexture2D inputDepth;
        private GlD3D12ImportableTexture2D inputMotionVectors;
        private GlD3D12ImportableTexture2D inputExposure;
        private GlD3D12ImportableTexture2D outputColor;
        private GlTexture2D flippedOutput;
        private IFrameBuffer outputFramebuffer;
        private int[] sharedTextureIds;

        private InteropResources(InteropSize size) {
            this.size = Objects.requireNonNull(size, "size");
        }

        private void pinDevice(D3D12Device device) {
            if (deviceBorrow != null) {
                throw new IllegalStateException(
                        "The interop resources already pin a D3D12 device");
            }
            deviceBorrow = Objects.requireNonNull(device, "device").borrowExternal();
        }

        private void complete() {
            Objects.requireNonNull(deviceBorrow, "deviceBorrow");
            d3d12 = new D3D12Resources(
                    Objects.requireNonNull(inputColorD3D12, "inputColorD3D12"),
                    Objects.requireNonNull(inputDepthD3D12, "inputDepthD3D12"),
                    Objects.requireNonNull(inputMotionVectorsD3D12, "inputMotionVectorsD3D12"),
                    Objects.requireNonNull(inputExposureD3D12, "inputExposureD3D12"),
                    Objects.requireNonNull(outputColorD3D12, "outputColorD3D12"));
            sharedTextureIds = new int[]{
                    Math.toIntExact(Objects.requireNonNull(inputColor, "inputColor").handle()),
                    Math.toIntExact(Objects.requireNonNull(inputDepth, "inputDepth").handle()),
                    Math.toIntExact(Objects.requireNonNull(
                            inputMotionVectors,
                            "inputMotionVectors").handle()),
                    Math.toIntExact(Objects.requireNonNull(inputExposure, "inputExposure").handle()),
                    Math.toIntExact(Objects.requireNonNull(outputColor, "outputColor").handle())
            };
            Objects.requireNonNull(flippedOutput, "flippedOutput");
            Objects.requireNonNull(outputFramebuffer, "outputFramebuffer");
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
            if (outputColorD3D12 != null) {
                outputColorD3D12.destroy();
                outputColorD3D12 = null;
            }
            if (inputExposureD3D12 != null) {
                inputExposureD3D12.destroy();
                inputExposureD3D12 = null;
            }
            if (inputMotionVectorsD3D12 != null) {
                inputMotionVectorsD3D12.destroy();
                inputMotionVectorsD3D12 = null;
            }
            if (inputDepthD3D12 != null) {
                inputDepthD3D12.destroy();
                inputDepthD3D12 = null;
            }
            if (inputColorD3D12 != null) {
                inputColorD3D12.destroy();
                inputColorD3D12 = null;
            }
            d3d12 = null;
            sharedTextureIds = null;
            if (deviceBorrow != null) {
                deviceBorrow.close();
                deviceBorrow = null;
            }
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

    private static final class Generation<U> {
        private InteropResources resources;
        private U upscaler;

        private void setResources(InteropResources resources) {
            if (this.resources != null) {
                throw new IllegalStateException(
                        "The D3D12 generation already owns interop resources");
            }
            this.resources = Objects.requireNonNull(resources, "resources");
        }

        private void setUpscaler(U upscaler) {
            if (this.upscaler != null) {
                throw new IllegalStateException(
                        "The D3D12 generation already owns an upscaler");
            }
            this.upscaler = Objects.requireNonNull(upscaler, "upscaler");
        }

        private InteropResources resources() {
            return resources;
        }

        private U upscaler() {
            return upscaler;
        }

        private void clearUpscaler() {
            upscaler = null;
        }

        private void clearResources() {
            resources = null;
        }
    }
}
