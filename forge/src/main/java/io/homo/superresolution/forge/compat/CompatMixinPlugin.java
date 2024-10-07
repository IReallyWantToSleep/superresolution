package io.homo.superresolution.forge.compat;

import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class CompatMixinPlugin implements IMixinConfigPlugin{
    private String CLASS_START = "io.homo.superresolution.forge.compat.";
    public CompatMixinPlugin(){}
    public void onLoad(String s) {}
    public String getRefMapperConfig() {
        return null;
    }
    public boolean shouldApplyMixin(String s, String s1) {
        String modid = s1.replace(CLASS_START,"").split("\\.")[0];
        return LoadingModList.get().getModFileById(modid) != null;
    }
    public void acceptTargets(Set<String> set, Set<String> set1) {}
    public List<String> getMixins() {
        return List.of();
    }
    public void preApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {}
    public void postApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {}
    public static boolean checkIfClassExists(String className) {
        try {
            Class.forName(className);
            return true; // 类存在
        } catch (ClassNotFoundException e) {
            return false; // 类不存在
        }
    }
}
