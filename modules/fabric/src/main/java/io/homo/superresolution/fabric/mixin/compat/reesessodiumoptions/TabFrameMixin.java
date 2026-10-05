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

package io.homo.superresolution.fabric.mixin.compat.reesessodiumoptions;

import io.homo.superresolution.api.platform.Platform;
import io.homo.superresolution.common.gui.ConfigScreenBuilder;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.Minecraft;
#if MC_VER < MC_1_21_10
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.Tab;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.TabFrame;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(value = TabFrame.class, remap = false)
public class TabFrameMixin {
    @Inject(method = "setTab", at = @At(value = "HEAD"), cancellable = true)
    private void onSetTab(Optional<Tab<?>> tab, CallbackInfo ci) {
        if (Platform.currentPlatform.getModVersionString("sodium").startsWith("0.8")) return;

        if (tab.orElseThrow().getTitle().getString().equals(Component.translatable("superresolution.screen.config.name").getString())) {
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