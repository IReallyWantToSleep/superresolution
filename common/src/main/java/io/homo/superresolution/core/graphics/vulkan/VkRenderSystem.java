package io.homo.superresolution.core.graphics.vulkan;

import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.system.IRenderSystem;
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
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2;
import static org.lwjgl.vulkan.VK11.vkGetPhysicalDeviceFeatures2;
import static org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2;
import static org.lwjgl.vulkan.VK12.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES;

public class VkRenderSystem implements IRenderSystem {
    public static final Logger LOGGER = LoggerFactory.getLogger("SuperResolution-Vulkan");
    public static final boolean ENABLE_VALIDATION = VulkanValidationLayers.checkValidationLayerSupport();
    private static final int DEFAULT_API_VERSION = VK_API_VERSION_1_2;

    private final List<String> instanceExtensions = new ArrayList<>();
    private final List<String> deviceExtensions = new ArrayList<>();
    protected VulkanValidationLayers validationLayers;

    public VkInstance getVulkanInstance() {
        return instance;
    }

    protected VkInstance instance;
    protected VulkanCapabilities capabilities = new VulkanCapabilities();
    private VulkanDevice vulkanDevice;

    public VkRenderSystem() {
    }

    private static PointerBuffer asPointerBuffer(MemoryStack stack, List<String> list) {
        PointerBuffer buffer = stack.mallocPointer(list.size());
        list.forEach(e -> buffer.put(stack.UTF8(e)));
        return buffer.rewind();
    }

    @Override
    public IDevice device() {
        return vulkanDevice;
    }

    @Override
    public void finish() {
        vkDeviceWaitIdle(vulkanDevice.getVkDevice());
    }

    public VkRenderSystem addInstanceExtension(String ext) {
        instanceExtensions.add(ext);
        return this;
    }

    public VkRenderSystem addDeviceExtension(String ext) {
        deviceExtensions.add(ext);
        return this;
    }

    public VulkanCapabilities getCapabilities() {
        return capabilities;
    }

    @Override
    public void initRenderSystem() {
        createInstance();

        validationLayers = new VulkanValidationLayers(instance);
        if (ENABLE_VALIDATION) validationLayers.setupDebugMessenger();
        VkPhysicalDevice physicalDevice = selectPhysicalDevice();
        capabilities.init(instance, physicalDevice);
        this.vulkanDevice = createLogicalDeviceWithCapabilities(physicalDevice);
        vulkanDevice.getCommandManager().init();
        LOGGER.info("Vulkan 初始化完成");
    }

    @Override
    public void destroyRenderSystem() {
        if (vulkanDevice != null) {
            vulkanDevice.destroy();
            vkDestroyDevice(vulkanDevice.getVkDevice(), null);
            vulkanDevice = null;
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
                createInfo.ppEnabledLayerNames(
                        VulkanValidationLayers.getValidationLayersPointerBuffer(stack)
                );
                VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
                VulkanValidationLayers.populateDebugMessengerCreateInfo(debugCreateInfo);
                createInfo.pNext(debugCreateInfo.address());
            }

            PointerBuffer instancePtr = stack.mallocPointer(1);
            VK_CHECK(vkCreateInstance(createInfo, null, instancePtr), "Failed to create VkInstance");
            instance = new VkInstance(instancePtr.get(0), createInfo);
        }
    }

    private VkPhysicalDevice selectPhysicalDevice() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer deviceCount = stack.ints(0);
            VK_CHECK(vkEnumeratePhysicalDevices(instance, deviceCount, null));
            if (deviceCount.get(0) == 0) {
                throw new RuntimeException("No Vulkan-compatible GPU found");
            }
            PointerBuffer devices = stack.mallocPointer(deviceCount.get(0));
            VK_CHECK(vkEnumeratePhysicalDevices(instance, deviceCount, devices));
            return new VkPhysicalDevice(devices.get(0), instance);
        }
    }

    private VulkanDevice createLogicalDeviceWithCapabilities(VkPhysicalDevice physicalDevice) {
        try (MemoryStack stack = stackPush()) {
            int graphicsFamilyIndex = findGraphicsQueueFamilyIndex(stack, physicalDevice);
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
            VkPhysicalDeviceVulkan12Features features12 = VkPhysicalDeviceVulkan12Features.calloc()
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES);

            VkPhysicalDeviceFeatures2 features2 = VkPhysicalDeviceFeatures2.calloc()
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2)
                    .pNext(features12);

            vkGetPhysicalDeviceFeatures2(physicalDevice, features2);

            if (features12.shaderFloat16()) {
                features12.shaderFloat16(true);
            }
            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack)
                    .pNext(features12)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                    .pQueueCreateInfos(queueCreateInfos)
                    .ppEnabledExtensionNames(asPointerBuffer(stack, enableDeviceExts));

            if (ENABLE_VALIDATION)
                createInfo.ppEnabledLayerNames(validationLayers.getValidationLayersPointerBuffer(stack));

            if (capabilities.getDeviceFeatures() != null) {
                createInfo.pEnabledFeatures(capabilities.getDeviceFeatures());
            }

            PointerBuffer pDevice = stack.mallocPointer(1);
            VK_CHECK(vkCreateDevice(physicalDevice, createInfo, null, pDevice),
                    "Failed to create logical device");
            return new VulkanDevice(
                    physicalDevice,
                    new VkDevice(pDevice.get(0), physicalDevice, createInfo),
                    graphicsFamilyIndex
            );
        }
    }

    private int findGraphicsQueueFamilyIndex(MemoryStack stack, VkPhysicalDevice physicalDevice) {
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
}
