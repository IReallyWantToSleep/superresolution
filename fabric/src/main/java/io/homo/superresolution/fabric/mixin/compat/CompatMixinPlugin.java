package io.homo.superresolution.fabric.mixin.compat;

import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.core.graphics.renderdoc.RenderDoc;
import io.homo.superresolution.fabric.platform.FabricPlatform;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class CompatMixinPlugin implements IMixinConfigPlugin {
    private final String CLASS_START = "io.homo.superresolution.fabric.mixin.compat.";

    public CompatMixinPlugin() {
    }

    public void onLoad(String s) {
        Platform.currentPlatform = new FabricPlatform();
        Platform.currentPlatform.init();
        if (FabricLoader.getInstance().isDevelopmentEnvironment() /*&& SuperResolutionConfig.isEnableRenderDoc()*/)
            RenderDoc.init();

    }

    public String getRefMapperConfig() {
        return null;
    }

    public boolean shouldApplyMixin(String s, String s1) {
        String modid = s1.replace(CLASS_START, "").split("\\.")[0];
        if (Objects.equals(modid, "reesessodiumoptions")) {
            return Platform.currentPlatform.isModLoaded("reeses-sodium-options");
        }
        return Platform.currentPlatform.isModLoaded(modid);
    }

    public void acceptTargets(Set<String> set, Set<String> set1) {
    }

    public List<String> getMixins() {
        return List.of();
    }

    public void preApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {
    }

    public void postApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {
    }
}
