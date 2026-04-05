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
import io.homo.superresolution.api.InitializationDescription;
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
import io.homo.superresolution.core.graphics.impl.framebuffer.FramebufferDescription;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.thirdparty.fsr2.common.*;
import org.joml.Matrix4f;
import org.joml.Vector2f;


public class FSR2 extends AbstractAlgorithm {
    public Fsr2Context fsr2Context;
    private IFrameBuffer outputFbo;
    private GlTexture2D output;
    private GlTexture2D exposureTexture;

    public FSR2() {
        super();
        RenderSystem.assertOnRenderThread();
    }

    @Override
    public void initialize(InitializationDescription desc) {
        this.initDesc = desc;
        output = (GlTexture2D) RenderSystems.current().device().createTexture(TextureDescription.create()
                .type(TextureType.Texture2D)
                .width(RenderHandlerManager.getScreenWidth())
                .height(RenderHandlerManager.getScreenHeight())
                .format(SuperResolutionConfig.getInternalTextureFormat())
                .usages(TextureUsages.create().sampler().storage())
                .label("SRFsr2Output")
                .build()
        );
        outputFbo = RenderSystems.current().device().createFramebuffer(
                FramebufferDescription.create()
                        .colorAttachment(output)
                        .label("SRFsr2OutputFbo")
                        .build());
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
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        return dispatchFSR2(dispatchResource);
    }

    @Override
    public void destroy() {
        this.output.destroy();
        outputFbo.destroy();
        fsr2Context.destroy();
        exposureTexture.destroy();
    }

    @Override
    public void resize(int width, int height) {
        destroy();
        initialize(initDesc);
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
    public boolean isSupportJitter() {
        return true;
    }

    private boolean dispatchFSR2(DispatchResource dispatchResource) {
        super.dispatch(dispatchResource);
        if (fsr2Context == null) {
            return false;
        }
        Matrix4f projectionMatrix = dispatchResource.projectionMatrix();
        float cameraFovAngleVertical = dispatchResource.verticalFov();
        Fsr2DispatchDescription dispatchDescription = new Fsr2DispatchDescription();
        dispatchDescription.setColor(resources.colorTexture());
        dispatchDescription.setDepth(resources.depthTexture());
        dispatchDescription.setMotionVectors(resources.motionVectorsTexture());
        dispatchDescription.setOutput(this.output);
        dispatchDescription.setJitterOffset(dispatchResource.jitterOffset());
        dispatchDescription.setExposure(exposureTexture);
        dispatchDescription.setRenderSize(new Vector2f(
                dispatchResource.renderWidth(),
                dispatchResource.renderHeight())
        );

        dispatchDescription.enableSharpening = false;
        dispatchDescription.sharpness = 1 - SuperResolutionConfig.getSharpness();
        dispatchDescription.frameTimeDelta = dispatchResource.frameTimeDelta();
        dispatchDescription.preExposure = dispatchResource.preExposure();
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
}
