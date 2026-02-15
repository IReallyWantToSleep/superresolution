/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.common.upscale.fsr2;

import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.minecraft.handler.shadercompat.ShaderCompatHandler;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsages;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.thirdparty.fsr2.common.*;
import org.joml.Matrix4f;
import org.joml.Vector2f;


public class FSR2 extends AbstractAlgorithm {
    public Fsr2Context fsr2Context;
    private GlFrameBuffer outputFbo;
    private GlTexture2D output;
    private GlTexture2D exposureTexture;

    public FSR2() {
        super();
        RenderSystem.assertOnRenderThread();
    }

    @Override
    public void init() {
        output = (GlTexture2D) RenderSystems.current().device().createTexture(TextureDescription.create()
                .type(TextureType.Texture2D)
                .width(RenderHandlerManager.getScreenWidth())
                .height(RenderHandlerManager.getScreenHeight())
                .format(SuperResolutionConfig.getInternalTextureFormat())
                .usages(TextureUsages.create().sampler().storage())
                .label("SRFsr2Output")
                .build()
        );
        outputFbo = GlFrameBuffer.create(
                output,
                null,
                RenderHandlerManager.getScreenWidth(),
                RenderHandlerManager.getScreenHeight()
        );
        outputFbo.label("SRFsr2OutputFbo");
        exposureTexture = (GlTexture2D) RenderSystems.current().device().createTexture(TextureDescription.create()
                .type(TextureType.Texture2D)
                .width(1)
                .height(1)
                .format(TextureFormat.R8)
                .usages(TextureUsages.create().sampler().storage())
                .label("SRFsr2ExposureTexture")
                .build()
        );
        fsr2Context = new Fsr2Context(
                Fsr2ContextConfig.create()
                        .flags(new Fsr2ContextFlags().enableDepthInverted(true))
                        .version(SuperResolutionConfig.SPECIAL.FSR2.VERSION.get()),
                new Fsr2Dimensions(
                        RenderHandlerManager.getRenderWidth(),
                        RenderHandlerManager.getRenderHeight(),
                        RenderHandlerManager.getScreenWidth(),
                        RenderHandlerManager.getScreenHeight()
                )
        );
        fsr2Context.init();
        this.resize(
                RenderHandlerManager.getScreenWidth(),
                RenderHandlerManager.getScreenHeight()
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

    public void resize(int width, int height) {
        RenderSystem.assertOnRenderThread();
        this.output.resize(width, height);
        outputFbo.resizeFrameBuffer(width, height);
        fsr2Context.resize(new Fsr2Dimensions(
                RenderHandlerManager.getRenderWidth(),
                RenderHandlerManager.getRenderHeight(),
                RenderHandlerManager.getScreenWidth(),
                RenderHandlerManager.getScreenHeight()
        ));
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

    @Override
    public boolean isSupportJitter() {
        return true;
    }

    private boolean dispatchFSR2(DispatchResource dispatchResource) {
        super.dispatch(dispatchResource);
        if (fsr2Context == null) {
            return false;
        }
        Matrix4f projectionMatrix = dispatchResource.projectionMatrix();
        float m11 = projectionMatrix.m11();
        float cameraFovAngleVertical = dispatchResource.verticalFov();
        Fsr2DispatchDescription dispatchDescription = new Fsr2DispatchDescription();
        dispatchDescription.setColor(resources.colorTexture());
        dispatchDescription.setDepth(resources.depthTexture());
        dispatchDescription.setMotionVectors(resources.motionVectorsTexture());
        dispatchDescription.setOutput(this.output);
        dispatchDescription.setJitterOffset(
                getOriginJitterOffset(
                        dispatchResource.frameCount(),
                        dispatchResource.renderSize(),
                        dispatchResource.screenSize()
                )
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
        ICommandBuffer commandBuffer = RenderSystems.opengl().device().defaultCommandPool().createCommandBuffer();
        commandBuffer.begin();
        dispatchDescription.commandBuffer = commandBuffer;
        fsr2Context.dispatch(dispatchDescription);
        commandBuffer.end();
        RenderSystems.opengl().device().submitCommandBuffer(commandBuffer);
        return true;
    }

    private Vector2f getOriginJitterOffset(int frameCount, Vector2f renderSize, Vector2f screenSize) {
        if (!ShaderCompatHandler.dontHackMinecraftRenderingPipeline()) {
            return new Vector2f(0);
        }
        //halton
        int jitterPhaseCount = Fsr2Utils.ffxFsr2GetJitterPhaseCount(renderSize.x, screenSize.x);
        return Fsr2Utils.ffxFsr2GetJitterOffset(frameCount, jitterPhaseCount);
        //R2 参考PhotonShader
        /*
        return new Vector2f(
                (float) (Mth.frac(1.3247179572 * frameCount + 0.5) * 2.0 - 1.0),
                (float) (Mth.frac(1.7548776662 * frameCount + 0.5) * 2.0 - 1.0)
        );
        */
    }
}
