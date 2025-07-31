package io.homo.superresolution.fabric.mixin.compat.reesessodiumoptions;

import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.Minecraft;

#if MC_VER < 100000000
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.homo.superresolution.common.gui.ConfigScreenBuilder;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.Tab;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.TabFrame;
import net.minecraft.network.chat.Component;


#if MC_VER > MC_1_20_1
import java.util.Optional;
#endif

@Mixin(value = TabFrame.class, remap = false)
public class TabFrameMixin {
    @Inject(method = "setTab", at = @At(value = "HEAD"), cancellable = true)
    #if MC_VER > MC_1_20_6
    private void onSetTab(Optional<Tab<?>> tab, CallbackInfo ci)
    #else
    private void onSetTab(Tab<?> tab, CallbackInfo ci)
    #endif {
        if (
                tab
                        #if MC_VER > MC_1_20_6
                        .orElseThrow()
                        #endif
                        .getTitle()
                        .getString().equals(Component.translatable("superresolution.screen.config.name").getString())) {
            Minecraft.getInstance().setScreen(ConfigScreenBuilder.create().buildConfigScreen(Minecraft.getInstance().screen));
            ci.cancel();
        }
    }
}
#else
@Mixin(value = Minecraft.class)
public class TabFrameMixin {
}
#endif