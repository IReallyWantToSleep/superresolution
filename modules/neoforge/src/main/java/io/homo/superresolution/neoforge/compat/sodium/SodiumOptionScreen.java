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

package io.homo.superresolution.neoforge.compat.sodium;

#if MC_VER != MC_1_20_4 && MC_VER < MC_1_21_6
import io.homo.superresolution.api.platform.Platform;
import toni.sodiumoptionsapi.api.OptionGUIConstruction;
import toni.sodiumoptionsapi.api.OptionIdentifier;
import toni.sodiumoptionsapi.util.IOptionGroupIdAccessor;
import com.google.common.collect.ImmutableList;
import io.homo.superresolution.common.SuperResolution;
import net.caffeinemc.mods.sodium.client.gui.options.OptionGroup;
import net.caffeinemc.mods.sodium.client.gui.options.OptionImpl;
import net.caffeinemc.mods.sodium.client.gui.options.OptionPage;
import net.caffeinemc.mods.sodium.client.gui.options.control.SliderControl;
import net.caffeinemc.mods.sodium.client.gui.options.storage.MinecraftOptionsStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
#endif

public class SodiumOptionScreen {
    public static void register() {
        #if MC_VER != MC_1_20_4 && MC_VER < MC_1_21_6
        if (Platform.currentPlatform.getModVersionString("sodium").startsWith("0.8")) return;

        OptionGUIConstruction.EVENT.register((pages) -> pages.add(
                        new OptionPage(Component.translatable("superresolution.screen.config.name"), ImmutableList.of()))
        );
        #endif
    }
}
