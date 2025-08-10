package io.homo.superresolution.shadercompat;


import io.homo.superresolution.api.InputResourceSet;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.DrawObject;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.opengl.Gl;
import io.homo.superresolution.core.graphics.opengl.GlState;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.graphics.system.IRenderState;
import io.homo.superresolution.core.math.Vector2f;
import io.homo.superresolution.shadercompat.mixin.core.ShaderPackAccessor;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.pipeline.CompositeRenderer;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL46;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.homo.superresolution.common.upscale.AlgorithmManager.param;

public class ShaderCompatUpscaleDispatcher {
    protected static SRShaderCompatConfig shaderCompatConfig;
    public static Map<String, Object> debugInfo = new HashMap<>();

    private static TextureConfigResolver.TextureInfo colorTexture;
    private static TextureConfigResolver.TextureInfo depthTexture;
    private static TextureConfigResolver.TextureInfo motionVectorsTexture;
    private static TextureConfigResolver.TextureInfo outputTexture;

    private static SRShaderCompatConfig.InputTextureConfig lastColorConfig;
    private static SRShaderCompatConfig.InputTextureConfig lastDepthConfig;
    private static SRShaderCompatConfig.InputTextureConfig lastMotionConfig;
    private static SRShaderCompatConfig.OutputTextureConfig lastOutputConfig;

    private static GlShaderProgram copyProgram;
    private static GlFrameBuffer copyDstFrameBuffer;

    public static SRShaderCompatConfig.WorldConfig getCurrentConfig() {
        if (shaderCompatConfig == null || shaderCompatConfig.sr == null || !shaderCompatConfig.sr.enabled) {
            return null;
        }
        String currentWorldId = ((ShaderPackAccessor) Iris.getCurrentPack().get())
                .getDimensionMap()
                .get(Iris.getCurrentDimension());
        return shaderCompatConfig.getUpscaleConfigForWorld(currentWorldId);
    }


    public static DispatchResource getDispatchResource(CompositeRenderer compositeRenderer) {
        SRShaderCompatConfig.UpscaleConfig currentConfig = getCurrentConfig().upscale_config;
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

        SRShaderCompatConfig.UpscaleConfig currentConfig = getCurrentConfig().upscale_config;

        SRShaderCompatConfig.InputTextureConfig colorConfig = currentConfig.input_textures.get("color");
        SRShaderCompatConfig.InputTextureConfig depthConfig = currentConfig.input_textures.get("depth");
        SRShaderCompatConfig.InputTextureConfig motionConfig = currentConfig.input_textures.get("motion_vectors");
        SRShaderCompatConfig.OutputTextureConfig outputConfig = currentConfig.output_textures.get("upscaled_color");

        if (colorTexture == null || !configEquals(colorConfig, lastColorConfig)) {
            if (colorTexture != null) colorTexture.getInternalTexture().destroy();
            colorTexture = TextureConfigResolver.createForInput(compositeRenderer, colorConfig);
            lastColorConfig = colorConfig;
        }
        if (depthTexture == null || !configEquals(depthConfig, lastDepthConfig)) {
            if (depthTexture != null) depthTexture.getInternalTexture().destroy();
            depthTexture = TextureConfigResolver.createForInput(compositeRenderer, depthConfig);
            lastDepthConfig = depthConfig;
        }
        if (motionVectorsTexture == null || !configEquals(motionConfig, lastMotionConfig)) {
            if (motionVectorsTexture != null) motionVectorsTexture.getInternalTexture().destroy();
            motionVectorsTexture = TextureConfigResolver.createForInput(compositeRenderer, motionConfig);
            lastMotionConfig = motionConfig;
        }
        if (outputTexture == null || !configEquals(outputConfig, lastOutputConfig)) {
            if (outputTexture != null) outputTexture.getInternalTexture().destroy();
            outputTexture = TextureConfigResolver.createForOutput(compositeRenderer, outputConfig);
            lastOutputConfig = outputConfig;
        }
        colorTexture.updateTexture();
        depthTexture.updateTexture();
        motionVectorsTexture.updateTexture();

        if (copyDstFrameBuffer != null) {
            copyDstFrameBuffer.destroy();
        }
        copyDstFrameBuffer = GlFrameBuffer.create(
                outputTexture.getSourceTexture(),
                null
        );

        try (GlState ignored_ = new GlState()) {
            DispatchResource dispatchResource = getDispatchResource(compositeRenderer);
            SuperResolution.getCurrentAlgorithm().dispatch(dispatchResource);
        }
        IFrameBuffer outFbo = SuperResolution.getCurrentAlgorithm().getOutputFrameBuffer();

        IRenderState.StateSnapshot stateSnapshot = RenderSystems.opengl().device().commendEncoder().renderState().get();

        RenderSystems.opengl().device().commendEncoder()
                .begin()
                .renderState()
                .colorMask(true, true, true, false)
                .depthTest(false)
                .depthWrite(false)
                .cullFace(false);
        copyProgram.uniforms().samplerTexture("tex").set(outFbo.getTexture(FrameBufferAttachmentType.Color));
        RenderSystems.opengl().device().commendEncoder()
                .draw(
                        copyProgram,
                        copyDstFrameBuffer,
                        DrawObject.fullscreenQuad(RenderSystems.opengl().device()).once(),
                        0,
                        DrawObject.fullscreenQuadVertexCount()
                );
        RenderSystems.opengl().device().commendEncoder()
                .renderState()
                .apply(stateSnapshot);
        RenderSystems.opengl().device().submitCommandBuffer(RenderSystems.opengl().device().commendEncoder().end());
    }

    public static Optional<SRShaderCompatConfig> getCurrentShaderPackConfig() {
        return Optional.ofNullable(
                IrisApi.getInstance().isShaderPackInUse() ? shaderCompatConfig : null
        );
    }

    public static void setShaderCompatConfig(SRShaderCompatConfig shaderCompatConfig) {
        ShaderCompatUpscaleDispatcher.shaderCompatConfig = shaderCompatConfig;
    }
}
