package io.homo.superresolution.shadercompat;

import com.google.common.collect.ImmutableList;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.shadercompat.mixin.core.CompositeRendererAccessor;
import io.homo.superresolution.shadercompat.mixin.core.IrisRenderingPipelineAccessor;
import io.homo.superresolution.shadercompat.mixin.core.RenderTargetsAccessor;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.pipeline.CompositeRenderer;
import net.irisshaders.iris.shaderpack.ShaderPack;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Optional;

/**
 * IMS212到自己家门口发现门是private的，于是用反射打开了门
 */
public class IrisShaderPipelineHandle {
    /**
     * 拿到CompositeRender.Pass里的pass名称
     */
    public static String getCompositePassName(Object obj) {
        if (obj instanceof NamedCompositePass) {
            return ((NamedCompositePass) obj).superresolution$getName();
        }
        try {
            Class<?> passClazz = Class.forName("net.irisshaders.iris.pipeline.CompositeRenderer$Pass");
            Field nameField = passClazz.getDeclaredField("name");
            nameField.setAccessible(true);
            return (String) nameField.get(obj);
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
            NamedCompositePass currentPass
    ) {
        if (!shouldApplySuperResolutionChanges()) return;
        try {
            //检查renderTargets是不是null以及是否被销毁，否则1.21.5+会报Tried to use destroyed RenderTargets
            if (((CompositeRendererAccessor) compositeRenderer).getRenderTargets() != null) {
                if (
                        !(
                                (RenderTargetsAccessor) (
                                        (
                                                (CompositeRendererAccessor) compositeRenderer
                                        )
                                                .getRenderTargets()
                                )
                        ).isDestroyed()) {

                    if (Iris.getPipelineManager().getPipeline().isPresent()) {
                        //检查CompositeRenderer是不是Composite阶段的
                        if (compositeRenderer.equals(((IrisRenderingPipelineAccessor) Iris.getPipelineManager().getPipeline().get()).getCompositeRenderer())) {
                            if (ShaderCompatUpscaleDispatcher.getCurrentConfig() != null && ShaderCompatUpscaleDispatcher.getCurrentConfig().enabled) {
                                String targetPassName = ShaderCompatUpscaleDispatcher.getCurrentConfig().upscale_config.before_upscale_shader_name;
                                String currentPassName = getCompositePassName(currentPass);
                                if (targetPassName.equals(currentPassName)) {
                                    ShaderCompatUpscaleDispatcher.dispatchUpscale(compositeRenderer);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable throwable) {
            SuperResolution.LOGGER.error("执行超分时发生错误");
            throwable.printStackTrace();
        }
    }

    public static Optional<SRShaderCompatConfig> getCurrentShaderPackConfig(Optional<ShaderPack> shaderPack) {
        return shaderPack.map(pack -> ((SRCompatShaderPack) pack).superresolution$getSuperResolutionComaptConfig());
    }

    public static @NotNull Optional<ShaderPack> getCurrentShaderPack() {
        return Iris.getCurrentPack();
    }

    public static boolean shouldApplySuperResolutionChanges() {
        return !SuperResolutionConfig.isForceDisableShaderCompat() && IrisApi.getInstance().isShaderPackInUse() && getCurrentShaderPack().isPresent() &&
                ((SRCompatShaderPack) getCurrentShaderPack().get()).superresolution$isSupportsSuperResolution()
                && ((SRCompatShaderPack) getCurrentShaderPack().get()).superresolution$getSuperResolutionComaptConfig() != null
                && ((SRCompatShaderPack) getCurrentShaderPack().get()).superresolution$getSuperResolutionComaptConfig().sr.enabled;
    }

    public static boolean shouldApplySuperResolutionChangesJitter() {
        return !SuperResolutionConfig.isForceDisableShaderCompat() && IrisApi.getInstance().isShaderPackInUse() && getCurrentShaderPack().isPresent() &&
                ((SRCompatShaderPack) getCurrentShaderPack().get()).superresolution$isSupportsSuperResolution()
                && ((SRCompatShaderPack) getCurrentShaderPack().get()).superresolution$getSuperResolutionComaptConfig() != null
                && ((SRCompatShaderPack) getCurrentShaderPack().get()).superresolution$getSuperResolutionComaptConfig().sr_jitter.enabled;
    }
}
