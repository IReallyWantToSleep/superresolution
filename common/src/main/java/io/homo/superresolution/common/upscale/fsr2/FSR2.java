package io.homo.superresolution.common.upscale.fsr2;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.render.MinecraftRenderHandle;
import io.homo.superresolution.common.render.gl.texture.GlTexture;
import io.homo.superresolution.common.render.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.common.render.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.common.render.impl.texture.TextureFrameBufferAdapter;
import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;
import io.homo.superresolution.common.upscale.DispatchResource;
import oiiaio.fsr.NativeLibManager;
import net.minecraft.client.Minecraft;
import oiiaio.fsr.FfxError;
import oiiaio.fsr.fsr2.FfxFSR2;
import oiiaio.fsr.fsr2.FfxFsr2ContextCreateResult;
import oiiaio.fsr.fsr2.impl.*;
import org.joml.Matrix4f;

import static io.homo.superresolution.common.render.gl.GlConst.*;
import static oiiaio.fsr.fsr2.enums.FfxFsr2InitializationFlagBits.*;

public class FSR2 extends AbstractAlgorithm {
    private static Window window = Minecraft.getInstance().getWindow();
    private final FfxFSR2 nativeApi;
    private FfxFsr2Context fsr2Context;
    private GlTexture output;

    public FSR2() {
        super();
        RenderSystem.assertOnRenderThread();
        this.isSupport = isSupport();
        nativeApi = NativeLibManager.getNativeApi();
        window = Minecraft.getInstance().getWindow();
    }

    @Override
    protected boolean isSupport() {
        return AlgorithmDescriptions.FSR2.getRequirement().check().support() && NativeLibManager.getNativeApi() != null;
    }

    public void resize(int width, int height) {
        RenderSystem.assertOnRenderThread();
        this.output.resize(width, height);
        updateFSR2(width, height);
    }

    @Override
    public void init() {
        input = MinecraftRenderHandle.getRenderTarget();
        output = new GlTexture(window.getScreenWidth(), window.getScreenHeight(), GL_RGBA8);
        this.resize(window.getScreenWidth(), window.getScreenHeight());
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        return dispatchFSR2(dispatchResource);
    }

    @Override
    public void blitToScreen(int width, int height) {
        GlTexture.blitToScreen(output.width, output.height, width, height, this.output.id);
    }

    public void destroy() {
        this.output.destroy();
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

    private boolean dispatchFSR2(DispatchResource dispatchResource) {
        if (fsr2Context == null) return false;
        RenderSystem.assertOnRenderThread();
        Matrix4f projectionMatrix = dispatchResource.projectionMatrix();
        float m11 = projectionMatrix.m11();
        float verticalFovRadians = 2.0f * (float) Math.atan(1.0f / m11);
        float cameraFovAngleVertical = dispatchResource.verticalFov();
        FfxResource colorResource = nativeApi.ffxGetTextureResourceGL(
                this.input.getTextureId(FrameBufferAttachmentType.COLOR),
                dispatchResource.renderWidth(),
                dispatchResource.renderHeight(),
                GL_RGBA8
        );
        FfxResource depthResource = nativeApi.ffxGetTextureResourceGL(
                this.input.getTextureId(FrameBufferAttachmentType.DEPTH),
                dispatchResource.renderWidth(),
                dispatchResource.renderHeight(),
                GL_DEPTH_COMPONENT24
        );
        FfxResource motionVectorsResource = nativeApi.ffxGetTextureResourceGL(
                dispatchResource.motionVectors().getTextureId(FrameBufferAttachmentType.COLOR),
                dispatchResource.renderWidth(),
                dispatchResource.renderHeight(),
                GL_RG16F
        );
        FfxResource outputResource = nativeApi.ffxGetTextureResourceGL(
                output.id,
                dispatchResource.screenWidth(),
                dispatchResource.screenHeight(),
                GL_RGBA8
        );
        FfxFsr2DispatchDescription dispatchDescription = FfxFsr2DispatchDescription.create();
        dispatchDescription.color = colorResource;
        dispatchDescription.depth = depthResource;
        dispatchDescription.motionVectors = motionVectorsResource;
        dispatchDescription.output = outputResource;
        dispatchDescription.jitterOffset = FfxFloatCoords2D.create(0, 0);
        dispatchDescription.motionVectorScale = FfxFloatCoords2D.create(
                dispatchResource.renderWidth(),
                dispatchResource.renderHeight()
        );
        dispatchDescription.renderSize = FfxDimensions2D.create(
                dispatchResource.renderWidth(),
                dispatchResource.renderHeight()
        );
        dispatchDescription.enableSharpening = false;
        dispatchDescription.sharpness = Config.getSharpness();
        dispatchDescription.frameTimeDelta = dispatchResource.frameTimeDelta();
        dispatchDescription.preExposure = 1.0f;
        dispatchDescription.reset = false;
        dispatchDescription.cameraNear = dispatchResource.cameraNear();
        dispatchDescription.cameraFar = dispatchResource.cameraFar();
        dispatchDescription.cameraFovAngleVertical = cameraFovAngleVertical;
        dispatchDescription.viewSpaceToMetersFactor = 1.0f;
        dispatchDescription.deviceDepthNegativeOneToOne = false;
        return FfxError.isOK(nativeApi.ffxFsr2ContextDispatch(dispatchDescription, fsr2Context));
    }

    @Override
    public IFrameBuffer getOutputFrameBuffer() {
        return TextureFrameBufferAdapter.of(output);
    }


    @Override
    public int getOutputTextureId() {
        return output.id;
    }

}
