package io.homo.superresolution.common.upscale.fsr2;

import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.core.gl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.gl.texture.GlTexture2D;
import io.homo.superresolution.core.impl.Vec2;
import io.homo.superresolution.core.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.impl.texture.TextureFormat;
import io.homo.superresolution.core.impl.texture.TextureFrameBufferAdapter;
import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.fsr2.*;
import org.joml.Matrix4f;


public class FSR2 extends AbstractAlgorithm {
    public Fsr2Context fsr2Context;
    private GlFrameBuffer outputFbo;
    private GlTexture2D output;
    private GlTexture2D exposureTexture;

    public FSR2() {
        super();
        RenderSystem.assertOnRenderThread();
    }

    public void resize(int width, int height) {
        RenderSystem.assertOnRenderThread();
        this.output.resize(width, height);
        outputFbo.resizeFrameBuffer(width, height);

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
        outputFbo = GlFrameBuffer.create(
                output,
                null,
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight()
        );
        exposureTexture = GlTexture2D.create(1, 1, TextureFormat.RGBA8);
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


    public void destroy() {
        this.output.destroy();
        outputFbo.destroy();
        fsr2Context.destroy();
        exposureTexture.destroy();
    }

    private boolean dispatchFSR2(DispatchResource dispatchResource) {
        if (fsr2Context == null) return false;
        Matrix4f projectionMatrix = dispatchResource.projectionMatrix();
        float m11 = projectionMatrix.m11();
        float cameraFovAngleVertical = dispatchResource.verticalFov();
        Fsr2DispatchDescription dispatchDescription = new Fsr2DispatchDescription();
        dispatchDescription.setColor(this.input.getTexture(FrameBufferAttachmentType.COLOR));
        dispatchDescription.setDepth(
                this.input.getTexture(FrameBufferAttachmentType.DEPTH) == null ?
                        this.input.getTexture(FrameBufferAttachmentType.DEPTH_STENCIL) :
                        this.input.getTexture(FrameBufferAttachmentType.DEPTH)
        );
        dispatchDescription.setMotionVectors(dispatchResource.motionVectors().getTexture(FrameBufferAttachmentType.COLOR));
        dispatchDescription.setOutput(this.output);
        dispatchDescription.setJitterOffset(new Vec2(0));
        dispatchDescription.setExposure(exposureTexture);
        dispatchDescription.setRenderSize(new Vec2(
                dispatchResource.renderWidth(),
                dispatchResource.renderHeight())
        );

        dispatchDescription.enableSharpening = true;
        dispatchDescription.sharpness = 1 - Config.getSharpness();
        dispatchDescription.frameTimeDelta = dispatchResource.frameTimeDelta();
        dispatchDescription.preExposure = 1.0f;
        dispatchDescription.reset = false;
        dispatchDescription.cameraNear = dispatchResource.cameraNear();
        dispatchDescription.cameraFar = dispatchResource.cameraFar();
        dispatchDescription.cameraFovAngleVertical = cameraFovAngleVertical;
        dispatchDescription.viewSpaceToMetersFactor = 1.0f;
        dispatchDescription.deviceDepthNegativeOneToOne = false;
        fsr2Context.dispatch(dispatchDescription);
        return true;
    }

    @Override
    public IFrameBuffer getOutputFrameBuffer() {
        return outputFbo;
    }


    @Override
    public int getOutputTextureId() {
        return output.getTextureId();
    }

}
