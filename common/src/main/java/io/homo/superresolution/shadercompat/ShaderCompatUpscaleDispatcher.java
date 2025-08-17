package io.homo.superresolution.shadercompat;


import io.homo.superresolution.api.InputResourceSet;
import io.homo.superresolution.api.event.AlgorithmDispatchEvent;
import io.homo.superresolution.api.event.AlgorithmDispatchFinishEvent;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.debug.PerformanceInfo;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.CopyOperation;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.opengl.GlState;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.graphics.opengl.utils.GlTextureCopier;
import io.homo.superresolution.core.graphics.renderdoc.RenderDoc;
import io.homo.superresolution.core.math.Vector2f;
import io.homo.superresolution.shadercompat.mixin.core.ShaderPackAccessor;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.CompositeRenderer;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL41;

import java.util.HashMap;
import java.util.Map;

import static io.homo.superresolution.common.upscale.AlgorithmManager.param;

/**
 * 狗屎一坨，以至于我不得不写注释
 */
public class ShaderCompatUpscaleDispatcher {
    public static Map<String, Object> debugInfo = new HashMap<>();

    private static TextureConfigResolver.TextureInfo colorTexture;
    private static TextureConfigResolver.TextureInfo depthTexture;
    private static TextureConfigResolver.TextureInfo motionVectorsTexture;

    private static SRShaderCompatConfig.InputTextureConfig lastColorConfig;
    private static SRShaderCompatConfig.InputTextureConfig lastDepthConfig;
    private static SRShaderCompatConfig.InputTextureConfig lastMotionConfig;

    private static GlShaderProgram copyProgram;
    private static GlFrameBuffer copyDstFrameBuffer;
    private static CompositeRenderer cachedCompositeRenderer;

    public static SRShaderCompatConfig.WorldConfig getCurrentConfig() {
        if (!IrisShaderPipelineHandle.shouldApplySuperResolutionChanges()) {
            return null;
        }
        String currentWorldId = ((ShaderPackAccessor) Iris.getCurrentPack().get())
                .getDimensionMap()
                .get(Iris.getCurrentDimension());
        return IrisShaderPipelineHandle.getCurrentShaderPack().isPresent() ?
                ((SRCompatShaderPack) IrisShaderPipelineHandle.getCurrentShaderPack().get())
                        .superresolution$getSuperResolutionComaptConfig().getUpscaleConfigForWorld(currentWorldId) : null;
    }


    public static DispatchResource getDispatchResource(CompositeRenderer compositeRenderer) {
        return new DispatchResource(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight(),
                new Vector2f(MinecraftRenderHandle.getRenderWidth(), MinecraftRenderHandle.getRenderHeight()),

                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight(),
                new Vector2f(MinecraftRenderHandle.getScreenWidth(), MinecraftRenderHandle.getScreenHeight()),

                MinecraftRenderHandle.getFrameCount(),
                MinecraftRenderHandle.frameTime,
                (float) param.verticalFov,
                (float) Math.tan(param.verticalFov / 2.0) * MinecraftRenderHandle.getRenderWidth() / MinecraftRenderHandle.getRenderHeight(),
                0.05F,
                Minecraft.getInstance().gameRenderer.getDepthFar(),
                param.currentModelViewMatrix,
                param.currentProjectionMatrix,
                param.currentModelViewProjectionMatrix,
                param.currentViewMatrix,

                param.lastModelViewMatrix,
                param.lastProjectionMatrix,
                param.lastModelViewProjectionMatrix,
                param.lastViewMatrix,

                new InputResourceSet(
                        colorTexture.getInternalTexture(),
                        depthTexture.getInternalTexture(),
                        motionVectorsTexture.getInternalTexture() == null ?
                                AlgorithmManager.getMotionVectorsFrameBuffer().getTexture(FrameBufferAttachmentType.Color) :
                                motionVectorsTexture.getInternalTexture()
                )

        );
    }

    private static boolean configEquals(SRShaderCompatConfig.OutputTextureConfig c1,
                                        SRShaderCompatConfig.OutputTextureConfig c2) {
        if (c1 == c2) return true;
        if (c1 == null || c2 == null) return false;

        return c1.enabled == c2.enabled &&
                c1.target.equals(c2.target) &&
                c1.region.equals(c2.region);
    }


    private static boolean configEquals(SRShaderCompatConfig.InputTextureConfig c1,
                                        SRShaderCompatConfig.InputTextureConfig c2) {
        if (c1 == c2) return true;
        if (c1 == null || c2 == null) return false;

        return c1.enabled == c2.enabled &&
                c1.src.equals(c2.src) &&
                c1.region.equals(c2.region);
    }

