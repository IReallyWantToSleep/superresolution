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
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ShaderChunkRenderer.class, remap = false)
public abstract class ShaderChunkRendererMixin {
    /*
    @Redirect(method = "createShader", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/gl/shader/ShaderLoader;loadShader(Lme/jellysquid/mods/sodium/client/gl/shader/ShaderType;Lnet/minecraft/resources/ResourceLocation;Lme/jellysquid/mods/sodium/client/gl/shader/ShaderConstants;)Lme/jellysquid/mods/sodium/client/gl/shader/GlShader;"))
    public GlShader overwriteShader(ShaderType type, ResourceLocation name, ShaderConstants constants) {
        if (type == ShaderType.FRAGMENT) {
            return ShaderLoader.loadShader(
                    ShaderType.FRAGMENT,
                    new ResourceLocation(SuperResolution.MOD_ID,
                            Platform.currentPlatform.getMinecraftVersion().equals("1.20.1") ?
                                    "block_layer_opaque_1201.fsh" : "block_layer_opaque.fsh"
                    ),
                    constants
            );
        } else if (type == ShaderType.VERTEX) {
            return ShaderLoader.loadShader(
                    ShaderType.VERTEX,
                    new ResourceLocation(SuperResolution.MOD_ID,
                            Platform.currentPlatform.getMinecraftVersion().equals("1.20.1") ?
                                    "block_layer_opaque_1201.vsh" : "block_layer_opaque.vsh"
                    ),
                    constants
            );
        }
        return null;
    }*/
}
