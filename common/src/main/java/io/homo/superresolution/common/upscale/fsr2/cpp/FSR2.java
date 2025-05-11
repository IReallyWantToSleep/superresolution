package io.homo.superresolution.common.upscale.fsr2.cpp;

import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.core.gl.texture.GlTexture2D;
import io.homo.superresolution.core.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.impl.texture.TextureFormat;
import io.homo.superresolution.core.impl.texture.TextureFrameBufferAdapter;
import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.DispatchResource;
import oiiaio.fsr.FfxError;
import oiiaio.fsr.fsr2.FfxFSR2;
import oiiaio.fsr.fsr2.FfxFsr2ContextCreateResult;
import oiiaio.fsr.fsr2.impl.*;
import org.joml.Matrix4f;

import static io.homo.superresolution.core.gl.GlConst.*;
import static oiiaio.fsr.fsr2.enums.FfxFsr2InitializationFlagBits.*;

public class FSR2 extends AbstractAlgorithm {
    private FfxFsr2Context fsr2Context;
    private GlTexture2D output;

    public FSR2() {
        super();
        RenderSystem.assertOnRenderThread();
    }

    public void resize(int width, int height) {
        RenderSystem.assertOnRenderThread();
        this.output.resize(width, height);
        updateFSR2(width, height);
    }

    @Override
    public void init() {
        input = MinecraftRenderHandle.getRenderTarget();
        output = GlTexture2D.create(
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight(),
                TextureFormat.RGBA8
        );
        this.resize(
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight()
        );
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        return dispatchFSR2(dispatchResource);
    }

    @Override
    public void blitToScreen(int width, int height) {
        GlTexture2D.blitToScreen(output.getWidth(), output.getHeight(), width, height, this.output.getTextureId());
    }

    public void destroy() {
        this.output.destroy();
        FfxFSR2.ffxFsr2ContextDestroy(fsr2Context);
    }

    private void updateFSR2(int width, int height) {
        RenderSystem.assertOnRenderThread();
        if (fsr2Context != null && fsr2Context.cppPointer > 0) {
            FfxFSR2.ffxFsr2ContextDestroy(fsr2Context);
            fsr2Context = null;
        }
        FfxFsr2ContextCreateResult result = FfxFSR2.ffxFsr2CreateGL(
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
        FfxResource colorResource = FfxFSR2.ffxGetTextureResourceGL(
                this.input.getTextureId(FrameBufferAttachmentType.COLOR),
                dispatchResource.renderWidth(),
                dispatchResource.renderHeight(),
                GL_RGBA8
        );
        FfxResource depthResource = FfxFSR2.ffxGetTextureResourceGL(
                this.input.getTextureId(FrameBufferAttachmentType.DEPTH),
                dispatchResource.renderWidth(),
                dispatchResource.renderHeight(),
                GL_DEPTH_COMPONENT24
        );
        FfxResource motionVectorsResource = FfxFSR2.ffxGetTextureResourceGL(
                dispatchResource.motionVectors().getTextureId(FrameBufferAttachmentType.COLOR),
                dispatchResource.renderWidth(),
                dispatchResource.renderHeight(),
                GL_RG16F
        );
        FfxResource outputResource = FfxFSR2.ffxGetTextureResourceGL(
                output.getTextureId(),
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
        return FfxError.isOK(FfxFSR2.ffxFsr2ContextDispatch(dispatchDescription, fsr2Context));
    }

    @Override
    public IFrameBuffer getOutputFrameBuffer() {
        return TextureFrameBufferAdapter.of(output);
    }


    @Override
    public int getOutputTextureId() {
        return output.getTextureId();
    }

}
