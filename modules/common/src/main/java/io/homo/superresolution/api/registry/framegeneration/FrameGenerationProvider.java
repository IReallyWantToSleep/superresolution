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

package io.homo.superresolution.api.registry.framegeneration;

import io.homo.superresolution.common.framegeneration.FrameGenerationMode;
import io.homo.superresolution.common.framegeneration.constants.FrameGenerationConstants;
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
 * {@code FrameGeneration} performs backend-agnostic gating before invoking a provider.
 * Providers must declare an {@link #executionModel()} so Super Resolution does not mix
 * external-interposer ownership with its application-managed async presenter.
 * <p>
 * Lifecycle and external-interposer methods are serialized by {@code FrameGeneration}.
 * {@link FrameGenerationExecutionModel#APPLICATION_MANAGED_ASYNC} snapshot capture runs
 * on the render thread, while dispatch, provider session/history work, output-lease
 * acquisition, and release run only on FrameGenerationWorker. Async dispatch is not
 * performed while holding {@code FrameGeneration}'s monitor, so implementations must
 * protect state shared with lifecycle calls.
 */
public interface FrameGenerationProvider {

    /**
     * Defaults to external-interposer ownership; application-managed providers opt in.
     */
    default FrameGenerationExecutionModel executionModel() {
        return FrameGenerationExecutionModel.EXTERNAL_INTERPOSER;
    }

    /**
     * One-time setup, called when frame generation initializes. Application-managed
     * providers must limit this to thread-agnostic setup; FG-thread-affine feature or
     * session creation belongs to async dispatch lifecycle.
     */
    void initialize();

    /**
     * One-time teardown after presentation has drained. This method runs on FrameGenerationWorker for an
     * {@link FrameGenerationExecutionModel#APPLICATION_MANAGED_ASYNC} provider that owns a
     * live presenter. Use it for thread-affine feature, session, and output-pool release.
     */
    default void shutdownOnFrameGenerationThread() {
    }

    /**
     * One-time thread-agnostic teardown after any required
     * {@link #shutdownOnFrameGenerationThread()} call has completed.
     */
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
    boolean isDependenciesSatisfied();

    /**
     * External-interposer compatibility entry point. Records or configures this frame and
     * returns whether external frame generation is active for the current real frame.
     * <p>
     * The input contains the swapchain description and the command buffer used to notify
     * the external interposer.
     * Application-managed async providers are never dispatched through this method.
     */
    default ExternalFrameGenerationDispatchResult prepareExternalFrame(
            ExternalFrameGenerationDispatchInput input
    ) {
        return ExternalFrameGenerationDispatchResult.INACTIVE;
    }

    /**
     * Captures immutable provider inputs before the render thread transfers ownership of
     * {@code frameResources}. Implementations may return a custom immutable snapshot.
     * Feature/session creation, reset, dispatch, and release are forbidden here.
     */
    default ProviderInputSnapshot captureInputSnapshot(
            String providerId,
            FrameResources frameResources,
            FrameGenerationConstants constants,
            FrameGenerationMode mode
    ) {
        return ProviderInputSnapshot.of(
                providerId,
                frameResources.logicalFrameIndex(),
                mode,
                constants,
                constants.reset() != 0
        );
    }

    /**
     * Records one complete application-managed dispatch on FrameGenerationWorker.
     * The provider must not acquire swapchain images, call {@code vkQueuePresentKHR}, or
     * publish partial outputs. On failure, return
     * {@link FrameGenerationDispatchResult#failed(String)} and let the worker
     * construct the real-only batch.
     * <p>
     * The request supplies one command buffer per requested generated frame, and each is
     * submitted separately so a generated frame can be presented as soon as its own work
     * retires. Record the work producing generated frame {@code k} into
     * {@link FrameGenerationDispatchInput#dispatchCommandBuffer(int)} for
     * {@code k}, and shared setup into
     * {@link FrameGenerationDispatchInput#commandBuffer()}. Returning fewer frames
     * than requested means only the buffers below that count were recorded into.
     */
    default FrameGenerationDispatchResult dispatchAsync(
            FrameGenerationDispatchInput request
    ) {
        return FrameGenerationDispatchResult.failed(
                "Provider does not implement application-managed async dispatch"
        );
    }

    /** Called once the external path's real frame has been submitted. */
    default void finishExternalFrame(
            FrameResources frameResources,
            ExternalFrameGenerationDispatchResult result
    ) {
    }

    /** Stops generating while keeping resources resident; must be idempotent. */
    void disable();
}
