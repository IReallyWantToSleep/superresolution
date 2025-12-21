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

package io.homo.superresolution.common.minecraft.handler;

import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.api.event.AlgorithmDispatchEvent;
import io.homo.superresolution.api.event.AlgorithmDispatchFinishEvent;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.config.enums.CaptureMode;
import io.homo.superresolution.common.minecraft.*;
import io.homo.superresolution.common.perf.PerformanceRecorder;
import io.homo.superresolution.api.platform.Platform;
import io.homo.superresolution.common.upscale.MotionVectorsGenerator;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.CopyOperation;
import io.homo.superresolution.core.graphics.impl.buffer.*;
import io.homo.superresolution.core.graphics.impl.framebuffer.IBindableFrameBuffer;
import io.homo.superresolution.core.graphics.impl.grape.GrapeJobBuilders;
import io.homo.superresolution.core.graphics.impl.grape.GrapeJobResource;
import io.homo.superresolution.core.graphics.impl.pipeline.GraphicsPipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.RenderPass;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.Gl;
import io.homo.superresolution.core.graphics.opengl.GlDebug;
import io.homo.superresolution.core.graphics.opengl.GlState;
import io.homo.superresolution.core.graphics.opengl.GlStates;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.core.graphics.opengl.utils.GlTextureCopier;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferBindPoint;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.DispatchResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import org.lwjgl.opengl.GL46;
#if MC_VER < MC_1_21_4
import io.homo.superresolution.common.mixin.core.accessor.PostChainAccessor;
#endif
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.*;

public class MinecraftRenderHandler implements IMinecraftRenderHandler {
    private final Map<MinecraftRenderTargetType, IBindableFrameBuffer> renderTargets = new HashMap<>();
    public ITexture colorTexture;
    public ITexture depthTexture;
    private IBindableFrameBuffer renderTarget;

    public void initialize() {
        RenderSystem.assertOnRenderThread();
        #if MC_VER > MC_1_21_4
        renderTarget = io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer.create(
                SuperResolutionConfig.getInternalTextureFormat(),
                TextureFormat.DEPTH32,
                RenderHandlerManager.getRenderWidth(),
                RenderHandlerManager.getRenderHeight()
        );
        #else
        renderTarget = new LegacyStorageFrameBuffer(true);
        #endif
        renderTarget.label("SRMainRenderTarget");
        renderTarget.setClearColorRGBA(1.0f, 1.0f, 1.0f, 1.0f);
        renderTarget.resizeFrameBuffer(
                RenderHandlerManager.getRenderWidth(),
                RenderHandlerManager.getRenderHeight()
        );

        colorTexture = RenderSystems.current().device().createTexture(
                TextureDescription.create()
                        .label("SRMainColorTexture")
                        .format(SuperResolutionConfig.getInternalTextureFormat())
                        .type(TextureType.Texture2D)
                        .usages(TextureUsages.create().storage().sampler())
                        .mipmapsDisabled()
                        .wrapMode(TextureWrapMode.ClampToEdge)
                        .size(
                                RenderHandlerManager.getRenderWidth(),
                                RenderHandlerManager.getRenderHeight()
                        )
                        .build()
        );
        depthTexture = RenderSystems.current().device().createTexture(
                TextureDescription.create()
                        .label("SRMainDepthTexture")
                        .mipmapsDisabled()
                        .format(TextureFormat.R32F)
                        .usages(TextureUsages.create().storage().sampler())
                        .type(TextureType.Texture2D)
                        .wrapMode(TextureWrapMode.ClampToEdge)
                        .size(
                                RenderHandlerManager.getRenderWidth(),
                                RenderHandlerManager.getRenderHeight()
                        )
                        .build()
        );
    }

    public void onProcessPostChain(PostChain postChain) {
        #if MC_VER < MC_1_21_4
        int renderWidth = RenderHandlerManager.getRenderWidth();
        int renderHeight = RenderHandlerManager.getRenderHeight();
        //修复PostChain中的RenderTarget大小不正确
        for (com.mojang.blaze3d.pipeline.RenderTarget renderTarget : ((PostChainAccessor) postChain).getFullSizedTargets()) {
            if (renderTarget.width != renderWidth ||
                    renderTarget.height != renderHeight ||
                    ((PostChainAccessor) postChain).getScreenWidth() != renderWidth ||
                    ((PostChainAccessor) postChain).getScreenHeight() != renderHeight) {
                postChain.resize(renderWidth, renderHeight);
                break;
            }
        }
        #endif
    }

