package io.homo.superresolution.common.workmode;

import io.homo.superresolution.common.compat.iris.IrisCompatHelper;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.debug.imgui.ImGuiDebugContext;
import io.homo.superresolution.common.minecraft.handler.IMinecraftRenderHandler;
import io.homo.superresolution.common.minecraft.handler.MinecraftRenderHandler;

public class HackSRWorkModeProvider implements SRWorkModeProvider {
    @Override
    public String id() {
        return SRWorkModeManager.HACK;
    }

    @Override
    public boolean isActive() {
        return IrisCompatHelper.isCandidateEligible();
    }

    @Override
    public IMinecraftRenderHandler createRenderHandler() {
        return new MinecraftRenderHandler();
    }

    @Override
    public SRWorkModeState getState() {
        SRWorkModeState defaults = SRWorkModeState.defaults();
        return new SRWorkModeState(
                defaults.initializationDescription(),
                defaults.internalTextureFormat(),
                defaults.motionVectorPreprocessingFunction(),
                IrisCompatHelper.hasActiveShaderpack(),
                defaults.shaderPackLoading(),
                defaults.onlySupportsFrameGeneration(),
                defaults.disabledAlgorithms()
        );
    }

    @Override
    public void renderImGuiDebug(ImGuiDebugContext ctx) {
        ctx.property("Upscale Enabled", SuperResolutionConfig.isEnableUpscale());
        ctx.property("Upscale Enabled (Original)", SuperResolutionConfig.isEnableUpscaleOriginal());
        ctx.property("Capture Mode", SuperResolutionConfig.getCaptureMode());
        ctx.property("Scale Factor", SuperResolutionConfig.getRenderScaleFactor());
    }
}
