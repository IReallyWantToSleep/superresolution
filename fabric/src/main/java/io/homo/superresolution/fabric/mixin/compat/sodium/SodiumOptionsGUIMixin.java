/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.fabric.mixin.compat.sodium;

#if MC_VER > MC_1_20_6 && MC_VER < MC_1_21_8

import io.homo.superresolution.api.platform.Platform;
import net.caffeinemc.mods.sodium.client.gui.SodiumOptionsGUI;
#elif MC_VER < MC_1_21_10
import io.homo.superresolution.api.platform.Platform;
import me.jellysquid.mods.sodium.client.gui.SodiumOptionsGUI;
#endif


import com.google.common.collect.ImmutableList;
import io.homo.superresolution.common.gui.ConfigScreenBuilder;
#if MC_VER > MC_1_20_6 && MC_VER < MC_1_21_10
import net.caffeinemc.mods.sodium.client.gui.options.OptionGroup;
import net.caffeinemc.mods.sodium.client.gui.options.OptionImpl;
import net.caffeinemc.mods.sodium.client.gui.options.OptionPage;
import net.caffeinemc.mods.sodium.client.gui.options.control.SliderControl;
import net.caffeinemc.mods.sodium.client.gui.options.storage.MinecraftOptionsStorage;
#elif MC_VER < MC_1_21_9
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

#if MC_VER < MC_1_21_9
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
        if (Platform.currentPlatform.isModLoaded("sodiumoptionsapi")) {
            return;
        }
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

#else
@Mixin(Minecraft.class)
public class SodiumOptionsGUIMixin {
}
#endif
