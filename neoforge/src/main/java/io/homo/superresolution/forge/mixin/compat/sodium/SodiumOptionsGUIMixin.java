package io.homo.superresolution.forge.mixin.compat.sodium;

import com.google.common.collect.ImmutableList;
import io.homo.superresolution.gui.ConfigScreenBuilder;
import net.caffeinemc.mods.sodium.client.gui.SodiumOptionsGUI;
import net.caffeinemc.mods.sodium.client.gui.options.OptionPage;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SodiumOptionsGUI.class)
public class SodiumOptionsGUIMixin extends Screen {
    @Shadow(remap = false)
    @Final
    private List<OptionPage> pages;
    @Unique
    private OptionPage page;
    protected SodiumOptionsGUIMixin(Component title) {
        super(title);
    }

    @Inject(
            method = "<init>",
            at = {@At("RETURN")}
    )
    private void onInit(Screen prevScreen, CallbackInfo ci) {
        Component shaderPacksTranslated = Component.literal("超分辨率配置");
        this.page = new OptionPage(shaderPacksTranslated, ImmutableList.of());
        this.pages.add(this.page);
    }

    @Inject(
            method = "setPage",
            at = {@At("HEAD")},
            remap = false,
            cancellable = true
    )
    private void onSetPage(OptionPage page, CallbackInfo ci) {
        if (page == this.page) {
            this.minecraft.setScreen(ConfigScreenBuilder.create().build(this));
            ci.cancel();
        }
    }
}
