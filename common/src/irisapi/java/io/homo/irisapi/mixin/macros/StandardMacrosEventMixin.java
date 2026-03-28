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

package io.homo.irisapi.mixin.macros;

import com.google.common.collect.ImmutableList;
import io.homo.irisapi.MacroRegistry;
import net.irisshaders.iris.gl.shader.StandardMacros;
import net.irisshaders.iris.helpers.StringPair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;

@Mixin(StandardMacros.class)
public class StandardMacrosEventMixin {
    @Inject(
            method = "createStandardEnvironmentDefines",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/irisshaders/iris/gl/shader/StandardMacros;getIrisDefines()Ljava/util/List;"
            ), remap = false,
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void triggerMacroRegistrationEvent(
            CallbackInfoReturnable<ImmutableList<StringPair>> cir,
            ArrayList<StringPair> standardDefines
    ) {
        standardDefines.addAll(MacroRegistry.collectMacros());
    }
}
