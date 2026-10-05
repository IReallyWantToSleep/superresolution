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

/**
 * Declares who owns frame-generation dispatch and presentation.
 */
public enum FrameGenerationExecutionModel {
    /**
     * The provider or its swapchain interposer owns generated-frame dispatch,
     * pacing, and presentation. Super Resolution presents only the application's
     * normal frame and notifies the provider through ExternalFrameGenerationDispatchInput.
     */
    EXTERNAL_INTERPOSER,

    /**
     * Super Resolution owns generation submissions on FrameGenerationWorker and
     * all generated/real image preparation, pacing and {@code vkQueuePresentKHR}
     * on PresentWorker. The provider records pure FG work and returns leased outputs.
     */
    APPLICATION_MANAGED_ASYNC
}
