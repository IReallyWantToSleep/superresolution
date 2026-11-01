package io.homo.superresolution.common.mixin.presentation.v26_3;

#if MC_VER >= MC_26_3
import io.homo.superresolution.core.graphics.vulkan.VulkanQueue;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkQueue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.mojang.renderpearl.backend.vulkan.VulkanQueue")
public abstract class MinecraftVulkanQueueWaitMixin {
    @Redirect(
            method = "waitIdle",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/vulkan/VK12;vkQueueWaitIdle(Lorg/lwjgl/vulkan/VkQueue;)I"
            )
    )
    private int super_resolution$waitIdle(VkQueue queue) {
        synchronized (VulkanQueue.submissionLock(queue)) {
            return VK12.vkQueueWaitIdle(queue);
        }
    }
}
#else
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
public abstract class MinecraftVulkanQueueWaitMixin {
}
#endif
