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

package io.homo.superresolution.common.mixin.core;

import io.homo.superresolution.common.mixin.core.accessor.OptionInstanceAccessor;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
#if !(MC_VER > MC_1_21_10)
import net.minecraft.client.GraphicsStatus;

#endif
@Mixin(Options.class)
public class OptionsMixin {
    #if !(MC_VER > MC_1_21_10)
    @Final
    @Shadow
    private OptionInstance<GraphicsStatus> graphicsMode;

    @Inject(method = "graphicsMode", at = @At("TAIL"))
    private void overwriteGraphicsMode(CallbackInfoReturnable<OptionInstance<GraphicsStatus>> cir) {
        if (((GraphicsStatus) (((OptionInstanceAccessor) (Object) cir.getReturnValue()).getValue())).getId() == 2) {
            graphicsMode.set(GraphicsStatus.FANCY);
        }
    }
    #endif
}
