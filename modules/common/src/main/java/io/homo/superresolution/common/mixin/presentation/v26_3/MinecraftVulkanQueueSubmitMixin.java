package io.homo.superresolution.common.mixin.presentation.v26_3;

#if MC_VER >= MC_26_3
import io.homo.superresolution.common.perf.FramePacingTrace;
import io.homo.superresolution.core.graphics.vulkan.VulkanLowLatency;
import io.homo.superresolution.core.graphics.vulkan.VulkanQueue;
import org.lwjgl.vulkan.KHRSynchronization2;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkSubmitInfo2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.mojang.renderpearl.backend.vulkan.VulkanQueue$Submission")
public abstract class MinecraftVulkanQueueSubmitMixin {
    @Redirect(
            method = "close",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/vulkan/KHRSynchronization2;vkQueueSubmit2KHR(Lorg/lwjgl/vulkan/VkQueue;Lorg/lwjgl/vulkan/VkSubmitInfo2$Buffer;J)I"
            )
    )
    private int super_resolution$submit(
            VkQueue queue,
            VkSubmitInfo2.Buffer submits,
            long fence
    ) {
        if (FramePacingTrace.INSTANCE.isRecording()) {
            // Lines Minecraft's submissions up with the Reflex markers of the frame they belong to.
            FramePacingTrace.INSTANCE.instant("mc_queue_submit",
                    new FramePacingTrace.Context(-1, -1L, -1L, -1L,
                            VulkanLowLatency.currentFramePresentId(), "", ""),
                    "complete", "count=" + submits.remaining()
                            + " thread=" + Thread.currentThread().getName());
        }
        synchronized (VulkanQueue.submissionLock(queue)) {
            return KHRSynchronization2.vkQueueSubmit2KHR(queue, submits, fence);
        }
    }
}
#else
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
public abstract class MinecraftVulkanQueueSubmitMixin {
}
#endif
