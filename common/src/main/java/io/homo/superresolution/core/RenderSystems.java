/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
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

package io.homo.superresolution.core;

import io.homo.superresolution.api.platform.OperatingSystemType;
import io.homo.superresolution.api.platform.Platform;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.B3DVulkanBridge;
import io.homo.superresolution.common.presentation.vulkan.VulkanPresentationFeature;
import io.homo.superresolution.core.graphics.d3d12.D3D12OpenGlInterop;
import io.homo.superresolution.core.graphics.d3d12.D3D12RenderSystem;
import io.homo.superresolution.core.graphics.opengl.GlRenderSystem;
import io.homo.superresolution.core.graphics.system.IRenderSystem;
import io.homo.superresolution.core.graphics.vulkan.VkRenderSystem;
import io.homo.superresolution.core.graphics.vulkan.VulkanException;
import io.homo.superresolution.core.streamline.Streamline;
import io.homo.superresolution.core.utils.ThrowableUtil;
import org.lwjgl.vulkan.KHRExternalMemoryFd;
import org.lwjgl.vulkan.KHRExternalSemaphoreFd;
import org.lwjgl.vulkan.VK;

import static org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME;
import static org.lwjgl.vulkan.EXTMutableDescriptorType.VK_EXT_MUTABLE_DESCRIPTOR_TYPE_EXTENSION_NAME;
import static org.lwjgl.vulkan.EXTPrivateData.VK_EXT_PRIVATE_DATA_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRDedicatedAllocation.VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalMemory.VK_KHR_EXTERNAL_MEMORY_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalMemoryCapabilities.VK_KHR_EXTERNAL_MEMORY_CAPABILITIES_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalMemoryWin32.VK_KHR_EXTERNAL_MEMORY_WIN32_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalSemaphore.VK_KHR_EXTERNAL_SEMAPHORE_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalSemaphoreCapabilities.VK_KHR_EXTERNAL_SEMAPHORE_CAPABILITIES_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalSemaphoreWin32.VK_KHR_EXTERNAL_SEMAPHORE_WIN32_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRGetMemoryRequirements2.VK_KHR_GET_MEMORY_REQUIREMENTS_2_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRPushDescriptor.VK_KHR_PUSH_DESCRIPTOR_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRShaderFloat16Int8.VK_KHR_SHADER_FLOAT16_INT8_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRShaderIntegerDotProduct.VK_KHR_SHADER_INTEGER_DOT_PRODUCT_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRDynamicRendering.VK_KHR_DYNAMIC_RENDERING_EXTENSION_NAME;

public class RenderSystems {
    private static VkRenderSystem vulkan;
    private static GlRenderSystem opengl;
    private static D3D12RenderSystem d3d12;

    public static void init() {
        opengl = new GlRenderSystem();
        opengl.initRenderSystem();
        initVulkan();
    }


    public static synchronized void destroy() {
        Throwable failure = null;
        D3D12RenderSystem d3d12ToDestroy = d3d12;
        if (d3d12ToDestroy != null) {
            try {
                d3d12ToDestroy.destroyRenderSystem();
            } catch (Throwable throwable) {
                if (!d3d12ToDestroy.isDestroyed()) {
                    throwDestroyFailure(throwable);
                }
                failure = appendDestroyFailure(failure, throwable);
            } finally {
                if (d3d12ToDestroy.isDestroyed()) {
                    d3d12 = null;
                }
            }
        }
        if (opengl != null) {
            try {
                opengl.destroyRenderSystem();
            } catch (Throwable throwable) {
                failure = appendDestroyFailure(failure, throwable);
            }
        }
        if (vulkan != null) {
            try {
                vulkan.destroyRenderSystem();
            } catch (Throwable throwable) {
                failure = appendDestroyFailure(failure, throwable);
            }
        }
        throwDestroyFailure(failure);
    }

