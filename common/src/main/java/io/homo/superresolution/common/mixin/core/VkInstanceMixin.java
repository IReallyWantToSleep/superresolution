package io.homo.superresolution.common.mixin.core;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = VkInstance.class, remap = false)
public class VkInstanceMixin {
    @Redirect(
            method = "getAvailableDeviceExtensions",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/system/MemoryStack;push()Lorg/lwjgl/system/MemoryStack;"
            )
    )
    private static MemoryStack fuckMoreMemoryStackPushAndPop(MemoryStack instance) {
        return new MemoryStack(null, 1, 1) {
            @Override
            public void close() {
            }
        };
    }

    @Redirect(
            method = "getAvailableDeviceExtensions",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/vulkan/VkExtensionProperties;malloc(ILorg/lwjgl/system/MemoryStack;)Lorg/lwjgl/vulkan/VkExtensionProperties$Buffer;"
            )
    )
    private static VkExtensionProperties.Buffer fuckMoreMemoryStackMalloc(int capacity, MemoryStack stack) {
        return VkExtensionProperties.malloc(capacity);
    }
}