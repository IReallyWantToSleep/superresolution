package io.homo.superresolution.shadercompat;

import io.homo.superresolution.api.InitializationDescription;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.IMinecraftRenderHandler;
import io.homo.superresolution.common.minecraft.handler.shadercompat.SRShaderCompatData;
import io.homo.superresolution.common.minecraft.handler.shadercompat.ShaderCompatHandler;
import io.homo.superresolution.common.workmode.SRWorkModeManager;
import io.homo.superresolution.common.workmode.SRWorkModeProvider;
import io.homo.superresolution.common.workmode.SRWorkModeState;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;

import java.util.Optional;

public class ShaderCompatSRWorkModeProvider implements SRWorkModeProvider {
    private boolean listenersRegistered;

    @Override
    public String id() {
        return SRWorkModeManager.SHADER_COMPAT;
    }

    @Override
    public boolean isActive() {
        try {
            return IrisShaderCompatUtils.shouldApplySuperResolutionChanges();
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public IMinecraftRenderHandler createRenderHandler() {
        return new ShaderCompatHandler();
    }

    @Override
    public SRWorkModeState getState() {
        Optional<SRShaderCompatData.WorldProfile> profile;
        try {
            profile = IrisShaderCompatUtils.getCurrentConfig();
        } catch (Throwable ignored) {
            profile = Optional.empty();
        }
        InitializationDescription desc = InitializationDescription.defaults();
        TextureFormat internalFormat = TextureFormat.RGBA16F;
        String motionVectorPreprocessingFunction = null;

        if (profile.isPresent() && profile.get().enabled) {
            SRShaderCompatData.UpscaleConfig upscale = profile.get().upscale;
            desc.setHdrInput(upscale.isHdrInput)
                    .setAutoExposure(upscale.isAutoExposure)
                    .setMotionJittered(upscale.isMotionJittered);
            internalFormat = upscale.internalFormat;
            if (upscale.customs != null) {
                motionVectorPreprocessingFunction = upscale.customs.motionVectorPreprocessingFunction;
            }
        }

        return new SRWorkModeState(
                desc,
                internalFormat,
                motionVectorPreprocessingFunction,
                ShaderCompatHandler.irisApiIsShaderPackInUse(),
                ShaderCompatHandler.isLoadingShader()
        );
    }

    @Override
    public void onClientSetup() {
        if (listenersRegistered || !io.homo.superresolution.api.platform.Platform.currentPlatform.isInstallIris()) {
            return;
        }
        IrisShaderCompatEventHandler.registerEventListeners();
        listenersRegistered = true;
    }

    @Override
    public void reloadShaderPack() {
        ShaderCompatHandler.irisApiReloadShader();
    }
}
