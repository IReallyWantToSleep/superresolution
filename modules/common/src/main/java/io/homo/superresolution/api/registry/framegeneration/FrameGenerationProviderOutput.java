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

import io.homo.superresolution.core.graphics.vulkan.VulkanTexture;

import javax.annotation.Nullable;

import java.util.List;

public interface FrameGenerationProviderOutput extends AutoCloseable {
    List<VulkanTexture> generatedOutputs();

    @Nullable VulkanTexture realOutput();

    FrameGenerationDispatchCompletion completion();

    OutputKey outputKey();

    boolean isReleased();

    default void abort() {
        release();
    }

    void release();

    @Override
    default void close() {
        release();
    }

    record OutputKey(int width, int height, int format) {
        public OutputKey {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Output dimensions must be positive");
            }
        }
    }
}
