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

package io.homo.superresolution.iris_velocity_ext.v26_1.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.iris_velocity_ext.v26_1.IrisExtVertexFormats;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = BufferBuilder.class, priority = 1100)
public abstract class BufferBuilderFormatMixin {
    @Shadow
    private boolean extending;

    @Shadow
    private boolean injectNormalAndUV1;
    @ModifyVariable(method = "<init>", at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/vertex/VertexFormatElement;POSITION:Lcom/mojang/blaze3d/vertex/VertexFormatElement;", ordinal = 0), argsOnly = true)
    private VertexFormat irisExt$extendFormat(VertexFormat format) {
        if (!SuperResolutionConfig.isIrisExtensionEnabledAtStartup()) {
            return format;
        }
        if (format == IrisVertexFormats.ENTITY || format == IrisExtVertexFormats.ENTITY_VELOCITY) {
            this.extending = true;
            this.injectNormalAndUV1 = false;
            return IrisExtVertexFormats.ENTITY_VELOCITY;
        }
        return format;
    }
}
