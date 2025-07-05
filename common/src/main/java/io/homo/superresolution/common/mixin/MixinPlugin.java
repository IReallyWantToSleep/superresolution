package io.homo.superresolution.common.mixin;

import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.objectweb.asm.tree.ClassNode;
import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {
    private final String MIXIN_CLASS_START = "io.homo.superresolution.common.mixin.";

    public MixinPlugin() {
    }

    public void onLoad(String s) {
        GraphicsCapabilities.init();
    }

    public String getRefMapperConfig() {
        return null;
    }

    public boolean shouldApplyMixin(String tClass, String mClassPath) {
        String mixinClassify = getClassName(mClassPath).split("\\.")[0];
        String mixinName = getClassName(mClassPath).split("\\.")[1];
        return shouldApplyMixinByName(mixinName) && (
                switch (mixinClassify) {
                    case "core", "gui", "compat" -> true;
                    case "debug" -> Platform.currentPlatform.isDevelopmentEnvironment();
                    default -> false;
                }
        );
    }

    private boolean shouldApplyMixinByName(String name) {
        return !name.contains("ForceOpenGLVersion_WindowMixin") || !Platform.currentPlatform.isModLoaded("threatengl");
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
