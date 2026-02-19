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

package io.homo.superresolution.core.graphics.vulkan.utils;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.Checks;
import org.lwjgl.system.FunctionProvider;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import static org.lwjgl.system.JNI.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.vulkan.VK10.VK_API_VERSION_1_0;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

/**
 * Since lwjgl-vulkan will be loaded into the LAYER PLUGIN,
 * Mixin VkInstance.class will be impossible.
 * <p>
 * F❤❤K package private classes.
 *
 * @author ChloePrime
 */
public class VkReflectionHelper {
    private static final Unsafe UNSAFE = runReflective(() -> {
        Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        return (Unsafe) theUnsafe.get(null);
    });

    @SuppressWarnings("deprecation")
    private static final MethodHandles.Lookup IMPL_LOOKUP = runReflective(() -> {
        // © burningtnt https://gist.github.com/burningtnt/e8f43d6917a60a3c2be59f41b2b2e653
        Field implLookup = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
        Object base = UNSAFE.staticFieldBase(implLookup);
        long l = UNSAFE.staticFieldOffset(implLookup);
        return (MethodHandles.Lookup) UNSAFE.getObject(base, l);
    });
    private static final VarHandle capabilitiesField = runReflective(() -> IMPL_LOOKUP.findVarHandle(
            Class.forName("org.lwjgl.vulkan.DispatchableHandleInstance"),
            "capabilities", VKCapabilitiesInstance.class
    ));
    private static final VarHandle addressField = runReflective(() -> IMPL_LOOKUP.findVarHandle(
            Class.forName("org.lwjgl.system.Pointer$Default"),
            "address", long.class
    ));
    private static final MethodHandle newVKCapabilitiesInstance = runReflective(() -> IMPL_LOOKUP.findConstructor(VKCapabilitiesInstance.class, MethodType.methodType(void.class, FunctionProvider.class, int.class, Set.class, Set.class)));
    private static final MethodHandle getEnabledExtensionSet = runReflective(() -> IMPL_LOOKUP.findStatic(VK.class, "getEnabledExtensionSet", MethodType.methodType(Set.class, int.class, PointerBuffer.class)));
    private static final Class<?> GlobalCommands = runReflective(() -> Class.forName("org.lwjgl.vulkan.VK$GlobalCommands"));
    private static final MethodHandle getGlobalCommands = runReflective(() -> IMPL_LOOKUP.findStatic(VK.class, "getGlobalCommands", MethodType.methodType(GlobalCommands)));
    // reflection replacement for VK's package-private methods.
    private static final VarHandle vkGetInstanceProcAddr = runReflective(() -> IMPL_LOOKUP.findVarHandle(GlobalCommands, "vkGetInstanceProcAddr", long.class));

    public static VkInstance createVkInstanceSafely(long handle, VkInstanceCreateInfo ci) {
        try {
            var capabilities = getInstanceCapabilities(handle, ci);
            var instance = (VkInstance) UNSAFE.allocateInstance(VkInstance.class);
            capabilitiesField.set(instance, capabilities);
            addressField.set(instance, handle);
            return instance;
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Copied from LWJGL3.
     *
     * @author Spasi
     */
    private static VKCapabilitiesInstance getInstanceCapabilities(long handle, VkInstanceCreateInfo ci) {
        VkApplicationInfo appInfo = ci.pApplicationInfo();

        int apiVersion = appInfo != null && appInfo.apiVersion() != 0
                ? appInfo.apiVersion()
                : VK_API_VERSION_1_0;

        try {
            return (VKCapabilitiesInstance) newVKCapabilitiesInstance.invoke((FunctionProvider) functionName -> {
                long address = callPPP(handle, memAddress(functionName), getVkGetInstanceProcAddr());
                if (address == NULL && Checks.DEBUG_FUNCTIONS) {
                    //lwjgl3.3.3-没有这个函数，鉴于用不到，直接注释了
                    //apiLogMissing("VK instance", functionName);
                }
                return address;
            }, apiVersion, getEnabledExtensionSet.invoke(apiVersion, ci.ppEnabledExtensionNames()), getAvailableDeviceExtensions(handle));
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Fixed version after lwjgl3 3.4.0.
     * Copied from LWJGL3, replace package private access with reflection magic :)
     *
     * @author Spasi, ChloePrime
     */
    private static Set<String> getAvailableDeviceExtensions(long instance) {
        HashSet<String> extensions = new HashSet<>();

        out:
        try (MemoryStack stack = stackPush()) {
            IntBuffer ip = stack.callocInt(1);

            // long GetInstanceProcAddr                = VK.getGlobalCommands().vkGetInstanceProcAddr;
            long GetInstanceProcAddr = getVkGetInstanceProcAddr();
            long EnumeratePhysicalDevices = callPPP(instance, memAddress(stack.ASCII("vkEnumeratePhysicalDevices")), GetInstanceProcAddr);
            long EnumerateDeviceExtensionProperties = callPPP(instance, memAddress(stack.ASCII("vkEnumerateDeviceExtensionProperties")), GetInstanceProcAddr);
            if (EnumeratePhysicalDevices == NULL || EnumerateDeviceExtensionProperties == NULL) {
                break out;
            }

            int err = callPPPI(instance, memAddress(ip), NULL, EnumeratePhysicalDevices);
            if (err != VK_SUCCESS || ip.get(0) == 0) {
                break out;
            }

            PointerBuffer physicalDevices = stack.mallocPointer(ip.get(0));
            err = callPPPI(instance, memAddress(ip), memAddress(physicalDevices), EnumeratePhysicalDevices);
            if (err != VK_SUCCESS) {
                break out;
            }

            for (int i = 0; i < physicalDevices.remaining(); i++) {
                err = callPPPPI(physicalDevices.get(i), NULL, memAddress(ip), NULL, EnumerateDeviceExtensionProperties);
                if (err != VK_SUCCESS || ip.get(0) == 0) {
                    continue;
                }

                try (VkExtensionProperties.Buffer deviceExtensions = VkExtensionProperties.malloc(ip.get(0))) {
                    err = callPPPPI(physicalDevices.get(i), NULL, memAddress(ip), deviceExtensions.address(), EnumerateDeviceExtensionProperties);
                    if (err != VK_SUCCESS) {
                        continue;
                    }

                    for (int j = 0; j < deviceExtensions.remaining(); j++) {
                        extensions.add(deviceExtensions.get(j).extensionNameString());
                    }
                }
            }
        }

        return extensions;
    }

    public static long getVkGetInstanceProcAddr() {
        try {
            return (Long) vkGetInstanceProcAddr.get(getGlobalCommands.invoke());
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    private static <T> T runReflective(Callable<T> getter) {
        try {
            return getter.call();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
}
