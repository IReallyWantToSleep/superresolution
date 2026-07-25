/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.api.registry;

import io.homo.superresolution.common.framegeneration.FrameGenerationMode;
import io.homo.superresolution.common.framegeneration.FramePresentPlan;
import io.homo.superresolution.common.framegeneration.constants.FGConstants;
import io.homo.superresolution.common.presentation.capture.FrameResources;

/**
 * Runtime side of a frame generation backend.
 * <p>
 * The presentation pipeline (swapchain image acquisition, present pacing, present-id and
 * Reflex coupling) stays in Super Resolution and drives the selected backend only through
 * this interface, so a backend can live in another mod. Register one by adding a
 * {@link FrameGenerationDescription} to {@link FrameGenerationRegistry}, which is what the
 * {@code FrameGenerationRegisterEvent} exists for.
 * <p>
 * {@code FrameGeneration} performs the backend-agnostic gating (presentation enabled, mode
 * enabled, required render targets present, per-frame constants available) before calling
 * {@link #prepareFrame}; an implementation only handles its own runtime. All methods run
 * on the render thread under {@code FrameGeneration}'s monitor, so they need no additional
 * locking against each other.
 */
public interface FrameGenerationProvider {

    /** One-time setup, called when frame generation initializes. */
    void initialize();

    /** One-time teardown, called when frame generation shuts down. */
    void shutdown();

    /** Whether the backend's hardware / driver support is actually present. */
    boolean isAvailable();

    /** Maximum interpolated frames this backend can produce per rendered frame. */
    int supportedGeneratedFrameCount();

    /**
     * Interpolated frames the presentation layer must itself acquire and present before
     * the real frame for {@code mode}. Zero when the backend presents them internally
     * (e.g. a swapchain interposer), which tells the swapchain not to reserve images.
     */
    int presentationManagedGeneratedFrameCount(FrameGenerationMode mode);

    /**
     * Backend-specific runtime prerequisites, checked in addition to the shared
     * presentation requirements. Consulted only for the active backend.
     */
    boolean dependenciesSatisfied();

    /**
     * Records this frame's generation into {@code commandBuffer} and returns the plan the
     * presentation layer should follow. Never returns {@code null}; return
     * {@link FramePresentPlan#none()} to fall back to presenting the raw backbuffer.
     * <p>
     * {@code colorFormat} and {@code backBufferCount} describe the swapchain and are only
     * needed by backends that configure a swapchain interposer; others may ignore them.
     */
    FramePresentPlan prepareFrame(
            FrameResources frameResources,
            FGConstants constants,
            FrameGenerationMode mode,
            int colorWidth,
            int colorHeight,
            int colorFormat,
            int backBufferCount,
            long commandBuffer);

    /** Called once the frame's presents have been submitted. */
    void finishPresent(FrameResources frameResources, boolean frameGenerationActive);

    /** Stops generating while keeping resources resident; must be idempotent. */
    void disable();
}
