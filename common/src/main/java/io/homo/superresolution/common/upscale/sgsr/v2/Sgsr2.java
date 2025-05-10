package io.homo.superresolution.common.upscale.sgsr.v2;

import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.config.enums.SgsrVariant;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.core.gl.buffer.GlUniformBuffer;
import io.homo.superresolution.core.gl.framebuffer.GlFrameBufferAttachment;
import io.homo.superresolution.core.gl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.gl.texture.GlTexture;
import io.homo.superresolution.core.impl.texture.TextureFormat;
import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.sgsr.v2.variants.Sgsr2PassCompute;
import io.homo.superresolution.common.upscale.sgsr.v2.variants.Sgsr2PassFragment;
import io.homo.superresolution.common.upscale.sgsr.v2.variants.Sgsr3PassCompute;

import java.util.function.Consumer;

public class Sgsr2 extends AbstractAlgorithm {
    private AbstractSgsrVariant variantInstance;
    private SgsrVariant currentVariant;
    private GlUniformBuffer<SgsrParams> params;

    private void initVariant() {
        if (checkVariant()) {
            if (variantInstance != null) {
                variantInstance.destroy();
            }
            variantInstance = switch (Config.getSpecial().sgsr2.variant) {
                case CS_2 -> new Sgsr2PassCompute();
                case CS_3 -> new Sgsr3PassCompute();
                case FS_2 -> new Sgsr2PassFragment();
            };
            variantInstance.init(this);
        }
    }

    private boolean checkVariant() {
        if (variantInstance == null) return true;
        if (Config.getSpecial().sgsr2.variant != currentVariant) {
            currentVariant = Config.getSpecial().sgsr2.variant;
            return true;
        }
        return false;
    }

    @Override
    public void init() {
        input = MinecraftRenderHandle.getRenderTarget();
        GlFrameBuffer output_ = new GlFrameBuffer();
        output_.addAttachment(new GlFrameBufferAttachment(
                GlFrameBufferAttachment.FrameBufferAttachmentType.COLOR,
                GlTexture.create(
                        MinecraftRenderHandle.getRenderWidth(),
                        MinecraftRenderHandle.getRenderHeight(),
                        TextureFormat.RGBA8
                )
        ));
        output = output_;
        this.resize(MinecraftRenderHandle.getScreenWidth(), MinecraftRenderHandle.getScreenHeight());
        params = new GlUniformBuffer<>(new SgsrParams());
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        initVariant();
        variantInstance.setOutput(output);
        params.struct().updateData(dispatchResource);
        params.update();
        variantInstance.dispatch(dispatchResource, this);
        return false;
    }


    @Override
    public void blitToScreen(int width, int height) {
        GlTexture.blitToScreen(
                width,
                height,
                width,
                height,
                output.getTextureId(FrameBufferAttachmentType.COLOR)
        );
    }

    @Override
    public void resize(int width, int height) {
        safeVariantInstance((sgsrVariant -> sgsrVariant.resize(width, height)));
        this.output.resizeFrameBuffer(width, height);
    }

    private void safeVariantInstance(Consumer<AbstractSgsrVariant> callback) {
        if (variantInstance != null) callback.accept(variantInstance);
    }

    public GlUniformBuffer<SgsrParams> getParams() {
        return params;
    }
}
