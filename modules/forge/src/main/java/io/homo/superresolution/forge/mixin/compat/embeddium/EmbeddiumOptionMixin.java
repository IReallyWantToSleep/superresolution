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

package io.homo.superresolution.forge.mixin.compat.embeddium;

import me.jellysquid.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.embeddedt.embeddium.client.gui.options.OptionIdentifier;
import org.embeddedt.embeddium.gui.EmbeddiumVideoOptionsScreen;
import org.embeddedt.embeddium.gui.frame.tab.Tab;
import net.minecraft.network.chat.Component;
import com.google.common.collect.Multimap;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.gui.ConfigScreenBuilder;

@Mixin(EmbeddiumVideoOptionsScreen.class)
public class EmbeddiumOptionMixin {
    @Inject(method = "createShaderPackButton", at = @At(value = "RETURN"), remap = false)
    private void addMyConfigScreen(Multimap<String, Tab<?>> tabs, CallbackInfo ci) {
        tabs.put(SuperResolution.MOD_ID,
                Tab.createBuilder()
                        .setTitle(Component.translatable("superresolution.name"))
                        .setId(OptionIdentifier.create(SuperResolution.MOD_ID, "emb_configscreen"))
                        .setOnSelectFunction(() -> {
                            Minecraft.getInstance().setScreen(ConfigScreenBuilder.create().buildConfigScreen((EmbeddiumVideoOptionsScreen) (Object) this));
                            return false;
                        }).build()
        );

    }
}
