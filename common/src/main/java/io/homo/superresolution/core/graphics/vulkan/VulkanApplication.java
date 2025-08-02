package io.homo.superresolution.core.graphics.vulkan;

import io.homo.superresolution.core.graphics.vulkan.cmd.VulkanCommandManager;
import io.homo.superresolution.core.graphics.vulkan.utils.VulkanCapabilities;
import io.homo.superresolution.core.graphics.vulkan.utils.VulkanValidationLayers;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static io.homo.superresolution.core.graphics.vulkan.utils.VulkanUtils.VK_CHECK;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memUTF8;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRDedicatedAllocation.VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalMemory.VK_KHR_EXTERNAL_MEMORY_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalMemoryCapabilities.VK_KHR_EXTERNAL_MEMORY_CAPABILITIES_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalMemoryWin32.VK_KHR_EXTERNAL_MEMORY_WIN32_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalSemaphore.VK_KHR_EXTERNAL_SEMAPHORE_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalSemaphoreCapabilities.VK_KHR_EXTERNAL_SEMAPHORE_CAPABILITIES_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalSemaphoreWin32.VK_KHR_EXTERNAL_SEMAPHORE_WIN32_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2;

public class VulkanApplication {
    public static final Logger LOGGER = LoggerFactory.getLogger("SuperResolution-Vulkan");
    public static final boolean ENABLE_VALIDATION = VulkanValidationLayers.checkValidationLayerSupport();
    private static final int DEFAULT_API_VERSION = VK_API_VERSION_1_2;

    private final List<String> instanceExtensions = new ArrayList<>();
    private final List<String> deviceExtensions = new ArrayList<>();
    protected VulkanValidationLayers validationLayers;
    protected VkInstance instance;
    protected VkPhysicalDevice physicalDevice;
    protected VkDevice device;
    protected VulkanCapabilities capabilities = new VulkanCapabilities();
    private VulkanCommandManager commandManager;

    public VulkanApplication() {
    }

    private static PointerBuffer asPointerBuffer(MemoryStack stack, List<String> list) {
        PointerBuffer buffer = stack.mallocPointer(list.size());
        list.forEach(e -> buffer.put(stack.UTF8(e)));
        return buffer.rewind();
    }

    public static void main(String[] args) {
        VulkanApplication vulkanApp = new VulkanApplication();
        vulkanApp
                .addInstanceExtension(VK_KHR_EXTERNAL_SEMAPHORE_CAPABILITIES_EXTENSION_NAME)
                .addInstanceExtension(VK_KHR_EXTERNAL_MEMORY_CAPABILITIES_EXTENSION_NAME)
                .addInstanceExtension(VK_EXT_DEBUG_UTILS_EXTENSION_NAME)
                .addDeviceExtension(VK_KHR_EXTERNAL_SEMAPHORE_EXTENSION_NAME)
                .addDeviceExtension(VK_KHR_EXTERNAL_MEMORY_EXTENSION_NAME)
                .addDeviceExtension(VK_KHR_EXTERNAL_MEMORY_WIN32_EXTENSION_NAME)
                .addDeviceExtension(VK_KHR_EXTERNAL_SEMAPHORE_WIN32_EXTENSION_NAME)
                .addDeviceExtension(VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME);
        vulkanApp.init();
    }

    public VkDevice getDevice() {
        return device;
    }

    public VkPhysicalDevice getPhysicalDevice() {
        return physicalDevice;
    }

    public VkInstance getInstance() {
        return instance;
    }

    public VulkanValidationLayers getValidationLayers() {
        return validationLayers;
    }

    public VulkanCapabilities getCapabilities() {
        return capabilities;
    }

    public VulkanApplication addInstanceExtension(String ext) {
        instanceExtensions.add(ext);
        return this;
    }

    public VulkanApplication addDeviceExtension(String ext) {
        deviceExtensions.add(ext);
        return this;
    }

    public void init() {
        validationLayers = new VulkanValidationLayers(this);
        commandManager = new VulkanCommandManager(this);
        createInstance();
        if (ENABLE_VALIDATION) validationLayers.setupDebugMessenger();
        selectPhysicalDevice();
        capabilities.init(getInstance(), getPhysicalDevice());
        createLogicalDeviceWithCapabilities();
        commandManager.init();
        LOGGER.info("Vulkan 初始化完成");
    }

