package io.homo.superresolution.neoforge.mixin.compat.reesessodiumoptions;


import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.TabFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Optional;

import io.homo.superresolution.common.gui.ConfigScreenBuilder;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.Tab;

@Mixin(value = TabFrame.class, remap = false)
public class TabFrameMixin {
    @Inject(method = "setTab", at = @At(value = "HEAD"), cancellable = true)
    private void onSetTab(Optional<Tab<?>> tab, CallbackInfo ci) {
        if (tab.orElseThrow().getTitle().getString().equals(Component.translatable("superresolution.screen.config.name").getString())) {
            Minecraft.getInstance().setScreen(ConfigScreenBuilder.create().buildConfigScreen(Minecraft.getInstance().screen));
            ci.cancel();
        }
    }
}
