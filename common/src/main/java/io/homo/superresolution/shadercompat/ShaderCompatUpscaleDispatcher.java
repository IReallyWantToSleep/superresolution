package io.homo.superresolution.shadercompat;


import com.sun.jna.Pointer;
import io.homo.superresolution.api.InputResourceSet;
import io.homo.superresolution.api.event.AlgorithmDispatchEvent;
import io.homo.superresolution.api.event.AlgorithmDispatchFinishEvent;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.MotionVectorsGenerator;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferTextureAdapter;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.Gl;
import io.homo.superresolution.core.graphics.opengl.GlState;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.core.graphics.renderdoc.RenderDoc;
import io.homo.superresolution.core.math.Vector2f;
import io.homo.superresolution.shadercompat.mixin.core.CompositeRendererAccessor;
import io.homo.superresolution.shadercompat.mixin.core.ShaderPackAccessor;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.gl.texture.GlTexture;
import net.irisshaders.iris.pbr.TextureInfoCache;
import net.irisshaders.iris.pipeline.CompositeRenderer;
import net.irisshaders.iris.targets.RenderTargets;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL46;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static io.homo.superresolution.common.upscale.AlgorithmManager.param;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL43.glCopyImageSubData;

public class ShaderCompatUpscaleDispatcher {
    protected static SRShaderCompatConfig shaderCompatConfig;
    private static GlTexture2D colorTexture;
    public static Map<String, Object> debugInfo = new HashMap<>();

    public static SRShaderCompatConfig.UpscaleConfig getCurrentConfig() {
        return Optional.ofNullable(
                shaderCompatConfig.getWorldConfigs().get(
                        ((ShaderPackAccessor) Iris.getCurrentPack().get())
                                .getDimensionMap()
                                .get(Iris.getCurrentDimension())
                )
        ).orElse(
                shaderCompatConfig.getWorldConfigs().get("world0")
        );
    }

