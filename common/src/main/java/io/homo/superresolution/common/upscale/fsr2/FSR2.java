package io.homo.superresolution.common.upscale.fsr2;

import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.core.math.Vector2f;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.thirdparty.fsr2.common.*;
import org.joml.Matrix4f;


public class FSR2 extends AbstractAlgorithm {
    public Fsr2Context fsr2Context;
    private GlFrameBuffer outputFbo;
    private GlTexture2D output;
    private GlTexture2D exposureTexture;
    private GlTexture2D inputTexture;

    public FSR2() {
        super();
        RenderSystem.assertOnRenderThread();
    }

    public void resize(int width, int height) {
        RenderSystem.assertOnRenderThread();
        this.output.resize(width, height);
        outputFbo.resizeFrameBuffer(width, height);
        inputTexture.getMipmapSettings().bias((float) (Math.log((double) MinecraftRenderHandle.getRenderWidth() / MinecraftRenderHandle.getScreenWidth()) / Math.log(2) - 1f));
        inputTexture.resize(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight()
        );
        fsr2Context.resize(new Fsr2Dimensions(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight(),
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight()
        ));
    }

    @Override
    public void init() {
        output = (GlTexture2D) RenderSystems.current().device().createTexture(TextureDescription.create()
                .type(TextureType.Texture2D)
                .width(MinecraftRenderHandle.getScreenWidth())
                .height(MinecraftRenderHandle.getScreenHeight())
                .format(TextureFormat.R11G11B10F)
                .usages(TextureUsages.create().sampler().storage().sampler())
                .label("SRFsr2Output")
                .build()
        );
        outputFbo = GlFrameBuffer.create(
                output,
                null,
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight()
        );
        outputFbo.label("SRFsr2OutputFbo");
        exposureTexture = (GlTexture2D) RenderSystems.current().device().createTexture(TextureDescription.create()
                .type(TextureType.Texture2D)
                .width(1)
                .height(1)
                .format(TextureFormat.RGBA8)
                .usages(TextureUsages.create().sampler().storage().sampler())
                .label("SRFsr2ExposureTexture")
                .build()
        );
        inputTexture = (GlTexture2D) RenderSystems.current().device().createTexture(TextureDescription.create()
                .type(TextureType.Texture2D)
                .width(MinecraftRenderHandle.getRenderWidth())
                .height(MinecraftRenderHandle.getRenderHeight())
                .format(TextureFormat.R11G11B10F)
                .mipmapsAuto()
                .usages(TextureUsages.create().sampler().storage())
                .label("SRFsr2InputTexture")
                .build()
        );
        fsr2Context = new Fsr2Context(
                Fsr2ContextConfig.create()
                        .flags(new Fsr2ContextFlags())
                        .version(SuperResolutionConfig.SPECIAL.FSR2.VERSION.get()),
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
        super.dispatch(dispatchResource);
        if (fsr2Context == null) return false;
        Matrix4f projectionMatrix = dispatchResource.projectionMatrix();
        float m11 = projectionMatrix.m11();
        float cameraFovAngleVertical = dispatchResource.verticalFov();
        Fsr2DispatchDescription dispatchDescription = new Fsr2DispatchDescription();
        dispatchDescription.setColor(resources.colorTexture());
        dispatchDescription.setDepth(resources.depthTexture());
        dispatchDescription.setMotionVectors(resources.motionVectorsTexture());
        dispatchDescription.setOutput(this.output);
        dispatchDescription.setJitterOffset(
                new Vector2f(0)
        );
        /*
        SuperResolution.LOGGER.info(
                "FSR2 {} {}", getJitterOffset(
                        dispatchResource.frameCount(),
                        dispatchResource.renderSize(),
                        dispatchResource.screenSize()
                ).x,
                getJitterOffset(
                        dispatchResource.frameCount(),
                        dispatchResource.renderSize(),
                        dispatchResource.screenSize()
                ).y

        );
        SuperResolution.LOGGER.info(
                "FSR2OG {} {}", getOriginJitterOffset(
                        dispatchResource.frameCount(),
                        dispatchResource.renderSize(),
                        dispatchResource.screenSize()
                ).x,
                getOriginJitterOffset(
                        dispatchResource.frameCount(),
                        dispatchResource.renderSize(),
                        dispatchResource.screenSize()
                ).y

        );
        */
        dispatchDescription.setExposure(exposureTexture);
        dispatchDescription.setRenderSize(new Vector2f(
                dispatchResource.renderWidth(),
                dispatchResource.renderHeight())
        );

        dispatchDescription.enableSharpening = false;
        dispatchDescription.sharpness = 1 - SuperResolutionConfig.getSharpness();
        dispatchDescription.frameTimeDelta = dispatchResource.frameTimeDelta();
        dispatchDescription.preExposure = 1.0f;
        dispatchDescription.reset = false;
        dispatchDescription.cameraNear = dispatchResource.cameraNear();
        dispatchDescription.cameraFar = dispatchResource.cameraFar();
        dispatchDescription.cameraFovAngleVertical = cameraFovAngleVertical;
        dispatchDescription.viewSpaceToMetersFactor = 1.0f;
        dispatchDescription.deviceDepthNegativeOneToOne = false;
        RenderSystems.opengl().device().commendEncoder().begin();
        dispatchDescription.commandBuffer = RenderSystems.opengl().device().commendEncoder().getCommandBuffer();
        fsr2Context.dispatch(dispatchDescription);
        RenderSystems.opengl().device().submitCommandBuffer(RenderSystems.opengl().device().commendEncoder().end());
        return true;
    }

    @Override
    public IFrameBuffer getOutputFrameBuffer() {
        return outputFbo;
    }


    @Override
    public int getOutputTextureId() {
        return Math.toIntExact(output.handle());
    }

    @Override
    public Vector2f getJitterOffset(int frameCount, Vector2f renderSize, Vector2f screenSize) {
        //return new Vector2f(0);
        Vector2f originJitter = getOriginJitterOffset(frameCount, renderSize, screenSize);
        return new Vector2f(
                originJitter.x,
                originJitter.y
        );
    }

    private Vector2f getOriginJitterOffset(int frameCount, Vector2f renderSize, Vector2f screenSize) {
        //return new Vector2f(0);
        int jitterPhaseCount = Fsr2Utils.ffxFsr2GetJitterPhaseCount(renderSize.x, screenSize.x);
        return Fsr2Utils.ffxFsr2GetJitterOffset(frameCount, jitterPhaseCount);

    }
}
