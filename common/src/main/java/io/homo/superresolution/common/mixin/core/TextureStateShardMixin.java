/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

package io.homo.superresolution.common.mixin.core;

import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.core.graphics.opengl.Gl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL33;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/*
@Mixin(targets = "net.minecraft.client.renderer.RenderStateShard$TextureStateShard")
public class TextureStateShardMixin {
    @Inject(method = "<init>", at = @At(value = "TAIL"))
    public void applyLodBias(ResourceLocation texture, boolean blur, boolean mipmap, CallbackInfo ci) {
        ((RenderStateShardAccessor) this).setSetupState(
                () -> {
                    TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
                    texturemanager.getTexture(texture).setFilter(blur, mipmap);
                    Gl.DSA.textureParameterf(
                            texturemanager.getTexture(texture).getId(),
                            GL33.GL_TEXTURE_LOD_BIAS,
                            (float) Math.log(
                                    (double) RenderHandlerManager.getRenderWidth() /
                                            RenderHandlerManager.getScreenWidth()

                            )
                    );
                    RenderSystem.setShaderTexture(0, texture);
                }
        );
    }
}
*/