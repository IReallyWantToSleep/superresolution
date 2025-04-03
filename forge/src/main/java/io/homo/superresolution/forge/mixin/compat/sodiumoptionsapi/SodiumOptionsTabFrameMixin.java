package io.homo.superresolution.forge.mixin.compat.sodiumoptionsapi;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.gui.ConfigScreenBuilder;
import org.embeddedt.embeddium.gui.frame.tab.Tab;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import toni.sodiumoptionsapi.gui.SodiumOptionsTabFrame;

import java.util.Objects;

@Mixin(value = SodiumOptionsTabFrame.class, remap = false)
public class SodiumOptionsTabFrameMixin {
    @Shadow
    private String selectedHeader;

    @Inject(method = "setTab", at = @At(value = "HEAD"), cancellable = true)
    private void onSetTab(Tab<?> tab, CallbackInfo ci) {
        if (Objects.equals(this.selectedHeader, SuperResolution.MOD_ID)) {
            Minecraft.getInstance().setScreen(ConfigScreenBuilder.create().buildConfigScreen(Minecraft.getInstance().screen));
            ci.cancel();
        }
    }
}
