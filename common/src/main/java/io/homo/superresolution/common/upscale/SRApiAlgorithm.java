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

package io.homo.superresolution.common.upscale;

import io.homo.superresolution.api.InitializationDescription;
import io.homo.superresolution.common.upscale.interoplayer.GlVulkanInteropAlgorithm;
import io.homo.superresolution.core.graphics.vulkan.VulkanCommandBuffer;
import io.homo.superresolution.srapi.SRUpscaleContext;

/**
 * Vulkan/GL interop algorithm backed by an SR API upscale context.
 */
public abstract class SRApiAlgorithm extends GlVulkanInteropAlgorithm {
    protected SRUpscaleContext context;

    protected abstract void recreateSRApiContext(InitializationDescription desc);

    protected abstract void destroySRApiContext();

    protected abstract void dispatchSRApiContext(
            VulkanCommandBuffer commandBuffer,
            InFlightFrameResourcesSet inFlightFrameResourcesSet
    );

    @Override
    protected final boolean isVulkanInteropReady() {
        return context != null && context.nativePtr > 0;
    }

    @Override
    protected final void onInteropResourcesCreated() {
        recreateSRApiContext(initDesc);
    }

    @Override
    protected final void onBeforeInteropResourcesDestroyed() {
        destroySRApiContext();
        context = null;
    }

    @Override
    protected final void dispatchVulkanUpscale(
            VulkanCommandBuffer commandBuffer,
            InFlightFrameResourcesSet inFlightFrameResourcesSet
    ) {
        dispatchSRApiContext(commandBuffer, inFlightFrameResourcesSet);
    }
}
