package io.homo.superresolution.common.render;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.impl.Pair;
import io.homo.superresolution.common.render.interop.GlVkInteropManager;
import io.homo.superresolution.common.render.vulkan.VkApplication;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK11;

import java.lang.reflect.Array;
import java.nio.IntBuffer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.homo.superresolution.common.render.gl.Gl.*;
import static io.homo.superresolution.common.render.gl.GlConst.*;

public class GraphicsCapabilities {
    private static final ArrayList<Pair<Integer, Integer>> glVersions = new ArrayList<>();
    private static Set<String> glExtensions = null;

    public static void init() {
    }

    private static int[] detectGLVersion() {
        int[] version = new int[2];
        int major = glGetInteger(GL_MAJOR_VERSION);
        int minor = glGetInteger(GL_MINOR_VERSION);
        version[0] = major;
        version[1] = minor;
        return version;
    }

    public static void detectSupportedVersions() {
        int[][] versionMatrix = {
                {4, 6},
                {4, 5},
                {4, 3},
                {3, 3},
                {3, 2},
        };
        for (int[] version : versionMatrix) {
            int major = version[0];
            int minor = version[1];

            GLFW.glfwDefaultWindowHints();
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, major);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, minor);

            long testWindow = GLFW.glfwCreateWindow(1, 1, "", 0, 0);
            if (testWindow != 0) {
                GLFW.glfwMakeContextCurrent(testWindow);
                int actualMajor = GLFW.glfwGetWindowAttrib(testWindow, GLFW.GLFW_CONTEXT_VERSION_MAJOR);
                int actualMinor = GLFW.glfwGetWindowAttrib(testWindow, GLFW.GLFW_CONTEXT_VERSION_MINOR);
                glVersions.add(Pair.of(actualMajor, actualMinor));
                GLFW.glfwDestroyWindow(testWindow);
                SuperResolution.LOGGER.info("添加可用OpenGL版本 {}.{}", actualMajor, actualMinor);
            }
        }
        SuperResolution.LOGGER.info("最高OpenGL版本 {}.{}", getHighestOpenGLVersion().left(), getHighestOpenGLVersion().right());
    }

    public static Pair<Integer, Integer> getHighestOpenGLVersion() {
        return glVersions.stream()
                .max(Comparator.comparingInt((Pair<Integer, Integer> p) -> p.left())
                        .thenComparingInt(Pair::right))
                .orElse(null);
    }

    private static Set<String> detectGLExtensions() {
        int count = glGetInteger(GL_NUM_EXTENSIONS);
        return Collections.unmodifiableSet(
                IntStream.range(0, count)
                        .mapToObj(i -> glGetStringi(GL_EXTENSIONS, i))
                        .collect(Collectors.toCollection(() ->
                                new TreeSet<>(String.CASE_INSENSITIVE_ORDER)))
        );
    }


    private static int[] detectVulkanVersion() {
        if (!isVulkanSupported()) {
            return new int[]{0, 0, 0};
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer version = stack.mallocInt(1);
            VK11.vkEnumerateInstanceVersion(version);
            return new int[]{
                    VK10.VK_API_VERSION_MAJOR(version.get(0)),
                    VK10.VK_API_VERSION_MINOR(version.get(0)),
                    VK10.VK_API_VERSION_PATCH(version.get(0))
            };
        }
    }

    private static boolean isVulkanSupported() {
        return GlVkInteropManager.isSupportVulkan();
    }

    public static int[] getGLVersion() {
        return detectGLVersion();
    }

    public static String getGLVersionString() {
        int[] glVersion = detectGLVersion();
        return glVersion[0] + "." + glVersion[1];
    }

    public static Set<String> getGLExtensions() {
        if (glExtensions == null) {
            glExtensions = Collections.unmodifiableSet(detectGLExtensions());
        }
        return glExtensions;
    }

    public static boolean hasGLExtension(String name) {
        if (glExtensions == null) {
            glExtensions = Collections.unmodifiableSet(detectGLExtensions());
        }
        return glExtensions.contains(name);
    }

    public static int[] getVulkanVersion() {
        return detectVulkanVersion();
    }

    public static String getVulkanVersionString() {
        int[] vkVersion = detectVulkanVersion();
        return vkVersion[0] + "." + vkVersion[1] + "." + vkVersion[2];
    }

    public static boolean hasVulkanDeviceExtension(String name) {
        if (isVulkanSupported()) {
            VkApplication vk = SuperResolution.interopManager.vulkanApp;
            return vk.deviceManager.deviceExtensions.contains(name);
        }
        return false;
    }

    public static Set<String> getVulkanDeviceExtensions() {
        if (isVulkanSupported()) {
            VkApplication vk = SuperResolution.interopManager.vulkanApp;
            return Set.copyOf(
                    vk.deviceManager.deviceExtensions.stream()
                            .collect(Collectors.toCollection(() ->
                                    new TreeSet<>(String.CASE_INSENSITIVE_ORDER)))
            );
        }
        return new HashSet<>();
    }

}