package io.homo.superresolution.common.workmode;

import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.debug.imgui.ImGuiDebugContext;
import io.homo.superresolution.common.minecraft.handler.IMinecraftRenderHandler;
import io.homo.superresolution.common.minecraft.handler.MinecraftRenderHandler;

import java.lang.reflect.Method;
import java.util.Optional;

public class HackSRWorkModeProvider implements SRWorkModeProvider {
    private static boolean irisReloadReflectionInitialized;
    private static Method irisGetCurrentPackMethod;

    @Override
    public String id() {
        return SRWorkModeManager.HACK;
    }

    @Override
    public boolean isActive() {
        return true;
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
                isShaderPackInUse(),
                defaults.shaderPackLoading()
        );
    }

    private boolean isShaderPackInUse() {
        initIrisReloadReflection();
        if (irisGetCurrentPackMethod == null) {
            return false;
        }
        try {
            Optional<?> shaderPack = (Optional<?>) irisGetCurrentPackMethod.invoke(null);
            return shaderPack.isPresent();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void initIrisReloadReflection() {
        if (irisReloadReflectionInitialized) {
            return;
        }
        synchronized (HackSRWorkModeProvider.class) {
            if (irisReloadReflectionInitialized) {
                return;
            }
            try {
                Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
                irisGetCurrentPackMethod = irisClass.getMethod("getCurrentPack");
            } catch (Throwable ignored) {
            }
            irisReloadReflectionInitialized = true;
        }
    }

    @Override
    public void renderImGuiDebug(ImGuiDebugContext ctx) {
        ctx.property("Upscale Enabled", SuperResolutionConfig.isEnableUpscale());
        ctx.property("Upscale Enabled (Original)", SuperResolutionConfig.isEnableUpscaleOriginal());
        ctx.property("Capture Mode", SuperResolutionConfig.getCaptureMode());
        ctx.property("Scale Factor", SuperResolutionConfig.getRenderScaleFactor());
    }
}
