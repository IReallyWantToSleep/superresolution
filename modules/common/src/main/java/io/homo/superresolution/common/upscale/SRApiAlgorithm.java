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
import io.homo.superresolution.common.upscale.interoplayer.InteropDispatchResource;
import io.homo.superresolution.common.upscale.interoplayer.InteropResourceAccess;
import io.homo.superresolution.common.upscale.interoplayer.VulkanInteropAlgorithm;
import io.homo.superresolution.common.upscale.interoplayer.VulkanCommandContext;
import io.homo.superresolution.srapi.SRUpscaleContext;

public abstract class SRApiAlgorithm extends VulkanInteropAlgorithm {
    protected SRUpscaleContext context;

    protected abstract void recreateSRApiContext(
            InitializationDescription desc, InteropResourceAccess resources);

    protected abstract void destroySRApiContext();

    protected abstract boolean dispatchSRApiContext(
            InteropDispatchResource<VulkanCommandContext> resource);

    @Override
    protected final void createAlgorithmContext(InteropResourceAccess resources) {
        recreateSRApiContext(initDesc, resources);
    }

    @Override
    protected final void destroyAlgorithmContext() {
        destroySRApiContext();
        context = null;
    }

    @Override
    protected final boolean dispatchAlgorithm(
            InteropDispatchResource<VulkanCommandContext> resource) {
        if (context == null || context.nativePtr <= 0) {
            return false;
        }
        return dispatchSRApiContext(resource);
    }
}
