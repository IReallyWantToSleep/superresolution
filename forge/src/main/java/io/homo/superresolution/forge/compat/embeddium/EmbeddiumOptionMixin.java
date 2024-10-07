package io.homo.superresolution.forge.compat.embeddium;


import com.google.common.collect.Multimap;
import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.gui.ConfigScreenBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.embeddedt.embeddium.client.gui.options.OptionIdentifier;
import org.embeddedt.embeddium.gui.EmbeddiumVideoOptionsScreen;
import org.embeddedt.embeddium.gui.frame.tab.Tab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EmbeddiumVideoOptionsScreen.class)
public class EmbeddiumOptionMixin{
    @Inject(method = "createShaderPackButton",at= @At(value = "RETURN"),remap = false)
    private void addMyConfigScreen(Multimap<String, Tab<?>> tabs, CallbackInfo ci){
        tabs.put(SuperResolution.MOD_ID, Tab.createBuilder().setTitle(Component.literal("FSR配置界面")).setId(OptionIdentifier.create(SuperResolution.MOD_ID, "emb_configscreen")).setOnSelectFunction(() -> {
            Minecraft.getInstance().setScreen(ConfigScreenBuilder.create().build((EmbeddiumVideoOptionsScreen)(Object)this));
            return false;
        }).build());
    }
}
