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

package io.homo.superresolution.iris_velocity_ext.v26_2.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.iris_velocity_ext.v26_2.IrisExtVertexFormats;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = RenderPipeline.class, priority = 1100)
public class RenderPipelineFormatMixin {
    @WrapMethod(method = "getVertexFormatBinding")
    private VertexFormat irisExt$change(int bindingIndex, Operation<VertexFormat> originalFunc) {
        return irisExt$replace(originalFunc.call(bindingIndex));
    }

    @WrapMethod(method = "getVertexFormatBindings")
    private VertexFormat[] irisExt$changeAll(Operation<VertexFormat[]> originalFunc) {
        VertexFormat[] original = originalFunc.call();
        if (!SuperResolutionConfig.isIrisExtensionEnabledAtStartup()) {
            return original;
        }
        VertexFormat[] replaced = original.clone();
        for (int i = 0; i < replaced.length; i++) {
            replaced[i] = irisExt$replace(replaced[i]);
        }
        return replaced;
    }

    private static VertexFormat irisExt$replace(VertexFormat format) {
        if (SuperResolutionConfig.isIrisExtensionEnabledAtStartup()
                && (format == DefaultVertexFormat.ENTITY || format == IrisVertexFormats.ENTITY)) {
            return IrisExtVertexFormats.ENTITY_VELOCITY;
        }
        return format;
    }
}
