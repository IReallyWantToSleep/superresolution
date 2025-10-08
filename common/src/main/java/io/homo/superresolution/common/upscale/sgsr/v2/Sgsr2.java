/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

package io.homo.superresolution.common.upscale.sgsr.v2;

import io.homo.superresolution.api.InputResourceSet;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.config.enums.SgsrVariant;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.buffer.*;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsages;
import io.homo.superresolution.core.graphics.opengl.buffer.GlBuffer;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBufferAttachment;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.sgsr.v2.variants.Sgsr2PassCompute;
import io.homo.superresolution.common.upscale.sgsr.v2.variants.Sgsr2PassFragment;
import io.homo.superresolution.common.upscale.sgsr.v2.variants.Sgsr3PassCompute;
import io.homo.superresolution.core.impl.Destroyable;
import org.joml.Vector2f;
import org.joml.Matrix4f;

import java.util.function.Consumer;

public class Sgsr2 extends AbstractAlgorithm {
    private AbstractSgsrVariant variantInstance;
    private SgsrVariant currentVariant;
    private StructuredUniformBuffer paramsData;
    private GlBuffer paramsUbo;
    private GlFrameBuffer output;

    private int sameFrameNum = 0;

    @Override
    public void init() {
        this.output = new GlFrameBuffer();
        output.addAttachment(new GlFrameBufferAttachment(
                GlFrameBufferAttachment.FrameBufferAttachmentType.COLOR,
                RenderSystems.current().device().createTexture(TextureDescription.create()
                        .type(TextureType.Texture2D)
                        .width(RenderHandlerManager.getRenderWidth())
                        .height(RenderHandlerManager.getRenderHeight())
                        .usages(TextureUsages.create().sampler().storage().sampler())
                        .format(SuperResolutionConfig.getInternalTextureFormat())
                        .label("Sgsr2Output")
                        .build())
        ));
        this.resize(RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight());
        paramsData = UniformStructBuilder.start()
                .vec2Entry("renderSize")
                .vec2Entry("displaySize")
                .vec2Entry("renderSizeRcp")
                .vec2Entry("displaySizeRcp")
                .vec2Entry("jitterOffset")
                .mat4Entry("clipToPrevClip")
                .floatEntry("preExposure")
                .floatEntry("cameraFovAngleHor")
                .floatEntry("cameraNear")
                .floatEntry("minLerpContribution")
                .uintEntry("bSameCamera")
                .uintEntry("reset")
                .build();
        paramsUbo = RenderSystems.current().device().createBuffer(BufferDescription.create()
                .usage(BufferUsage.Ubo)
                .size(paramsData.size())
                .build()
        );
        paramsUbo.setBufferData(paramsData);
    }

