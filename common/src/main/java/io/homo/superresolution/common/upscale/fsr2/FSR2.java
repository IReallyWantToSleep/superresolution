package io.homo.superresolution.common.upscale.fsr2;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.render.MinecraftRenderHandle;
import io.homo.superresolution.common.render.gl.texture.Texture;
import io.homo.superresolution.common.upscale.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.AlgorithmType;
import io.homo.superresolution.common.upscale.utils.NativeLibManager;
import net.minecraft.client.Minecraft;
import oiiaio.fsr.FfxError;
import oiiaio.fsr.fsr2.FfxFSR2;
import oiiaio.fsr.fsr2.FfxFsr2ContextCreateResult;
import oiiaio.fsr.fsr2.impl.*;

import static io.homo.superresolution.common.render.gl.GlConst.*;
import static oiiaio.fsr.fsr2.enums.FfxFsr2InitializationFlagBits.*;

public class FSR2 extends AbstractAlgorithm {
    public static FSR2Helper helper = new FSR2Helper();
    private static Window window = Minecraft.getInstance().getWindow();
    private final FfxFSR2 nativeApi;
    private FfxFsr2Context fsr2Context;
    private Texture output;

    private FSR2() {
        RenderSystem.assertOnRenderThread();
        this.isSupport = isSupport();
        nativeApi = NativeLibManager.nativeApi;
        window = Minecraft.getInstance().getWindow();
    }

    public static FSR2 create() {
        return new FSR2();
    }

    @Override
    protected boolean isSupport() {
        return AlgorithmType.FSR2.getValue().check().support();
    }

    public void resize(int width, int height) {
        RenderSystem.assertOnRenderThread();
        helper = new FSR2Helper();
        helper.resize(width, height);
        this.output.resize(width, height);
        updateFSR2(width, height);
    }

    @Override
    public void init() {
        input = MinecraftRenderHandle.getRenderTarget();
        output = new Texture(window.getScreenWidth(), window.getScreenHeight(), GL_RGBA8);
        this.resize(window.getScreenWidth(), window.getScreenHeight());
    }

    @Override
    public boolean dispatch(float frameTimeDelta) {
        helper.update();
        return dispatchFSR2(frameTimeDelta);
    }

    @Override
    public void blitToScreen(int width, int height) {
        Texture.blitToScreen(output.width, output.height, width, height, this.output.id);
    }

    public void destroy() {
        this.output.destroy();
        helper.destroy();
        nativeApi.ffxFsr2ContextDestroy(fsr2Context);
    }

    private void updateFSR2(int width, int height) {
        RenderSystem.assertOnRenderThread();
        FfxFsr2ContextCreateResult result = nativeApi.ffxFsr2CreateGL(
                Config.getUpscaleRatio(),
                width,
                height,
                FFX_FSR2_ENABLE_DEBUG_CHECKING.getValue() |
                        FFX_FSR2_ENABLE_AUTO_EXPOSURE.getValue() |
                        FFX_FSR2_ALLOW_NULL_DEVICE_AND_COMMAND_LIST.getValue()

        );
        fsr2Context = result.context;

        if (!FfxError.isOK(result.ffxFsr2GetInterfaceErrorCode)) {
            SuperResolution.LOGGER.error("ffxFsr2GetInterfaceGL:{}", FfxError.returnErrorText(result.ffxFsr2GetInterfaceErrorCode));
        }
        if (!FfxError.isOK(result.ffxFsr2ContextCreateErrorCode)) {
            SuperResolution.LOGGER.error("ffxFsr2ContextCreate:{}", FfxError.returnErrorText(result.ffxFsr2ContextCreateErrorCode));
        }
    }

    private boolean dispatchFSR2(float frameTimeDelta) {
        if (fsr2Context == null) return false;
        RenderSystem.assertOnRenderThread();
        FfxResource colorResource = nativeApi.ffxGetTextureResourceGL(
                this.input.getColorTextureId(),
                AlgorithmManager.helper.getRenderWidth(),
                AlgorithmManager.helper.getRenderHeight(),
                GL_RGBA8
        );
        FfxResource depthResource = nativeApi.ffxGetTextureResourceGL(
                this.input.getDepthTextureId(),
                AlgorithmManager.helper.getRenderWidth(),
                AlgorithmManager.helper.getRenderHeight(),
                GL_DEPTH_COMPONENT24
        );
        FfxResource motionVectorsResource = nativeApi.ffxGetTextureResourceGL(
                helper.getMotionVectorsTex(),
                AlgorithmManager.helper.getRenderWidth(),
                AlgorithmManager.helper.getRenderHeight(),
                GL_RG16F
        );
        FfxResource outputResource = nativeApi.ffxGetTextureResourceGL(
                output.id,
                AlgorithmManager.helper.getScreenWidth(),
                AlgorithmManager.helper.getScreenHeight(),
                GL_RGBA8
        );
        FfxFsr2DispatchDescription dispatchDescription = FfxFsr2DispatchDescription.create();
        dispatchDescription.color = colorResource;
        dispatchDescription.depth = depthResource;
        dispatchDescription.motionVectors = motionVectorsResource;
        dispatchDescription.output = outputResource;
        dispatchDescription.jitterOffset = FfxFloatCoords2D.create(0, 0);
        dispatchDescription.motionVectorScale = FfxFloatCoords2D.create(AlgorithmManager.helper.getRenderWidth(), AlgorithmManager.helper.getRenderHeight());
        dispatchDescription.renderSize = FfxDimensions2D.create(AlgorithmManager.helper.getRenderWidth(), AlgorithmManager.helper.getRenderHeight());
        dispatchDescription.enableSharpening = false;
        dispatchDescription.sharpness = 1.0f;
        dispatchDescription.frameTimeDelta = frameTimeDelta;
        dispatchDescription.preExposure = 1.0f;
        dispatchDescription.reset = false;
        dispatchDescription.cameraNear = helper.getCameraNear();
        dispatchDescription.cameraFar = helper.getCameraFar();
        dispatchDescription.cameraFovAngleVertical = helper.getCameraFovAngleVertical();
        dispatchDescription.viewSpaceToMetersFactor = 1.0f;
        dispatchDescription.deviceDepthNegativeOneToOne = false;
        return FfxError.isOK(nativeApi.ffxFsr2ContextDispatch(dispatchDescription, fsr2Context));
    }

    @Override
    public int getOutputTextureId() {
        return output.id;
    }

}
