package io.homo.superresolution.forge.mixin.compat.oculus;

import org.spongepowered.asm.mixin.Mixin;


/**
 * 修复跟沉浸工程的兼容性问题？或者更多？
 * bug原因：
 * 添加深度模板/深度纹理没有清理fbo已附加的纹理
 * iris源代码看起来是iris假定addDepthAttachment方法只会在一个fbo上调用一次
 * 而且附件类型不是GL_DEPTH_STENCIL_ATTACHMENT就是GL_DEPTH_ATTACHMENT
 * 导致切换纹理类型后同时附加GL_DEPTH_ATTACHMENT和GL_DEPTH_STENCIL_ATTACHMENT
 * 然后fbo就4了
 * iris甚至没有检测fbo状态（）
 * 修复方法：直接移除上次附加的深度纹理
 */
import io.homo.superresolution.common.SuperResolution;
import net.irisshaders.iris.gl.GlResource;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.gl.texture.DepthBufferFormat;
import net.irisshaders.iris.texture.TextureInfoCache;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.opengl.GL45.*;

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
            SuperResolution.LOGGER.error("FBO不完整 CODE:{}", status);
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