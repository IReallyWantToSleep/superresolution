package io.homo.superresolution.neoforge.mixin.compat.sodiumoptionsapi;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.gui.ConfigScreenBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

#if MC_VER != 1 && MC_VER < MC_1_21_6
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.Tab;
import toni.sodiumoptionsapi.gui.SodiumOptionsTabFrame;

@Mixin(SodiumOptionsTabFrame.class)
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
#else
@Mixin(Minecraft.class)
public class SodiumOptionsTabFrameMixin {
}
#endif