    public void updateRenderTarget() {
        renderTargets.clear();
        for (MinecraftRenderTargetType minecraftRenderTargetType : MinecraftRenderTargetType.values()) {
            IBindableFrameBuffer renderTarget = minecraftRenderTargetType.get(Minecraft.getInstance().levelRenderer);
            if (renderTarget != null) {
                renderTargets.put(
                        minecraftRenderTargetType,
                        renderTarget
                );
            }
        }
    }

    public IBindableFrameBuffer getRenderTarget(MinecraftRenderTargetType type) {
        return renderTargets.get(type);
    }

    public void resize() {
        int screenWidth = RenderHandlerManager.getScreenWidth();
        int screenHeight = RenderHandlerManager.getScreenHeight();
        int renderWidth = RenderHandlerManager.getRenderWidth();
        int renderHeight = RenderHandlerManager.getRenderHeight();
        callOnRenderTargets((renderTarget) -> resizeRenderTarget(renderTarget, renderWidth, renderHeight), false);
    }

    private void resizeRenderTarget(IFrameBuffer renderTarget, int width, int height) {
        renderTarget.resizeFrameBuffer(width, height);
    }

    private boolean checkRenderWorldCallPos(CallType type) {
        return switch (SuperResolutionConfig.getCaptureMode()) {
            case A, C -> type == CallType.GAME_RENDERER;
            case B -> type == CallType.LEVEL_RENDERER;
        };
    }

    private boolean checkRenderHandCallPos() {
        return switch (SuperResolutionConfig.getCaptureMode()) {
            case A, B -> false;
            case C -> true;
        } && !Platform.currentPlatform.iris().isShaderPackInUse();
    }

    public void onRenderWorldBegin(CallType type) {
        if (!checkRenderWorldCallPos(type)) return;
        updateRenderTarget();
        updateRenderTargetSize();

        /*
        =1.21.5 在渲染世界前后resizeRenderTarget
        !=1.21.5 在渲染世界前后更换RenderTarget
        */
        if (SuperResolutionConfig.isEnableUpscale()) {
            RenderHandlerManager.setClientRenderTarget(renderTarget.asMcRenderTarget());
            renderTarget.bind(FrameBufferBindPoint.Write);
            /*
            #if MC_VER >= MC_1_21_5
            RenderHandlerManager.getOriginRenderTarget().asMcRenderTarget().resize(
                    RenderHandlerManager.getRenderWidth(),
                    RenderHandlerManager.getRenderHeight()
            );
            #else
            RenderHandlerManager.setClientRenderTarget(renderTarget.asMcRenderTarget());
            renderTarget.bind(FrameBufferBindPoint.Write);
            #endif
            */

        }
    }

