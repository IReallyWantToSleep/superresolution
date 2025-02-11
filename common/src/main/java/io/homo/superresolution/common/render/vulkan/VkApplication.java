package io.homo.superresolution.common.render.vulkan;

import io.homo.superresolution.common.impl.Destroyable;
import io.homo.superresolution.common.impl.Resizable;
import io.homo.superresolution.common.platform.Platform;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.LongBuffer;
import java.util.ArrayList;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memUTF8;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2;

public class VkApplication implements Destroyable, Resizable {
    public static final boolean ENABLE_VALIDATION = Platform.currentPlatform.isDevelopmentEnvironment();
    public static final Logger LOGGER = LoggerFactory.getLogger("SuperResolution-Vulkan");
    private static final int DEFAULT_API_VERSION = VK_API_VERSION_1_2;
    public final VkValidationLayers validationLayers;
    public final VkDeviceManager deviceManager;
    protected final ArrayList<String> instanceRequiredExtensions = new ArrayList<>();
    protected final ArrayList<String> deviceRequiredExtensions = new ArrayList<>();
    public VkInstance instance;
    public long commandPool;
    public VkCommandBuffer commandBuffer;
    protected int width;
    protected int height;
    private boolean needResize;

    protected VkApplication() {
        this.validationLayers = new VkValidationLayers(this);
        this.deviceManager = new VkDeviceManager(this);
        this.instanceRequiredExtensions.add(VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
    }

    public static VkApplication create() {
        VkApplication app = new VkApplication();
        if ((!VkValidationLayers.checkValidationLayerSupport() && ENABLE_VALIDATION)) {
            throw new VkException("Validation is necessary but not supported");
        }
        return app;
    }

    public static void main(String[] args) {
        ArrayList<String> supportedExtensions = new ArrayList<>();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int[] extensionCount = new int[1];
            vkEnumerateInstanceExtensionProperties((String) null, extensionCount, null);
            VkExtensionProperties.Buffer extensions = VkExtensionProperties.calloc(extensionCount[0], stack);
            vkEnumerateInstanceExtensionProperties((String) null, extensionCount, extensions);
            for (int i = 0; i < extensions.capacity(); i++) {
                supportedExtensions.add(extensions.get(i).extensionNameString());
            }
        }
        for (String extension : supportedExtensions) {
            System.out.println(extension);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private void createInstance() {
        try (MemoryStack stack = stackPush()) {
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                    .apiVersion(DEFAULT_API_VERSION)
                    .pEngineName(memUTF8("Engine"))
                    .engineVersion(VK_MAKE_VERSION(0, 1, 3))
                    .pApplicationName(memUTF8("App"))
                    .applicationVersion(VK_MAKE_VERSION(1, 0, 0));
            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                    .pApplicationInfo(appInfo)
                    .ppEnabledExtensionNames(getInstanceRequiredExtensions(stack));
            if (VkApplication.ENABLE_VALIDATION) {
                createInfo.ppEnabledLayerNames(validationLayers.validationLayersAsPointerBuffer(stack));
                VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
                validationLayers.populateDebugMessengerCreateInfo(debugCreateInfo);
                createInfo.pNext(debugCreateInfo.address());
            }
            PointerBuffer instancePtr = stack.mallocPointer(1);
            if (vkCreateInstance(createInfo, null, instancePtr) != VK_SUCCESS) {
                throw new VkException("Failed to create instance");
            }
            instance = new VkInstance(instancePtr.get(0), createInfo);
        }
    }

    private void initVulkan() {
        createInstance();
        if (ENABLE_VALIDATION)validationLayers.setupDebugMessenger();
        deviceManager.init();
        createCommand();
    }

    private PointerBuffer getInstanceRequiredExtensions(MemoryStack stack) {
        PointerBuffer extensions;
        //PointerBuffer glfwExtensions = glfwGetRequiredInstanceExtensions();
        extensions = stack.mallocPointer(instanceRequiredExtensions.size());
        //extensions.put(glfwExtensions);
        for (String name : instanceRequiredExtensions) {
            extensions.put(stack.UTF8(name));
        }
        return extensions.rewind();
    }

    public VkApplication addInstanceRequiredExtensions(String name) {
        if (this.instance != null) return this;
        instanceRequiredExtensions.add(name);
        return this;
    }

    private PointerBuffer getDeviceRequiredExtensions(MemoryStack stack) {
        PointerBuffer extensions = stack.mallocPointer(deviceRequiredExtensions.size());
        for (String name : deviceRequiredExtensions) {
            extensions.put(stack.UTF8(name));
        }
        return extensions.rewind();
    }

    public VkApplication addDeviceRequiredExtensions(String name) {
        if (this.instance != null) return this;
        deviceRequiredExtensions.add(name);
        return this;
    }

    public VkApplication init() {
        initVulkan();
        LOGGER.info("Vulkan初始化完成");
        return this;
    }

    @Override
    public void destroy() {
        VkDevice device = deviceManager.device;
        deviceManager.destroy();
        validationLayers.destroy();
        vkDestroyInstance(instance, null);
    }

    private void onResize(long window, int width, int height) {
        resize(width, height);
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        this.needResize = true;
    }

    public void onResize() {
        VkDevice device = deviceManager.device;
        vkDeviceWaitIdle(device);
    }

    public void createCommand() {
        {
            VkCommandPoolCreateInfo info = VkCommandPoolCreateInfo.calloc();
            info.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            info.queueFamilyIndex(deviceManager.queueFamilyIndices.graphicsFamily);
            LongBuffer ptr = MemoryStack.stackCallocLong(1);
            vkCreateCommandPool(deviceManager.device, info, null, ptr);
            commandPool = ptr.get(0);
        }
        {
            VkCommandBufferAllocateInfo info = VkCommandBufferAllocateInfo.calloc();
            info.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            info.commandPool(commandPool);
            info.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            info.commandBufferCount(1);
            PointerBuffer ptr = MemoryStack.stackCallocPointer(1);
            vkAllocateCommandBuffers(deviceManager.device, info, ptr);
            commandBuffer = new VkCommandBuffer(ptr.get(0), deviceManager.device);
        }
    }

    public VkCommandBuffer beginOneTimeSubmitCmd() {
        vkResetCommandPool(deviceManager.device, commandPool, 0);
        VkCommandBufferBeginInfo info = VkCommandBufferBeginInfo.calloc();
        info.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
        info.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
        vkBeginCommandBuffer(commandBuffer, info);
        return commandBuffer;
    }

    public void endOneTimeSubmitCmd() {
        VkSubmitInfo info = VkSubmitInfo.calloc();
        info.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
        PointerBuffer ptr = MemoryStack.stackCallocPointer(1);
        ptr.put(commandBuffer.address());
        info.pCommandBuffers(ptr);
        vkEndCommandBuffer(commandBuffer);
        vkQueueSubmit(deviceManager.graphicsQueue, info, VK_NULL_HANDLE);
        vkDeviceWaitIdle(deviceManager.device);
    }

    public void loop() {
        while (true) {
            if (needResize) {
                onResize();
                needResize = false;
            }
            vkDeviceWaitIdle(deviceManager.device);
        }
    }
}
