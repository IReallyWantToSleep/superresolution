package io.homo.superresolution.render.vulkan;

import dev.architectury.platform.Platform;
import io.homo.superresolution.impl.Destroyable;
import io.homo.superresolution.impl.Resizable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.LongBuffer;
import java.util.ArrayList;

import static io.homo.superresolution.render.vulkan.Utils.asPointerBuffer;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memUTF8;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkDestroySwapchainKHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2;

public class VkApplication implements Destroyable, Resizable {
    public static final Logger LOGGER = LoggerFactory.getLogger("SuperResolution-Vulkan");
    private static final int DEFAULT_API_VERSION = VK_API_VERSION_1_2;
    public final VkValidationLayers validationLayers;
    public final VkDeviceManager deviceManager;
    protected final ArrayList<String> instanceRequiredExtensions = new ArrayList<>();
    protected final ArrayList<String> deviceRequiredExtensions = new ArrayList<>();
    public VkInstance instance;
    public VkSwapChainManager swapChainManager;
    public long renderPass;
    public long window;
    public VkCommandPool command;
    public long surface;
    protected int width;
    protected int height;
    private boolean needResize;

    protected VkApplication() {
        this.validationLayers = new VkValidationLayers(this);
        this.deviceManager = new VkDeviceManager(this);
        this.swapChainManager = new VkSwapChainManager(this);
        this.instanceRequiredExtensions.add(VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
        this.command = new VkCommandPool(this);
    }

    public static VkApplication create() {
        VkApplication app = new VkApplication();
        if (!VkValidationLayers.checkValidationLayerSupport()) {
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
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack);
            appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
            appInfo.apiVersion(DEFAULT_API_VERSION);
            appInfo.pEngineName(memUTF8("Engine"));
            appInfo.engineVersion(VK_MAKE_VERSION(0, 1, 3));
            appInfo.pApplicationName(memUTF8("App"));
            appInfo.applicationVersion(VK_MAKE_VERSION(1, 0, 0));
            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
            createInfo.pApplicationInfo(appInfo);
            createInfo.ppEnabledExtensionNames(getInstanceRequiredExtensions(stack));
            createInfo.ppEnabledLayerNames(validationLayers.validationLayersAsPointerBuffer(stack));
            VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
            validationLayers.populateDebugMessengerCreateInfo(debugCreateInfo);
            createInfo.pNext(debugCreateInfo.address());
            PointerBuffer instancePtr = stack.mallocPointer(1);
            if (vkCreateInstance(createInfo, null, instancePtr) != VK_SUCCESS) {
                throw new VkException("Failed to create instance");
            }
            instance = new VkInstance(instancePtr.get(0), createInfo);
        }
    }

    private void initVulkan() {
        createInstance();
        createSurface();
        validationLayers.setupDebugMessenger();
        deviceManager.init();
        swapChainManager.init();
        createRenderPass();
        swapChainManager.createFramebuffers();
        command.createCommandPool();
        command.createCommandBuffers();
    }

    private PointerBuffer getInstanceRequiredExtensions(MemoryStack stack) {
        PointerBuffer glfwExtensions = glfwGetRequiredInstanceExtensions();
        PointerBuffer extensions;
        if (glfwExtensions != null) {
            extensions = stack.mallocPointer(glfwExtensions.capacity() + instanceRequiredExtensions.size());
        } else {
            throw new VkException();
        }
        extensions.put(glfwExtensions);
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
        createWindow();
        initVulkan();
        LOGGER.info("Vulkan初始化完成");
        return this;
    }

    @Override
    public void destroy() {
        VkDevice device = deviceManager.device;
        swapChainManager.swapChainFramebuffers.forEach(framebuffer -> vkDestroyFramebuffer(device, framebuffer, null));
        try (MemoryStack stack = stackPush()) {
            vkFreeCommandBuffers(device, command.commandPool, asPointerBuffer(stack, command.commandBuffers));
        }
        vkDestroyRenderPass(device, renderPass, null);
        swapChainManager.swapChainImageViews.forEach(imageView -> vkDestroyImageView(device, imageView, null));
        vkDestroySwapchainKHR(device, swapChainManager.swapChain, null);
        deviceManager.destroy();
        vkDestroySurfaceKHR(instance, surface, null);
        validationLayers.destroy();
        vkDestroyInstance(instance, null);
    }

    private void createWindow() {
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        window = glfwCreateWindow(854, 480, "SuperResolution Vulkan", NULL, NULL);
        glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
        if (window == NULL) {
            throw new VkException("Cannot create window");
        }
        if (!Platform.isDevelopmentEnvironment()) glfwHideWindow(window);
    }

    private void onResize(long window, int width, int height) {
        resize(width, height);
    }

    private void createRenderPass() {

        try (MemoryStack stack = stackPush()) {

            VkAttachmentDescription.Buffer colorAttachment = VkAttachmentDescription.calloc(1, stack);
            colorAttachment.format(swapChainManager.swapChainImageFormat);
            colorAttachment.samples(VK_SAMPLE_COUNT_1_BIT);
            colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
            colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            colorAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            colorAttachment.finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            VkAttachmentReference.Buffer colorAttachmentRef = VkAttachmentReference.calloc(1, stack);
            colorAttachmentRef.attachment(0);
            colorAttachmentRef.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack);
            subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
            subpass.colorAttachmentCount(1);
            subpass.pColorAttachments(colorAttachmentRef);

            VkSubpassDependency.Buffer dependency = VkSubpassDependency.calloc(1, stack);
            dependency.srcSubpass(VK_SUBPASS_EXTERNAL);
            dependency.dstSubpass(0);
            dependency.srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            dependency.srcAccessMask(0);
            dependency.dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            dependency.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);

            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
            renderPassInfo.pAttachments(colorAttachment);
            renderPassInfo.pSubpasses(subpass);
            renderPassInfo.pDependencies(dependency);

            LongBuffer pRenderPass = stack.mallocLong(1);

            if (vkCreateRenderPass(deviceManager.device, renderPassInfo, null, pRenderPass) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create render pass");
            }

            renderPass = pRenderPass.get(0);
        }
    }

    private void createSurface() {
        try (MemoryStack stack = stackPush()) {
            LongBuffer pSurface = stack.longs(VK_NULL_HANDLE);
            if (glfwCreateWindowSurface(instance, window, null, pSurface) != VK_SUCCESS) {
                throw new VkException("Failed to create window surface");
            }
            surface = pSurface.get(0);
        }
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
        swapChainManager.swapChainFramebuffers.forEach(framebuffer -> vkDestroyFramebuffer(device, framebuffer, null));
        try (MemoryStack stack = stackPush()) {
            vkFreeCommandBuffers(device, command.commandPool, asPointerBuffer(stack, command.commandBuffers));
        }
        vkDestroyRenderPass(device, renderPass, null);
        swapChainManager.swapChainImageViews.forEach(imageView -> vkDestroyImageView(device, imageView, null));
        vkDestroySwapchainKHR(device, swapChainManager.swapChain, null);
        swapChainManager.init();
        createRenderPass();
        swapChainManager.createFramebuffers();
        command.createCommandPool();
        command.createCommandBuffers();
    }

    public void loop() {
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            if (needResize) {
                onResize();
                needResize = false;
            }
        }
        vkDeviceWaitIdle(deviceManager.device);
    }
}
