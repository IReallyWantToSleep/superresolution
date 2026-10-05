/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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
