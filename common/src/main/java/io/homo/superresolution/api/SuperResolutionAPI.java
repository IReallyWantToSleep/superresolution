package io.homo.superresolution.api;

import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.renderdoc.RenderDoc;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.DispatchResource;

public class SuperResolutionAPI {
    public static IFrameBuffer getOriginMinecraftFrameBuffer() {
        return MinecraftRenderHandle.getOriginRenderTarget();
    }

    public static IFrameBuffer getMinecraftFrameBuffer() {
        return MinecraftRenderHandle.getRenderTarget();
    }

    public static int getScreenWidth() {
        return MinecraftRenderHandle.getScreenWidth();
    }

    public static int getScreenHeight() {
        return MinecraftRenderHandle.getScreenHeight();
    }

    public static int getRenderWidth() {
        return MinecraftRenderHandle.getRenderWidth();
    }

    public static int getRenderHeight() {
        return MinecraftRenderHandle.getRenderHeight();
    }

    public static AlgorithmDescription<?> getCurrentAlgorithmDescription() {
        return SuperResolution.algorithmDescription;
    }

    public static AbstractAlgorithm getCurrentAlgorithm() {
        return SuperResolution.currentAlgorithm;
    }

    public static void debugRenderdocCapture() {
        MinecraftRenderHandle.needCapture();
    }

    public static void debugRenderdocCaptureUpscale() {
        MinecraftRenderHandle.needCaptureUpscale();
    }

    public static void debugRenderdocCaptureVulkan() {
        MinecraftRenderHandle.needCaptureVulkan();
    }

    public static void debugRenderdocTriggerCapture() {
        if (RenderDoc.renderdoc != null) {
            RenderDoc.renderdoc.TriggerCapture.call();
        }
    }
}
