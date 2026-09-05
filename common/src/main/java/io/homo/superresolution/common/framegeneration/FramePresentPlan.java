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

package io.homo.superresolution.common.framegeneration;

import io.homo.superresolution.api.registry.AsyncFrameGenerationDispatchResult;
import io.homo.superresolution.api.registry.FrameGenerationDispatchCompletion;
import io.homo.superresolution.api.registry.ProviderOutputLease;
import io.homo.superresolution.core.graphics.vulkan.VulkanTexture;

import javax.annotation.Nullable;

import java.util.List;

/**
 * Result of preparing frame generation for one presented frame.
 * <p>
 * Backends that produce and present interpolated frames themselves (e.g. through a
 * swapchain interposer) return {@link #externallyPresented()}, and {@link #generatedFrames()}
 * is empty. Backends that hand back the interpolated frames for the presentation layer to
 * present return {@link #generated}, and the caller must present each frame (in order)
 * before the real frame, with pacing. Application-managed async providers use
 * {@link #applicationManaged(AsyncFrameGenerationDispatchResult)} so the output lease and
 * dispatch completion remain attached to the display plan.
 */
public final class FramePresentPlan {
    private static final FramePresentPlan NONE =
            new FramePresentPlan(false, false, List.of(), null, null, null);
    private static final FramePresentPlan EXTERNALLY_PRESENTED =
            new FramePresentPlan(true, true, List.of(), null, null, null);

    private final boolean frameGenerationActive;
    private final boolean externallyPresented;
    private final List<VulkanTexture> generatedFrames;
    private final @Nullable VulkanTexture realFrame;
    private final @Nullable ProviderOutputLease providerOutputLease;
    private final @Nullable AsyncFrameGenerationDispatchResult.HistoryDisposition historyDisposition;

    private FramePresentPlan(
            boolean frameGenerationActive,
            boolean externallyPresented,
            List<VulkanTexture> generatedFrames,
            @Nullable VulkanTexture realFrame,
            @Nullable ProviderOutputLease providerOutputLease,
            @Nullable AsyncFrameGenerationDispatchResult.HistoryDisposition historyDisposition
    ) {
        this.frameGenerationActive = frameGenerationActive;
        this.externallyPresented = externallyPresented;
        this.generatedFrames = generatedFrames;
        this.realFrame = realFrame;
        this.providerOutputLease = providerOutputLease;
        this.historyDisposition = historyDisposition;
    }

    public static FramePresentPlan none() {
        return NONE;
    }

    /**
     * The active backend produced and will present the generated frames itself (via, e.g.,
     * a swapchain interposer); the presentation layer must not reserve extra swapchain
     * images or present interpolated frames on the backend's behalf.
     */
    public static FramePresentPlan externallyPresented() {
        return EXTERNALLY_PRESENTED;
    }

    public static FramePresentPlan generated(
            List<VulkanTexture> generatedFrames,
            @Nullable VulkanTexture realFrame
    ) {
        if (generatedFrames == null || generatedFrames.isEmpty()) {
            return NONE;
        }
        return new FramePresentPlan(
                true,
                false,
                List.copyOf(generatedFrames),
                realFrame,
                null,
                null
        );
    }

    /**
     * Converts one complete async dispatch into an immutable display plan. Failed
     * dispatches remain {@link #none()} so the scheduler builds its real-only batch.
     */
    public static FramePresentPlan applicationManaged(AsyncFrameGenerationDispatchResult result) {
        if (result == null || !result.succeeded() || result.outputLease() == null) {
            return NONE;
        }
        return new FramePresentPlan(
                result.actualGeneratedCount() > 0,
                false,
                List.copyOf(result.generatedOutputs()),
                result.realOutput(),
                result.outputLease(),
                result.historyDisposition()
        );
    }

    public boolean frameGenerationActive() {
        return frameGenerationActive;
    }

    public boolean isExternallyPresented() {
        return externallyPresented;
    }

    public List<VulkanTexture> generatedFrames() {
        return generatedFrames;
    }

    /**
     * Optional provider-produced real output. Present this instead of the captured
     * backbuffer for the real display item. Null when the captured real frame should be
     * presented directly.
     */
    public @Nullable VulkanTexture realFrame() {
        return realFrame;
    }

    public @Nullable ProviderOutputLease providerOutputLease() {
        return providerOutputLease;
    }

    public FrameGenerationDispatchCompletion dispatchCompletion() {
        return providerOutputLease == null
                ? FrameGenerationDispatchCompletion.completed()
                : providerOutputLease.completion();
    }

    public @Nullable AsyncFrameGenerationDispatchResult.HistoryDisposition historyDisposition() {
        return historyDisposition;
    }
}
