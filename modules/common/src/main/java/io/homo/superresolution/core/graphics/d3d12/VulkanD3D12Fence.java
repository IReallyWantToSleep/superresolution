package io.homo.superresolution.core.graphics.d3d12;

import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkImportSemaphoreWin32HandleInfoKHR;
import org.lwjgl.vulkan.VkPhysicalDeviceIDProperties;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties2;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreSignalInfo;
import org.lwjgl.vulkan.VkSemaphoreTypeCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreWaitInfo;

import java.nio.ByteOrder;

import static io.homo.superresolution.core.graphics.vulkan.VulkanUtils.VK_CHECK;
import static org.lwjgl.vulkan.KHRExternalSemaphoreWin32.*;
import static org.lwjgl.vulkan.VK12.*;

/** Persistent timeline import; the D3D12 fence remains the Win32 handle owner. */
public final class VulkanD3D12Fence implements AutoCloseable {
    private final VulkanDevice device;
    private long semaphore;
    private D3D12Fence.ExternalBorrowLease borrow;

    public VulkanD3D12Fence(VulkanDevice device, D3D12Fence source) {
        this.device = device;
        source.device().withLifecycleLock(() -> {
            borrow = source.borrowExternal();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkSemaphoreTypeCreateInfo timeline = VkSemaphoreTypeCreateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_SEMAPHORE_TYPE_CREATE_INFO)
                        .semaphoreType(VK_SEMAPHORE_TYPE_TIMELINE).initialValue(0);
                VkSemaphoreCreateInfo create = VkSemaphoreCreateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO).pNext(timeline);
                var result = stack.mallocLong(1);
                VK_CHECK(vkCreateSemaphore(device.getVkDevice(), create, null, result),
                        "Create D3D12 timeline import");
                semaphore = result.get(0);
                VkImportSemaphoreWin32HandleInfoKHR info = VkImportSemaphoreWin32HandleInfoKHR.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_IMPORT_SEMAPHORE_WIN32_HANDLE_INFO_KHR)
                        .semaphore(semaphore).handleType(VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_D3D12_FENCE_BIT)
                        .handle(source.sharedHandleLocked());
                VK_CHECK(vkImportSemaphoreWin32HandleKHR(device.getVkDevice(), info),
                        "Import D3D12 shared fence into Vulkan");
            } catch (RuntimeException | Error failure) {
                close();
                throw failure;
            }
        });
    }

    public static long adapterLuid(VulkanDevice device) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceIDProperties id = VkPhysicalDeviceIDProperties.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_ID_PROPERTIES);
            VkPhysicalDeviceProperties2 properties = VkPhysicalDeviceProperties2.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2).pNext(id);
            vkGetPhysicalDeviceProperties2(device.getPhysicalDevice(), properties);
            if (!id.deviceLUIDValid()) {
                throw new IllegalStateException("Minecraft Vulkan device does not expose a D3D12 adapter LUID");
            }
            long luid = id.deviceLUID().order(ByteOrder.LITTLE_ENDIAN).getLong(0);
            if (luid == 0) {
                throw new IllegalStateException("Minecraft Vulkan device returned a null adapter LUID");
            }
            return luid;
        }
    }

    /** Signal only after the ordered Minecraft copy/release fence has completed. */
    public void signal(long value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VK_CHECK(vkSignalSemaphore(device.getVkDevice(), VkSemaphoreSignalInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_SIGNAL_INFO).semaphore(semaphore).value(value)),
                    "Signal Vulkan/D3D12 shared timeline");
        }
    }

    public void await(long value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VK_CHECK(vkWaitSemaphores(device.getVkDevice(), VkSemaphoreWaitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_WAIT_INFO)
                    .pSemaphores(stack.longs(semaphore)).pValues(stack.longs(value)), Long.MAX_VALUE),
                    "Wait Vulkan/D3D12 shared timeline");
        }
    }

    @Override
    public void close() {
        if (semaphore != VK_NULL_HANDLE) {
            vkDestroySemaphore(device.getVkDevice(), semaphore, null);
            semaphore = VK_NULL_HANDLE;
        }
        if (borrow != null) {
            borrow.close();
            borrow = null;
        }
    }
}
