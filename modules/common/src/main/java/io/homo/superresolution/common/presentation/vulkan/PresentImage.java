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

package io.homo.superresolution.common.presentation.vulkan;

import io.homo.superresolution.core.graphics.vulkan.VulkanTexture;

import javax.annotation.Nullable;

/** A display source. Swapchain acquisition and submission belong to PresentWorker. */
public record PresentImage(
        long displayIndex,

        long realIndex,

        long latencyFrameId,

        Kind kind,

        @Nullable
        VulkanTexture source,

        long presentId,

        long timingPresentId,

        long latencyMarkerId,

        boolean outOfBand
) {
    public enum Kind {
        GENERATED,
        REAL
    }
}