    public void onRenderWorldEnd(CallType type) {
        if (!checkRenderWorldCallPos(type)) return;

        /*
        =1.21.5 在渲染世界后把MainRenderTarget直接复制到缩放后的RenderTarget
        !=1.21.5 在渲染世界后还原RenderTarget
        */
        //TODO:不用copy直接blitFrameBuffer
        if (SuperResolutionConfig.isEnableUpscale()) {
            /*#if MC_VER >= MC_1_21_5
            glEnable(GL_BLEND);
            GL42.glBlendFuncSeparate(GL_ONE, GL_ZERO, GL_ZERO, GL_ONE);
            GlTextureCopier.copy(
                    CopyOperation.create()
                            .src(RenderHandlerManager.getOriginRenderTarget().getTexture(FrameBufferAttachmentType.Color))
                            .dst(colorTexture)
                            .fromTo(CopyOperation.TextureChancel.R, CopyOperation.TextureChancel.R)
                            .fromTo(CopyOperation.TextureChancel.G, CopyOperation.TextureChancel.G)
                            .fromTo(CopyOperation.TextureChancel.B, CopyOperation.TextureChancel.B)
                            .fromTo(CopyOperation.TextureChancel.A, CopyOperation.TextureChancel.A)

            );
            GlTextureCopier.copy(
                    CopyOperation.create()
                            .src(RenderHandlerManager.getOriginRenderTarget().getTexture(FrameBufferAttachmentType.AnyDepth))
                            .dst(depthTexture)
                            .fromTo(CopyOperation.TextureChancel.R, CopyOperation.TextureChancel.R)
            );
            RenderHandlerManager.getOriginRenderTarget().asMcRenderTarget().resize(RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight());
            #else*/
            RenderHandlerManager.setClientRenderTarget(RenderHandlerManager.getOriginRenderTarget().asMcRenderTarget());
            //#endif
        }
        RenderHandlerManager.getOriginRenderTarget().bind(FrameBufferBindPoint.Write, true);
        //push SRUpscale
        GlDebug.pushGroup(64108435, "SRUpscale");
        PerformanceRecorder.beginUpscale();
        try (GlState ignored = new GlState()) {
            AlgorithmManager.update();
            if (SuperResolutionConfig.isEnableUpscale()) {
                {
                    //ScaledRenderTarget.ColorTex copy to MinecraftRenderHandler.colorTexture
                    //#if MC_VER < MC_1_21_5
                    GlTextureCopier.copy(
                            CopyOperation.create()
                                    .src(renderTarget.getTexture(FrameBufferAttachmentType.Color))
                                    .dst(colorTexture)
                                    .fromTo(CopyOperation.TextureChancel.R, CopyOperation.TextureChancel.R)
                                    .fromTo(CopyOperation.TextureChancel.G, CopyOperation.TextureChancel.G)
                                    .fromTo(CopyOperation.TextureChancel.B, CopyOperation.TextureChancel.B)
                    );
                    GlTextureCopier.copy(
                            CopyOperation.create()
                                    .src(renderTarget.getTexture(FrameBufferAttachmentType.AnyDepth))
                                    .dst(depthTexture)
                                    .fromTo(CopyOperation.TextureChancel.R, CopyOperation.TextureChancel.R)
                    );
                    //#endif
                    DispatchResource dispatchResource;
                    if (SuperResolutionConfig.isGenerateMotionVectors()) {
                        MotionVectorsGenerator.update(
                                colorTexture,
                                renderTarget.getTexture(FrameBufferAttachmentType.AnyDepth)
                        );
                        dispatchResource = AlgorithmManager.getDispatchResource(
                                colorTexture,
                                depthTexture,
                                AlgorithmManager.getMotionVectorsFrameBuffer().getTexture(FrameBufferAttachmentType.Color)
                        );
                    } else {
                        dispatchResource = AlgorithmManager.getDispatchResource(
                                colorTexture,
                                depthTexture,
                                null
                        );
                    }


                    if (SuperResolution.currentAlgorithm != null) {
                        SuperResolutionAPI.EVENT_BUS.post(
                                new AlgorithmDispatchEvent(
                                        SuperResolution.currentAlgorithm,
                                        dispatchResource
                                )
                        );
                    }
                    SuperResolution.getCurrentAlgorithm().dispatch(dispatchResource);
                    if (SuperResolution.currentAlgorithm != null) {
                        SuperResolutionAPI.EVENT_BUS.post(
                                new AlgorithmDispatchFinishEvent(
                                        SuperResolution.currentAlgorithm,
                                        SuperResolution.currentAlgorithm.getOutputFrameBuffer()
                                )
                        );
                    }

                }
                //TODO:允许指定Filter
                IFrameBuffer outFbo = SuperResolution.getCurrentAlgorithm().getOutputFrameBuffer();
                Gl.DSA.blitFramebuffer(
                        (int) outFbo.handle(),
                        (int) RenderHandlerManager.getOriginRenderTarget().handle(),
                        0, 0, outFbo.getWidth(), outFbo.getHeight(),
                        0, 0, RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight(),
                        GL46.GL_COLOR_BUFFER_BIT,
                        GL46.GL_NEAREST
                );

                if (SuperResolutionConfig.getCaptureMode() == CaptureMode.C && !Platform.currentPlatform.iris().isShaderPackInUse())
                    blitHandRenderTarget();
            }
        }
        glViewport(
                0,
                0,
                RenderHandlerManager.getScreenWidth(),
                RenderHandlerManager.getScreenHeight()
        );
        PerformanceRecorder.endUpscale();
        //pop SRUpscale
        GlDebug.popGroup();
        RenderHandlerManager.getOriginRenderTarget().bind(FrameBufferBindPoint.Write);
    }


