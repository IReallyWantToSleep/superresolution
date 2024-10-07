package io.homo.superresolution.upscale.fsr2;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.config.Config;
import io.homo.superresolution.resolutioncontrol.ResolutionControl;
import io.homo.superresolution.upscale.AbstractAlgorithm;
import io.homo.superresolution.upscale.AlgorithmManager;
import io.homo.superresolution.upscale.NativeApiHelper;
import io.homo.superresolution.upscale.utils.NativeLibManager;
import io.homo.superresolution.upscale.fsr2.utils.FFXError;
import io.homo.superresolution.render.gl.utils.Texture;
import net.minecraft.client.Minecraft;

import static io.homo.superresolution.render.gl.GlConst.*;
import static io.homo.superresolution.upscale.fsr2.types.enums.FfxFsr2InitializationFlagBits.*;

/**
 * 使用FSR2作为上采样算法
 */
public class FSR2 extends AbstractAlgorithm {
    private static Window window = Minecraft.getInstance().getWindow();
    private final NativeApiHelper fsr2_api;
    public static FSR2Helper helper = new FSR2Helper();
    private Texture inputTex;
    private Texture inputDTex;
    private Texture output;

    private FSR2() {
        RenderSystem.assertOnRenderThread();
        this.isSupport = isSupport();
        fsr2_api = NativeLibManager.fsr2api;
        window = Minecraft.getInstance().getWindow();
    }

    public static FSR2 create() {
        return new FSR2();
    }

    @Override
    public boolean isSupport() {
        return AlgorithmManager.helper.hasGLExtension("GL_KHR_shader_subgroup");
    }

    public void resize(int width, int height) {
        RenderSystem.assertOnRenderThread();
        helper = new FSR2Helper();
        helper.resize(width, height);
        this.inputTex.resize(width, height);
        this.inputDTex.resize(width, height);
        this.output.resize(width, height);
        updateFSR2(width, height);
    }

    @Override
    public void init() {
        input = ResolutionControl.getInstance().getFramebuffer();
        inputTex = new Texture(window.getScreenWidth(), window.getScreenHeight(), GL_RGBA8);
        inputDTex = new Texture(window.getScreenWidth(), window.getScreenHeight(), GL_DEPTH_COMPONENT24);
        output = new Texture(window.getScreenWidth(), window.getScreenHeight(), GL_RGBA8);
        this.resize(window.getScreenWidth(), window.getScreenHeight());
    }

    @Override
    public boolean run(float frameTimeDelta) {
        this.inputTex.copyFromFBO(input.frameBufferId);
        this.inputDTex.copyFromTex(input.getDepthTextureId());
        return CallFSR2(frameTimeDelta);
    }

    @Override
    public void blitToScreen(int width, int height) {
        Texture.texBlitToScreen(width, height, this.output.id);
    }

    public void destroy() {
        this.inputTex.destroy();
        this.output.destroy();
        helper.destroy();
    }

    private void updateFSR2(int width, int height) {
        RenderSystem.assertOnRenderThread();
        int[] p_code = fsr2_api.ffxFsr2CreateGL(
                fsr2_api.ffxFsr2GetScratchMemorySizeGL(),
                Config.getUpscaleRatio(),
                width,
                height,
                FFX_FSR2_ENABLE_DEBUG_CHECKING.getValue() |
                        FFX_FSR2_ENABLE_AUTO_EXPOSURE.getValue() |
                        FFX_FSR2_ALLOW_NULL_DEVICE_AND_COMMAND_LIST.getValue()
        );

        if (!FFXError.isOK(p_code[0])) {
            SuperResolution.LOGGER.error("ffxFsr2GetInterfaceGL:{}", FFXError.returnErrorText(p_code[0]));
        }
        if (!FFXError.isOK(p_code[1])) {
            if (p_code[1] == FFXError.FFX_ERROR_GL_KHR_shader_subgroup) this.isSupport = false;
            SuperResolution.LOGGER.error("ffxFsr2ContextCreate:{}", FFXError.returnErrorText(p_code[0]));
        }
    }

    private boolean CallFSR2(float frameTimeDelta) {
        if (SuperResolution.notSupportFSR2) return false;
        RenderSystem.assertOnRenderThread();
        window = Minecraft.getInstance().getWindow();
        //helper.update();
        return FFXError.isOK(
                fsr2_api.ffxFsr2ContextDispatch(
                        fsr2_api.ffxGetTextureResourceGL(
                                inputTex.id,
                                helper.renderWidth,
                                helper.renderHeight,
                                GL_RGBA8
                        ),
                        fsr2_api.ffxGetTextureResourceGL(
                                inputTex.id,
                                helper.renderWidth,
                                helper.renderHeight,
                                GL_DEPTH_COMPONENT24
                        ),
                        fsr2_api.ffxGetTextureResourceGL(
                                helper.getMotionVectorsTex(),
                                helper.renderWidth,
                                helper.renderHeight,
                                GL_RG16F
                        ),
                        null,
                        null,
                        fsr2_api.ffxGetTextureResourceGL(
                                output.id,
                                helper.screenWidth,
                                helper.screenHeight,
                                GL_RGBA8
                        ),
                        0,
                        0,
                        helper.renderWidth,
                        helper.renderHeight,
                        helper.renderWidth,
                        helper.renderHeight,
                        false,
                        1.0f,
                        frameTimeDelta,
                        1.0f,
                        false,
                        helper.getCameraNear(),
                        helper.getCameraFar(),
                        helper.getCameraFovAngleVertical(),
                        1F,
                        false,
                        helper.screenWidth,
                        helper.screenHeight
                )
        );
    }

    @Override
    public int getInputTexId() {
        return inputTex.id;
    }

    @Override
    public int getOutputTexId() {
        return output.id;
    }
}
