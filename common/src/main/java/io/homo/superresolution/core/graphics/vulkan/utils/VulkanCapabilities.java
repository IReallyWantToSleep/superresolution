package io.homo.superresolution.core.graphics.vulkan.utils;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanCapabilities {
    private VkInstance instance;
    private VkPhysicalDevice physicalDevice;
    private List<String> instanceExtensions = new ArrayList<>();
    private List<String> deviceExtensions = new ArrayList<>();
    private VkPhysicalDeviceProperties deviceProperties;
    private VkPhysicalDeviceFeatures deviceFeatures;
    private VkPhysicalDeviceMemoryProperties memoryProperties;

    private boolean initialized = false;

    public VulkanCapabilities() {
    }

    public void init(VkInstance instance, VkPhysicalDevice physicalDevice) {
        if (initialized) return;
        this.instance = instance;
        this.physicalDevice = physicalDevice;
        try (MemoryStack stack = stackPush()) {

            IntBuffer extensionCount = stack.ints(0);
            vkEnumerateInstanceExtensionProperties((String) null, extensionCount, null);
            VkExtensionProperties.Buffer iExts = VkExtensionProperties.malloc(extensionCount.get(0));
            vkEnumerateInstanceExtensionProperties((String) null, extensionCount, iExts);
            instanceExtensions.clear();
            for (int i = 0; i < iExts.capacity(); ++i) {
                instanceExtensions.add(iExts.get(i).extensionNameString());
            }

            extensionCount.rewind();
            vkEnumerateDeviceExtensionProperties(physicalDevice, (String) null, extensionCount, null);
            VkExtensionProperties.Buffer dExts = VkExtensionProperties.malloc(extensionCount.get(0));
            vkEnumerateDeviceExtensionProperties(physicalDevice, (String) null, extensionCount, dExts);
            deviceExtensions.clear();
            for (int i = 0; i < dExts.capacity(); ++i) {
                deviceExtensions.add(dExts.get(i).extensionNameString());
            }

            deviceProperties = VkPhysicalDeviceProperties.malloc();
            vkGetPhysicalDeviceProperties(physicalDevice, deviceProperties);

            deviceFeatures = VkPhysicalDeviceFeatures.malloc();
            vkGetPhysicalDeviceFeatures(physicalDevice, deviceFeatures);

            memoryProperties = VkPhysicalDeviceMemoryProperties.malloc();
            vkGetPhysicalDeviceMemoryProperties(physicalDevice, memoryProperties);

            initialized = true;
        }
    }

    public void destroy() {
        if (deviceProperties != null) {
            deviceProperties.free();
            deviceProperties = null;
        }
        if (deviceFeatures != null) {
            deviceFeatures.free();
            deviceFeatures = null;
        }
        if (memoryProperties != null) {
            memoryProperties.free();
            memoryProperties = null;
        }
        instanceExtensions = Collections.emptyList();
        deviceExtensions = Collections.emptyList();
        initialized = false;
    }

    public List<String> getInstanceExtensions() {
        return instanceExtensions;
    }

    public List<String> getDeviceExtensions() {
        return deviceExtensions;
    }

    public VkPhysicalDeviceProperties getDeviceProperties() {
        return deviceProperties;
    }

    public VkPhysicalDeviceFeatures getDeviceFeatures() {
        return deviceFeatures;
    }

    public VkPhysicalDeviceMemoryProperties getMemoryProperties() {
        return memoryProperties;
    }

    public boolean isInitialized() {
        return initialized;
    }
}