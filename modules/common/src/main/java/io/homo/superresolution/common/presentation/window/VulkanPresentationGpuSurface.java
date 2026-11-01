/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.presentation.window;

#if MC_VER == MC_26_3 && IS_VULKAN == 1
import com.mojang.renderpearl.api.device.GpuSurface;
import com.mojang.renderpearl.api.device.SurfaceException;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.backend.api.CommandEncoderBackend;
import com.mojang.renderpearl.backend.api.GpuSurfaceBackend;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import io.homo.superresolution.common.presentation.vulkan.VulkanPresentationFeature;
import io.homo.superresolution.common.presentation.vulkan.VulkanPresentationWindow;
import io.homo.superresolution.common.presentation.vulkan.VulkanSurface;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;

import java.nio.IntBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.BooleanSupplier;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

/**
 * Keeps RenderPearl's frontend surface protocol while SR owns all swapchain work.
 * Minecraft's command encoder is submitted before SR consumes the captured frame.
 */
public final class VulkanPresentationGpuSurface implements GpuSurfaceBackend {
    private final VulkanDevice device;
    private final VulkanSurface surface;
    private final BooleanSupplier isIconified;
    private final Set<GpuSurface.PresentMode> presentModes;
    private GpuSurface.Configuration configuration;
    private boolean acquired;
    private boolean blitted;
    private boolean closed;

    public VulkanPresentationGpuSurface(VulkanDevice device, long windowHandle, BooleanSupplier isIconified) {
        this.device = device;
        this.isIconified = isIconified;
        try {
            surface = VulkanPresentationFeature.attachBorrowedWindow(
                    windowHandle, device.vkDevice(), device.graphicsQueue().queueFamilyIndex());
            presentModes = queryPresentModes();
        } catch (RuntimeException | Error failure) {
            try {
                VulkanPresentationFeature.shutdown();
            } catch (Throwable cleanupFailure) {
                if (failure != cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            throw failure;
        }
    }

    private Set<GpuSurface.PresentMode> queryPresentModes() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer count = stack.ints(0);
            check(vkGetPhysicalDeviceSurfacePresentModesKHR(device.vkDevice().getPhysicalDevice(), surface.surface(), count, null));
            IntBuffer modes = stack.mallocInt(count.get(0));
            check(vkGetPhysicalDeviceSurfacePresentModesKHR(device.vkDevice().getPhysicalDevice(), surface.surface(), count, modes));
            EnumSet<GpuSurface.PresentMode> result = EnumSet.noneOf(GpuSurface.PresentMode.class);
            for (int i = 0; i < count.get(0); i++) {
                switch (modes.get(i)) {
                    case VK_PRESENT_MODE_IMMEDIATE_KHR -> result.add(GpuSurface.PresentMode.IMMEDIATE);
                    case VK_PRESENT_MODE_MAILBOX_KHR -> result.add(GpuSurface.PresentMode.MAILBOX);
                    case VK_PRESENT_MODE_FIFO_KHR -> result.add(GpuSurface.PresentMode.FIFO);
                    case VK_PRESENT_MODE_FIFO_RELAXED_KHR -> result.add(GpuSurface.PresentMode.FIFO_RELAXED);
                }
            }
            if (result.isEmpty()) {
                throw new IllegalStateException("SR Vulkan surface exposes no supported RenderPearl present mode");
            }
            return Collections.unmodifiableSet(result);
        }
    }

    @Override
    public void configure(GpuSurface.Configuration config) throws SurfaceException {
        requireOpen();
        if (acquired) {
            throw new IllegalStateException("Cannot configure an acquired SR surface");
        }
        if (!presentModes.contains(config.presentMode())) {
            throw new SurfaceException("Unsupported SR surface present mode: " + config.presentMode());
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSurfaceCapabilitiesKHR capabilities = VkSurfaceCapabilitiesKHR.calloc(stack);
            int result = vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device.vkDevice().getPhysicalDevice(), surface.surface(), capabilities);
            if (result != VK_SUCCESS) {
                throw new SurfaceException("Failed to query SR surface capabilities, VkResult=" + result);
            }
            if (config.width() <= 0 || config.height() <= 0
                    || config.width() < capabilities.minImageExtent().width()
                    || config.width() > capabilities.maxImageExtent().width()
                    || config.height() < capabilities.minImageExtent().height()
                    || config.height() > capabilities.maxImageExtent().height()) {
                throw new SurfaceException("Invalid SR surface extent: " + config.width() + "x" + config.height());
            }
        }
        surface.configureFramebufferSize(config.width(), config.height());
        VulkanPresentationWindow.configureNativeSurface(
                config.width(), config.height(),
                switch (config.presentMode()) {
                    case IMMEDIATE -> VK_PRESENT_MODE_IMMEDIATE_KHR;
                    case MAILBOX -> VK_PRESENT_MODE_MAILBOX_KHR;
                    case FIFO -> VK_PRESENT_MODE_FIFO_KHR;
                    case FIFO_RELAXED -> VK_PRESENT_MODE_FIFO_RELAXED_KHR;
                },
                config.presentMode() == GpuSurface.PresentMode.FIFO || config.presentMode() == GpuSurface.PresentMode.FIFO_RELAXED);
        configuration = config;
    }

    @Override
    public boolean isSuboptimal() {
        requireOpen();
        surface.refreshFramebufferSize();
        return configuration != null && !isIconified.getAsBoolean()
                && (configuration.width() != surface.framebufferWidth() || configuration.height() != surface.framebufferHeight());
    }

    @Override
    public void acquireNextTexture() throws SurfaceException {
        requireOpen();
        if (configuration == null || acquired) {
            throw new IllegalStateException("SR surface must be configured and unacquired");
        }
        acquired = true;
        blitted = false;
    }

    @Override
    public void blitFromTexture(CommandEncoderBackend commandEncoder, GpuTextureView textureView) {
        requireOpen();
        if (!acquired || blitted) {
            throw new IllegalStateException("SR surface must be acquired once before its frontend blit");
        }
        blitted = true;
    }

    @Override
    public void present() {
        requireOpen();
        if (!acquired || !blitted) {
            throw new IllegalStateException("SR surface frontend frame has not been blitted");
        }
        acquired = false;
    }

    @Override
    public Collection<GpuSurface.PresentMode> supportedPresentModes() {
        requireOpen();
        return presentModes;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        if (acquired) {
            throw new IllegalStateException("Cannot close an acquired SR surface");
        }
        VulkanPresentationFeature.shutdown();
        closed = true;
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("SR Vulkan surface is closed");
        }
    }

    private static void check(int result) {
        if (result != VK_SUCCESS) {
            throw new IllegalStateException("Failed to query SR surface present modes, VkResult=" + result);
        }
    }
}
#else
public final class VulkanPresentationGpuSurface {
    private VulkanPresentationGpuSurface() {
    }
}
#endif
