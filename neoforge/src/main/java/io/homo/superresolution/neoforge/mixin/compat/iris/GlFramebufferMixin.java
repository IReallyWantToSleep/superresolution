package io.homo.superresolution.neoforge.mixin.compat.iris;

import net.irisshaders.iris.gl.GlResource;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.gl.texture.DepthBufferFormat;
import net.irisshaders.iris.pbr.TextureInfoCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static io.homo.superresolution.core.gl.GlConst.*;

@Mixin(value = GlFramebuffer.class, remap = false)
public abstract class GlFramebufferMixin extends GlResource {
    @Unique
    private int super_resolution$currentDepthAttachmentType = 0;

    public GlFramebufferMixin() {
        super(0);
    }

    @Shadow
    public abstract int getStatus();

    @Inject(method = "addDepthAttachment", at = @At("RETURN"))
    private void checkFboCompleteness(int texture, CallbackInfo ci) {
        int status = getStatus();
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException(String.valueOf(status));
        }
    }

    @Inject(method = "addDepthAttachment", at = @At("HEAD"))
    public void addDepthAttachment(int texture, CallbackInfo ci) {
        if (super_resolution$currentDepthAttachmentType != 0) {
            IrisRenderSystem.framebufferTexture2D(
                    getGlId(),
                    GL_FRAMEBUFFER,
                    super_resolution$currentDepthAttachmentType,
                    GL_TEXTURE_2D,
                    0,
                    0
            );
        }

        super_resolution$currentDepthAttachmentType = super_resolution$detectAttachmentType(TextureInfoCache.INSTANCE.getInfo(texture).getInternalFormat());
    }

    @Unique
    private int super_resolution$detectAttachmentType(int format) {
        return DepthBufferFormat.fromGlEnumOrDefault(format).isCombinedStencil() ? GL_DEPTH_STENCIL_ATTACHMENT : GL_DEPTH_ATTACHMENT;
    }
}