    public static DispatchResource getDispatchResource(CompositeRenderer compositeRenderer) {
        SRShaderCompatConfig.UpscaleConfig currentConfig = getCurrentConfig();
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
                        colorTexture,
                        null,
                        currentConfig.inputTextures.get("motion_vectors") != null &&
                                currentConfig.inputTextures.get("motion_vectors").enabled ?
                                new _Texture(
                                        () -> TextureFormat.RG16F,
                                        MinecraftRenderHandle::getRenderWidth,
                                        MinecraftRenderHandle::getRenderHeight,
                                        () -> (long) getIrisTextureByName(compositeRenderer, currentConfig.inputTextures.get("motion_vectors").src)
                                ) :
                                AlgorithmManager.getMotionVectorsFrameBuffer().getTexture(FrameBufferAttachmentType.Color)
                )

        );
    }

    public static void dispatchUpscale(CompositeRenderer compositeRenderer) {
        SRShaderCompatConfig.UpscaleConfig currentConfig = getCurrentConfig();

        if (colorTexture == null) {
            colorTexture = GlTexture2D.create(
                    TextureDescription.create()
                            .width(MinecraftRenderHandle.getRenderWidth())
                            .height(MinecraftRenderHandle.getRenderHeight())
                            .type(TextureType.Texture2D)
                            .mipmapsDisabled()
                            .usages(TextureUsages.create().sampler())
                            .format(getIrisTextureFormatByName(compositeRenderer, currentConfig.inputTextures.get("color").src))
                            .build()
            );
        } else if (getIrisTextureFormatByName(compositeRenderer, currentConfig.inputTextures.get("color").src) != colorTexture.getTextureFormat()) {
            colorTexture.destroy();
            colorTexture = GlTexture2D.create(
                    TextureDescription.create()
                            .width(MinecraftRenderHandle.getRenderWidth())
                            .height(MinecraftRenderHandle.getRenderHeight())
                            .type(TextureType.Texture2D)
                            .mipmapsDisabled()
                            .usages(TextureUsages.create().sampler())
                            .format(getIrisTextureFormatByName(compositeRenderer, currentConfig.inputTextures.get("color").src))
                            .build()
            );
        }
        if (
                colorTexture.getWidth() != MinecraftRenderHandle.getRenderWidth() ||
                        colorTexture.getHeight() != MinecraftRenderHandle.getRenderHeight()
        ) {
            colorTexture.resize(
                    MinecraftRenderHandle.getRenderWidth(),
                    MinecraftRenderHandle.getRenderHeight()
            );
        }
        glCopyImageSubData(
                (int) getIrisTextureByName(compositeRenderer, currentConfig.inputTextures.get("color").src), GL_TEXTURE_2D,
                0, 0, 0, 0,
                (int) colorTexture.handle(), GL_TEXTURE_2D,
                0, 0, 0, 0,
                switch (currentConfig.inputTextures.get("color").region.get(2)) {
                    case -1, 0 -> MinecraftRenderHandle.getRenderWidth();
                    case -2 -> MinecraftRenderHandle.getScreenWidth();
                    default -> currentConfig.inputTextures.get("color").region.get(2);
                },
                switch (currentConfig.inputTextures.get("color").region.get(3)) {
                    case -1, 0 -> MinecraftRenderHandle.getRenderHeight();
                    case -2 -> MinecraftRenderHandle.getScreenHeight();
                    default -> currentConfig.inputTextures.get("color").region.get(3);
                },
                1
        );

        try (GlState ignored_ = new GlState()) {
            DispatchResource dispatchResource = getDispatchResource(compositeRenderer);
            debugInfo.put("color", dispatchResource.resources().colorTexture().handle());
            debugInfo.put("colora", colorTexture.handle());
            SuperResolution.getCurrentAlgorithm().dispatch(dispatchResource);
        }
        IFrameBuffer outFbo = SuperResolution.getCurrentAlgorithm().getOutputFrameBuffer();
        debugInfo.put("out", outFbo.getTextureId(FrameBufferAttachmentType.Color));
        Gl.DSA.blitFramebuffer(
                (int) outFbo.handle(),
                GL33.glGetInteger(GL33.GL_DRAW_FRAMEBUFFER_BINDING),
                0, 0, MinecraftRenderHandle.getScreenWidth(), MinecraftRenderHandle.getScreenHeight(),
                0, 0, MinecraftRenderHandle.getScreenWidth(), MinecraftRenderHandle.getScreenHeight(),
                GL46.GL_COLOR_BUFFER_BIT,
                GL46.GL_NEAREST
        );
    }

    public static Optional<SRShaderCompatConfig> getCurrentShaderPackConfig() {
        return Optional.ofNullable(IrisApi.getInstance().isShaderPackInUse() ? shaderCompatConfig : null);
    }

    public static void setShaderCompatConfig(SRShaderCompatConfig shaderCompatConfig) {
        ShaderCompatUpscaleDispatcher.shaderCompatConfig = shaderCompatConfig;
    }

    public static RenderTargets getCompositeRendererRenderTargets(CompositeRenderer renderer) {
        return ((CompositeRendererAccessor) renderer).getRenderTargets();
    }

    public static TextureFormat getIrisTextureFormatByName(CompositeRenderer renderer, String name) {
        if (name.startsWith("colortex")) {
            try {
                int index = Integer.parseInt(name.replace("colortex", ""));
                return TextureFormat.fromGl(getCompositeRendererRenderTargets(renderer).getOrCreate(
                        index
                ).getInternalFormat().getGlFormat());

            } catch (NumberFormatException e) {
                return TextureFormat.RGBA8;
            }
        } else if (name.startsWith("alttex")) {
            try {
                int index = Integer.parseInt(name.replace("alttex", ""));
                return TextureFormat.fromGl(getCompositeRendererRenderTargets(renderer).getOrCreate(
                        index
                ).getInternalFormat().getGlFormat());
            } catch (NumberFormatException e) {
                return TextureFormat.RGBA8;
            }
        } else if (name.equals("depthtex")) {
            return TextureFormat.fromGl(TextureInfoCache.INSTANCE.getInfo(((CompositeRendererAccessor) renderer).getRenderTargets().getDepthTexture()).getInternalFormat());
        } else if (name.equals("noHandDepthtex")) {
            return TextureFormat.fromGl(TextureInfoCache.INSTANCE.getInfo(((CompositeRendererAccessor) renderer).getRenderTargets().getDepthTextureNoHand().getTextureId()).getInternalFormat());
        } else if (name.equals("noTranslucentDepthtex")) {
            return TextureFormat.fromGl(TextureInfoCache.INSTANCE.getInfo(((CompositeRendererAccessor) renderer).getRenderTargets().getDepthTextureNoTranslucents().getTextureId()).getInternalFormat());
        }
        return TextureFormat.RGBA8;
    }

    public static int getIrisTextureByName(CompositeRenderer renderer, String name) {
        if (name.startsWith("colortex")) {
            try {
                int index = Integer.parseInt(name.replace("colortex", ""));
                return getCompositeRendererRenderTargets(renderer).getOrCreate(
                        index
                ).getMainTexture();

            } catch (NumberFormatException e) {
                return -1;
            }
        } else if (name.startsWith("alttex")) {
            try {
                int index = Integer.parseInt(name.replace("alttex", ""));
                return getCompositeRendererRenderTargets(renderer).getOrCreate(
                        index
                ).getAltTexture();
            } catch (NumberFormatException e) {
                return -1;
            }
        } else if (name.equals("depthtex")) {
            return ((CompositeRendererAccessor) renderer).getRenderTargets().getDepthTexture();
        } else if (name.equals("noHandDepthtex")) {
            return ((CompositeRendererAccessor) renderer).getRenderTargets().getDepthTextureNoHand().getTextureId();
        } else if (name.equals("noTranslucentDepthtex")) {
            return ((CompositeRendererAccessor) renderer).getRenderTargets().getDepthTextureNoTranslucents().getTextureId();
        }
        return -1;
    }

    static class _FrameBuffer implements IFrameBuffer {
        public _FrameBuffer(ITexture texture) {
            this.texture = texture;
        }

        public final ITexture texture;

        @Override
        public int getWidth() {
            return texture.getWidth();
        }

        @Override
        public int getHeight() {
            return texture.getWidth();
        }

        @Override
        public void clearFrameBuffer() {

        }

        @Override
        public void resizeFrameBuffer(int width, int height) {

        }

        @Override
        public int getTextureId(FrameBufferAttachmentType attachmentType) {
            return Math.toIntExact(texture.handle());
        }

        @Override
        public ITexture getTexture(FrameBufferAttachmentType attachmentType) {
            return texture;
        }

        @Override
        public void setClearColorRGBA(float red, float green, float blue, float alpha) {

        }

        @Override
        public TextureFormat getColorTextureFormat() {
            return texture.getTextureFormat();
        }

        @Override
        public TextureFormat getDepthTextureFormat() {
            return texture.getTextureFormat();
        }

        @Override
        public long handle() {
            return texture.handle();
        }

        @Override
        public void destroy() {

        }
    }

    static class _Texture implements ITexture {
        private final Supplier<TextureFormat> textureFormatSupplier;
        private final Supplier<Integer> widthSupplier;
        private final Supplier<Integer> heightSupplier;
        private final Supplier<Long> handleSupplier;

        public _Texture(Supplier<TextureFormat> textureFormatSupplier, Supplier<Integer> widthSupplier, Supplier<Integer> heightSupplier, Supplier<Long> handleSupplier) {
            this.textureFormatSupplier = textureFormatSupplier;
            this.widthSupplier = widthSupplier;
            this.heightSupplier = heightSupplier;
            this.handleSupplier = handleSupplier;
        }

        @Override
        public TextureFormat getTextureFormat() {
            return textureFormatSupplier.get();
        }

        @Override
        public TextureUsages getTextureUsages() {
            return TextureUsages.create();
        }

        @Override
        public TextureType getTextureType() {
            return TextureType.Texture2D;
        }

        @Override
        public TextureFilterMode getTextureFilterMode() {
            return TextureFilterMode.NEAREST;
        }

        @Override
        public TextureWrapMode getTextureWrapMode() {
            return TextureWrapMode.CLAMP_TO_EDGE;
        }

        @Override
        public TextureMipmapSettings getMipmapSettings() {
            return TextureMipmapSettings.disabled();
        }

        @Override
        public int getWidth() {
            return widthSupplier.get();
        }

        @Override
        public int getHeight() {
            return heightSupplier.get();
        }

        @Override
        public long handle() {
            return handleSupplier.get();
        }

        @Override
        public void destroy() {

        }

        @Override
        public void resize(int width, int height) {

        }
    }
}
