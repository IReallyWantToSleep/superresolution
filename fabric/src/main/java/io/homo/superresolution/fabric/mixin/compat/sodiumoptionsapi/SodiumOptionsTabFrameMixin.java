package io.homo.superresolution.fabric.mixin.compat.sodiumoptionsapi;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.gui.ConfigScreenBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
#if MC_VER != MC_1_20_4 && MC_VER < MC_1_21_6 && MC_VER != MC_1_20_5 && MC_VER != MC_1_20_6
import toni.sodiumoptionsapi.gui.SodiumOptionsTabFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.Tab;
#endif
import java.util.Objects;

#if MC_VER != MC_1_20_4 && MC_VER < MC_1_21_6 && MC_VER != MC_1_20_5 && MC_VER != MC_1_20_6
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
#else
@Mixin(value = Minecraft.class)
public class SodiumOptionsTabFrameMixin {
}
#endif