    private static Throwable appendDestroyFailure(Throwable failure, Throwable addition) {
        if (failure == null) {
            return addition;
        }
        if (failure != addition) {
            failure.addSuppressed(addition);
        }
        return failure;
    }

    private static void throwDestroyFailure(Throwable failure) {
        if (failure == null) {
            return;
        }
        ThrowableUtil.rethrowError(failure);
        if (failure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new RuntimeException("Failed to destroy render systems", failure);
    }

    public static boolean isSupportVulkan() {
        return vulkan != null;
    }

    public static boolean isSupportOpenGL() {
        return true;
    }

    public static boolean isSupportD3D12() {
        return Platform.currentPlatform.getOS().type == OperatingSystemType.WINDOWS &&
                NativeLibManager.d3d12Available();
    }

    public static boolean initBorrowedB3DVulkanIfAvailable() {
        #if MC_VER >= MC_26_2
        if (vulkan != null) {
            return true;
        }
        if (!B3DVulkanBridge.isB3DVulkanBackend()) {
            return false;
        }
        try {
            VK.create();
        } catch (Exception | Error e) {
            if (e.getMessage() == null || !e.getMessage().contains("Vulkan has already been created")) {
                VkRenderSystem.LOGGER.error("Vulkan initialization failed; the Vulkan runtime may be missing. Error: {}", e.getMessage());
                VkRenderSystem.LOGGER.error("Vulkan initialization failure details", e);
                return false;
            }
        }
        try {
            vulkan = VkRenderSystem.borrowed(
                    B3DVulkanBridge.vkInstance(),
                    B3DVulkanBridge.vkPhysicalDevice(),
                    B3DVulkanBridge.vkDevice(),
                    B3DVulkanBridge.graphicsQueueFamilyIndex()
            );
            return true;
        } catch (Throwable t) {
            VkRenderSystem.LOGGER.error("Unable to create a Vulkan device from Blaze3D", t);
            vulkan = null;
            return false;
        }
        #else
        return false;
        #endif
    }

    private static void initVulkan() {
        if (SuperResolutionConfig.isSkipInitVulkan()) {
            return;
        }

        try {
            VK.create();
        } catch (Exception | Error e) {
            String message = e.getMessage();
            if (message == null || !message.contains("Vulkan has already been created")) {
                VkRenderSystem.LOGGER.error("Vulkan initialization failed; the Vulkan runtime may be missing. Error: {}", e.getMessage());
                VkRenderSystem.LOGGER.error("Vulkan initialization failure details", e);
                if (VulkanPresentationFeature.isRequested()) {
                    VulkanPresentationFeature.disableAfterFailure(e);
                    throw new RuntimeException("Vulkan presentation requires a working Vulkan loader", e);
                }
                return;
            }
        }
        vulkan = new VkRenderSystem();
        vulkan.addInstanceExtension(VK_KHR_EXTERNAL_SEMAPHORE_CAPABILITIES_EXTENSION_NAME)
                .addInstanceExtension(VK_KHR_EXTERNAL_MEMORY_CAPABILITIES_EXTENSION_NAME)
                .addInstanceExtension(VK_EXT_DEBUG_UTILS_EXTENSION_NAME)
                .addDeviceExtension(VK_KHR_EXTERNAL_SEMAPHORE_EXTENSION_NAME)
                .addDeviceExtension(VK_EXT_MUTABLE_DESCRIPTOR_TYPE_EXTENSION_NAME)//XeSS
                .addDeviceExtension(VK_KHR_SHADER_FLOAT16_INT8_EXTENSION_NAME)//XeSS
                .addDeviceExtension(VK_KHR_SHADER_INTEGER_DOT_PRODUCT_EXTENSION_NAME)//XeSS
                .addDeviceExtension(VK_KHR_EXTERNAL_MEMORY_EXTENSION_NAME)
                .addDeviceExtension(VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME)
                .addDeviceExtension(VK_KHR_GET_MEMORY_REQUIREMENTS_2_EXTENSION_NAME)
                .addDeviceExtension("VK_EXT_descriptor_indexing")
                .addDeviceExtension("VK_NVX_binary_import")
                .addDeviceExtension("VK_NVX_image_view_handle")
                .addDeviceExtension(VK_KHR_PUSH_DESCRIPTOR_EXTENSION_NAME)
                .addDeviceExtension(VK_KHR_DYNAMIC_RENDERING_EXTENSION_NAME)
                .addDeviceExtension(VK_EXT_PRIVATE_DATA_EXTENSION_NAME)
                .addDeviceExtension("VK_NV_optical_flow")//DLSS-FG
                .addDeviceExtension("VK_KHR_synchronization2")//DLSS-FG
                .addDeviceExtension("VK_KHR_format_feature_flags2")//DLSS-FG
                .addDeviceExtension("VK_KHR_timeline_semaphore")//DLSS-FG
                .addDeviceExtension("VK_EXT_calibrated_timestamps");//DLSS-FG
        if (Platform.currentPlatform.getOS().type == OperatingSystemType.WINDOWS) {
            vulkan.addDeviceExtension(VK_KHR_EXTERNAL_MEMORY_WIN32_EXTENSION_NAME)
                    .addDeviceExtension(VK_KHR_EXTERNAL_SEMAPHORE_WIN32_EXTENSION_NAME);
        }
        if (Platform.currentPlatform.getOS().type == OperatingSystemType.LINUX) {
            vulkan.addDeviceExtension(KHRExternalMemoryFd.VK_KHR_EXTERNAL_MEMORY_FD_EXTENSION_NAME)
                    .addDeviceExtension(KHRExternalSemaphoreFd.VK_KHR_EXTERNAL_SEMAPHORE_FD_EXTENSION_NAME);
        }
        try {
            vulkan.initRenderSystem();
            return;
        } catch (VulkanException vkException) {
            VkRenderSystem.LOGGER.error("Vulkan initialization failed; Vulkan has been disabled", vkException);
            if (VulkanPresentationFeature.isRequested()) {
                VulkanPresentationFeature.disableAfterFailure(vkException);
                throw vkException;
            }
        } catch (Throwable e) {
            VkRenderSystem.LOGGER.error("Vulkan initialization failed with an unknown error; Vulkan has been disabled", e);
            if (VulkanPresentationFeature.isRequested()) {
                VulkanPresentationFeature.disableAfterFailure(e);
                throw new RuntimeException("Vulkan presentation initialization failed", e);
            }
        }
        vulkan = null;
    }

    public static GlRenderSystem opengl() {
        return opengl;
    }

    public static VkRenderSystem vulkan() {
        return vulkan;
    }

    public static synchronized D3D12RenderSystem d3d12() {
        if (d3d12 != null) {
            if (!d3d12.isInitialized()) {
                throw new IllegalStateException(
                        "A failed D3D12 render system is retained for destruction retry.");
            }
            return d3d12;
        }
        if (!isSupportD3D12()) {
            throw new UnsupportedOperationException(
                    "D3D12 RHI is unavailable on this platform or its native library failed to load.");
        }

        D3D12RenderSystem candidate = new D3D12RenderSystem(
                D3D12OpenGlInterop.queryAdapterLuid());
        try {
            candidate.initRenderSystem();
            d3d12 = candidate;
            return candidate;
        } catch (RuntimeException | Error failure) {
            try {
                candidate.destroyRenderSystem();
            } catch (RuntimeException | Error cleanupFailure) {
                if (!candidate.isDestroyed()) {
                    d3d12 = candidate;
                }
                if (failure != cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            ThrowableUtil.rethrowError(failure);
            throw failure;
        }
    }

    public static IRenderSystem current() {

        return SuperResolution.isUsingVulkan?vulkan: opengl;
    }
}
