package io.homo.superresolution.shadercompat.mixin.core;

import com.google.common.collect.ImmutableList;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.shadercompat.SRShaderCompatConfig;
import io.homo.superresolution.shadercompat.SRCompatShaderPack;
import net.irisshaders.iris.shaderpack.ShaderPack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@Mixin(value = ShaderPack.class, remap = false)
public class ShaderPackMixin implements SRCompatShaderPack {
    @Unique
    private SRShaderCompatConfig superresolution$config;

    #if MC_VER > MC_1_20_6
    @Inject(method = "<init>(Ljava/nio/file/Path;Ljava/util/Map;Lcom/google/common/collect/ImmutableList;Z)V", at = @At("RETURN"), remap = false)
    private void loadSuperResolutionComaptConfig(
            Path root,
            Map<?, ?> changedConfigs,
            ImmutableList<?> environmentDefines,
            boolean isZip,
            CallbackInfo ci
    )
    #else
    @Inject(method = "<init>(Ljava/nio/file/Path;Ljava/util/Map;Lcom/google/common/collect/ImmutableList;)V", at = @At("RETURN"), remap = false)
    private void loadSuperResolutionComaptConfig(
            Path root,
            Map<?, ?> changedConfigs,
            ImmutableList<?> environmentDefines,
            CallbackInfo ci
    )
    #endif {
        try {
            Path srConfigPath = root.resolve("superresolution.json");
            if (Files.exists(srConfigPath)) {

                superresolution$config = SRShaderCompatConfig.loadFromJson(srConfigPath);
                SuperResolution.LOGGER.info("光影包 {} 支持超分辨率功能", root);
                return;
            }
        } catch (NoSuchFileException ignored) {
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            SuperResolution.LOGGER.warn("加载 {} 光影包中的 superresolution.json 时发生错误", root);
        }
        superresolution$config = null;
    }

    @Unique
    public SRShaderCompatConfig superresolution$getSuperResolutionComaptConfig() {
        return SuperResolutionConfig.isForceDisableShaderCompat() ? null : superresolution$config;
    }

    @Unique
    public boolean superresolution$isSupportsSuperResolution() {
        return !SuperResolutionConfig.isForceDisableShaderCompat() && superresolution$config != null && superresolution$config.sr.enabled;
    }
}
