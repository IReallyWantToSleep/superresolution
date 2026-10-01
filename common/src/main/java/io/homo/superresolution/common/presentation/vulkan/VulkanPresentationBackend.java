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

import io.homo.superresolution.common.presentation.api.PresentationBackend;
import io.homo.superresolution.common.presentation.api.PresentationBackendType;

public final class VulkanPresentationBackend implements PresentationBackend {
    @Override
    public PresentationBackendType type() {
        return PresentationBackendType.VULKAN;
    }

    @Override
    public boolean isAvailable() {
        return VulkanPresentationFeature.isAvailable();
    }

    @Override
    public boolean isInitialized() {
        return VulkanPresentationWindow.isInitialized();
    }

    @Override
    public void beginRealFrameRendering() {
        VulkanPresentationWindow.beginRealFrameRendering();
    }

    @Override
    public void endRealFrameRendering() {
        VulkanPresentationWindow.endRealFrameRendering();
    }

    @Override
    public void endMinecraftFrame() {
        VulkanPresentationWindow.endMinecraftFrame();
    }

    @Override
    public void flushCapturedFrame() {
        VulkanPresentationWindow.flushCapturedFrame();
    }

    @Override
    public void setVsync(boolean enabled) {
        VulkanPresentationWindow.setVsync(enabled);
    }

    @Override
    public boolean shutdownApplicationManagedProvider(String providerId, Runnable teardown) {
        return VulkanPresentationWindow.shutdownApplicationManagedProvider(providerId, teardown);
    }

    @Override
    public void shutdown() {
        VulkanPresentationFeature.shutdown();
    }

    public void collectGpuTimestamps() {
        VulkanPresentationFeature.collectGpuTimestamps();
    }
}
