package io.homo.superresolution.common.upscale.sgsr;

import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.config.enums.SgsrVariant;
import io.homo.superresolution.common.render.MinecraftRenderHandle;
import io.homo.superresolution.common.render.gl.buffer.GlUniformBuffer;
import io.homo.superresolution.common.render.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.common.render.impl.framebuffer.StorageFrameBuffer;
import io.homo.superresolution.common.render.gl.texture.GlTexture;
import io.homo.superresolution.common.render.impl.texture.TextureWrapper;
import io.homo.superresolution.common.upscale.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.sgsr.variants.Sgsr2PassCompute;
import io.homo.superresolution.common.upscale.sgsr.variants.Sgsr2PassFragment;
import io.homo.superresolution.common.upscale.sgsr.variants.Sgsr3PassCompute;
import org.joml.Matrix4f;

import java.util.function.Consumer;

public class Sgsr extends AbstractAlgorithm {
    private AbstractSgsrVariant variantInstance;
    private SgsrVariant currentVariant;
    private GlUniformBuffer<SgsrParams> params;

    public static Sgsr create() {
        return new Sgsr();
    }

    private void initVariant() {
        if (checkVariant()) {
            if (variantInstance != null) {
                variantInstance.destroy();
            }
            variantInstance = switch (Config.getSpecial().sgsr.variant) {
                case CS_2 -> new Sgsr2PassCompute();
                case CS_3 -> new Sgsr3PassCompute();
                case FS_2 -> new Sgsr2PassFragment();
            };
            variantInstance.init(this);
        }
    }

    private boolean checkVariant() {
        if (variantInstance == null) return true;
        if (Config.getSpecial().sgsr.variant != currentVariant) {
            currentVariant = Config.getSpecial().sgsr.variant;
            return true;
        }
        return false;
    }

    @Override
    public void init() {
        input = MinecraftRenderHandle.getRenderTarget();
        output = new StorageFrameBuffer(false);
        this.resize(AlgorithmManager.helper.getScreenWidth(), AlgorithmManager.helper.getScreenHeight());
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
                output.getTextureId(IFrameBuffer.FrameBufferAttachmentType.COLOR)
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