    public static void dispatchUpscale(CompositeRenderer compositeRenderer) {
        if (!SuperResolutionConfig.isEnableUpscale()) return;
        if (getCurrentConfig() == null) return;
        SRShaderCompatConfig.WorldUpscaleConfig currentConfig = getCurrentConfig().upscale_config;
        /*
          检查用于复制纹理的着色器是否可用，不可用就初始化
         */
        {
            if (copyProgram == null) {
                copyProgram = RenderSystems.opengl().device().createShaderProgram(
                        ShaderDescription.graphics(
                                        new ShaderSource(ShaderType.FRAGMENT, "/shader/copy.frag.glsl", true),
                                        new ShaderSource(ShaderType.VERTEX, "/shader/copy.vert.glsl", true)
                                )
                                .addDefine("COPY_CHANCEL", "4")
                                .addDefine("COPY_SRC_CHANCEL0", "0").addDefine("COPY_DST_CHANCEL0", "0")
                                .addDefine("COPY_SRC_CHANCEL1", "1").addDefine("COPY_DST_CHANCEL1", "1")
                                .addDefine("COPY_SRC_CHANCEL2", "2").addDefine("COPY_DST_CHANCEL2", "2")
                                .addDefine("COPY_SRC_CHANCEL3", "3").addDefine("COPY_DST_CHANCEL3", "3")
                                .uniformSamplerTexture("tex", 0)
                                .build()
                );
                copyProgram.compile();
            }
        }
        boolean needUpdate = false;

        if (!compositeRenderer.equals(cachedCompositeRenderer)) {
            cachedCompositeRenderer = compositeRenderer;
            needUpdate = true;
        }

        /*
        检查+初始化超分输入配置
        createForInput使用getIrisTexture方法会从Iris拿到纹理然后从纹理ID创建超分自己的ITexture对象
        updateTexture内部会把Iris纹理复制到内部纹理，便于读写（其实只有读）
         */
        SRShaderCompatConfig.InputTextureConfig colorConfig;
        SRShaderCompatConfig.InputTextureConfig depthConfig;
        SRShaderCompatConfig.InputTextureConfig motionConfig;
        SRShaderCompatConfig.OutputTextureConfig outputConfig;
        {
            colorConfig = currentConfig.input_textures.get("color");
            depthConfig = currentConfig.input_textures.get("depth");
            motionConfig = currentConfig.input_textures.get("motion_vectors");
            outputConfig = currentConfig.output_textures.get("upscaled_color");

            if (colorTexture == null || !configEquals(colorConfig, lastColorConfig) || needUpdate) {
                if (colorTexture != null && colorTexture.getInternalTexture() != null)
                    colorTexture.getInternalTexture().destroy();
                colorTexture = TextureConfigResolver.createForInput(compositeRenderer, colorConfig);
                lastColorConfig = colorConfig;
            }
            if (depthTexture == null || !configEquals(depthConfig, lastDepthConfig) || needUpdate) {
                if (depthTexture != null && depthTexture.getInternalTexture() != null)
                    depthTexture.getInternalTexture().destroy();
                depthTexture = TextureConfigResolver.createForInput(compositeRenderer, depthConfig);
                lastDepthConfig = depthConfig;
            }
            if (motionVectorsTexture == null || !configEquals(motionConfig, lastMotionConfig) || needUpdate) {
                if (motionVectorsTexture != null && motionVectorsTexture.getInternalTexture() != null)
                    motionVectorsTexture.getInternalTexture().destroy();
                motionVectorsTexture = TextureConfigResolver.createForInput(compositeRenderer, motionConfig);
                lastMotionConfig = motionConfig;
            }
            colorTexture.updateTexture();
            depthTexture.updateTexture();
            motionVectorsTexture.updateTexture();
        }

        /*
        升采样阶段开始
         */
        {
            if (SuperResolutionConfig.isEnableDetailedProfiling()) {
                PerformanceInfo.begin("upscale");
                GL41.glBeginQuery(GL41.GL_TIME_ELAPSED, MinecraftRenderHandle.timeQueryIds[1]);
            }
            if (MinecraftRenderHandle.needCaptureUpscale) {
                if (RenderDoc.renderdoc != null) {
                    RenderDoc.renderdoc.StartFrameCapture.call(null, null);
                }
            }
        }
        AlgorithmManager.update();
        DispatchResource dispatchResource = getDispatchResource(compositeRenderer);
        if (SuperResolution.currentAlgorithm != null) {
            AlgorithmDispatchEvent.EVENT.invoker().onAlgorithmDispatch(
                    SuperResolution.currentAlgorithm,
                    dispatchResource
            );
        }
        try (GlState ignored_ = new GlState()) {
            SuperResolution.getCurrentAlgorithm().dispatch(dispatchResource);
        }
        if (SuperResolution.currentAlgorithm != null) {
            AlgorithmDispatchFinishEvent.EVENT.invoker().onAlgorithmDispatchFinish(
                    SuperResolution.currentAlgorithm,
                    SuperResolution.currentAlgorithm.getOutputFrameBuffer().getTexture(FrameBufferAttachmentType.Color)
            );
        }

        /*
        升采样阶段结束
         */
        {
            if (MinecraftRenderHandle.needCaptureUpscale) {
                if (RenderDoc.renderdoc != null) {
                    MinecraftRenderHandle.needCaptureUpscale = false;
                    RenderDoc.renderdoc.EndFrameCapture.call(null, null);
                }
            }
            if (SuperResolutionConfig.isEnableDetailedProfiling()) {
                GL41.glEndQuery(GL41.GL_TIME_ELAPSED);
                long[] upscaleTime = {0};
                GL41.glGetQueryObjectui64v(MinecraftRenderHandle.timeQueryIds[1], GL41.GL_QUERY_RESULT, upscaleTime);
                PerformanceInfo.end("upscale", upscaleTime[0]);
            }
        }
        IFrameBuffer outFbo = SuperResolution.getCurrentAlgorithm().getOutputFrameBuffer();
        for (String targetName : currentConfig.output_textures.get("upscaled_color").target) {
            GlTextureCopier.copy(
                    CopyOperation.create()
                            .src(outFbo.getTexture(FrameBufferAttachmentType.Color))
                            .dst(IrisTextureResolver.getIrisTexture(compositeRenderer, targetName))
                            .fromTo(CopyOperation.TextureChancel.A, CopyOperation.TextureChancel.A)
                            .fromTo(CopyOperation.TextureChancel.R, CopyOperation.TextureChancel.R)
                            .fromTo(CopyOperation.TextureChancel.G, CopyOperation.TextureChancel.G)
                            .fromTo(CopyOperation.TextureChancel.B, CopyOperation.TextureChancel.B)
            );
        }
    }
}
