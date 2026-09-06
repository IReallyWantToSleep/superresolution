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
