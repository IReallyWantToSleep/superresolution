package io.homo.superresolution.fabric.mixin.compat.sodium;

#if MC_VER > MC_1_20_6

import io.homo.superresolution.common.platform.Platform;
import net.caffeinemc.mods.sodium.client.gui.SodiumOptionsGUI;
#else

import io.homo.superresolution.common.platform.Platform;
import me.jellysquid.mods.sodium.client.gui.SodiumOptionsGUI;
#endif


import com.google.common.collect.ImmutableList;
import io.homo.superresolution.common.gui.ConfigScreenBuilder;
#if MC_VER > MC_1_20_6
import net.caffeinemc.mods.sodium.client.gui.options.OptionGroup;
import net.caffeinemc.mods.sodium.client.gui.options.OptionImpl;
import net.caffeinemc.mods.sodium.client.gui.options.OptionPage;
import net.caffeinemc.mods.sodium.client.gui.options.control.SliderControl;
import net.caffeinemc.mods.sodium.client.gui.options.storage.MinecraftOptionsStorage;
#else
import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpl;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import me.jellysquid.mods.sodium.client.gui.options.storage.MinecraftOptionsStorage;
#endif
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

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
        if (Platform.currentPlatform.isModLoaded("sodiumoptionsapi")) return;
        this.page = new OptionPage(
                Component.translatable("superresolution.screen.config.name"), ImmutableList.of(
                OptionGroup.createBuilder()
                        .add(OptionImpl.createBuilder(Integer.class, new MinecraftOptionsStorage())
                                .setBinding((Options o, Integer b) -> {
                                }, (Options o) -> 1)
                                .setControl((option) -> new SliderControl(option, 0, Minecraft.getInstance().getWindow().calculateScale(0, Minecraft.getInstance().isEnforceUnicode()), 1,
                                        (a) -> Component.literal("")
                                ))
                                .setName(Component.literal(""))
                                .setTooltip(Component.literal(""))
                                .build()
                        )
                        .build()
        )
        );
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
            this.minecraft.setScreen(ConfigScreenBuilder.create().buildConfigScreen(this));
            ci.cancel();
        }
    }
}
