package io.homo.superresolution.mixin;

import dev.architectury.platform.Platform;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {
    private final String MIXIN_CLASS_START = "io.homo.superresolution.mixin.";

    public MixinPlugin() {
    }

    public void onLoad(String s) {
    }

    public String getRefMapperConfig() {
        return null;
    }

    public boolean shouldApplyMixin(String tClass, String mClassPath) {
        String mixinClassify = getClassName(mClassPath).split("\\.")[0];
        boolean debug = Platform.isDevelopmentEnvironment();
        return switch (mixinClassify) {
            case "core", "gui" -> true;
            case "debug" -> debug;
            default -> false;
        };
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

    private String getClassName(String mClassPath) {
        return mClassPath.replace(MIXIN_CLASS_START, "");
    }
}
