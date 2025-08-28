package io.homo.superresolution.shadercompat.mixin.core;

import com.google.common.collect.ImmutableList;
import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.api.registry.AlgorithmRegistry;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.shadercompat.IrisShaderPipelineHandle;
import io.homo.superresolution.shadercompat.SRCompatShaderPack;
import net.irisshaders.iris.gl.shader.StandardMacros;
import net.irisshaders.iris.helpers.StringPair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(StandardMacros.class)
public class StandardMacrosMixin {
    @Inject(method = "createStandardEnvironmentDefines", at = @At("TAIL"), cancellable = true, remap = false)
    private static void addSRDefines(CallbackInfoReturnable<ImmutableList<StringPair>> cir) {
        if (SuperResolutionConfig.isForceDisableShaderCompat())
            return;

        var defines = new ArrayList<>(cir.getReturnValue());
        defines.add(new StringPair("SR_INSTALLED", "1"));
        Map<AlgorithmDescription<?>, Integer> idMap = new HashMap<>();
        List<AlgorithmDescription<?>> algorithms = new ArrayList<>(AlgorithmRegistry.getAlgorithmMap().values());
        AlgorithmRegistry.getAlgorithmMap().values().forEach((desc) -> {
            int id = algorithms.indexOf(desc) + 0x546F0;
            idMap.put(desc, id);
            defines.add(new StringPair("SR_ALGO_" + desc.codeName.toUpperCase(), Integer.toString(id)));
        });
        if (SuperResolutionConfig.isEnableUpscale()) {
            defines.add(new StringPair("SR_ENABLE", "1"));
            defines.add(new StringPair("SR_DISABLE", "0"));
            defines.add(new StringPair("SR_SUPPORTS_JITTER", SuperResolution.getCurrentAlgorithm().isSupportJitter() ? "1" : "0"));
            defines.add(new StringPair("SR_USING_ALGO", Integer.toString(
                    idMap.get(SuperResolutionConfig.getUpscaleAlgorithm()
                    )
            )));
        } else {
            defines.add(new StringPair("SR_ENABLE", "0"));
            defines.add(new StringPair("SR_DISABLE", "1"));
            defines.add(new StringPair("SR_SUPPORTS_JITTER", "0"));
            defines.add(new StringPair("SR_USING_ALGO", "0"));
        }
        cir.setReturnValue(ImmutableList.copyOf(defines));
    }
}