    private void createInstance() {
        try (MemoryStack stack = stackPush()) {
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                    .apiVersion(DEFAULT_API_VERSION)
                    .pEngineName(memUTF8("Engine"))
                    .engineVersion(VK_MAKE_VERSION(0, 1, 0))
                    .pApplicationName(memUTF8("App"))
                    .applicationVersion(VK_MAKE_VERSION(1, 0, 0));

            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                    .pApplicationInfo(appInfo)
                    .ppEnabledExtensionNames(asPointerBuffer(stack, instanceExtensions));

            if (ENABLE_VALIDATION) {
                createInfo.ppEnabledLayerNames(validationLayers.getValidationLayersPointerBuffer(stack));
                VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
                validationLayers.populateDebugMessengerCreateInfo(debugCreateInfo);
                createInfo.pNext(debugCreateInfo.address());
            }

            PointerBuffer instancePtr = stack.mallocPointer(1);
            VK_CHECK(vkCreateInstance(createInfo, null, instancePtr), "Failed to create VkInstance");
            instance = new VkInstance(instancePtr.get(0), createInfo);
        }
    }

    private void selectPhysicalDevice() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer deviceCount = stack.ints(0);
            VK_CHECK(vkEnumeratePhysicalDevices(instance, deviceCount, null));
            if (deviceCount.get(0) == 0) {
                throw new RuntimeException("No Vulkan-compatible GPU found");
            }
            PointerBuffer devices = stack.mallocPointer(deviceCount.get(0));
            VK_CHECK(vkEnumeratePhysicalDevices(instance, deviceCount, devices));
            physicalDevice = new VkPhysicalDevice(devices.get(0), instance);
        }
    }

    private void createLogicalDeviceWithCapabilities() {
        try (MemoryStack stack = stackPush()) {
            int graphicsFamilyIndex = findGraphicsQueueFamilyIndex(stack);
            if (graphicsFamilyIndex == -1) throw new RuntimeException("No suitable queue family found");

            VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(1, stack);
            queueCreateInfos.get(0)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .queueFamilyIndex(graphicsFamilyIndex)
                    .pQueuePriorities(stack.floats(1.0f));

            List<String> enableDeviceExts = new ArrayList<>();
            List<String> supportedDeviceExts = capabilities.getDeviceExtensions();
            for (String ext : deviceExtensions) {
                if (supportedDeviceExts.contains(ext)) {
                    enableDeviceExts.add(ext);
                } else {
                    LOGGER.warn("扩展 {} 不被当前物理设备支持，已跳过", ext);
                }
            }

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                    .pQueueCreateInfos(queueCreateInfos)
                    .ppEnabledExtensionNames(asPointerBuffer(stack, enableDeviceExts));

            if (ENABLE_VALIDATION)
                createInfo.ppEnabledLayerNames(validationLayers.getValidationLayersPointerBuffer(stack));

            if (capabilities.getDeviceFeatures() != null) {
                createInfo.pEnabledFeatures(capabilities.getDeviceFeatures());
            }

            PointerBuffer pDevice = stack.mallocPointer(1);
            VK_CHECK(vkCreateDevice(physicalDevice, createInfo, null, pDevice), "Failed to create logical device");
            device = new VkDevice(pDevice.get(0), physicalDevice, createInfo);
        }
    }

    private int findGraphicsQueueFamilyIndex(MemoryStack stack) {

        IntBuffer queueFamilyCount = stack.ints(0);
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCount, null);
        VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.malloc(queueFamilyCount.get(0), stack);

        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCount, queueFamilies);
        for (int i = 0; i < queueFamilies.capacity(); i++) {
            if ((queueFamilies.get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
                return i;
            }
        }
        return -1;

    }

    public void destroy() {
        if (device != null) {
            vkDestroyDevice(device, null);
            device = null;
        }
        if (validationLayers != null) {
            validationLayers.destroy();
            validationLayers = null;
        }
        if (instance != null) {
            vkDestroyInstance(instance, null);
            instance = null;
        }
        if (capabilities != null) {
            capabilities.destroy();
            capabilities = null;
        }
        LOGGER.info("Vulkan 已销毁");
    }
}