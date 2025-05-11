package io.homo.superresolution.common.upscale.fsr2.java;

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
import io.homo.superresolution.fsr2.Fsr2Context;
import io.homo.superresolution.fsr2.Fsr2ContextConfig;
import io.homo.superresolution.fsr2.Fsr2ContextFlags;
import io.homo.superresolution.fsr2.Fsr2Dimensions;

public class FSR2 extends AbstractAlgorithm {
    private Fsr2Context fsr2Context;
    private GlTexture2D output;

    public FSR2() {
        super();
        RenderSystem.assertOnRenderThread();
    }

    public void resize(int width, int height) {
        RenderSystem.assertOnRenderThread();
        this.output.resize(width, height);
        fsr2Context.resize(new Fsr2Dimensions(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight(),
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight()
        ));
    }

    @Override
    public void init() {
        input = MinecraftRenderHandle.getRenderTarget();
        output = GlTexture2D.create(
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight(),
                TextureFormat.RGBA8
        );
        fsr2Context = new Fsr2Context(
                Fsr2ContextConfig.create(
                        new Fsr2ContextFlags()
                ),
                new Fsr2Dimensions(
                        MinecraftRenderHandle.getRenderWidth(),
                        MinecraftRenderHandle.getRenderHeight(),
                        MinecraftRenderHandle.getScreenWidth(),
                        MinecraftRenderHandle.getScreenHeight()
                )
        );
        fsr2Context.init();
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
        fsr2Context.destroy();
    }

    private void updateFSR2(int width, int height) {
        RenderSystem.assertOnRenderThread();
        if (fsr2Context != null) {
            fsr2Context.destroy();
            fsr2Context = null;
        }

    }

    private boolean dispatchFSR2(DispatchResource dispatchResource) {
        return true;
        /*
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
        return FfxError.isOK(FfxFSR2.ffxFsr2ContextDispatch(dispatchDescription, fsr2Context));*/
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
