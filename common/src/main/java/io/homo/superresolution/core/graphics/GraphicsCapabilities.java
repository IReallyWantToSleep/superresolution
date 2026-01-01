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

package io.homo.superresolution.core.graphics;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.impl.Pair;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.IntBuffer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.lwjgl.opengl.GL43.*;

public class GraphicsCapabilities {
    public static final Logger LOGGER = LoggerFactory.getLogger("SuperResolution-GraphicsCapabilities");

    private static final ArrayList<Pair<Integer, Integer>> glVersions = new ArrayList<>();
    private static Set<String> glExtensions = null;
    private static GpuVendor gpuVendor = null;
    private static int[] glVersion = new int[]{-1, -1};

    public static void init() {
    }

    public static GpuVendor detectGpuVendor() {
        if (gpuVendor == null) {
            String renderer = glGetString(GL_RENDERER);
            String vendor = glGetString(GL_VENDOR);
            gpuVendor = parseVendorFromName(vendor + " " + renderer);
        }
        return gpuVendor;
    }

    private static GpuVendor parseVendorFromName(String rawName) {
        if (rawName == null) return GpuVendor.Unknown;

        String lowerName = rawName.toLowerCase();
        if (lowerName.contains("nvidia") || lowerName.contains("geforce")) {
            return GpuVendor.Nvidia;
        } else if (lowerName.contains("amd") || lowerName.contains("radeon") || lowerName.contains("ati ")) {
            return GpuVendor.Amd;
        } else if (lowerName.contains("intel") || lowerName.contains("iris") || lowerName.contains("hd graphics")) {
            return GpuVendor.Intel;
        }
        return GpuVendor.Unknown;
    }

    private static int[] detectGLVersion() {
        if (glVersion[0] != -1 && glVersion[1] != -1) {
            return glVersion;
        }
        int major = glGetInteger(GL_MAJOR_VERSION);
        int minor = glGetInteger(GL_MINOR_VERSION);
        glVersion[0] = major;
        glVersion[1] = minor;
        return glVersion;
    }

    public static void detectSupportedVersions() {
        glVersions.clear();
        int[][] versionMatrix = {
                {4, 6},
                {4, 5},
                {4, 3},
                {4, 2},
                {4, 1}
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
                LOGGER.info("添加可用OpenGL版本 {}.{}", actualMajor, actualMinor);
                break;
            }
        }
        LOGGER.info("最高OpenGL版本 {}.{}", getHighestOpenGLVersion().left(), getHighestOpenGLVersion().right());
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
        return RenderSystems.isSupportVulkan();
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
        if (!isVulkanSupported()) {
            return "0.0.0";
        }
        int[] vkVersion = detectVulkanVersion();
        return vkVersion[0] + "." + vkVersion[1] + "." + vkVersion[2];
    }

    public static boolean hasVulkanDeviceExtension(String name) {
        if (isVulkanSupported()) {
            return RenderSystems.vulkan().getCapabilities().getDeviceExtensions().contains(name);
        }
        return false;
    }

    public static Set<String> getVulkanDeviceExtensions() {
        if (isVulkanSupported()) {
            return Set.copyOf(
                    RenderSystems.vulkan().getCapabilities().getDeviceExtensions().stream()
                            .collect(Collectors.toCollection(() ->
                                    new TreeSet<>(String.CASE_INSENSITIVE_ORDER)))
            );
        }
        return new HashSet<>();
    }

}