    protected void updateParams(DispatchResource dispatchResource) {
        Matrix4f currentViewMatrix = new Matrix4f(dispatchResource.viewMatrix());
        Matrix4f currentProjectionMatrix = new Matrix4f(dispatchResource.projectionMatrix());
        Matrix4f currentViewProjectionMatrix = currentViewMatrix.mul(currentProjectionMatrix, new Matrix4f());

        Matrix4f previousViewMatrix = new Matrix4f(dispatchResource.lastViewMatrix());
        Matrix4f previousProjectionMatrix = new Matrix4f(dispatchResource.lastProjectionMatrix());
        Matrix4f previousViewProjectionMatrix = previousViewMatrix.mul(previousProjectionMatrix, new Matrix4f());

        Matrix4f invertViewMatrix = new Matrix4f(dispatchResource.viewMatrix()).invert();
        Matrix4f invertProjectionMatrix = new Matrix4f(dispatchResource.projectionMatrix()).invert();
        Matrix4f invertViewProjectionMatrix = invertViewMatrix.mul(invertProjectionMatrix, new Matrix4f());

        paramsData.setVec2("renderSize", dispatchResource.renderSize());
        paramsData.setVec2("displaySize", dispatchResource.screenSize());
        paramsData.setVec2("renderSizeRcp", new Vector2f().set(1).div(dispatchResource.renderSize()));
        paramsData.setVec2("displaySizeRcp", new Vector2f().set(1).div(dispatchResource.screenSize()));
        paramsData.setVec2("jitterOffset", getOriginJitterOffset(dispatchResource.frameCount(), dispatchResource.renderSize(), dispatchResource.screenSize()));
        /*
        glm::mat4 inv_view       = glm::inverse(current_view);
        glm::mat4 inv_proj       = glm::inverse(current_proj);
        glm::mat4 inv_vp         = inv_view * inv_proj;
        glm::mat4 clipToPrevClip = (previous_view_proj * inv_vp);
        */

        Matrix4f clipToPrevClipMat = previousViewProjectionMatrix.mul(invertViewProjectionMatrix, new Matrix4f());

        paramsData.setMat4("clipToPrevClip", clipToPrevClipMat);
        paramsData.setFloat("preExposure", 1.0f);
        paramsData.setFloat("cameraFovAngleHor", dispatchResource.horizontalFov());
        paramsData.setFloat("cameraNear", dispatchResource.cameraNear());

        boolean isCameraStill = isCameraStill(
                currentViewProjectionMatrix,
                previousViewProjectionMatrix,
                0.00001f
        );
        double minLerpContribution = 0.0;
        if (isCameraStill) {
            sameFrameNum += 1;
            if (sameFrameNum > 5) {
                minLerpContribution = 0.3;
            }
            if (sameFrameNum == 0xFFFF) {
                sameFrameNum = 1;
            }
        } else {
            sameFrameNum = 0;
        }
        paramsData.setFloat("minLerpContribution", (float) minLerpContribution);
        paramsData.setUint("bSameCamera", isCameraStill ? 1 : 0);
        paramsData.setUint("reset", 0);
        paramsData.fillBuffer();
        paramsUbo.upload();
    }

    private static boolean isCameraStill(Matrix4f currentMVP, Matrix4f prevMVP, float threshold) {
        float diff = 0;
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                diff += Math.abs(currentMVP.get(r, c) - prevMVP.get(r, c));
            }
        }
        return diff < threshold;
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        super.dispatch(dispatchResource);

        initVariant();
        updateParams(dispatchResource);
        variantInstance.setOutput(output);
        variantInstance.dispatch(dispatchResource, this);
        return false;
    }

    @Override
    public Vector2f getJitterOffset(int frameCount, Vector2f renderSize, Vector2f screenSize) {
        Vector2f originJitter = getOriginJitterOffset(frameCount, renderSize, screenSize);
        return new Vector2f(
                originJitter.x,
                originJitter.y
        );
    }

    private Vector2f getOriginJitterOffset(int frameCount, Vector2f renderSize, Vector2f screenSize) {
        return new Vector2f(0);
        /*
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

    @Override
    public void destroy() {
        safeVariantInstance(Destroyable::destroy);
        paramsData.free();
        paramsUbo.destroy();
    }

    @Override
    public void resize(int width, int height) {
        safeVariantInstance((sgsrVariant -> sgsrVariant.resize(width, height)));
        this.output.resizeFrameBuffer(width, height);
    }

    @Override
    public IFrameBuffer getOutputFrameBuffer() {
        return output;
    }

    private void initVariant() {
        if (checkVariant()) {
            if (variantInstance != null) {
                variantInstance.destroy();
            }
            variantInstance = switch (SuperResolutionConfig.SPECIAL.SGSR2.VARIANT.get()) {
                case CS_2 -> new Sgsr2PassCompute();
                case CS_3 -> new Sgsr3PassCompute();
                case FS_2 -> new Sgsr2PassFragment();
            };
            variantInstance.init(this);
        }
    }

    private boolean checkVariant() {
        if (variantInstance == null) return true;
        if (SuperResolutionConfig.SPECIAL.SGSR2.VARIANT.get() != currentVariant) {
            currentVariant = SuperResolutionConfig.SPECIAL.SGSR2.VARIANT.get();
            return true;
        }
        return false;
    }

    private void safeVariantInstance(Consumer<AbstractSgsrVariant> callback) {
        if (variantInstance != null) callback.accept(variantInstance);
    }

    public GlBuffer getParams() {
        return paramsUbo;
    }

    public InputResourceSet getInputResourceSet() {
        return resources;
    }
}
