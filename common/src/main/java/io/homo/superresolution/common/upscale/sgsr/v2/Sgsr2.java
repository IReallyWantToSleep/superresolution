package io.homo.superresolution.common.upscale.sgsr.v2;

import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.config.enums.SgsrVariant;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.buffer.BufferDescription;
import io.homo.superresolution.core.graphics.impl.buffer.BufferUsage;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsages;
import io.homo.superresolution.core.graphics.opengl.buffer.GlBuffer;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBufferAttachment;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.sgsr.v2.variants.Sgsr2PassCompute;
import io.homo.superresolution.common.upscale.sgsr.v2.variants.Sgsr2PassFragment;
import io.homo.superresolution.common.upscale.sgsr.v2.variants.Sgsr3PassCompute;

import java.util.function.Consumer;

public class Sgsr2 extends AbstractAlgorithm {
    private AbstractSgsrVariant variantInstance;
    private SgsrVariant currentVariant;
    private SgsrParams params;
    private GlBuffer paramsUbo;

    @Override
    public void init() {
        input = MinecraftRenderHandle.getRenderTarget();
        GlFrameBuffer output_ = new GlFrameBuffer();
        output_.addAttachment(new GlFrameBufferAttachment(
                GlFrameBufferAttachment.FrameBufferAttachmentType.COLOR,
                RenderSystems.current().device().createTexture(TextureDescription.create()
                        .type(TextureType.Texture2D)
                        .width(MinecraftRenderHandle.getRenderWidth())
                        .height(MinecraftRenderHandle.getRenderHeight())
                        .usages(TextureUsages.create().sampler().storage().sampler())
                        .format(TextureFormat.RGBA8)
                        .build())
        ));
        output = output_;
        this.resize(MinecraftRenderHandle.getScreenWidth(), MinecraftRenderHandle.getScreenHeight());
        params = new SgsrParams();
        paramsUbo = RenderSystems.current().device().createBuffer(BufferDescription.create()
                .usage(BufferUsage.UBO)
                .size(params.size())
                .build()
        );
        paramsUbo.setBufferData(params);
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        initVariant();
        variantInstance.setOutput(output);
        params.updateData(dispatchResource);
        paramsUbo.upload();
        variantInstance.dispatch(dispatchResource, this);
        return false;
    }

    @Override
    public void destroy() {
        this.variantInstance.destroy();
        params.free();
        paramsUbo.destroy();
    }

    @Override
    public void resize(int width, int height) {
        safeVariantInstance((sgsrVariant -> sgsrVariant.resize(width, height)));
        this.output.resizeFrameBuffer(width, height);
    }

    @Override
    public IFrameBuffer getOutputFrameBuffer() {
        return super.getOutputFrameBuffer();
    }

    private void initVariant() {
        if (checkVariant()) {
            if (variantInstance != null) {
                variantInstance.destroy();
            }
            variantInstance = switch (Config.SPECIAL.SGSR2.VARIANT.get()) {
                case CS_2 -> new Sgsr2PassCompute();
                case CS_3 -> new Sgsr3PassCompute();
                case FS_2 -> new Sgsr2PassFragment();
            };
            variantInstance.init(this);
        }
    }

    private boolean checkVariant() {
        if (variantInstance == null) return true;
        if (Config.SPECIAL.SGSR2.VARIANT.get() != currentVariant) {
            currentVariant = Config.SPECIAL.SGSR2.VARIANT.get();
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
}