    public void callOnRenderTargets(Consumer<IFrameBuffer> callback) {
        renderTargets.forEach(((minecraftRenderTargetType, renderTarget) -> {
            if (renderTarget != null && minecraftRenderTargetType != MinecraftRenderTargetType.HAND) {
                callback.accept(renderTarget);
            }
        }));
    }

    public void callOnRenderTargets(BiConsumer<IFrameBuffer, MinecraftRenderTargetType> callback) {
        renderTargets.forEach(((minecraftRenderTargetType, renderTarget) -> {
            if (renderTarget != null) {
                callback.accept(renderTarget, minecraftRenderTargetType);
            }
        }));
    }

    public void callOnRenderTarget(MinecraftRenderTargetType type, Consumer<IFrameBuffer> callback) {
        if (getRenderTarget(type) != null) {
            callback.accept(getRenderTarget(type));
        }
    }

    public void callOnRenderTarget(Consumer<IFrameBuffer> callback) {
        if (renderTarget != null) {
            callback.accept(renderTarget);
        }
    }

    public void callOnRenderTargets(Consumer<IFrameBuffer> callback, boolean includeMainRenderTarget) {
        callOnRenderTargets(callback);
        if (includeMainRenderTarget && renderTarget != null) callback.accept(renderTarget);
    }

    public void updateRenderTargetSize() {
        int renderWidth = RenderHandlerManager.getRenderWidth();
        int renderHeight = RenderHandlerManager.getRenderHeight();
        int screenWidth = RenderHandlerManager.getScreenWidth();
        int screenHeight = RenderHandlerManager.getScreenHeight();
        callOnRenderTargets(
                (renderTarget) -> {
                    if (renderTarget.getWidth() != renderWidth || renderTarget.getHeight() != renderHeight) {
                        renderTarget.resizeFrameBuffer(
                                renderWidth,
                                renderHeight
                        );
                    }
                }, true
        );
        IFrameBuffer handRenderTarget = getRenderTarget(MinecraftRenderTargetType.HAND);
        if (handRenderTarget.getWidth() != screenWidth || handRenderTarget.getHeight() != screenHeight) {
            handRenderTarget.resizeFrameBuffer(
                    screenWidth,
                    screenHeight
            );
        }

        if (colorTexture.getWidth() != renderWidth || colorTexture.getHeight() != renderHeight) {
            colorTexture.resize(
                    renderWidth,
                    renderHeight
            );
        }
        if (depthTexture.getWidth() != renderWidth || depthTexture.getHeight() != renderHeight) {
            depthTexture.resize(
                    renderWidth,
                    renderHeight
            );
        }
    }

    public void onRenderHandBegin() {
        if (!checkRenderHandCallPos()) return;
        GlStates.save("hand");
        RenderHandlerManager.setClientRenderTarget(getRenderTarget(MinecraftRenderTargetType.HAND).asMcRenderTarget());
        getRenderTarget(MinecraftRenderTargetType.HAND).clearFrameBuffer();
        getRenderTarget(MinecraftRenderTargetType.HAND).bind(FrameBufferBindPoint.All);
    }

    public void blitHandRenderTarget() {
        glEnable(GL_BLEND);
        callOnRenderTarget(MinecraftRenderTargetType.HAND, (renderTarget -> GlTexture2D.blitToScreen(
                RenderHandlerManager.getScreenWidth(),
                RenderHandlerManager.getScreenHeight(),
                RenderHandlerManager.getScreenWidth(),
                RenderHandlerManager.getScreenHeight(),
                renderTarget.getTexture(FrameBufferAttachmentType.Color)
        )));
        glDisable(GL_BLEND);
    }

    public void onRenderHandEnd() {
        if (!checkRenderHandCallPos()) return;
        RenderHandlerManager.setClientRenderTarget(renderTarget.asMcRenderTarget());
        GlStates.pop("hand").restore();
    }

    @Override
    public IBindableFrameBuffer getFullSizeRenderTarget() {
        return RenderHandlerManager.getOriginRenderTarget();
    }

    @Override
    public IBindableFrameBuffer getScaledRenderTarget() {
        return renderTarget;
    }

    @Override
    public void destroy() {
        colorTexture.destroy();
        depthTexture.destroy();
        renderTarget.destroy();
    }

    public ITexture getColorTexture() {
        return colorTexture;
    }

    public ITexture getDepthTexture() {
        return depthTexture;
    }
}