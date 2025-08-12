package io.homo.superresolution.shadercompat;

import com.google.common.collect.ImmutableList;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.shadercompat.mixin.SRCompatShaderPack;
import io.homo.superresolution.shadercompat.mixin.core.CompositeRendererAccessor;
import io.homo.superresolution.shadercompat.mixin.core.IrisRenderingPipelineAccessor;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.CompositeRenderer;
import net.irisshaders.iris.shaderpack.ShaderPack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * IMS212到自己家门口发现门是private的，于是用反射打开了门
 */
public class IrisShaderPipelineHandle {
    /**
     * 拿到CompositeRender.Pass里的pass名称
     */
    public static String getCompositePassName(Object obj) {
        try {
            Class<?> passClazz = Class.forName("net.irisshaders.iris.pipeline.CompositeRenderer.Pass");
            return (String) passClazz.getDeclaredField("name").get(obj);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return null;
        }
    }

    /**
     * 拿到CompositeRenderer的全部Pass
     */
    public static ImmutableList<?> getCompositeRendererPasses(CompositeRenderer compositeRenderer) {
        return ((CompositeRendererAccessor) compositeRenderer).getPasses();
    }

    /**
     * 当调用CompositeRenderer.renderAll时，方法内部遍历pass列表，每当该pass完成调用此方法
     */
    public static void onCompositeRendererRender(
            CompositeRenderer compositeRenderer,
            int currentPassIndex
    ) {
        if (!shouldApplySuperResolutionChanges()) return;

        if (Iris.getPipelineManager().getPipeline().isPresent()) {
            //检查CompositeRenderer是不是Composite阶段的
            if (compositeRenderer.equals(((IrisRenderingPipelineAccessor) Iris.getPipelineManager().getPipeline().get()).getCompositeRenderer())) {
                if (ShaderCompatUpscaleDispatcher.getCurrentConfig() != null && ShaderCompatUpscaleDispatcher.getCurrentConfig().enabled) {
                    String targetPassName = ShaderCompatUpscaleDispatcher.getCurrentConfig().upscale_config.before_upscale_shader_name;
                    String currentPassName = getCompositePassName(getCompositeRendererPasses(compositeRenderer).get(currentPassIndex));
                    if (targetPassName.equals(currentPassName)) {
                        ShaderCompatUpscaleDispatcher.dispatchUpscale(compositeRenderer);
                    }
                }
            }
        }
    }

    public static Optional<SRShaderCompatConfig> getCurrentShaderPackConfig(Optional<ShaderPack> shaderPack) {
        return shaderPack.map(pack -> ((SRCompatShaderPack) pack).superresolution$getSuperResolutionComaptConfig());
    }

    public static @NotNull Optional<ShaderPack> getCurrentShaderPack() {
        return Iris.getCurrentPack();
    }

    public static boolean shouldApplySuperResolutionChanges() {
        return getCurrentShaderPack().isPresent() &&
                ((SRCompatShaderPack) getCurrentShaderPack().get()).superresolution$isSupportsSuperResolution();
    }
}
