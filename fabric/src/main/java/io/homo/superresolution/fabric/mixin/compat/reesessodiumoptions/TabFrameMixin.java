package io.homo.superresolution.fabric.mixin.compat.reesessodiumoptions;

import io.homo.superresolution.common.gui.ConfigScreenBuilder;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.Tab;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.TabFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TabFrame.class, remap = false)
public class TabFrameMixin {
    @Inject(method = "setTab", at = @At(value = "HEAD"), cancellable = true)
    #if MC_VER > MC_1_20_1
    private void onSetTab(Optional<Tab<?>> tab, CallbackInfo ci)
    #else
    private void onSetTab(Tab<?> tab, CallbackInfo ci)
    #endif {
        if (tab.getTitle().getString().equals(Component.translatable("superresolution.screen.config.name").getString())) {
            Minecraft.getInstance().setScreen(ConfigScreenBuilder.create().build(Minecraft.getInstance().screen));
            ci.cancel();
        }
    }
}
