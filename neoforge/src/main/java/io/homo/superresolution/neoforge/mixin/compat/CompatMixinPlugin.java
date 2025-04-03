package io.homo.superresolution.neoforge.mixin.compat;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.neoforge.platform.NeoForgePlatform;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class CompatMixinPlugin implements IMixinConfigPlugin {
    public static final Logger LOGGER = LoggerFactory.getLogger("SuperResolution-Mixin");
    private final String CLASS_START = "io.homo.superresolution.neoforge.mixin.compat.";

    public CompatMixinPlugin() {
    }

    public void onLoad(String s) {
        Platform.currentPlatform = new NeoForgePlatform();
        Platform.currentPlatform.init();
    }

    public String getRefMapperConfig() {
        return null;
    }

    public boolean shouldApplyMixin(String s, String s1) {
        boolean b = _shouldApplyMixin(s, s1);
        if (!b) {
            LOGGER.info("已禁用Mixin {}", s1);
        } else {
            LOGGER.info("已启用Mixin {}", s1);
        }
        return b;
    }

    public boolean _shouldApplyMixin(String s, String s1) {
        String modid = s1.replace(CLASS_START, "").split("\\.")[0];
        if (Objects.equals(modid, "reesessodiumoptions")) {
            return Platform.currentPlatform.isModLoaded("reeses_sodium_options");
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
