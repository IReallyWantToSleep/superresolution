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

package io.homo.superresolution.api.registry;

/**
 * Declares who owns frame-generation dispatch and presentation.
 */
public enum FrameGenerationExecutionModel {
    /**
     * The provider or its swapchain interposer owns generated-frame dispatch,
     * pacing, and presentation. Super Resolution presents only the application's
     * normal frame and must not route this provider through its async scheduler.
     */
    EXTERNAL_INTERPOSER,

    /**
     * Super Resolution owns the bounded queues, dedicated dispatch thread and
     * queue, display order, pacing, and {@code vkQueuePresentKHR}. The provider
     * only records a complete dispatch and returns leased display outputs.
     */
    APPLICATION_MANAGED_ASYNC
}
